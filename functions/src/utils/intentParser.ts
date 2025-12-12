export type CourseIntent =
  | { type: "missed"; course: string; week: null }
  | { type: "specific_week"; course: string; week: number };

/**
 * Detects intents for Moodle course overviews using regex (deterministic, no LLM).
 * Supports English/French phrasing for "what did I miss" and "week X of COURSE".
 *
 * Returns:
 *  - { type: "missed", course, week: null } for "What did I miss in Algebra?"
 *  - { type: "specific_week", course, week } for "De quoi parle la semaine 3 de Physique ?"
 *  - null if no pattern matches.
 */
export function detectCourseIntent(userQuery: string): CourseIntent | null {
  if (!userQuery || !userQuery.trim()) {
    return null;
  }

  const text = userQuery.trim();

  // Pattern A: "What did I miss in Algebra?" / "Qu'est-ce que j'ai raté en Analyse ?"
  // Triggers: what did I miss, catch up on, résumé de, qu'est-ce que j'ai raté en/au
  const missedPattern =
    /(?:what\s+did\s+i\s+miss\s+(?:in|for)|catch\s+up\s+on|qu['’]est-ce\s+que\s+j['’]ai\s+rat[ée]\s+(?:en|au)|résumé\s+de|resume\s+of|summary\s+of)\s+(?<course>[\p{L}\p{N}][\p{L}\p{N}\s'’\-&\.]*)/iu;

  const missedMatch = text.match(missedPattern);
  if (missedMatch?.groups?.course) {
    const course = missedMatch.groups.course.trim();
    if (course) {
      return { type: "missed", course, week: null };
    }
  }

  // Pattern B: "Week 2 of Algebra" / "Contenu semaine 4 Security" / "De quoi parle la semaine 3 de Physique ?"
  const specificWeekPattern =
    /(?:week|semaine)\s+(?<week>\d{1,2})\s*(?:\s+(?:of|in|de|d['’]))?\s*(?<course>[\p{L}\p{N}][\p{L}\p{N}\s'’\-&\.]*)/iu;

  const weekMatch = text.match(specificWeekPattern);
  if (weekMatch?.groups?.course && weekMatch.groups.week) {
    const course = weekMatch.groups.course.trim();
    const week = parseInt(weekMatch.groups.week, 10);
    if (course && !Number.isNaN(week)) {
      return { type: "specific_week", course, week };
    }
  }

  return null;
}

