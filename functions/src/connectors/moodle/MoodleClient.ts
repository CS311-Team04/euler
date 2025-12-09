/**
    * A simple Moodle web service client for testing connectivity and fetching files.
*/
import * as logger from "firebase-functions/logger";

/**
 * Escapes special regex characters in a string to prevent regex injection attacks.
 */
function escapeRegex(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Redacts tokens from URLs before logging to prevent sensitive data leakage.
 */
function redactTokensFromUrl(url: string): string {
  try {
    const urlObj = new URL(url);
    // Remove token query parameters
    urlObj.searchParams.delete("token");
    urlObj.searchParams.delete("wstoken");
    // Redact any remaining suspicious query params
    const redactedParams = new URLSearchParams();
    urlObj.searchParams.forEach((value, key) => {
      if (key.toLowerCase().includes("token") || key.toLowerCase().includes("auth")) {
        redactedParams.set(key, "[REDACTED]");
      } else {
        redactedParams.set(key, value);
      }
    });
    urlObj.search = redactedParams.toString();
    return urlObj.toString();
  } catch {
    // If URL parsing fails, do a simple string replacement
    return url.replace(/[?&](?:token|wstoken)=[^&]*/gi, "[REDACTED]");
  }
}

export class MoodleClient {
  constructor(
    private readonly baseUrl: string,
    private readonly token: string
  ) {}

  /**
    * Builds the full URL for a given Moodle web service function.
    * Moodle REST API requires parameters to be passed in a specific format.
   */
  private buildUrl(wsFunction: string, params?: Record<string, any>): string {
    const trimmedBase = this.baseUrl.replace(/\/+$/, "");
    const url = new URL(trimmedBase + "/webservice/rest/server.php");
    url.searchParams.set("wstoken", this.token);
    url.searchParams.set("wsfunction", wsFunction);
    url.searchParams.set("moodlewsrestformat", "json");

    // Moodle REST API requires parameters to be passed as query parameters
    // For functions that need complex parameters, we need to use POST with form data
    // But for simple parameters like courseid, query params work
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          // Ensure numeric values are properly converted
          const paramValue = typeof value === "number" ? value.toString() : String(value);
          url.searchParams.set(key, paramValue);
        }
      });
    }

    return url.toString();
  }

  /**
   * Calls core_webservice_get_site_info to validate connectivity & token.
   * Returns true if the call succeeds and does not contain an error.
   */
  async testConnection(): Promise<boolean> {
    const url = this.buildUrl("core_webservice_get_site_info");

    let res: Response;
    try {
      res = await fetch(url, { method: "GET" });
    } catch (e) {
      console.error("MoodleClient.testConnection network error", e);
      return false;
    }

    if (!res.ok) {
      console.error("MoodleClient.testConnection bad status", res.status);
      return false;
    }

    try {
      const body = (await res.json()) as any;

      // Moodle errors often come back with 'exception' or 'errorcode'
      if (body && (body.exception || body.errorcode)) {
        console.error("MoodleClient.testConnection Moodle error", body);
        return false;
      }

      return true;
    } catch (e) {
      console.error("MoodleClient.testConnection JSON parse error", e);
      return false;
    }
  }

  /**
   * Gets the current user's information from Moodle.
   * Uses core_webservice_get_site_info to get user ID.
   */
  async getCurrentUserInfo(): Promise<{ userid: number; [key: string]: any }> {
    const trimmedBase = this.baseUrl.replace(/\/+$/, "");
    const url = `${trimmedBase}/webservice/rest/server.php`;

    const formData = new URLSearchParams();
    formData.append("wstoken", this.token);
    formData.append("wsfunction", "core_webservice_get_site_info");
    formData.append("moodlewsrestformat", "json");

    logger.info("MoodleClient.getCurrentUserInfo.request", {
      url: url.replace(this.token, "***"),
      wsfunction: "core_webservice_get_site_info",
    });

    const res = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "Accept": "application/json",
      },
      body: formData.toString()
    });

    const responseText = await res.text();
    logger.info("MoodleClient.getCurrentUserInfo.response", {
      status: res.status,
      statusText: res.statusText,
      bodyPreview: responseText.substring(0, 500),
    });

    if (!res.ok) {
      throw new Error(`Failed to get user info: ${res.status} ${responseText.substring(0, 200)}`);
    }

    let body: any;
    try {
      body = JSON.parse(responseText);
    } catch (e) {
      logger.error("MoodleClient.getCurrentUserInfo.jsonParseError", {
        responseText: responseText.substring(0, 500),
        error: String(e),
      });
      throw new Error(`Invalid JSON response from Moodle: ${responseText.substring(0, 200)}`);
    }

    // Check for Moodle-specific errors
    if (body.exception) {
      logger.error("MoodleClient.getCurrentUserInfo.moodleException", {
        exception: body.exception,
        message: body.message,
        errorcode: body.errorcode,
        debuginfo: body.debuginfo,
      });
      throw new Error(`Moodle error: ${body.message || body.exception}`);
    }
    if (body.errorcode) {
      logger.error("MoodleClient.getCurrentUserInfo.moodleError", {
        errorcode: body.errorcode,
        message: body.message,
        debuginfo: body.debuginfo,
      });
      throw new Error(`Moodle error: ${body.message || body.errorcode}`);
    }

    if (!body.userid) {
      throw new Error("Moodle response missing userid");
    }

    return body;
  }

  /**
   * Gets all courses the user is enrolled in.
   * Uses core_enrol_get_users_courses web service.
   * First gets the user ID, then fetches courses.
   */
  async getEnrolledCourses(): Promise<any[]> {
    // First, get the user ID from site info
    const userInfo = await this.getCurrentUserInfo();
    const userId = userInfo.userid;

    logger.info("MoodleClient.getEnrolledCourses", {
      userId,
    });

    // Moodle REST API: use POST with form data
    const trimmedBase = this.baseUrl.replace(/\/+$/, "");
    const url = `${trimmedBase}/webservice/rest/server.php`;

    const formData = new URLSearchParams();
    formData.append("wstoken", this.token);
    formData.append("wsfunction", "core_enrol_get_users_courses");
    formData.append("moodlewsrestformat", "json");
    formData.append("userid", userId.toString());

    logger.info("MoodleClient.getEnrolledCourses.request", {
      url: url.replace(this.token, "***"),
      userId,
      wsfunction: "core_enrol_get_users_courses",
    });

    const res = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "Accept": "application/json",
      },
      body: formData.toString()
    });

    const responseText = await res.text();
    logger.info("MoodleClient.getEnrolledCourses.response", {
      status: res.status,
      statusText: res.statusText,
      bodyPreview: responseText.substring(0, 500),
    });

    if (!res.ok) {
      throw new Error(`Failed to fetch courses: ${res.status} ${responseText.substring(0, 200)}`);
    }

    let body: any;
    try {
      body = JSON.parse(responseText);
    } catch (e) {
      logger.error("MoodleClient.getEnrolledCourses.jsonParseError", {
        responseText: responseText.substring(0, 500),
        error: String(e),
      });
      throw new Error(`Invalid JSON response from Moodle: ${responseText.substring(0, 200)}`);
    }

    // Check for Moodle-specific errors
    if (body.exception) {
      logger.error("MoodleClient.getEnrolledCourses.moodleException", {
        exception: body.exception,
        message: body.message,
        errorcode: body.errorcode,
        debuginfo: body.debuginfo,
        userId,
      });
      throw new Error(`Moodle error: ${body.message || body.exception}`);
    }
    if (body.errorcode) {
      logger.error("MoodleClient.getEnrolledCourses.moodleError", {
        errorcode: body.errorcode,
        message: body.message,
        debuginfo: body.debuginfo,
        userId,
      });
      throw new Error(`Moodle error: ${body.message || body.errorcode}`);
    }

    // Moodle returns an array directly for core_enrol_get_users_courses
    if (!Array.isArray(body)) {
      logger.warn("MoodleClient.getEnrolledCourses.nonArrayResponse", {
        bodyType: typeof body,
        bodyKeys: body ? Object.keys(body) : null,
        bodyPreview: JSON.stringify(body).substring(0, 200),
      });
      return [];
    }

    return body;
  }

  /**
   * Gets course contents (modules, resources, files) for a given course.
   * Uses core_course_get_contents web service.
   * Moodle REST API requires courseid parameter.
   */
  async getCourseContents(courseId: number): Promise<any[]> {
    // Moodle REST API: courseid must be a number
    const courseIdNum = typeof courseId === "number" ? courseId : parseInt(String(courseId), 10);
    if (isNaN(courseIdNum) || courseIdNum <= 0) {
      throw new Error(`Invalid course ID: ${courseId} (must be a positive integer)`);
    }

    logger.info("MoodleClient.getCourseContents", { courseId: courseIdNum });

    // Moodle REST API: Use POST with form data (standard format)
    const trimmedBase = this.baseUrl.replace(/\/+$/, "");
    const url = `${trimmedBase}/webservice/rest/server.php`;

    const formData = new URLSearchParams();
    formData.append("wstoken", this.token);
    formData.append("wsfunction", "core_course_get_contents");
    formData.append("moodlewsrestformat", "json");
    formData.append("courseid", courseIdNum.toString());

    logger.info("MoodleClient.getCourseContents.request", {
      url: url.replace(this.token, "***"),
      courseid: courseIdNum,
      wsfunction: "core_course_get_contents",
    });

    const res = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "Accept": "application/json",
      },
      body: formData.toString()
    });

    const responseText = await res.text();
    logger.info("MoodleClient.getCourseContents.response", {
      status: res.status,
      statusText: res.statusText,
      bodyPreview: responseText.substring(0, 500),
    });

    if (!res.ok) {
      throw new Error(`Failed to fetch course contents: ${res.status} ${responseText.substring(0, 200)}`);
    }

    let body: any;
    try {
      body = JSON.parse(responseText);
    } catch (e) {
      logger.error("MoodleClient.getCourseContents.jsonParseError", {
        responseText: responseText.substring(0, 500),
        error: String(e),
      });
      throw new Error(`Invalid JSON response from Moodle: ${responseText.substring(0, 200)}`);
    }

    // Check for Moodle-specific errors
    if (body.exception) {
      logger.error("MoodleClient.getCourseContents.moodleException", {
        exception: body.exception,
        message: body.message,
        errorcode: body.errorcode,
        debuginfo: body.debuginfo,
        courseid: courseIdNum,
      });
      throw new Error(`Moodle error: ${body.message || body.exception}`);
    }
    if (body.errorcode) {
      logger.error("MoodleClient.getCourseContents.moodleError", {
        errorcode: body.errorcode,
        message: body.message,
        debuginfo: body.debuginfo,
        courseid: courseIdNum,
      });
      throw new Error(`Moodle error: ${body.message || body.errorcode}`);
    }

    // Moodle returns an array directly for core_course_get_contents
    if (!Array.isArray(body)) {
      logger.warn("MoodleClient.getCourseContents.nonArrayResponse", {
        bodyType: typeof body,
        bodyKeys: body ? Object.keys(body) : null,
        bodyPreview: JSON.stringify(body).substring(0, 200),
      });
      return [];
    }

    return body;
  }

  /**
   * Searches for a file in course contents by matching name patterns.
   * Supports searching for lectures, homework, and homework solutions.
   * Can search by file number, week number, or both.
   * Searches through ALL sections and modules of the course.
   */
  async findFile(
    courseId: number,
    fileType: "lecture" | "homework" | "homework_solution",
    fileNumber: string | null,
    week: number | null = null
  ): Promise<{ fileurl: string; filename: string; mimetype: string } | null> {
    const contents = await this.getCourseContents(courseId);

    // Build search patterns based on file type
    const patterns: RegExp[] = [];

    if (fileNumber) {
      const normalizedNumber = escapeRegex(fileNumber.toLowerCase().trim());

      switch (fileType) {
        case "lecture":
          patterns.push(
            new RegExp(`lecture\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`cours\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`leçon\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`lesson\\s*${normalizedNumber}(?:\\.|\\b)`, "i")
          );
          break;
        case "homework":
          patterns.push(
            new RegExp(`homework\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`devoir\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`travail\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`hw\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`serie\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`série\\s*${normalizedNumber}(?:\\.|\\b)`, "i")
          );
          break;
        case "homework_solution":
          patterns.push(
            new RegExp(`homework\\s*solution\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`solution\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`correction\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`corrigé\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`solution.*homework\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`correction.*devoir\\s*${normalizedNumber}(?:\\.|\\b)`, "i"),
            new RegExp(`corrigé.*devoir\\s*${normalizedNumber}(?:\\.|\\b)`, "i")
          );
          break;
      }
    } else {
      switch (fileType) {
        case "lecture":
          patterns.push(/lecture/i, /leçon/i, /lesson/i, /cours/i);
          break;
        case "homework":
          patterns.push(/homework/i, /devoir/i, /travail/i, /hw/i, /serie/i, /série/i);
          break;
        case "homework_solution":
          patterns.push(/homework\s*solution/i, /solution/i, /correction/i, /corrigé/i);
          break;
      }
    }

    // Search through ALL sections and modules (ignore week filtering; rely on explicit numbers in names)
    for (const section of contents) {
      if (!section.modules) continue;

      for (const module of section.modules) {
        // Check module name
        if (module.name) {
          const moduleNameLower = module.name.toLowerCase();

          // If we have a file number, require exact match in module name
          if (fileNumber) {
            const normalizedNumber = fileNumber.toLowerCase().trim();
            let matchesType = false;
            let matchesNumber = false;

            // Check if module name matches the file type pattern
            for (const pattern of patterns) {
              if (pattern.test(module.name)) {
                matchesType = true;
                break;
              }
            }

            // If type matches, verify the number matches EXACTLY
            if (matchesType) {
              // Extract number from module name and compare EXACTLY
              // Match patterns like "lecture 2", "cours2", "homework2", "solution2"
              const numberPattern =
                fileType === "lecture"
                  ? new RegExp(`(?:lecture|leçon|lesson|cours)\\s*(\\d+)`, "i")
                  : fileType === "homework"
                    ? new RegExp(`(?:homework|devoir|travail|hw|serie|série)\\s*(\\d+)`, "i")
                    : new RegExp(`(?:homework\\s*solution|solution|correction|corrigé)\\s*(\\d+)`, "i");
              const numberMatch = moduleNameLower.match(numberPattern);
              if (numberMatch && numberMatch[1] === normalizedNumber) {
                matchesNumber = true;
              }
            }

            // Proceed if exact type+number
            if (matchesType && matchesNumber) {
              if (module.contents && Array.isArray(module.contents)) {
                for (const content of module.contents) {
                  if (content.type === "file" && content.fileurl) {
                    const mimetype = content.mimetype || "";
                    if (mimetype.includes("pdf") || content.filename?.toLowerCase().endsWith(".pdf")) {
                      return {
                        fileurl: content.fileurl,
                        filename: content.filename || "file.pdf",
                        mimetype: mimetype || "application/pdf",
                      };
                    }
                  }
                }
              }
            }
          } else {
            // No file number - just check type match
            for (const pattern of patterns) {
              if (pattern.test(module.name)) {
                // Found matching module, look for PDF files
                if (module.contents && Array.isArray(module.contents)) {
                  for (const content of module.contents) {
                    if (content.type === "file" && content.fileurl) {
                      const mimetype = content.mimetype || "";
                      if (mimetype.includes("pdf") || content.filename?.toLowerCase().endsWith(".pdf")) {
                        return {
                          fileurl: content.fileurl,
                          filename: content.filename || "file.pdf",
                          mimetype: mimetype || "application/pdf",
                        };
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Do not rely on filename for type/number detection; module name matching governs selection.
      }
    }

    return null;
  }

  /**
   * Finds a course by name with fuzzy matching.
   * Handles variations like "The software enterprise - from ideas to products" matching "software enterprise" or "swent".
   * Works for ANY course name - searches through all enrolled courses dynamically.
   */
  async findCourseByName(courseName: string): Promise<any | null> {
    const courses = await this.getEnrolledCourses();
    if (courses.length === 0) return null;

    const normalizedSearch = courseName.toLowerCase().trim();

    // Normalize course name for matching (remove common words, special chars)
    const normalize = (name: string): string => {
      return name
        .toLowerCase()
        .replace(/[^\w\s]/g, " ") // Remove special chars
        .replace(/\b(the|a|an|le|la|les|du|de|des|from|of|to|and|or)\b/g, "") // Remove articles and common words
        .replace(/\s+/g, " ") // Normalize whitespace
        .replace(/\bentreprise\b/g, "enterprise") // Normalize French "entreprise" to "enterprise"
        .trim();
    };

    // Try exact substring or shortname match first (case-insensitive)
    for (const course of courses) {
      const courseFullname = (course.fullname || "").toLowerCase();
      const courseShortname = (course.shortname || "").toLowerCase();
      if (courseFullname.includes(normalizedSearch) || courseShortname.includes(normalizedSearch)) {
        return course;
      }
    }

    // Try normalized matching
    const normalizedSearchClean = normalize(normalizedSearch);
    if (!normalizedSearchClean) return null; // Empty after normalization

    for (const course of courses) {
      const courseFullname = course.fullname || course.shortname || "";
      if (!courseFullname) continue;

      const normalizedCourseName = normalize(courseFullname);

      // Check if search term is contained in course name or shortname
      const normalizedShort = (course.shortname || "").toLowerCase();
      if (normalizedCourseName.includes(normalizedSearchClean) ||
          normalizedShort.includes(normalizedSearchClean)) {
        return course;
      }

      // Check for word-by-word matching (all search words appear in course)
      const courseWords = normalizedCourseName.split(/\s+/).filter(w => w.length > 0);
      const searchWords = normalizedSearchClean.split(/\s+/).filter(w => w.length > 0);

      if (searchWords.length > 0) {
        // Check if all search words appear in course name (in order, as prefixes or full words)
        let allWordsMatch = true;
        let courseWordIndex = 0;

        for (const searchWord of searchWords) {
          let found = false;
          while (courseWordIndex < courseWords.length) {
            if (courseWords[courseWordIndex].startsWith(searchWord) ||
                searchWord.startsWith(courseWords[courseWordIndex])) {
              found = true;
              courseWordIndex++;
              break;
            }
            courseWordIndex++;
          }
          if (!found) {
            allWordsMatch = false;
            break;
          }
        }

        if (allWordsMatch) {
          return course;
        }
      }

      // Check if search is an abbreviation (first letters of course words)
      // e.g., "swent" → "software enterprise" (s-w-e-n-t from "software enterprise")
      const courseAbbrev = courseWords.map(w => w[0]).join("");
      if (normalizedSearchClean === courseAbbrev ||
          courseAbbrev.startsWith(normalizedSearchClean) ||
          normalizedSearchClean.startsWith(courseAbbrev)) {
        return course;
      }

      // Check if search matches the beginning of course words (e.g., "swent" → "software enterprise")
      // This handles cases where user types partial words or abbreviations
      // For "swent", it's "s"+"w" from "software" + "e"+"n"+"t" from "enterprise"
      if (normalizedSearchClean.length >= 3) {
        // Try matching first 2-4 letters of each course word
        let abbreviationMatch = "";
        for (const word of courseWords) {
          if (word.length >= 2) {
            abbreviationMatch += word.substring(0, Math.min(4, word.length));
          }
        }
        if (abbreviationMatch.toLowerCase().includes(normalizedSearchClean) ||
            normalizedSearchClean.includes(abbreviationMatch.toLowerCase().substring(0, normalizedSearchClean.length))) {
          return course;
        }

        // Also try first 2 letters of each word (more common abbreviation style)
        const twoLetterAbbrev = courseWords.map(w => w.substring(0, Math.min(2, w.length))).join("");
        if (twoLetterAbbrev.toLowerCase().includes(normalizedSearchClean) ||
            normalizedSearchClean.includes(twoLetterAbbrev.toLowerCase())) {
          return course;
        }

        // Try complex abbreviation pattern (e.g., "swent" = letters from "software enterprise")
        // Check if all characters of the search term appear in order in the combined course name
        const combinedCourseName = normalizedCourseName.replace(/\s+/g, ""); // Remove all spaces
        let searchIndex = 0;
        let courseIndex = 0;

        // Try to match each character of the search term in order within the course name
        while (searchIndex < normalizedSearchClean.length && courseIndex < combinedCourseName.length) {
          if (combinedCourseName[courseIndex] === normalizedSearchClean[searchIndex]) {
            searchIndex++; // Found a match, move to next search character
          }
          courseIndex++; // Always move forward in course name
        }

        // If we matched all characters, it's a valid abbreviation
        if (searchIndex === normalizedSearchClean.length) {
          return course;
        }
      }
    }

    return null;
  }

  /**
   * Gets a direct download URL for a file (adds token and forces download).
   * Moodle file URLs often go through an intermediate page, so we add forcedownload=1
   * to get the direct PDF URL.
   */
  getFileDownloadUrl(fileUrl: string): string {
    // Moodle file URLs need the token appended and forcedownload=1 to bypass intermediate page
    const url = new URL(fileUrl);
    url.searchParams.set("token", this.token);
    url.searchParams.set("forcedownload", "1");
    return url.toString();
  }

  /**
   * Follows redirects to get the final direct PDF URL.
   * Some Moodle installations redirect through intermediate pages.
   */
  async getDirectFileUrl(fileUrl: string): Promise<string> {
    const urlWithToken = this.getFileDownloadUrl(fileUrl);

    try {
      // Make a HEAD request to follow redirects and get the final URL
      const res = await fetch(urlWithToken, {
        method: "HEAD",
        redirect: "follow",
        headers: {
          "Accept": "application/pdf",
        },
      });

      // If we got redirected, use the final URL
      const finalUrl = res.url;

      logger.info("MoodleClient.getDirectFileUrl", {
        originalUrl: redactTokensFromUrl(fileUrl).substring(0, 100),
        finalUrl: redactTokensFromUrl(finalUrl).substring(0, 100),
        status: res.status,
        contentType: res.headers.get("content-type"),
      });

      // If the final URL is different and looks like a direct file, use it
      if (finalUrl !== urlWithToken && (finalUrl.includes(".pdf") || res.headers.get("content-type")?.includes("pdf"))) {
        return finalUrl;
      }

      // Otherwise return the URL with token
      return urlWithToken;
    } catch (e) {
      logger.warn("MoodleClient.getDirectFileUrl.failed", {
        error: String(e),
        url: fileUrl.substring(0, 100),
      });
      // Fallback to URL with token
      return urlWithToken;
    }
  }
}
