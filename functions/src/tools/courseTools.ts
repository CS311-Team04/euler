import { Firestore } from "firebase-admin/firestore";
import { MoodleConnectorRepository } from "../connectors/moodle/MoodleConnectorRepository";
import { getCourseOverview } from "../utils/getCourseOverview";

// JSON schema for tool parameters
export const getWeeklyOverviewSchema = {
  type: "object",
  properties: {
    courseName: {
      type: "string",
      description: "Name of the Moodle course to fetch the current (or latest) week overview for.",
    },
  },
  required: ["courseName"],
};

// Tool definition (OpenAI / Vercel AI compatible)
export const getWeeklyOverviewTool = {
  name: "get_weekly_overview",
  description:
    "Get the summary, bullet points, and resources for a specific course for the current (or latest) week. Use this when the user asks 'what did I miss in [Course]?'.",
  parameters: getWeeklyOverviewSchema,
};

export type CourseToolContext = {
  uid: string;
  decrypt: (cipher: string) => string;
  db: Firestore;
  moodleRepo: MoodleConnectorRepository;
};

type GetWeeklyOverviewArgs = {
  courseName: string;
  weekNumber?: number;
};

/**
 * Resolver: calls getCourseOverview and formats the deterministic output.
 */
export async function resolveGetWeeklyOverview(
  args: GetWeeklyOverviewArgs,
  ctx: CourseToolContext
): Promise<string> {
  const { courseName, weekNumber } = args;
  try {
    const result = await getCourseOverview(
      courseName,
      { uid: ctx.uid, decrypt: ctx.decrypt },
      ctx.db,
      ctx.moodleRepo,
      weekNumber
    );

    const payload = {
      type: "moodle_overview",
      content: result.markdown,
      metadata: {
        courseName,
        weekLabel: result.sectionName ?? result.weekId,
        lastUpdated: result.lastUpdated.toISOString(),
        source: "Moodle",
      },
    };

    return JSON.stringify(payload);
  } catch (e: any) {
    const msg = (e?.message || "").toLowerCase();
    if (msg.includes("not found")) {
      return `I couldn't find a course named '${courseName}'. Please check your enrollment in Pocket Campus.`;
    }
    return `Unable to fetch Moodle overview for '${courseName}': ${e?.message || "unknown error"}`;
  }
}

