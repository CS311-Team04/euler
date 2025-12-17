// functions/src/features/schedule.ts

import * as logger from "firebase-functions/logger";
import admin from "firebase-admin";
import { db } from "../core/firebase";

/** Parsed calendar event from ICS */
interface ScheduleEvent {
  uid: string;
  summary: string;
  location?: string;
  description?: string;
  dtstart: string; // ISO string
  dtend: string;   // ISO string
  rrule?: string;  // recurrence rule if any
}

/** Weekly slot (template) - day of week + time */
export interface WeeklySlot {
  dayOfWeek: number; // 0=Sunday, 1=Monday, ..., 6=Saturday
  dayName: string;   // "Monday", "Tuesday", etc.
  startTime: string; // "15:15"
  endTime: string;   // "16:00"
  summary: string;
  location?: string;
  courseCode?: string;
}

/** Final exam event */
export interface FinalExam {
  date: string;      // "2026-01-17"
  startTime: string; // "08:15"
  endTime: string;   // "11:15"
  summary: string;
  location?: string;
  courseCode?: string;
}

/** Optimized schedule structure */
export interface OptimizedSchedule {
  weeklySlots: WeeklySlot[];
  finalExams: FinalExam[];
  semesterStart?: string;
  semesterEnd?: string;
}

/** Pattern to detect final exams (written or oral) */
const EXAM_PATTERN = /\b(written|oral|exam|examen|écrit|final|midterm|test)\b/i;

/** Extract course code from summary (e.g., "COM-300" from description) */
function extractCourseCode(description?: string): string | undefined {
  if (!description) return undefined;
  const match = description.match(/Course Code\n([A-Z]+-\d+)/);
  return match ? match[1] : undefined;
}

/** Convert ICS date format to ISO string with proper timezone handling */
function parseICSDate(icsDate: string, isSwissTime: boolean = false): string {
  // Handle formats: 20251127T081500Z or 20251127T081500 or 20251127
  const isUTC = icsDate.endsWith('Z');
  const clean = icsDate.replace('Z', '');
  
  if (clean.length === 8) {
    // Date only: YYYYMMDD
    const dateStr = `${clean.slice(0, 4)}-${clean.slice(4, 6)}-${clean.slice(6, 8)}T00:00:00`;
    // If Swiss time, add timezone offset; if UTC, add Z; otherwise no suffix
    return isSwissTime ? dateStr + '+01:00' : (isUTC ? dateStr + 'Z' : dateStr);
  }
  
  // DateTime: YYYYMMDDTHHMMSS
  const datePart = clean.slice(0, 8);
  const timePart = clean.slice(9, 15);
  const isoString = `${datePart.slice(0, 4)}-${datePart.slice(4, 6)}-${datePart.slice(6, 8)}T${timePart.slice(0, 2)}:${timePart.slice(2, 4)}:${timePart.slice(4, 6)}`;
  
  // If already in Swiss time (TZID=Europe/Zurich), add timezone offset
  // CET (+01:00) in winter, CEST (+02:00) in summer
  if (isSwissTime) {
    const month = parseInt(datePart.slice(4, 6));
    // DST in Switzerland: approximately April to September
    const isDST = month >= 4 && month <= 9;
    return isoString + (isDST ? '+02:00' : '+01:00');
  }
  
  // If originally UTC, add Z suffix
  return isUTC ? isoString + 'Z' : isoString;
}

/** Get day of week in Zurich timezone (0=Sunday, 6=Saturday) */
export function getZurichDayOfWeek(date: Date): number {
  const zurichStr = date.toLocaleString('en-US', { timeZone: 'Europe/Zurich', weekday: 'short' });
  const dayMap: Record<string, number> = { 'Sun': 0, 'Mon': 1, 'Tue': 2, 'Wed': 3, 'Thu': 4, 'Fri': 5, 'Sat': 6 };
  const dayAbbr = zurichStr.split(',')[0];
  return dayMap[dayAbbr] ?? date.getDay();
}

/** Get current date in Zurich timezone */
export function getZurichDate(): Date {
  // Create a date string in Zurich timezone and parse it back
  const zurichStr = new Date().toLocaleString('en-US', { timeZone: 'Europe/Zurich' });
  return new Date(zurichStr);
}

