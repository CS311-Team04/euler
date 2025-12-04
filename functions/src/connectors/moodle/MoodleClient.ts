/**
 * Moodle course structure returned by core_enrol_get_users_courses
 */
export interface MoodleCourse {
  id: number;
  shortname: string;
  fullname: string;
  displayname: string;
  enrolledusercount?: number;
  visible?: number;
  summary?: string;
  format?: string;
  startdate?: number;
  enddate?: number;
}

/**
 * File content within a Moodle module
 */
export interface MoodleFileContent {
  type: string; // "file" | "url" | etc.
  filename: string;
  filepath?: string;
  filesize?: number;
  fileurl: string;
  timecreated?: number;
  timemodified?: number;
  sortorder?: number;
  mimetype?: string;
  isexternalfile?: boolean;
  author?: string;
  license?: string;
}

/**
 * Module (activity/resource) within a course section
 */
export interface MoodleModule {
  id: number;
  url?: string;
  name: string;
  instance?: number;
  contextid?: number;
  visible?: number;
  uservisible?: boolean;
  modicon?: string;
  modname: string; // "resource", "folder", "assign", "quiz", etc.
  modplural?: string;
  indent?: number;
  onclick?: string;
  afterlink?: string;
  customdata?: string;
  completion?: number;
  contents?: MoodleFileContent[];
}

/**
 * Section within a course (weeks/topics)
 */
export interface MoodleSection {
  id: number;
  name: string;
  visible?: number;
  summary?: string;
  summaryformat?: number;
  section: number; // section number (0, 1, 2, ...)
  hiddenbynumsections?: number;
  uservisible?: boolean;
  modules: MoodleModule[];
}

/**
 * Result of a file search operation
 */
export interface MoodleFileSearchResult {
  found: boolean;
  file?: MoodleFileContent;
  module?: MoodleModule;
  section?: MoodleSection;
  course?: MoodleCourse;
  downloadUrl?: string;
  error?: string;
}

/**
 * A Moodle web service client for connectivity testing and course content retrieval.
 */
export class MoodleClient {
  constructor(
    private readonly baseUrl: string,
    private readonly token: string
  ) {}

  /**
   * Builds the full URL for a given Moodle web service function.
   */
  private buildUrl(wsFunction: string): string {
    const trimmedBase = this.baseUrl.replace(/\/+$/, "");
    const url = new URL(trimmedBase + "/webservice/rest/server.php");
    url.searchParams.set("wstoken", this.token);
    url.searchParams.set("wsfunction", wsFunction);
    url.searchParams.set("moodlewsrestformat", "json");
    return url.toString();
  }

  /**
   * Makes an API call and returns the JSON response.
   */
  private async callApi<T>(wsFunction: string, params?: Record<string, string>): Promise<T> {
    let url = this.buildUrl(wsFunction);
    if (params) {
      const urlObj = new URL(url);
      for (const [key, value] of Object.entries(params)) {
        urlObj.searchParams.set(key, value);
      }
      url = urlObj.toString();
    }

    const res = await fetch(url, { method: "GET" });
    if (!res.ok) {
      throw new Error(`Moodle API error: ${res.status} ${res.statusText}`);
    }

    const body = await res.json();
    if (body && (body.exception || body.errorcode)) {
      throw new Error(`Moodle error: ${body.message || body.errorcode || "unknown"}`);
    }

    return body as T;
  }

  /**
   * Site info response from core_webservice_get_site_info
   */
  private cachedUserId: number | null = null;

