// functions/src/map/epflMapService.ts
// EPFL Campus Map WFS Service - Fetches and parses POI data from plan.epfl.ch

import * as logger from "firebase-functions/logger";

// ============================================================
// TYPES
// ============================================================

/** Base interface for all POIs */
export interface EPFLMapPOI {
  id: string;
  type: string;
  name: string;
  floor: number;
  coordinates: { x: number; y: number }; // Swiss coordinates (EPSG:2056)
  building?: string; // Extracted from location (e.g., "BC" from "BC 094.6")
  room?: string; // Full room reference (e.g., "BC 094.6")
}

/** Printer POI */
export interface EPFLPrinter extends EPFLMapPOI {
  type: "printer";
  model: string;
  serial: string;
  printName: string;
}

/** Restaurant/Cafeteria POI */
export interface EPFLRestaurant extends EPFLMapPOI {
  type: "restaurant";
  url?: string;
}

/** Selfservice/Cafeteria POI */
export interface EPFLSelfservice extends EPFLMapPOI {
  type: "selfservice";
  url?: string;
}

/** Food distributor POI */
export interface EPFLFoodDistributor extends EPFLMapPOI {
  type: "distributor";
  url?: string;
}

/** ATM/Bancomat POI */
export interface EPFLATM extends EPFLMapPOI {
  type: "atm";
}

/** Building POI (from search) */
export interface EPFLBuilding {
  id: string;
  type: "building";
  name: string;
  label: string;
  coordinates: { x: number; y: number };
}

/** Union type for all POIs */
export type AnyEPFLPOI = EPFLPrinter | EPFLRestaurant | EPFLSelfservice | EPFLFoodDistributor | EPFLATM;

// ============================================================
// CONSTANTS
// ============================================================

const WFS_BASE_URL = "https://plan.epfl.ch/mapserv_proxy";
const SEARCH_BASE_URL = "https://plan.epfl.ch/search";

/** WFS layer names mapped to POI types */
export const LAYER_MAPPING = {
  imprimantes: "printer",
  restaurants: "restaurant",
  selfservices: "selfservice",
  distributeurs_nourriture: "distributor",
  bancomats: "atm",
} as const;

// ============================================================
// XML PARSING UTILITIES
// ============================================================

/** Simple regex-based XML parser for WFS GML responses */
function parseGMLValue(xml: string, tag: string): string | null {
  const regex = new RegExp(`<ms:${tag}>(.*?)</ms:${tag}>`, "s");
  const match = xml.match(regex);
  if (match && match[1]) {
    // Clean up HTML entities and escaped characters
    return match[1]
      .replace(/<[^>]+>/g, "") // Remove HTML tags
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/&amp;/g, "&")
      .replace(/&quot;/g, '"')
      .trim();
  }
  return null;
}

/** Extract coordinates from GML Point */
function parseGMLPoint(xml: string): { x: number; y: number } | null {
  const posMatch = xml.match(/<gml:pos>([0-9.]+)\s+([0-9.]+)<\/gml:pos>/);
  if (posMatch) {
    return {
      x: parseFloat(posMatch[1]),
      y: parseFloat(posMatch[2]),
    };
  }
  return null;
}

/** Extract building code from room reference (e.g., "BC 094.6" -> "BC") */
function extractBuilding(location: string | null): string | undefined {
  if (!location) return undefined;
  // Match pattern: LETTERS + space + numbers (like "BC 094" or "MXF 094.0")
  const match = location.match(/^([A-Z]+)\s/);
  return match ? match[1] : undefined;
}