/** Parse ICS and extract optimized schedule (one week + exams) */
export function parseICSOptimized(icsText: string): OptimizedSchedule {
  const allEvents: ScheduleEvent[] = [];
  const lines = icsText.replace(/\r\n /g, '').replace(/\r\n\t/g, '').split(/\r?\n/);

  let currentEvent: Partial<ScheduleEvent> | null = null;

  for (const line of lines) {
    if (line === 'BEGIN:VEVENT') {
      currentEvent = {};
    } else if (line === 'END:VEVENT' && currentEvent) {
      if (currentEvent.uid && currentEvent.summary && currentEvent.dtstart && currentEvent.dtend) {
        allEvents.push(currentEvent as ScheduleEvent);
      }
      currentEvent = null;
    } else if (currentEvent) {
      const colonIdx = line.indexOf(':');
      if (colonIdx === -1) continue;

      const fullKey = line.slice(0, colonIdx);
      const value = line.slice(colonIdx + 1);

      // Check if the key has parameters (like TZID)
      const semicolonIdx = fullKey.indexOf(';');
      const key = semicolonIdx !== -1 ? fullKey.slice(0, semicolonIdx) : fullKey;
      const hasSwissTZ = fullKey.includes('TZID=Europe/Zurich');

      switch (key) {
        case 'UID':
          currentEvent.uid = value;
          break;
        case 'SUMMARY':
          currentEvent.summary = value.replace(/\\,/g, ',').replace(/\\n/g, '\n').replace(/\\\\/g, '\\');
          break;
        case 'LOCATION':
          currentEvent.location = value.replace(/\\,/g, ',').replace(/\\n/g, '\n').replace(/\\\\/g, '\\');
          break;
        case 'DESCRIPTION':
          currentEvent.description = value.replace(/\\,/g, ',').replace(/\\n/g, '\n').replace(/\\\\/g, '\\');
          break;
        case 'DTSTART':
          currentEvent.dtstart = parseICSDate(value, hasSwissTZ);
          break;
        case 'DTEND':
          currentEvent.dtend = parseICSDate(value, hasSwissTZ);
          break;
        case 'RRULE':
          currentEvent.rrule = value;
          break;
      }
    }
  }

  // Separate regular classes from exams
  const regularClasses: ScheduleEvent[] = [];
  const examEvents: ScheduleEvent[] = [];
  
  const now = new Date();
  const fourWeeksFromNow = new Date(now.getTime() + 28 * 24 * 60 * 60 * 1000);

  for (const event of allEvents) {
    const eventDate = new Date(event.dtstart);
    const eventEnd = new Date(event.dtend);
    const durationHours = (eventEnd.getTime() - eventDate.getTime()) / (1000 * 60 * 60);
    
    // Treat as exam if:
    // 1. Summary contains exam-related words (written, oral, exam, etc.)
    // 2. OR event is far in the future (>4 weeks) with long duration (>3 hours) - likely an exam slot
    const isExamByPattern = EXAM_PATTERN.test(event.summary);
    const isExamByDateAndDuration = eventDate > fourWeeksFromNow && durationHours > 3;
    
    if (isExamByPattern || isExamByDateAndDuration) {
      examEvents.push(event);
      logger.info("parseICSOptimized.examDetected", {
        summary: event.summary,
        date: event.dtstart,
        byPattern: isExamByPattern,
        byDateDuration: isExamByDateAndDuration,
        durationHours,
      });
    } else {
      regularClasses.push(event);
    }
  }

  // Extract ONE canonical week from regular classes
  // Use a Map to dedupe by: dayOfWeek + startTime + summary
  const weeklyMap = new Map<string, WeeklySlot>();
  const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  for (const event of regularClasses) {
    const start = new Date(event.dtstart);
    const end = new Date(event.dtend);
    
    // Get day of week in Swiss timezone
    const dayOfWeek = getZurichDayOfWeek(start);
    
    // Get times in Swiss timezone (HH:MM format)
    const startTime = start.toLocaleTimeString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      hour: '2-digit', 
      minute: '2-digit', 
      hour12: false 
    });
    const endTime = end.toLocaleTimeString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      hour: '2-digit', 
      minute: '2-digit', 
      hour12: false 
    });

    // Create unique key for deduplication
    const key = `${dayOfWeek}-${startTime}-${event.summary}`;

    if (!weeklyMap.has(key)) {
      // Only include defined fields (Firestore doesn't accept undefined)
      const slot: WeeklySlot = {
        dayOfWeek,
        dayName: dayNames[dayOfWeek],
        startTime,
        endTime,
        summary: event.summary,
      };
      if (event.location) slot.location = event.location;
      const code = extractCourseCode(event.description);
      if (code) slot.courseCode = code;
      weeklyMap.set(key, slot);
    }
  }

  // Convert to sorted array (Monday first, then by time)
  const weeklySlots = Array.from(weeklyMap.values())
    .sort((a, b) => {
      // Monday=1 should come first, Sunday=0 last
      const dayA = a.dayOfWeek === 0 ? 7 : a.dayOfWeek;
      const dayB = b.dayOfWeek === 0 ? 7 : b.dayOfWeek;
      if (dayA !== dayB) return dayA - dayB;
      return a.startTime.localeCompare(b.startTime);
    });

  // Extract final exams (dedupe by course)
  const examMap = new Map<string, FinalExam>();

  for (const event of examEvents) {
    const start = new Date(event.dtstart);
    const end = new Date(event.dtend);
    
    // Get date in Swiss timezone (YYYY-MM-DD format)
    const dateStr = start.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' });
    
    // Get times in Swiss timezone (HH:MM format)
    const startTime = start.toLocaleTimeString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      hour: '2-digit', 
      minute: '2-digit', 
      hour12: false 
    });
    const endTime = end.toLocaleTimeString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      hour: '2-digit', 
      minute: '2-digit', 
      hour12: false 
    });
    const courseCode = extractCourseCode(event.description);

    // Dedupe by summary (one exam per course)
    const key = event.summary;
    if (!examMap.has(key)) {
      // Only include defined fields (Firestore doesn't accept undefined)
      const exam: FinalExam = {
        date: dateStr,
        startTime,
        endTime,
        summary: event.summary,
      };
      if (event.location) exam.location = event.location;
      if (courseCode) exam.courseCode = courseCode;
      examMap.set(key, exam);
    }
  }

  const finalExams = Array.from(examMap.values())
    .sort((a, b) => a.date.localeCompare(b.date));

  logger.info("parseICSOptimized.result", {
    totalEvents: allEvents.length,
    uniqueWeeklySlots: weeklySlots.length,
    finalExams: finalExams.length,
  });

  return { weeklySlots, finalExams };
}