  /**
   * Gets the current user's Moodle user ID from site info.
   * Result is cached for subsequent calls.
   */
  async getCurrentUserId(): Promise<number> {
    if (this.cachedUserId !== null) {
      return this.cachedUserId;
    }

    interface SiteInfo {
      userid: number;
      username: string;
      fullname: string;
      sitename: string;
      siteurl: string;
    }

    const siteInfo = await this.callApi<SiteInfo>("core_webservice_get_site_info");
    this.cachedUserId = siteInfo.userid;
    console.log("MoodleClient: got user ID", this.cachedUserId, "for user", siteInfo.fullname);
    return this.cachedUserId;
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

      // Cache the user ID while we're at it
      if (body.userid) {
        this.cachedUserId = body.userid;
      }

      return true;
    } catch (e) {
      console.error("MoodleClient.testConnection JSON parse error", e);
      return false;
    }
  }

  /**
   * Gets all courses the user is enrolled in.
   * First retrieves the user ID from site info, then fetches courses.
   */
  async getUserCourses(): Promise<MoodleCourse[]> {
    const userId = await this.getCurrentUserId();
    console.log("MoodleClient.getUserCourses: fetching courses for user ID", userId);
    return this.callApi<MoodleCourse[]>("core_enrol_get_users_courses", {
      userid: String(userId),
    });
  }

  /**
   * Gets the full content of a course (sections, modules, files).
   */
  async getCourseContents(courseId: number): Promise<MoodleSection[]> {
    return this.callApi<MoodleSection[]>("core_course_get_contents", {
      courseid: String(courseId),
    });
  }

  /**
   * Generates a download URL for a file with the token appended.
   * Moodle file URLs need the token as a query param for authenticated download.
   */
  getAuthenticatedFileUrl(fileUrl: string): string {
    const url = new URL(fileUrl);
    url.searchParams.set("token", this.token);
    return url.toString();
  }

  /**
   * Searches for a file across all user's courses based on course name and file descriptor.
   * 
   * @param courseQuery - Natural language course name (e.g., "probability and statistics")
   * @param fileQuery - Natural language file descriptor (e.g., "1st lecture", "week 2 slides")
   * @returns Search result with file details and download URL
   */
  async searchFile(courseQuery: string, fileQuery: string): Promise<MoodleFileSearchResult> {
    try {
      // Get all user courses
      const courses = await this.getUserCourses();
      if (courses.length === 0) {
        return { found: false, error: "No courses found for user" };
      }

      // Find matching course (fuzzy match on name)
      const courseQueryLower = courseQuery.toLowerCase();
      const matchingCourse = courses.find(
        (c) =>
          c.fullname.toLowerCase().includes(courseQueryLower) ||
          c.shortname.toLowerCase().includes(courseQueryLower) ||
          c.displayname.toLowerCase().includes(courseQueryLower)
      );

      if (!matchingCourse) {
        return {
          found: false,
          error: `Course matching "${courseQuery}" not found. Available courses: ${courses.map((c) => c.fullname).join(", ")}`,
        };
      }

      // Get course contents
      const sections = await this.getCourseContents(matchingCourse.id);

      // Parse file query to understand what user wants
      const fileInfo = this.parseFileQuery(fileQuery);

      // Search for matching file
      const result = this.findMatchingFile(sections, fileInfo, matchingCourse);
      return result;
    } catch (e: any) {
      return { found: false, error: e.message || "Failed to search for file" };
    }
  }

  /**
   * Parses natural language file query into structured search parameters.
   */
  private parseFileQuery(query: string): {
    ordinal?: number;
    type?: string;
    keywords: string[];
  } {
    const queryLower = query.toLowerCase();
    const keywords: string[] = [];

    // Extract ordinal (1st, 2nd, first, second, etc.)
    let ordinal: number | undefined;
    const ordinalPatterns: [RegExp, number][] = [
      [/\b(1st|first|première?|premier)\b/i, 1],
      [/\b(2nd|second|deuxième)\b/i, 2],
      [/\b(3rd|third|troisième)\b/i, 3],
      [/\b(4th|fourth|quatrième)\b/i, 4],
      [/\b(5th|fifth|cinquième)\b/i, 5],
      [/\b(6th|sixth|sixième)\b/i, 6],
      [/\b(7th|seventh|septième)\b/i, 7],
      [/\b(8th|eighth|huitième)\b/i, 8],
      [/\b(9th|ninth|neuvième)\b/i, 9],
      [/\b(10th|tenth|dixième)\b/i, 10],
      [/\blecture\s*(\d+)\b/i, -1], // special case: "lecture 3"
      [/\bweek\s*(\d+)\b/i, -1], // special case: "week 2"
    ];

    for (const [pattern, value] of ordinalPatterns) {
      const match = queryLower.match(pattern);
      if (match) {
        ordinal = value === -1 ? parseInt(match[1], 10) : value;
        break;
      }
    }

    // Detect file type
    let type: string | undefined;
    if (/\b(lecture|cours|leçon|lesson)\b/i.test(queryLower)) {
      type = "lecture";
      keywords.push("lecture", "cours");
    }
    if (/\b(slides?|présentation|presentation)\b/i.test(queryLower)) {
      type = "slides";
      keywords.push("slide");
    }
    if (/\b(exercise|exercice|td|tp|problem)\b/i.test(queryLower)) {
      type = "exercise";
      keywords.push("exercise", "exercice", "td", "tp");
    }
    if (/\b(solution|correction|corrigé)\b/i.test(queryLower)) {
      type = "solution";
      keywords.push("solution", "correction", "corrig");
    }

    // Extract other keywords
    const words = queryLower.split(/\s+/).filter((w) => w.length > 2);
    keywords.push(
      ...words.filter(
        (w) =>
          !["the", "for", "and", "pdf", "file", "get", "retrieve", "download"].includes(w)
      )
    );

    return { ordinal, type, keywords: [...new Set(keywords)] };
  }

  /**
   * Finds a matching file in course sections based on parsed query.
   */
  private findMatchingFile(
    sections: MoodleSection[],
    fileInfo: { ordinal?: number; type?: string; keywords: string[] },
    course: MoodleCourse
  ): MoodleFileSearchResult {
    // Collect all PDF/document files with metadata
    const candidates: {
      file: MoodleFileContent;
      module: MoodleModule;
      section: MoodleSection;
      score: number;
    }[] = [];

    for (const section of sections) {
      for (const mod of section.modules) {
        // Only consider resource modules with file contents
        if (mod.modname !== "resource" || !mod.contents || mod.contents.length === 0) {
          continue;
        }

        for (const file of mod.contents) {
          // Only consider actual files (PDFs, docs, etc.)
          if (file.type !== "file") continue;
          const isPdf = file.filename.toLowerCase().endsWith(".pdf");
          const isDoc =
            file.filename.toLowerCase().endsWith(".doc") ||
            file.filename.toLowerCase().endsWith(".docx") ||
            file.filename.toLowerCase().endsWith(".ppt") ||
            file.filename.toLowerCase().endsWith(".pptx");

          if (!isPdf && !isDoc) continue;

          // Calculate relevance score
          let score = 0;
          const fileNameLower = file.filename.toLowerCase();
          const moduleNameLower = mod.name.toLowerCase();

          // Keyword matching
          for (const keyword of fileInfo.keywords) {
            if (fileNameLower.includes(keyword)) score += 10;
            if (moduleNameLower.includes(keyword)) score += 8;
            if (section.name.toLowerCase().includes(keyword)) score += 5;
          }

          // Ordinal matching (section number or file name pattern)
          if (fileInfo.ordinal) {
            // Match section number (sections are usually 0-indexed, so section 1 = ordinal 1)
            if (section.section === fileInfo.ordinal || section.section === fileInfo.ordinal - 1) {
              score += 15;
            }
            // Match numbers in filename
            const fileNumbers = fileNameLower.match(/\d+/g);
            if (fileNumbers && fileNumbers.includes(String(fileInfo.ordinal))) {
              score += 12;
            }
            // Match numbers in module name
            const modNumbers = moduleNameLower.match(/\d+/g);
            if (modNumbers && modNumbers.includes(String(fileInfo.ordinal))) {
              score += 10;
            }
          }

          // Type matching (lecture, slides, exercise)
          if (fileInfo.type === "lecture" && /\b(lecture|cours|leçon)\b/i.test(moduleNameLower)) {
            score += 8;
          }
          if (fileInfo.type === "slides" && /\b(slide|présentation)\b/i.test(moduleNameLower)) {
            score += 8;
          }

          // Prefer PDFs
          if (isPdf) score += 3;

          candidates.push({ file, module: mod, section, score });
        }
      }
    }

    if (candidates.length === 0) {
      return { found: false, error: `No PDF/document files found in course "${course.fullname}"` };
    }

    // Sort by score and return best match
    candidates.sort((a, b) => b.score - a.score);
    const best = candidates[0];

    return {
      found: true,
      file: best.file,
      module: best.module,
      section: best.section,
      course,
      downloadUrl: this.getAuthenticatedFileUrl(best.file.fileurl),
    };
  }
}