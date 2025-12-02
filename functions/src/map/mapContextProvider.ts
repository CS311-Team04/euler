// functions/src/map/mapContextProvider.ts
// Provides map context for LLM based on detected intent

import * as logger from "firebase-functions/logger";
import {
  getAllMapData,
  formatPrintersForContext,
  formatDistributorsForContext,
  formatRestaurantsForContext,
  formatATMsForContext,
  EPFLMapData,
} from "./epflMapService";
import { detectMapIntent, MapIntentResult, MapIntentType } from "./mapIntent";

// ============================================================
// TYPES
// ============================================================

export interface MapContextResult {
  detected: boolean;
  intentType: MapIntentType | null;
  context: string;
  building?: string;
}

// ============================================================
// CONTEXT GENERATION
// ============================================================

/**
 * Generate map context based on the detected intent
 */
async function generateContextForIntent(
  intent: MapIntentResult,
  mapData: EPFLMapData
): Promise<string> {
  const building = intent.extractedBuilding;

  switch (intent.intentType) {
    case "find_printers": {
      if (building) {
        const printers = mapData.printers.filter(
          (p) => p.building === building
        );
        return formatPrintersForContext(printers, building);
      }
      // Return summary of all printers grouped by building
      return formatPrintersForContext(mapData.printers);
    }

    case "find_distributors": {
      if (building) {
        const distributors = mapData.distributors.filter(
          (d) => d.building === building || d.name.includes(building)
        );
        return formatDistributorsForContext(distributors, building);
      }
      return formatDistributorsForContext(mapData.distributors);
    }

    case "find_restaurant": {
      const allRestos = [...mapData.restaurants, ...mapData.selfservices];
      
      // Check if looking for a specific restaurant
      if (intent.extractedLocation) {
        const searchTerm = intent.extractedLocation.toLowerCase();
        const matching = allRestos.filter((r) =>
          r.name.toLowerCase().includes(searchTerm)
        );
        if (matching.length > 0) {
          return formatRestaurantsForContext(matching);
        }
      }
      
      return formatRestaurantsForContext(allRestos);
    }

    case "find_location": {
      // Provide general orientation info
      const lines: string[] = [];
      lines.push("📍 Informations de localisation EPFL:\n");
      
      // ATMs
      if (mapData.atms.length > 0) {
        lines.push(formatATMsForContext(mapData.atms));
      }
      
      return lines.join("\n\n");
    }

    case "navigation": {
      // For navigation, provide building layout context
      const lines: string[] = [];
      lines.push("🗺️ Navigation sur le campus EPFL:\n");
      lines.push("Le campus principal de l'EPFL est organisé autour de:");
      lines.push("- L'Esplanade centrale (axe principal)");
      lines.push("- Le Rolex Learning Center (sud)");
      lines.push("- L'Innovation Park (ouest)");
      lines.push("- Le SwissTech Convention Center (sud-ouest)");
      lines.push("\nBâtiments principaux:");
      lines.push("- CO/CM/CE: bâtiments centraux (Cours, Matériaux, etc.)");
      lines.push("- BC/BCH: Chimie et Sciences de base");
      lines.push("- MA: Mathématiques");
      lines.push("- INM/INF/INJ/INR: Informatique");
      lines.push("- PH: Physique");
      lines.push("- EL/ELA-ELH: Électricité");
      lines.push("- MX: Sciences des matériaux");
      
      if (building) {
        lines.push(`\nRecherche de: ${building}`);
      }
      
      return lines.join("\n");
    }

    case "general_map":
    default: {
      // Provide a general overview
      const lines: string[] = [];
      lines.push("📍 Campus EPFL - Informations générales:\n");
      lines.push(`🖨️ ${mapData.printers.length} imprimantes disponibles`);
      lines.push(`🍽️ ${mapData.restaurants.length + mapData.selfservices.length} restaurants/cafétérias`);
      lines.push(`🍫 ${mapData.distributors.length} distributeurs`);
      lines.push(`💰 ${mapData.atms.length} bancomats`);
      
      return lines.join("\n");
    }
  }
}

// ============================================================
// MAIN FUNCTION
// ============================================================

/**
 * Get map context for a user question
 * Returns context to be injected into the LLM prompt
 */
export async function getMapContext(question: string): Promise<MapContextResult> {
  // Detect if this is a map-related question
  const intent = detectMapIntent(question);

  if (!intent.detected) {
    return {
      detected: false,
      intentType: null,
      context: "",
    };
  }

  logger.info("mapContextProvider.detected", {
    intentType: intent.intentType,
    building: intent.extractedBuilding,
    location: intent.extractedLocation,
  });

  try {
    // Fetch map data
    const mapData = await getAllMapData();

    // Generate context based on intent
    const context = await generateContextForIntent(intent, mapData);

    logger.info("mapContextProvider.success", {
      intentType: intent.intentType,
      contextLen: context.length,
    });

    return {
      detected: true,
      intentType: intent.intentType,
      context,
      building: intent.extractedBuilding,
    };
  } catch (e: any) {
    logger.error("mapContextProvider.failed", { error: String(e) });
    return {
      detected: true,
      intentType: intent.intentType,
      context: "Données de la carte temporairement indisponibles.",
      building: intent.extractedBuilding,
    };
  }
}

// ============================================================
// EXPORTS
// ============================================================

export { detectMapIntent, isMapQuestion, getMapIntentType } from "./mapIntent";

