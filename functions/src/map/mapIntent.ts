// functions/src/map/mapIntent.ts
// Intent detection for EPFL Map/Orientation questions

import { IntentConfig, detectIntent, IntentDetectionResult } from "../intentDetection";

// ============================================================
// MAP INTENT TYPES
// ============================================================

/**
 * Map intent types that can be detected
 */
export type MapIntentType =
  | "find_printers"      // "Y'a des imprimantes au BC?"
  | "find_distributors"  // "Y'a combien de distributeurs dans le CM?"
  | "find_restaurant"    // "C'est où Zaha?"
  | "find_location"      // "Où est le Rolex Learning Center?"
  | "navigation"         // "Comment aller du CO au Rolex?"
  | "general_map";       // General map/orientation question

/**
 * Result of map intent detection with additional context
 */
export interface MapIntentResult extends IntentDetectionResult {
  intentType: MapIntentType | null;
  extractedBuilding?: string;  // e.g., "BC" from "imprimantes au BC"
  extractedLocation?: string;  // e.g., "Rolex" from "où est le Rolex"
}

// ============================================================
// BUILDING PATTERNS
// ============================================================

// EPFL building codes (most common ones)
const BUILDING_CODES = [
  "AA", "AAB", "AAC", "AI", "AL", "ANT",
  "BC", "BCH", "BCV", "BM", "BS", "BSP",
  "CE", "CH", "CM", "CO", "CPLS", "CRAFT",
  "DACH", "DIA",
  "EL", "ELA", "ELB", "ELC", "ELD", "ELE", "ELG", "ELH",
  "GA", "GC", "GR", 
  "INF", "INJ", "INM", "INR",
  "MA", "MED", "ME", "MXC", "MXD", "MXF", "MXG", "MXH",
  "ODY", "OSCO",
  "PH", "PO", "PPB", "PPH",
  "RLC", "ROLEX",
  "SG", "SSH", "STCC", "SV",
  "TCV", 
];

// Pattern to match building codes in text
const buildingPattern = BUILDING_CODES.join("|");

// ============================================================
// INTENT CONFIGURATIONS
// ============================================================

/**
 * Block patterns - general EPFL info questions (not map-specific)
 */
const MAP_BLOCK_PATTERNS: RegExp[] = [
  // Academic questions
  /\b(inscription|admission|programme|cours|master|bachelor|doctorat|phd|examen|notes?|résultat)\b/i,
  // Schedule questions (handled by schedule feature)
  /\b(horaire|planning|agenda|calendrier|quand|heure)\b/i,
  // Administrative questions
  /\b(délai|deadline|date limite|frais|tuition|bourse|scholarship)\b/i,
];

/**
 * Registry of map intents
 */
