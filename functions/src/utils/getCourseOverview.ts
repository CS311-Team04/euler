/**
 * On-demand course overview fetcher for Moodle.
 * Fetches, parses, and returns the current week's course overview from Moodle,
 * using Firestore as a temporary cache to reduce latency.
 */

import * as logger from "firebase-functions/logger";
import { FieldValue, Firestore } from "firebase-admin/firestore";
import { MoodleClient } from "../connectors/moodle/MoodleClient";
import { MoodleConnectorRepository } from "../connectors/moodle/MoodleConnectorRepository";
import { MoodleConnectorConfig } from "../connectors/moodle/MoodleConnectorModel";
import { parseMoodleHtmlToMarkdown } from "./moodleParser";

/**
 * User context for Moodle authentication.
 */
export interface UserContext {
  /** Firebase Auth UID */
  uid: string;
  /** Decrypt function for Moodle token */
  decrypt: (cipher: string) => string;
}

/**
 * Result of getting course overview.
 */
export interface CourseOverviewResult {
  /** Parsed Markdown content of the current week */
  markdown: string;
  /** Whether the result came from cache */
  fromCache: boolean;
  /** Course ID used */
  courseId: number;
  /** Current week identifier */
  weekId: string;
  /** Section name/label resolved from Moodle (e.g., "Week 11") */
  sectionName: string;
  /** Last updated timestamp (cache write or fetch time) */
  lastUpdated: Date;
}

/**
 * Gets the current week identifier based on today's date.
 * Format: YYYY-WW (ISO week format)
 */
function getCurrentWeekId(): string {
  const now = new Date();
  const year = now.getFullYear();

  // Get ISO week number
  const startOfYear = new Date(year, 0, 1);
  const daysSinceStart =
    Math.floor((now.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24));
  const weekNumber = Math.ceil((daysSinceStart + startOfYear.getDay() + 1) / 7);

  // ISO format: YYYY-WW (with leading zero)
  return `${year}-W${weekNumber.toString().padStart(2, "0")}`;
}

/**
 * Parses dates like "December 1, 2025" or "Dec 1, 2025" from text.
 */
function parseFriendlyDate(text: string): Date | null {
  const monthPattern =
    /\b(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sept|Sep|Oct|Nov|Dec)\s+(\d{1,2}),\s*(\d{4})\b/i;
  const match = text.match(monthPattern);
  if (!match) return null;
  const month = match[1];
  const day = parseInt(match[2], 10);
  const year = parseInt(match[3], 10);
  const parsed = new Date(`${month} ${day}, ${year}`);
  return isNaN(parsed.getTime()) ? null : parsed;
}

/**
 * Checks whether two dates are in the same calendar week (ISO style).
 */
function isSameWeek(a: Date, b: Date): boolean {
  return getWeekFromDate(a) === getWeekFromDate(b);
}

/**
 * Identifies the current week's section from Moodle course contents.
 * Moodle sections typically have names like "Week 1", "Semaine 1", or dates.
 * We match sections based on the current calendar week.
 */
function findCurrentWeekSection(sections: any[]): any | null {
  const today = new Date();
  let bestPastSection: { section: any; date: Date } | null = null;

  for (const section of sections) {
    const sectionName = section.name || "";
    const sectionSummary = section.summary || "";

    // Prefer explicit dates (e.g., "December 1, 2025")
    const dateFromText =
      parseFriendlyDate(sectionName) ||
      parseFriendlyDate(sectionSummary);

    if (dateFromText) {
      // If this section's date is this week, return immediately
      if (isSameWeek(dateFromText, today)) {
        return section;
      }
      // Track the most recent past section
      if (dateFromText.getTime() <= today.getTime()) {
        if (!bestPastSection || dateFromText.getTime() > bestPastSection.date.getTime()) {
          bestPastSection = { section, date: dateFromText };
        }
      }
      continue;
    }

    // If no date, try week labels like "Week 5" / "Semaine 5"
    const weekPattern = /\b(?:week|semaine|w)\s*(\d+)\b/i;
    const weekMatch =
      sectionName.toLowerCase().match(weekPattern) ||
      sectionSummary.toLowerCase().match(weekPattern);

    if (weekMatch) {
      // Without a course-specific calendar, fall back to recency: keep latest non-zero section as backup
      const sectionIndex = typeof section.section === "number" ? section.section : null;
      if (sectionIndex !== null && sectionIndex >= 1) {
        if (!bestPastSection) {
          bestPastSection = { section, date: new Date(0) }; // marker; will be used as fallback
        } else if (bestPastSection.date.getTime() === 0 && sectionIndex > (bestPastSection.section?.section ?? 0)) {
          bestPastSection = { section, date: new Date(0) };
        }
      }
    }
  }

  // Fallback: latest section whose start date is in the past
  if (bestPastSection) {
    return bestPastSection.section;
  }

  // Final fallback: last non-zero section, or first section if none
  const nonZeroSections = sections.filter((s) => s.section !== 0 && s.section !== undefined);
  if (nonZeroSections.length > 0) {
    return nonZeroSections[nonZeroSections.length - 1];
  }
  return sections.length > 0 ? sections[sections.length - 1] : null;
}