/** Format optimized schedule for LLM context */
function formatOptimizedScheduleForContext(schedule: OptimizedSchedule): string {
  const lines: string[] = [];

  // Weekly schedule template
  if (schedule.weeklySlots.length > 0) {
    lines.push("📚 WEEKLY TIMETABLE (every week):");

    let currentDay = '';
    for (const slot of schedule.weeklySlots) {
      if (slot.dayName !== currentDay) {
        currentDay = slot.dayName;
        lines.push(`\n${slot.dayName}:`);
      }
      const loc = slot.location ? ` @ ${slot.location}` : '';
      const code = slot.courseCode ? ` (${slot.courseCode})` : '';
      lines.push(`  • ${slot.startTime}–${slot.endTime}: ${slot.summary}${code}${loc}`);
    }
  }

  // Final exams - format with COURSE NAME FIRST and FULL DATE for better model matching
  if (schedule.finalExams.length > 0) {
    lines.push("\n\n📝 FINAL EXAMS (written exam dates):");
    lines.push("IMPORTANT: Always include the FULL DATE when answering about exams.\n");
    for (const exam of schedule.finalExams) {
      // Parse date carefully - exam.date is in YYYY-MM-DD format
      // Add time to avoid timezone issues: treat as noon in Zurich
      const dateObj = new Date(exam.date + 'T12:00:00');
      const dateStr = dateObj.toLocaleDateString('en-GB', {
        timeZone: 'Europe/Zurich',
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      });
      const loc = exam.location ? ` in room ${exam.location}` : '';
      // Put course name FIRST, then FULL DATE with time
      lines.push(`  • ${exam.summary}: ${dateStr}, ${exam.startTime}–${exam.endTime}${loc}`);
    }
  }

  if (lines.length === 0) {
    return "No classes or exams found.";
  }

  return lines.join('\n');
}

