// functions/src/features/moodle.ts

import * as logger from "firebase-functions/logger";
import { db } from "../core/firebase";
import { MoodleConnectorRepository } from "../connectors/moodle/MoodleConnectorRepository";
import { MoodleConnectorService } from "../connectors/moodle/MoodleConnectorService";
import { MoodleClient } from "../connectors/moodle/MoodleClient";
import { decryptSecret } from "../security/secretCrypto";
import { detectCourseIntent } from "../utils/intentParser";
import { resolveGetWeeklyOverview } from "../tools/courseTools";
import { detectMoodleFileFetchIntentCore, extractFileInfo } from "../moodleIntent";

/**
 * Handle Moodle intent (course overview or file fetch)
 * Returns response object if Moodle intent detected, null otherwise
 */
export async function handleMoodleIntent(
  question: string,
  uid: string | undefined,
  deps: {
    moodleRepo: MoodleConnectorRepository;
    moodleService: MoodleConnectorService;
  }
): Promise<{
  reply: string;
  primary_url: null;
  best_score: number;
  sources: any[];
  ed_intent_detected: false;
  ed_intent: null;
  moodle_intent_detected: boolean;
  moodle_intent: any;
  moodle_file: any;
} | null> {
  // === Deterministic Moodle course intent (regex) ===
  const courseIntent = detectCourseIntent(question);
  if (courseIntent) {
    logger.info("handleMoodleIntent.courseIntentDetected", {
      type: courseIntent.type,
      course: courseIntent.course,
      week: courseIntent.week ?? null,
      uid,
    });

    if (!uid) {
      return {
        reply: "Vous devez être connecté pour accéder à Moodle. Merci de vous authentifier.",
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: false,
        ed_intent: null,
        moodle_intent_detected: true,
        moodle_intent: { type: "course_overview" },
        moodle_file: null,
      };
    }

    try {
      const overview = await resolveGetWeeklyOverview(
        {
          courseName: courseIntent.course,
          weekNumber: courseIntent.type === "specific_week" ? courseIntent.week : undefined,
        },
        {
          uid,
          decrypt: decryptSecret,
          db,
          moodleRepo: deps.moodleRepo,
        }
      );

      return {
        reply: overview,
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: false,
        ed_intent: null,
        moodle_intent_detected: true,
        moodle_intent: { type: "course_overview" },
        moodle_file: null,
      };
    } catch (e: any) {
      logger.error("handleMoodleIntent.courseIntentFailed", {
        error: String(e),
        message: e?.message,
        stack: e?.stack,
        course: courseIntent.course,
        week: courseIntent.week ?? null,
        uid,
      });
      return {
        reply: `Impossible de récupérer le récapitulatif Moodle pour "${courseIntent.course}": ${e?.message || "erreur inconnue"}`,
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: false,
        ed_intent: null,
        moodle_intent_detected: true,
        moodle_intent: { type: "course_overview" },
        moodle_file: null,
      };
    }
  }
  // === End deterministic Moodle course intent ===

  // === Moodle Intent Detection (fast, regex-based) ===
  // First layer: General detection - determines if this is a Moodle fetch request
  const moodleIntentResult = detectMoodleFileFetchIntentCore(question);
  // Detect if user is writing in English for response language (moved outside try block for catch access)
  const isEnglishMoodle = /\b(get|fetch|download|show|display|retrieve|homework|lecture|solution|from|the|my|please|can\s+you|i\s+want|i\s+need)\b/i.test(question);
  
  if (moodleIntentResult.moodle_intent_detected && moodleIntentResult.moodle_intent && uid) {
    logger.info("handleMoodleIntent.moodleIntentDetected", {
      intent: moodleIntentResult.moodle_intent,
      questionLen: question.length,
      uid,
    });

    try {
      // Get Moodle config
      const moodleConfig = await deps.moodleService.getStatus(uid);
      if (moodleConfig.status !== "connected" || !moodleConfig.baseUrl || !moodleConfig.tokenEncrypted) {
      return {
        reply: isEnglishMoodle
          ? "You're not connected to Moodle. Please connect in the settings."
          : "Vous n'êtes pas connecté à Moodle. Veuillez vous connecter dans les paramètres.",
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: false,
        ed_intent: null,
        moodle_intent_detected: true,
        moodle_intent: moodleIntentResult.moodle_intent,
        moodle_file: null,
      };
      }

      // Second layer: Extract specific file type, number, course name, and week
      const fileInfo = extractFileInfo(question);
      if (!fileInfo) {
        return {
          reply: isEnglishMoodle
            ? "I couldn't identify the requested file type. Please specify (e.g., \"Lecture 5\", \"Homework 3\", \"Homework solution 2\")."
            : "Je n'ai pas pu identifier le type de fichier demandé. Veuillez préciser (ex: \"Lecture 5\", \"Homework 3\", \"Solution devoir 2\").",
          primary_url: null,
          best_score: 0,
          sources: [],
          ed_intent_detected: false,
          ed_intent: null,
          moodle_intent_detected: true,
          moodle_intent: moodleIntentResult.moodle_intent,
          moodle_file: null,
        };
      }

      // Decrypt the token and trim any whitespace
      let token: string;
      try {
        token = decryptSecret(moodleConfig.tokenEncrypted).trim();
        if (!token) {
          throw new Error("Decrypted token is empty");
        }
        logger.info("handleMoodleIntent.tokenDecrypted", {
          uid,
          tokenLength: token.length,
        });
      } catch (decryptError: any) {
        logger.error("handleMoodleIntent.tokenDecryptionFailed", {
          uid,
          error: String(decryptError),
          errorMessage: decryptError.message,
          hasTokenEncrypted: !!moodleConfig.tokenEncrypted,
          tokenEncryptedLength: moodleConfig.tokenEncrypted?.length || 0,
        });
        return {
          reply: isEnglishMoodle
            ? "Error decrypting Moodle token. Please reconnect to Moodle in the settings."
            : "Erreur lors du décryptage du token Moodle. Veuillez vous reconnecter à Moodle dans les paramètres.",
          primary_url: null,
          best_score: 0,
          sources: [],
          ed_intent_detected: false,
          ed_intent: null,
          moodle_intent_detected: true,
          moodle_intent: moodleIntentResult.moodle_intent,
          moodle_file: null,
        };
      }

      const client = new MoodleClient(moodleConfig.baseUrl, token);

      logger.info("handleMoodleIntent.moodleFileFetch", {
        uid,
        baseUrl: moodleConfig.baseUrl,
        fileType: fileInfo.fileType,
        fileNumber: fileInfo.fileNumber,
        courseName: fileInfo.courseName,
        week: fileInfo.week,
      });

      let foundFile: { fileurl: string; filename: string; mimetype: string } | null = null;
      let foundInCourse: any = null; // Track which course the file was found in

      // If course name is specified, search only in that course
      if (fileInfo.courseName) {
        const targetCourse = await client.findCourseByName(fileInfo.courseName);
        if (!targetCourse) {
          return {
            reply: isEnglishMoodle
              ? `I couldn't find the course "${fileInfo.courseName}" in your Moodle courses.`
              : `Je n'ai pas trouvé le cours "${fileInfo.courseName}" dans vos cours Moodle.`,
            primary_url: null,
            best_score: 0,
            sources: [],
            ed_intent_detected: false,
            ed_intent: null,
            moodle_intent_detected: true,
            moodle_intent: moodleIntentResult.moodle_intent,
            moodle_file: null,
          };
        }

        // Validate course ID - Moodle returns 'id' field for courses
        const courseId = targetCourse.id;
        if (!courseId) {
          logger.error("handleMoodleIntent.missingCourseId", {
            courseName: fileInfo.courseName,
            course: targetCourse,
            availableFields: Object.keys(targetCourse),
          });
          return {
            reply: isEnglishMoodle
              ? `Error: Could not find the course ID for "${fileInfo.courseName}".`
              : `Erreur: Impossible de trouver l'ID du cours "${fileInfo.courseName}".`,
            primary_url: null,
            best_score: 0,
            sources: [],
            ed_intent_detected: false,
            ed_intent: null,
            moodle_intent_detected: true,
            moodle_intent: moodleIntentResult.moodle_intent,
            moodle_file: null,
          };
        }

        const courseIdNum = typeof courseId === "number" ? courseId : parseInt(String(courseId), 10);
        if (isNaN(courseIdNum) || courseIdNum <= 0) {
          logger.error("handleMoodleIntent.invalidCourseId", {
            courseName: fileInfo.courseName,
            course: targetCourse,
            courseId,
            courseIdNum,
          });
          return {
            reply: isEnglishMoodle
              ? `Error: Invalid course ID for "${fileInfo.courseName}".`
              : `Erreur: ID de cours invalide pour "${fileInfo.courseName}".`,
            primary_url: null,
            best_score: 0,
            sources: [],
            ed_intent_detected: false,
            ed_intent: null,
            moodle_intent_detected: true,
            moodle_intent: moodleIntentResult.moodle_intent,
            moodle_file: null,
          };
        }

        logger.info("handleMoodleIntent.searchingCourse", {
          courseName: fileInfo.courseName,
          courseId: courseIdNum,
          courseFullname: targetCourse.fullname,
          courseShortname: targetCourse.shortname,
          fileType: fileInfo.fileType,
          fileNumber: fileInfo.fileNumber,
          week: fileInfo.week,
        });

        // Search in the specific course
        foundFile = await client.findFile(
          courseIdNum,
          fileInfo.fileType,
          fileInfo.fileNumber,
          fileInfo.week
        );
        if (foundFile) {
          foundInCourse = targetCourse;
        }
      } else {
        // No course specified - search in all courses
        const courses = await client.getEnrolledCourses();
        if (courses.length === 0) {
          return {
            reply: isEnglishMoodle
              ? "No courses found in your Moodle."
              : "Aucun cours trouvé dans votre Moodle.",
            primary_url: null,
            best_score: 0,
            sources: [],
            ed_intent_detected: false,
            ed_intent: null,
            moodle_intent_detected: true,
            moodle_intent: moodleIntentResult.moodle_intent,
            moodle_file: null,
          };
        }

        // Search for file in all courses in parallel for better performance
        // We use Promise.allSettled to search all courses simultaneously, then
        // check results in order and take the first match (maintains deterministic behavior)
        let foundInCourse: any = null;
        
        const searchPromises = courses.map(async (course) => {
          const courseId = course.id; // Moodle returns 'id' field
          if (!courseId) {
            logger.warn("handleMoodleIntent.courseWithoutId", {
              course,
              availableFields: Object.keys(course),
            });
            return { course, file: null };
          }

          const courseIdNum = typeof courseId === "number" ? courseId : parseInt(String(courseId), 10);
          if (isNaN(courseIdNum) || courseIdNum <= 0) {
            logger.warn("handleMoodleIntent.invalidCourseIdInList", {
              course,
              courseId,
              courseIdNum,
            });
            return { course, file: null };
          }

          logger.info("handleMoodleIntent.searchingInCourse", {
            courseId: courseIdNum,
            courseFullname: course.fullname,
            fileType: fileInfo.fileType,
            fileNumber: fileInfo.fileNumber,
            week: fileInfo.week,
          });

          const file = await client.findFile(
            courseIdNum,
            fileInfo.fileType,
            fileInfo.fileNumber,
            fileInfo.week
          );
          return { course, file };
        });

        // Wait for all searches to complete (in parallel), then find first match
        const results = await Promise.allSettled(searchPromises);
        for (const result of results) {
          if (result.status === "fulfilled" && result.value.file) {
            foundFile = result.value.file;
            foundInCourse = result.value.course;
            break; // Take first match (maintains original behavior)
          }
        }

        // Store the course for later use in response
        if (foundInCourse) {
          fileInfo.courseName = foundInCourse.fullname || foundInCourse.shortname || foundInCourse.displayname;
        }
      }

      if (!foundFile) {
        const fileTypeNamesFr: Record<string, string> = {
          "lecture": "cours",
          "homework": "devoir",
          "homework_solution": "solution de devoir",
        };
        const fileTypeNamesEn: Record<string, string> = {
          "lecture": "lecture",
          "homework": "homework",
          "homework_solution": "homework solution",
        };
        return {
          reply: isEnglishMoodle
            ? `I couldn't find ${fileTypeNamesEn[fileInfo.fileType]} ${fileInfo.fileNumber} in your Moodle courses.`
            : `Je n'ai pas trouvé ${fileTypeNamesFr[fileInfo.fileType]} ${fileInfo.fileNumber} dans vos cours Moodle.`,
          primary_url: null,
          best_score: 0,
          sources: [],
          ed_intent_detected: false,
          ed_intent: null,
          moodle_intent_detected: true,
          moodle_intent: moodleIntentResult.moodle_intent,
          moodle_file: null,
        };
      }

      // Get direct download URL (follows redirects if needed)
      const downloadUrl = await client.getDirectFileUrl(foundFile.fileurl);

      // Get course name for the response
      let courseDisplayName = "Moodle";
      if (foundInCourse) {
        courseDisplayName = foundInCourse.fullname || foundInCourse.shortname || foundInCourse.displayname || "Moodle";
      } else if (fileInfo.courseName) {
        // If course name was specified but we don't have the full course object, use the name
        courseDisplayName = fileInfo.courseName;
      }

      // Reply text with markdown formatting
      const fileTypeLabels = isEnglishMoodle
        ? { lecture: "Lecture", homework: "Homework", homework_solution: "Homework Solution" }
        : { lecture: "Cours", homework: "Devoir", homework_solution: "Solution" };
      const fileTypeLabel = fileTypeLabels[fileInfo.fileType as keyof typeof fileTypeLabels] || fileInfo.fileType;
      
      let replyText = isEnglishMoodle
        ? `📄 **${fileTypeLabel} ${fileInfo.fileNumber}** from **${courseDisplayName}**\n\n` +
          `- 📁 \`${foundFile.filename}\`\n` +
          `- 📚 *${courseDisplayName}*`
        : `📄 **${fileTypeLabel} ${fileInfo.fileNumber}** de **${courseDisplayName}**\n\n` +
          `- 📁 \`${foundFile.filename}\`\n` +
          `- 📚 *${courseDisplayName}*`;

      const moodleFileResponse = {
        url: downloadUrl,
        filename: foundFile.filename,
        mimetype: foundFile.mimetype,
        courseName: courseDisplayName,
        fileType: fileInfo.fileType,
        fileNumber: fileInfo.fileNumber,
        week: fileInfo.week,
      };

      logger.info("handleMoodleIntent.moodleFileFound", {
        uid,
        filename: foundFile.filename,
        url: downloadUrl.substring(0, 100) + "...",
        mimetype: foundFile.mimetype,
        courseName: courseDisplayName,
        moodleFileResponse,
      });

      return {
        reply: replyText,
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: false,
        ed_intent: null,
        moodle_intent_detected: true,
        moodle_intent: moodleIntentResult.moodle_intent,
        moodle_file: moodleFileResponse,
      };
    } catch (e: any) {
      const fileInfoForLog = extractFileInfo(question);
      logger.error("handleMoodleIntent.moodleFileFetchFailed", {
        error: String(e),
        errorMessage: e.message,
        errorStack: e.stack,
        uid,
        intent: moodleIntentResult.moodle_intent,
        fileType: fileInfoForLog?.fileType,
        fileNumber: fileInfoForLog?.fileNumber,
        courseName: fileInfoForLog?.courseName,
        week: fileInfoForLog?.week,
      });
      return {
        reply: isEnglishMoodle
          ? `Error retrieving file from Moodle: ${e.message || "unknown error"}`
          : `Erreur lors de la récupération du fichier Moodle : ${e.message || "erreur inconnue"}`,
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: false,
        ed_intent: null,
        moodle_intent_detected: true,
        moodle_intent: moodleIntentResult.moodle_intent,
        moodle_file: null,
      };
    }
  }
  // === End Moodle Intent Detection ===

  return null;
}

