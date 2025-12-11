/**
 * Deterministic HTML to Markdown parser for Moodle course content.
 * Converts Moodle HTML sections into clean, readable Markdown.
 * 
 * Rules:
 * - Convert <a> tags to markdown links
 * - Convert <li> to dashes (-)
 * - Remove empty <div> elements
 * - Remove generic Moodle labels ("Forum", "Chat")
 * - Preserve headers (h1-h6)
 * - Preserve structure (nested lists, paragraphs)
 */

/**
 * Extracts text content from an HTML string, converting to Markdown format.
 * This is a simple, deterministic parser that handles common Moodle HTML patterns.
 * 
 * @param html - Raw HTML string from Moodle course section
 * @returns Clean Markdown string
 */
export function parseMoodleHtmlToMarkdown(html: string): string {
  if (!html || html.trim().length === 0) {
    return "";
  }

  // Remove script and style tags and their content
  let cleaned = html.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, "");
  cleaned = cleaned.replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, "");

  // Remove common Moodle boilerplate elements
  cleaned = cleaned.replace(/<nav[^>]*>.*?<\/nav>/gi, "");
  cleaned = cleaned.replace(/<header[^>]*>.*?<\/header>/gi, "");
  cleaned = cleaned.replace(/<footer[^>]*>.*?<\/footer>/gi, "");
  cleaned = cleaned.replace(/<aside[^>]*>.*?<\/aside>/gi, "");

  // Remove empty divs (but keep their content if any)
  cleaned = cleaned.replace(/<div[^>]*>\s*<\/div>/gi, "");

  // Convert headers (preserve hierarchy)
  cleaned = cleaned.replace(/<h1[^>]*>(.*?)<\/h1>/gi, (_, text) => `# ${extractText(text)}\n\n`);
  cleaned = cleaned.replace(/<h2[^>]*>(.*?)<\/h2>/gi, (_, text) => `## ${extractText(text)}\n\n`);
  cleaned = cleaned.replace(/<h3[^>]*>(.*?)<\/h3>/gi, (_, text) => `### ${extractText(text)}\n\n`);
  cleaned = cleaned.replace(/<h4[^>]*>(.*?)<\/h4>/gi, (_, text) => `#### ${extractText(text)}\n\n`);
  cleaned = cleaned.replace(/<h5[^>]*>(.*?)<\/h5>/gi, (_, text) => `##### ${extractText(text)}\n\n`);
  cleaned = cleaned.replace(/<h6[^>]*>(.*?)<\/h6>/gi, (_, text) => `###### ${extractText(text)}\n\n`);

  // Convert anchor tags to markdown links [text](url)
  cleaned = cleaned.replace(
    /<a[^>]*href=["']([^"']+)["'][^>]*>(.*?)<\/a>/gi,
    (_, url, text) => {
      const linkText = extractText(text).trim();
      const linkUrl = url.trim();
      return linkText ? `[${linkText}](${linkUrl})` : linkUrl;
    }
  );

  // Convert list items to markdown bullets
  // Handle both ordered and unordered lists
  cleaned = cleaned.replace(/<ul[^>]*>/gi, "");
  cleaned = cleaned.replace(/<\/ul>/gi, "\n");
  cleaned = cleaned.replace(/<ol[^>]*>/gi, "");
  cleaned = cleaned.replace(/<\/ol>/gi, "\n");
  
  // Convert <li> to dashes with proper indentation
  cleaned = cleaned.replace(/<li[^>]*>(.*?)<\/li>/gi, (_, text) => {
    const itemText = extractText(text).trim();
    // Handle nested lists by preserving indentation
    const indentMatch = text.match(/^(\s*)/);
    const indent = indentMatch ? indentMatch[1] : "";
    const indentLevel = (indent.match(/\s{2}/g) || []).length;
    const prefix = "  ".repeat(indentLevel) + "- ";
    return prefix + itemText + "\n";
  });

  // Convert paragraphs (preserve spacing)
  cleaned = cleaned.replace(/<p[^>]*>(.*?)<\/p>/gi, (_, text) => {
    const paraText = extractText(text).trim();
    return paraText ? paraText + "\n\n" : "";
  });

  // Convert <br> tags to newlines
  cleaned = cleaned.replace(/<br\s*\/?>/gi, "\n");

  // Convert <strong> and <b> to bold
  cleaned = cleaned.replace(/<(strong|b)[^>]*>(.*?)<\/\1>/gi, "**$2**");

  // Convert <em> and <i> to italic
  cleaned = cleaned.replace(/<(em|i)[^>]*>(.*?)<\/\1>/gi, "*$2*");

  // Remove remaining HTML tags, but keep spacing to avoid merging tokens (e.g., math spans)
  cleaned = cleaned.replace(/<[^>]+>/g, " ");

  // Decode HTML entities
  cleaned = decodeHtmlEntities(cleaned);

  // Remove generic Moodle labels (case-insensitive)
  const moodleLabels = [
    /^Forum\s*:?\s*$/i,
    /^Chat\s*:?\s*$/i,
    /^Assignment\s*:?\s*$/i,
    /^File\s*:?\s*$/i,
    /^URL\s*:?\s*$/i,
    /^Page\s*:?\s*$/i,
  ];
  
  cleaned = cleaned.split("\n").filter((line) => {
    const trimmed = line.trim();
    return !moodleLabels.some((pattern) => pattern.test(trimmed));
  }).join("\n");

  // Post-process lines for executive summary rules
  const lines = cleaned.split("\n");
  const filtered: string[] = [];
  for (let line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;

    // Drop standalone link lines
    const standaloneLink = /^-?\s*\[[^\]]+\]\([^)]+\)\s*$/i;

    // Resource keywords to filter out (files, folders, assignments, quizzes)
    const resourceKeyword = /(file|folder|assignment|quiz|resource)/i;

    if (standaloneLink.test(trimmed)) {
      continue;
    }

    // Simplify bullet items
    if (trimmed.startsWith("-")) {
      let content = trimmed.replace(/^-+\s*/, "").trim();

      // If bold present, keep only bold text
      const boldMatch = content.match(/\*\*(.+?)\*\*/);
      if (boldMatch) {
        content = boldMatch[1].trim();
      }

      // If colon present, keep only before colon
      const colonIdx = content.indexOf(":");
      if (colonIdx !== -1) {
        content = content.slice(0, colonIdx).trim();
      }

      // Skip if content signals resource-only bullets
      if (resourceKeyword.test(content)) {
        continue;
      }

      if (content.length === 0) continue;
      line = `- ${content}`;
    } else {
      // Non-bullet lines: drop if they are resource-like (even without link)
      const fileLikeExt = /\.(pdf|docx?|pptx?|zip|rar|7z)$/i;
      if (resourceKeyword.test(trimmed) || fileLikeExt.test(trimmed)) {
        continue;
      }
      // Also drop pure standalone links
      if (standaloneLink.test(trimmed)) {
        continue;
      }
    }

    filtered.push(line);
  }

  cleaned = filtered.join("\n");

  // Clean up excessive whitespace (collapse to max 2 newlines)
  cleaned = cleaned.replace(/\n{3,}/g, "\n\n");
  cleaned = cleaned.replace(/[ \t]+/g, " ");
  cleaned = cleaned.replace(/\n[ \t]+/g, "\n");
  cleaned = cleaned.replace(/[ \t]+\n/g, "\n");

  // === Final executive-summary pass ===
  const resourceHeader = /^\s*#{1,3}\s+.*\b(lecture|homework|solution|file|assignment|quiz)\b/i;
  const finalLines: string[] = [];

  for (const rawLine of cleaned.split("\n")) {
    const trimmed = rawLine.trim();
    if (!trimmed) continue;

    // Remove resource headers (Lecture, Homework, etc.)
    if (resourceHeader.test(trimmed)) {
      continue;
    }

    const isHeader = /^#{1,6}\s+/.test(trimmed);
    const isBullet = /^-\s+/.test(trimmed);
    const headerPrefix = isHeader ? (trimmed.match(/^(#+\s*)/)?.[1] || "") : "";

    // Work on content without bullet/header markers
    let content = trimmed.replace(/^(#+\s*|-+\s*)/, "").trim();

    // Rule: cut after colon ONLY for bullet lines, except Dates/📅
    if (isBullet) {
      const startsWithDates = /^dates\b/i.test(content) || content.startsWith("📅");
      if (!startsWithDates) {
        const colonIdx = content.indexOf(":");
        if (colonIdx !== -1) {
          content = content.slice(0, colonIdx).trim();
        }
      }
    }

    // If bold exists, prefer the bold text only
    const boldMatch = content.match(/\*\*(.+?)\*\*/);
    if (boldMatch) {
      content = boldMatch[1].trim();
    }

    if (!content) continue;

    if (isHeader) {
      finalLines.push(`${headerPrefix}${content}`);
    } else {
      // Ensure key topics are bullets
      finalLines.push(`- ${content}`);
    }
  }

  const finalOutput = finalLines.join("\n").trim();

  // Debug log for the final cleaned output
  console.log("---------------- GENERATED MOODLE SUMMARY ----------------");
  console.log(finalOutput);
  console.log("----------------------------------------------------------");

  return finalOutput;
}

/**
 * Extracts plain text from HTML, removing tags but preserving structure hints.
 */
function extractText(html: string): string {
  let text = html;
  // Remove nested tags but keep their text content, preserving spacing
  text = text.replace(/<[^>]+>/g, " ");
  text = decodeHtmlEntities(text);
  text = text.replace(/\s+/g, " ");
  return text.trim();
}

/**
 * Decodes common HTML entities to their actual characters.
 */
function decodeHtmlEntities(text: string): string {
  const entities: Record<string, string> = {
    "&amp;": "&",
    "&lt;": "<",
    "&gt;": ">",
    "&quot;": '"',
    "&#39;": "'",
    "&apos;": "'",
    "&nbsp;": " ",
    "&copy;": "©",
    "&reg;": "®",
    "&trade;": "™",
    "&hellip;": "...",
    "&mdash;": "—",
    "&ndash;": "–",
    "&lsquo;": "'",
    "&rsquo;": "'",
    "&ldquo;": '"',
    "&rdquo;": '"',
  };

  let decoded = text;
  for (const [entity, char] of Object.entries(entities)) {
    decoded = decoded.replace(new RegExp(entity, "gi"), char);
  }

  // Handle numeric entities like &#160; or &#x20;
  decoded = decoded.replace(/&#(\d+);/g, (_, num) => String.fromCharCode(parseInt(num, 10)));
  decoded = decoded.replace(/&#x([0-9a-f]+);/gi, (_, hex) => String.fromCharCode(parseInt(hex, 16)));

  return decoded;
}