/** Generate schedule for a specific date range using the weekly template */
function generateScheduleForDateRange(schedule: OptimizedSchedule, startDate: Date, endDate: Date): string {
  const lines: string[] = [];

  // Group weekly slots by day
  const slotsByDay = new Map<number, WeeklySlot[]>();
  for (const slot of schedule.weeklySlots) {
    if (!slotsByDay.has(slot.dayOfWeek)) {
      slotsByDay.set(slot.dayOfWeek, []);
    }
    slotsByDay.get(slot.dayOfWeek)!.push(slot);
  }

  // Iterate through each day in range (using Zurich timezone)
  const current = new Date(startDate);
  while (current <= endDate) {
    // Use Zurich timezone for day of week calculation
    const dayOfWeek = getZurichDayOfWeek(current);
    const slots = slotsByDay.get(dayOfWeek) || [];

    // Check for exams on this date (use Zurich date string)
    const zurichDateParts = current.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' }); // YYYY-MM-DD format
    const examsToday = schedule.finalExams.filter(e => e.date === zurichDateParts);

    if (slots.length > 0 || examsToday.length > 0) {
      const dayLabel = current.toLocaleDateString('en-GB', {
        timeZone: 'Europe/Zurich',
        weekday: 'long',
        day: 'numeric',
        month: 'long'
      });
      lines.push(`\n📅 ${dayLabel.charAt(0).toUpperCase() + dayLabel.slice(1)}`);

      // Add regular classes
      for (const slot of slots) {
        const loc = slot.location ? ` @ ${slot.location}` : '';
        lines.push(`  • ${slot.startTime}–${slot.endTime}: ${slot.summary}${loc}`);
      }

      // Add exams
      for (const exam of examsToday) {
        const loc = exam.location ? ` @ ${exam.location}` : '';
        lines.push(`  • ${exam.startTime}–${exam.endTime}: 📝 ${exam.summary}${loc}`);
      }
    }

    current.setDate(current.getDate() + 1);
  }

  return lines.length > 0 ? lines.join('\n') : "No classes scheduled for this period.";
}

// Allowlist of hosts that are permitted for ICS URL fetching (SSRF protection)
const ALLOWED_ICS_HOSTS = [
  'campus.epfl.ch',
  'isa.epfl.ch',
  'isacademia.epfl.ch',
  'edu.epfl.ch',
  'moodle.epfl.ch',
];

function isAllowedIcsHost(urlString: string): boolean {
  try {
    const parsed = new URL(urlString);
    const host = parsed.hostname.toLowerCase();
    return ALLOWED_ICS_HOSTS.some(allowed =>
      host === allowed || host.endsWith('.' + allowed)
    );
  } catch {
    return false;
  }
}

/** Sync user's EPFL schedule from ICS URL */
export type SyncScheduleInput = { icsUrl: string };