/**
 * Finds a section by an explicit week number using multiple heuristics:
 * - Name/summary containing "Week X", "Semaine X", "Topic X", "Section X"
 * - Fallback: section index matching the week number (or weekNumber + 1 if Section 0 is general)
 */
function findSectionByWeekNumber(sections: any[], weekNumber: number): any | null {
  if (!Array.isArray(sections)) return null;
  const targets = [
    /\b(?:week|semaine|topic|section)\s+(\d{1,2})\b/i,
  ];

  // First pass: match explicit labels in name or summary
  for (const section of sections) {
    const name = section.name || "";
    const summary = section.summary || "";
    for (const pattern of targets) {
      const nameMatch = name.match(pattern);
      if (nameMatch && parseInt(nameMatch[1], 10) === weekNumber) {
        return section;
      }
      const summaryMatch = summary.match(pattern);
      if (summaryMatch && parseInt(summaryMatch[1], 10) === weekNumber) {
        return section;
      }
    }
  }

  // Second pass: match by section index (Moodle often uses section number as week/topic index)
  for (const section of sections) {
    const idx = typeof section.section === "number" ? section.section : null;
    if (idx === null) continue;
    if (idx === weekNumber || idx === weekNumber + 1) {
      return section;
    }
  }

  return null;
}

/**
 * Gets week ID from a date (YYYY-WW format).
 */
function getWeekFromDate(date: Date): string {
  const year = date.getFullYear();
  const startOfYear = new Date(year, 0, 1);
  const daysSinceStart = Math.floor((date.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24));
  const weekNumber = Math.ceil((daysSinceStart + startOfYear.getDay() + 1) / 7);
  return `${year}-W${weekNumber.toString().padStart(2, "0")}`;
}


/**
 * Extracts HTML content from Moodle section JSON structure.
 * The Moodle REST API returns structured data; we extract descriptions
 * and content from modules which may contain HTML.
 */
function extractSectionHtml(section: any): string {
  const parts: string[] = [];

  // Add section name as header
  if (section.name) {
    parts.push(`<h2>${section.name}</h2>`);
  }

  // Add section summary/description
  if (section.summary) {
    parts.push(section.summary);
  }

  // Add module descriptions and content
  if (section.modules && Array.isArray(section.modules)) {
    for (const module of section.modules) {
      // Skip certain module types that are not useful for overview
      const skipTypes = ["forum", "chat"];
      const modName = (module.modname || "").toLowerCase();
      if (skipTypes.includes(modName)) {
        continue;
      }

      // Add module name as header
      if (module.name) {
        parts.push(`<h3>${module.name}</h3>`);
      }

      // Add module description (often contains HTML)
      if (module.description) {
        parts.push(module.description);
      }

      // Add module intro/content if available
      if (module.intro) {
        parts.push(module.intro);
      }

      // Add module contents (files, resources, etc.)
      if (module.contents && Array.isArray(module.contents)) {
        const contentItems: string[] = [];
        for (const content of module.contents) {
          if (content.type === "file" && content.filename) {
            contentItems.push(`<li><a href="${content.fileurl || ""}">${content.filename}</a></li>`);
          } else if (content.type === "url" && content.fileurl) {
            contentItems.push(`<li><a href="${content.fileurl}">${content.fileurl}</a></li>`);
          }
        }
        if (contentItems.length > 0) {
          parts.push(`<ul>${contentItems.join("")}</ul>`);
        }
      }
    }
  }

  return parts.join("\n\n");
}

/**
 * Main handler: Gets course overview for the current week.
 * 
 * Flow:
 * 1. Check Firestore cache at `courses/{courseId}/weeks/{currentWeekId}`
 * 2. If cache miss or stale (>4 hours), scrape Moodle
 * 3. Parse HTML to Markdown
 * 4. Update cache
 * 5. Return markdown
 * 
 * @param courseName - Name of the course (used to find course ID)
 * @param userContext - User authentication context
 * @param db - Firestore instance
 * @param moodleRepo - Moodle connector repository
 * @returns Course overview markdown and metadata
 */