/** Parse a single feature from GML */
function parseFeatureMember(memberXml: string, layerName: string): AnyEPFLPOI | null {
  // Extract feature ID
  const idMatch = memberXml.match(/gml:id="([^"]+)"/);
  const id = idMatch ? idMatch[1] : `unknown-${Date.now()}`;

  // Extract coordinates
  const coords = parseGMLPoint(memberXml);
  if (!coords) return null;

  const poiType = LAYER_MAPPING[layerName as keyof typeof LAYER_MAPPING];
  if (!poiType) return null;

  // Parse based on layer type
  switch (layerName) {
    case "imprimantes": {
      const location = parseGMLValue(memberXml, "print_location");
      const model = parseGMLValue(memberXml, "print_model") || "Unknown";
      const serial = parseGMLValue(memberXml, "print_serial") || "";
      const printName = parseGMLValue(memberXml, "print_name") || "";
      const floor = parseInt(parseGMLValue(memberXml, "floor") || "0", 10);

      // Extract room from location (might contain HTML link)
      let room = location;
      const roomMatch = location?.match(/q==([^"]+)/);
      if (roomMatch) {
        room = roomMatch[1];
      }

      return {
        id,
        type: "printer",
        name: `Imprimante ${printName || room}`,
        floor,
        coordinates: coords,
        building: extractBuilding(room),
        room: room || undefined,
        model,
        serial,
        printName,
      } as EPFLPrinter;
    }

    case "restaurants":
    case "selfservices": {
      const name = parseGMLValue(memberXml, "nom") || "Unknown";
      const floor = parseInt(parseGMLValue(memberXml, "etage") || "0", 10);
      const url = parseGMLValue(memberXml, "url") || undefined;

      return {
        id,
        type: layerName === "restaurants" ? "restaurant" : "selfservice",
        name,
        floor,
        coordinates: coords,
        url,
      } as EPFLRestaurant | EPFLSelfservice;
    }

    case "distributeurs_nourriture": {
      const name = parseGMLValue(memberXml, "nom") || "Distributeur";
      const floor = parseInt(parseGMLValue(memberXml, "etage") || "0", 10);
      const url = parseGMLValue(memberXml, "url") || undefined;

      return {
        id,
        type: "distributor",
        name,
        floor,
        coordinates: coords,
        url,
      } as EPFLFoodDistributor;
    }

    case "bancomats": {
      const name = parseGMLValue(memberXml, "nom") || "Bancomat";
      const floor = parseInt(parseGMLValue(memberXml, "etage") || "0", 10);

      return {
        id,
        type: "atm",
        name,
        floor,
        coordinates: coords,
      } as EPFLATM;
    }

    default:
      return null;
  }
}

// ============================================================
// WFS API
// ============================================================

/** Fetch all POIs of a given layer type */
export async function fetchWFSLayer(layerName: keyof typeof LAYER_MAPPING): Promise<AnyEPFLPOI[]> {
  const url = `${WFS_BASE_URL}?ogcserver=MapServer&SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&TYPENAME=${layerName}`;

  logger.info("epflMapService.fetchWFSLayer.start", { layerName, url: url.slice(0, 80) });

  const response = await fetch(url, {
    headers: {
      "User-Agent": "EULER-App/1.0",
      Accept: "application/xml, text/xml, */*",
    },
  });

  if (!response.ok) {
    logger.error("epflMapService.fetchWFSLayer.failed", {
      layerName,
      status: response.status,
    });
    throw new Error(`WFS request failed: ${response.status}`);
  }

  const xmlText = await response.text();

  // Split by featureMember and parse each one
  const featureMembers = xmlText.split(/<gml:featureMember>/);
  const pois: AnyEPFLPOI[] = [];

  for (let i = 1; i < featureMembers.length; i++) {
    const memberXml = featureMembers[i];
    const poi = parseFeatureMember(memberXml, layerName);
    if (poi) {
      pois.push(poi);
    }
  }

  logger.info("epflMapService.fetchWFSLayer.success", {
    layerName,
    count: pois.length,
  });

  return pois;
}

// ============================================================
// SEARCH API
// ============================================================

/** Search for buildings, rooms, or people */
export async function searchEPFL(query: string): Promise<EPFLBuilding[]> {
  const url = `${SEARCH_BASE_URL}?limit=10&partitionlimit=5&interface=main&routing=validated&query=${encodeURIComponent(query)}*`;

  logger.info("epflMapService.search.start", { query });

  const response = await fetch(url, {
    headers: {
      "User-Agent": "EULER-App/1.0",
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    logger.error("epflMapService.search.failed", {
      query,
      status: response.status,
    });
    throw new Error(`Search request failed: ${response.status}`);
  }

  const data = await response.json();
  const buildings: EPFLBuilding[] = [];

  if (data.features && Array.isArray(data.features)) {
    for (const feature of data.features) {
      if (feature.properties?.layer_name === "batiments") {
        // Extract centroid from polygon (simplified)
        let coords = { x: 0, y: 0 };
        if (feature.geometry?.coordinates?.[0]?.[0]?.[0]) {
          // Get first point of polygon as approximate location
          const firstPoint = feature.geometry.coordinates[0][0][0];
          coords = { x: firstPoint[0], y: firstPoint[1] };
        }

        buildings.push({
          id: feature.id,
          type: "building",
          name: feature.properties.label,
          label: feature.properties.label,
          coordinates: coords,
        });
      }
    }
  }

  logger.info("epflMapService.search.success", {
    query,
    buildingsFound: buildings.length,
  });

  return buildings;
}

// ============================================================
// HIGH-LEVEL DATA ACCESS
// ============================================================

export interface EPFLMapData {
  printers: EPFLPrinter[];
  restaurants: EPFLRestaurant[];
  selfservices: EPFLSelfservice[];
  distributors: EPFLFoodDistributor[];
  atms: EPFLATM[];
  lastFetched: Date;
}

// Simple in-memory cache with 5-minute TTL
let cachedMapData: EPFLMapData | null = null;
let cacheTimestamp: number = 0;
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

/** Fetch all POI data (with caching) */
export async function getAllMapData(): Promise<EPFLMapData> {
  const now = Date.now();

  // Return cached data if still valid
  if (cachedMapData && now - cacheTimestamp < CACHE_TTL_MS) {
    logger.info("epflMapService.getAllMapData.cached");
    return cachedMapData;
  }

  logger.info("epflMapService.getAllMapData.fetching");

  // Fetch all layers in parallel
  const [printers, restaurants, selfservices, distributors, atms] = await Promise.all([
    fetchWFSLayer("imprimantes").catch((e) => {
      logger.error("epflMapService.getAllMapData.printers.failed", { error: String(e) });
      return [];
    }),
    fetchWFSLayer("restaurants").catch((e) => {
      logger.error("epflMapService.getAllMapData.restaurants.failed", { error: String(e) });
      return [];
    }),
    fetchWFSLayer("selfservices").catch((e) => {
      logger.error("epflMapService.getAllMapData.selfservices.failed", { error: String(e) });
      return [];
    }),
    fetchWFSLayer("distributeurs_nourriture").catch((e) => {
      logger.error("epflMapService.getAllMapData.distributors.failed", { error: String(e) });
      return [];
    }),
    fetchWFSLayer("bancomats").catch((e) => {
      logger.error("epflMapService.getAllMapData.atms.failed", { error: String(e) });
      return [];
    }),
  ]);

  cachedMapData = {
    printers: printers as EPFLPrinter[],
    restaurants: restaurants as EPFLRestaurant[],
    selfservices: selfservices as EPFLSelfservice[],
    distributors: distributors as EPFLFoodDistributor[],
    atms: atms as EPFLATM[],
    lastFetched: new Date(),
  };
  cacheTimestamp = now;

  logger.info("epflMapService.getAllMapData.success", {
    printers: cachedMapData.printers.length,
    restaurants: cachedMapData.restaurants.length,
    selfservices: cachedMapData.selfservices.length,
    distributors: cachedMapData.distributors.length,
    atms: cachedMapData.atms.length,
  });

  return cachedMapData;
}

// ============================================================
// QUERY HELPERS
// ============================================================

/** Filter POIs by building code */
export function filterByBuilding<T extends AnyEPFLPOI>(pois: T[], building: string): T[] {
  const upperBuilding = building.toUpperCase();
  return pois.filter((poi) => poi.building === upperBuilding);
}

/** Count POIs in a specific building */
export function countInBuilding(pois: AnyEPFLPOI[], building: string): number {
  return filterByBuilding(pois, building).length;
}

/** Get printers in a building */
export async function getPrintersInBuilding(building: string): Promise<EPFLPrinter[]> {
  const data = await getAllMapData();
  return filterByBuilding(data.printers, building);
}

/** Get distributors in a building */
export async function getDistributorsInBuilding(building: string): Promise<EPFLFoodDistributor[]> {
  const data = await getAllMapData();
  return filterByBuilding(data.distributors, building);
}

/** Get all restaurants and cafeterias */
export async function getAllRestaurants(): Promise<(EPFLRestaurant | EPFLSelfservice)[]> {
  const data = await getAllMapData();
  return [...data.restaurants, ...data.selfservices];
}

// ============================================================
// FORMATTING FOR LLM CONTEXT
// ============================================================

/** Format printer list for LLM */
export function formatPrintersForContext(printers: EPFLPrinter[], building?: string): string {
  if (printers.length === 0) {
    return building
      ? `Aucune imprimante trouvée dans le bâtiment ${building}.`
      : "Aucune imprimante trouvée.";
  }

  const lines: string[] = [];
  const header = building
    ? `🖨️ Imprimantes dans ${building} (${printers.length}):`
    : `🖨️ Imprimantes EPFL (${printers.length}):`;
  lines.push(header);

  // Group by building for better readability
  const byBuilding = new Map<string, EPFLPrinter[]>();
  for (const printer of printers) {
    const bldg = printer.building || "Autre";
    if (!byBuilding.has(bldg)) {
      byBuilding.set(bldg, []);
    }
    byBuilding.get(bldg)!.push(printer);
  }

  for (const [bldg, bldgPrinters] of byBuilding) {
    if (!building) lines.push(`\n${bldg}:`);
    for (const p of bldgPrinters) {
      const loc = p.room || `Étage ${p.floor}`;
      lines.push(`  • ${loc} - ${p.model}`);
    }
  }

  return lines.join("\n");
}

/** Format distributors for LLM */
export function formatDistributorsForContext(distributors: EPFLFoodDistributor[], building?: string): string {
  if (distributors.length === 0) {
    return building
      ? `Aucun distributeur trouvé dans le bâtiment ${building}.`
      : "Aucun distributeur trouvé.";
  }

  const lines: string[] = [];
  const header = building
    ? `🍫 Distributeurs dans ${building} (${distributors.length}):`
    : `🍫 Distributeurs EPFL (${distributors.length}):`;
  lines.push(header);

  for (const d of distributors) {
    lines.push(`  • ${d.name} (étage ${d.floor})`);
  }

  return lines.join("\n");
}

/** Format restaurants for LLM */
export function formatRestaurantsForContext(
  restaurants: (EPFLRestaurant | EPFLSelfservice)[]
): string {
  if (restaurants.length === 0) {
    return "Aucun restaurant trouvé.";
  }

  const lines: string[] = [];
  lines.push(`🍽️ Restaurants et cafétérias EPFL (${restaurants.length}):`);

  const restos = restaurants.filter((r) => r.type === "restaurant");
  const selfs = restaurants.filter((r) => r.type === "selfservice");

  if (restos.length > 0) {
    lines.push("\nRestaurants:");
    for (const r of restos) {
      lines.push(`  • ${r.name}`);
    }
  }

  if (selfs.length > 0) {
    lines.push("\nSelf-services:");
    for (const s of selfs) {
      lines.push(`  • ${s.name}`);
    }
  }

  return lines.join("\n");
}

/** Format ATMs for LLM */
export function formatATMsForContext(atms: EPFLATM[]): string {
  if (atms.length === 0) {
    return "Aucun bancomat trouvé.";
  }

  const lines: string[] = [];
  lines.push(`💰 Bancomats EPFL (${atms.length}):`);

  for (const a of atms) {
    lines.push(`  • ${a.name} (étage ${a.floor})`);
  }

  return lines.join("\n");
}