export async function syncEpflScheduleCore(uid: string, { icsUrl }: SyncScheduleInput) {
  // Validate URL
  if (!icsUrl || typeof icsUrl !== 'string') {
    throw new Error("Missing or invalid 'icsUrl'");
  }

  // Basic URL validation - should look like an EPFL/IS-Academia ICS URL
  const url = icsUrl.trim();
  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    throw new Error("Invalid URL format");
  }

  // SSRF protection: Only allow fetching from known EPFL hosts
  if (!isAllowedIcsHost(url)) {
    logger.warn("syncEpflSchedule.rejectedHost", { uid, urlPreview: url.slice(0, 60) });
    throw new Error("URL must be from an EPFL domain (campus.epfl.ch, isa.epfl.ch, etc.)");
  }

  // Fetch the ICS file
  logger.info("syncEpflSchedule.fetching", { uid, urlPreview: url.slice(0, 60) });

  const response = await fetch(url, {
    headers: {
      'User-Agent': 'EULER-App/1.0',
      'Accept': 'text/calendar, */*',
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch ICS: ${response.status} ${response.statusText}`);
  }

  const icsText = await response.text();

  if (!icsText.includes('BEGIN:VCALENDAR')) {
    throw new Error("Invalid ICS format: not a valid calendar file");
  }

  // Parse with OPTIMIZED parser (one week + exams only)
  const schedule = parseICSOptimized(icsText);

  logger.info("syncEpflSchedule.parsed", {
    uid,
    weeklySlots: schedule.weeklySlots.length,
    finalExams: schedule.finalExams.length,
    // Log each slot's day for debugging
    slotDetails: schedule.weeklySlots.map(s => ({
      dayOfWeek: s.dayOfWeek,
      dayName: s.dayName,
      time: s.startTime,
      summary: s.summary.substring(0, 30),
    })),
    examDetails: schedule.finalExams.map(e => ({
      date: e.date,
      summary: e.summary.substring(0, 30),
    })),
  });

  // Store OPTIMIZED structure in Firestore (much smaller!)
  const userRef = db.collection('users').doc(uid);
  await userRef.set({
    epflSchedule: {
      icsUrl: url,
      weeklySlots: schedule.weeklySlots,
      finalExams: schedule.finalExams,
      lastSync: admin.firestore.FieldValue.serverTimestamp(),
      slotCount: schedule.weeklySlots.length,
      examCount: schedule.finalExams.length,
    }
  }, { merge: true });

  logger.info("syncEpflSchedule.stored", {
    uid,
    weeklySlots: schedule.weeklySlots.length,
    finalExams: schedule.finalExams.length,
  });

  return {
    success: true,
    eventCount: schedule.weeklySlots.length + schedule.finalExams.length,
    weeklySlots: schedule.weeklySlots.length,
    finalExams: schedule.finalExams.length,
    message: `Synced ${schedule.weeklySlots.length} weekly classes and ${schedule.finalExams.length} final exams.`,
  };
}

/** Get user's schedule context for LLM */
export async function getScheduleContextCore(uid: string): Promise<string> {
  const userDoc = await db.collection('users').doc(uid).get();
  const data = userDoc.data();

  if (!data?.epflSchedule) {
    return "";
  }

  // Check for new optimized format
  if (data.epflSchedule.weeklySlots) {
    const schedule: OptimizedSchedule = {
      weeklySlots: data.epflSchedule.weeklySlots || [],
      finalExams: data.epflSchedule.finalExams || [],
    };

    // For context, show the weekly template + upcoming week + exams
    // Use Zurich timezone for "now"
    const now = getZurichDate();
    const nextWeek = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    
    // Get today's info in Swiss timezone
    const todayStr = now.toLocaleDateString('en-US', {
      timeZone: 'Europe/Zurich',
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
    const todayDayOfWeek = getZurichDayOfWeek(now);
    const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

    const lines: string[] = [];
    
    // CRITICAL: Put today's schedule FIRST and prominently
    lines.push(`🚨 TODAY IS: ${todayStr} (${dayNames[todayDayOfWeek]})`);
    lines.push(`When the user asks about "today", answer with ${dayNames[todayDayOfWeek]}'s classes ONLY.\n`);
    
    // Show TODAY's classes first (most important)
    const todaySlots = schedule.weeklySlots.filter(s => s.dayOfWeek === todayDayOfWeek);
    if (todaySlots.length > 0) {
      lines.push(`📅 TODAY'S CLASSES (${dayNames[todayDayOfWeek]}):`);
      for (const slot of todaySlots) {
        const loc = slot.location ? ` @ ${slot.location}` : '';
        const code = slot.courseCode ? ` (${slot.courseCode})` : '';
        lines.push(`  • ${slot.startTime}–${slot.endTime}: ${slot.summary}${code}${loc}`);
      }
      lines.push("");
    } else {
      lines.push(`📅 TODAY (${dayNames[todayDayOfWeek]}): No classes scheduled.\n`);
    }

    // Show EXAMS prominently with FULL DATES (critical for exam questions)
    if (schedule.finalExams.length > 0) {
      lines.push(`\n🎓 YOUR FINAL EXAMS (ALWAYS include full date in answers):`);
      for (const exam of schedule.finalExams) {
        // Parse date carefully - exam.date is in YYYY-MM-DD format
        const dateObj = new Date(exam.date + 'T12:00:00');
        const fullDateStr = dateObj.toLocaleDateString('en-GB', {
          timeZone: 'Europe/Zurich',
          weekday: 'long',
          day: 'numeric',
          month: 'long',
          year: 'numeric'
        });
        const loc = exam.location ? ` in room ${exam.location}` : '';
        lines.push(`  • ${exam.summary}: ${fullDateStr}, ${exam.startTime}–${exam.endTime}${loc}`);
      }
      lines.push("");
    }

    // Then show the full weekly template
    lines.push(formatOptimizedScheduleForContext(schedule));

    // Also show specific dates for the next 7 days
    lines.push("\n\n📆 THIS WEEK (specific dates):");
    lines.push(generateScheduleForDateRange(schedule, now, nextWeek));
    
    // Log for debugging
    logger.info("getScheduleContextCore.todayInfo", {
      uid,
      todayStr,
      todayDayOfWeek,
      dayName: dayNames[todayDayOfWeek],
      weeklySlotDays: schedule.weeklySlots.map(s => ({ day: s.dayOfWeek, name: s.dayName, summary: s.summary })),
    });

    return lines.join('\n');
  }

  // Fallback for old format (legacy)
  if (data.epflSchedule.events && data.epflSchedule.events.length > 0) {
    // Old format - just show a message to re-sync
    return "Schedule available. For better performance, please reconnect your EPFL Campus calendar.";
  }

  return "";
}