export async function getCourseOverview(
  courseName: string,
  userContext: UserContext,
  db: Firestore,
  moodleRepo: MoodleConnectorRepository,
  targetWeekNumber?: number
): Promise<CourseOverviewResult> {
  const { uid, decrypt } = userContext;
  const currentWeekId = getCurrentWeekId();
  const cacheWeekId = typeof targetWeekNumber === "number" ? `specific-week-${targetWeekNumber}` : currentWeekId;//This is a deterministic lookup key. It may differ from the human-readable 'weekId' stored inside the document.

  logger.info("getCourseOverview.start", {
    courseName,
    uid,
    currentWeekId,
    targetWeekNumber: targetWeekNumber ?? null,
    cacheWeekId,
  });

  // 1. Get Moodle credentials
  const moodleConfig: MoodleConnectorConfig | null = await moodleRepo.getConfig(uid);
  if (!moodleConfig || !moodleConfig.baseUrl || !moodleConfig.tokenEncrypted) {
    throw new Error("Moodle not connected. Please connect your Moodle account first.");
  }

  if (moodleConfig.status !== "connected") {
    throw new Error(`Moodle connection status: ${moodleConfig.status}`);
  }

  const token = decrypt(moodleConfig.tokenEncrypted);
  const moodleClient = new MoodleClient(moodleConfig.baseUrl, token);

  // 2. Find course by name
  const course = await moodleClient.findCourseByName(courseName);
  if (!course || !course.id) {
    throw new Error(`Course "${courseName}" not found. Please check the course name.`);
  }

  const courseId = course.id;

  logger.info("getCourseOverview.courseFound", {
    courseId,
    courseName: course.fullname || course.shortname,
  });

  // 3. Check cache
  const cacheRef =
    db.collection("courses").doc(String(courseId)).collection("weeks").doc(cacheWeekId);
  const cacheDoc = await cacheRef.get();

  if (cacheDoc.exists) {
    const cacheData = cacheDoc.data();
    if (cacheData?.markdown && cacheData?.last_updated) {
      const lastUpdated = cacheData.last_updated.toDate();
      const fourHoursAgo = new Date(Date.now() - 4 * 60 * 60 * 1000);

      // If cache is less than 4 hours old, return it
      if (lastUpdated > fourHoursAgo) {
        logger.info("getCourseOverview.cacheHit", {
          courseId,
          weekId: cacheWeekId,
          lastUpdated: lastUpdated.toISOString(),
        });

        return {
          markdown: cacheData.markdown,
          fromCache: true,
          courseId,
          weekId: cacheData.weekId ?? cacheWeekId,
          sectionName: cacheData.weekId ?? cacheWeekId,
          lastUpdated,
        };
      }
    }
  }

  logger.info("getCourseOverview.cacheMiss", {
    courseId,
    weekId: cacheWeekId,
  });

  // 4. Scrape Moodle - get course contents
  const contents = await moodleClient.getCourseContents(courseId);

  // 5. Find target/current week's section
  let currentSection: any | null = null;
  if (typeof targetWeekNumber === "number") {
    currentSection = findSectionByWeekNumber(contents, targetWeekNumber);
    if (!currentSection) {
      logger.warn("getCourseOverview.targetWeekNotFound", {
        courseId,
        targetWeekNumber,
      });
    }
  }

  if (!currentSection) {
    currentSection = findCurrentWeekSection(contents);
  }
  if (!currentSection && contents.length > 0) {
    // Fallback: use the last available section if nothing matched
    currentSection = contents[contents.length - 1];
    logger.warn("getCourseOverview.sectionFallback", {
      reason: "No matching current week; using last available section",
      sectionId: currentSection?.id || currentSection?.section,
      sectionName: currentSection?.name,
    });
  }
  if (!currentSection) {
    throw new Error(`No sections available for course "${courseName}".`);
  }

  const resolvedWeekLabel =
    currentSection.name ||
    (currentSection.section !== undefined ? `Section ${currentSection.section}` : undefined) ||
    (typeof targetWeekNumber === "number" ? `Week ${targetWeekNumber}` : undefined) ||
    currentWeekId;

  logger.info("getCourseOverview.sectionFound", {
    sectionId: currentSection.id || currentSection.section,
    sectionName: currentSection.name,
    resolvedWeekLabel,
  });

  // 6. Extract HTML from section JSON structure
  // The Moodle REST API returns structured JSON; we extract HTML from descriptions and content
  const htmlContent = extractSectionHtml(currentSection);

  // 7. Parse HTML to Markdown
  const markdown = parseMoodleHtmlToMarkdown(htmlContent);

  if (!markdown || markdown.trim().length === 0) {
    throw new Error(
      typeof targetWeekNumber === "number"
        ? `No content found for week ${targetWeekNumber} in course "${courseName}".`
        : `No content found for current week in course "${courseName}".`
    );
  }

  const writeTime = new Date();

  // 8. Update cache
  await cacheRef.set({
    markdown,
    last_updated: FieldValue.serverTimestamp(),
    courseId,
    courseName: course.fullname || course.shortname,
    weekId: resolvedWeekLabel,
  });

  logger.info("getCourseOverview.complete", {
    courseId,
    weekId: resolvedWeekLabel,
    markdownLength: markdown.length,
  });

  return {
    markdown,
    fromCache: false,
    courseId,
    weekId: resolvedWeekLabel,
    sectionName: resolvedWeekLabel,
    lastUpdated: writeTime,
  };
}