export const MAP_INTENT_CONFIGS: IntentConfig[] = [
  // ===== FIND PRINTERS =====
  {
    id: "find_printers",
    matchPatterns: [
      /\b(imprimante[s]?|printer[s]?)\b/i,
      /\bimprimer\b.*\b(où|ou)\b/i,
      /\b(où|ou)\b.*\bimprimer\b/i,
      /\bprint(er)?\b/i,
    ],
    blockPatterns: MAP_BLOCK_PATTERNS,
  },

  // ===== FIND DISTRIBUTORS =====
  {
    id: "find_distributors",
    matchPatterns: [
      /\b(distributeur[s]?|vending|machine[s]?\s+(à|a)\s+(café|boissons?|snacks?))\b/i,
      /\b(combien|nombre)\b.*\bdistributeur/i,
      /\bdistributeur.*\b(combien|nombre)\b/i,
      /\bsnack[s]?\b.*\bmachine/i,
      /\blocalomat\b/i,
    ],
    blockPatterns: MAP_BLOCK_PATTERNS,
  },

  // ===== FIND RESTAURANT/CAFETERIA =====
  {
    id: "find_restaurant",
    matchPatterns: [
      /\b(restaurant[s]?|resto[s]?|cafét[ée]ria[s]?|caf[ée]t?[s]?|self[s]?|manger|déjeuner|lunch)\b/i,
      /\b(zaha|parmentier|vinci|vallotton|giacometti|table\s+de\s+vallotton)\b/i,
      /\b(montreux\s+jazz|osteria|le\s+corbusier|rooftop)\b/i,
      /\b(où|ou)\s+(peut[- ]on|on\s+peut)\s+manger\b/i,
      /\b(food|bouffe|nourriture)\b.*\b(où|ou)\b/i,
    ],
    blockPatterns: MAP_BLOCK_PATTERNS,
  },

  // ===== FIND SPECIFIC LOCATION =====
  {
    id: "find_location",
    matchPatterns: [
      /\b(où|ou)\s+(est|se\s+trouve|trouver?)\b/i,
      /\bc['']?est\s+où\b/i,
      /\b(localiser?|situer?|position)\b/i,
      /\b(rolex|innovation\s+park|swisstech|pavilion[s]?|epfl\s+pavilion)\b/i,
      /\b(bancomat[s]?|atm|distributeur\s+(de\s+)?billets?)\b/i,
      /\b(guichet\s+(étudiant[s]?|camipro|service))\b/i,
      /\b(bibliothèque|library)\b/i,
      /\b(point\s+santé|infirmerie)\b/i,
      /\btrouver\s+(le|la|les|un|une)\b/i,
    ],
    blockPatterns: MAP_BLOCK_PATTERNS,
  },

  // ===== NAVIGATION =====
  {
    id: "navigation",
    matchPatterns: [
      /\b(comment|how)\s+(aller|go|rendre|me\s+rendre)\b/i,
      /\b(aller|rendre)\s+(à|au|à\s+la|aux?|vers|du|de\s+la|des?)\b/i,
      /\b(depuis|from|vers|to|direction)\b.*\b(vers|to|au|à|jusqu['']?[aà]?)\b/i,
      /\b(chemin|route|trajet|itinéraire|path)\b/i,
      /\bje\s+suis\s+(au?|dans|en)\b.*\b(aller|rendre)\b/i,
      /\bdepuis\s+(le|la|l[''])\b.*\bjusqu['']?[aà]?\b/i,
    ],
    blockPatterns: MAP_BLOCK_PATTERNS,
  },

  // ===== GENERAL MAP QUERY =====
  {
    id: "general_map",
    matchPatterns: [
      /\b(plan|map|carte)\s+(epfl|campus|du\s+campus)\b/i,
      /\b(orientation|naviguer|localisation)\s+(sur\s+le\s+)?campus\b/i,
      new RegExp(`\\b(${buildingPattern})\\s+\\d{3}`, "i"), // Building + room number (e.g., "BC 094")
    ],
    blockPatterns: MAP_BLOCK_PATTERNS,
  },
];

// ============================================================
// DETECTION FUNCTIONS
// ============================================================

/**
 * Detect map-related intent in user message
 */
export function detectMapIntent(question: string): MapIntentResult {
  const result = detectIntent(question, MAP_INTENT_CONFIGS);

  // Extract building code if present
  const buildingMatch = question.toUpperCase().match(new RegExp(`\\b(${buildingPattern})\\b`));
  const extractedBuilding = buildingMatch ? buildingMatch[1] : undefined;

  // Extract location names for restaurants
  const locationPatterns = [
    /\b(zaha|arcadie|parmentier|vinci|esplanade|vallotton|giacometti|montreux|osteria|corbusier|rooftop)\b/i,
    /\b(rolex|innovation\s+park|swisstech|pavilion)\b/i,
  ];
  let extractedLocation: string | undefined;
  for (const pattern of locationPatterns) {
    const match = question.match(pattern);
    if (match) {
      extractedLocation = match[1];
      break;
    }
  }

  return {
    ...result,
    intentType: result.intentId as MapIntentType | null,
    extractedBuilding,
    extractedLocation,
  };
}

/**
 * Check if question is map-related
 */
export function isMapQuestion(question: string): boolean {
  return detectMapIntent(question).detected;
}

/**
 * Get the specific map intent type
 */
export function getMapIntentType(question: string): MapIntentType | null {
  return detectMapIntent(question).intentType;
}

