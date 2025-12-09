// functions/src/food/epflFoodScraper.ts
/**
 * EPFL Food Scraper
 * 
 * Fetches daily menu data from EPFL's restaurant website.
 * Focuses on 6 main restaurants: Alpine, Arcadie, Esplanade, Native, Ornithorynque, Piano
 */

import * as cheerio from "cheerio";

/** Target restaurants to scrape */
export const TARGET_RESTAURANTS = [
  "Alpine",
  "Arcadie", 
  "Esplanade",
  "Native-Restauration végétale - Bar à café",
  "Ornithorynque",
  "Piano",
] as const;

export type TargetRestaurant = typeof TARGET_RESTAURANTS[number];

/** Normalized restaurant names for easier matching in queries */
export const RESTAURANT_ALIASES: Record<string, TargetRestaurant> = {
  "alpine": "Alpine",
  "arcadie": "Arcadie",
  "esplanade": "Esplanade",
  "native": "Native-Restauration végétale - Bar à café",
  "native-restauration": "Native-Restauration végétale - Bar à café",
  "ornithorynque": "Ornithorynque",
  "orni": "Ornithorynque",
  "piano": "Piano",
};

/** Meal data structure */
export interface Meal {
  name: string;
  restaurant: TargetRestaurant;
  studentPrice: number | null; // Price in CHF for students (E)
  isVegetarian: boolean;
  isVegan: boolean;
  allergens: string[];
}

/** Daily menu for all restaurants */
export interface DailyMenu {
  date: string; // YYYY-MM-DD format
  dayName: string; // e.g., "lundi", "mardi"
  meals: Meal[];
}

/** Weekly menu */
export interface WeeklyMenu {
  weekStart: string; // YYYY-MM-DD of Monday
  days: DailyMenu[];
  lastSync: Date;
}

/** Base URL for the EPFL food page */
const EPFL_FOOD_URL = "https://www.epfl.ch/campus/restaurants-shops-hotels/fr/offre-du-jour-de-tous-les-points-de-restauration/";

/**
 * Fetches the HTML content for a specific date
 */
async function fetchFoodPage(date: string): Promise<string> {
  const url = `${EPFL_FOOD_URL}?date=${date}`;
  
  const response = await fetch(url, {
    headers: {
      "User-Agent": "EULER-App/1.0 (EPFL Food Scraper)",
      "Accept": "text/html",
      "Accept-Language": "fr-CH,fr;q=0.9",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch food page: ${response.status} ${response.statusText}`);
  }

  return response.text();
}

/**
 * Parse price from a string like "E 7.10 chf" or "7.10 CHF"
 * Returns the student price (E) or null if not found
 */
function parseStudentPrice(priceText: string): number | null {
  // Look for student price (E prefix)
  const studentMatch = priceText.match(/E\s*(\d+[.,]\d+)\s*(?:chf|CHF)/i);
  if (studentMatch) {
    return parseFloat(studentMatch[1].replace(",", "."));
  }
  
  // If no E prefix, try to find any price
  const anyPriceMatch = priceText.match(/(\d+[.,]\d+)\s*(?:chf|CHF)/i);
  if (anyPriceMatch) {
    return parseFloat(anyPriceMatch[1].replace(",", "."));
  }
  
  return null;
}

/**
 * Parse the HTML and extract meal data for target restaurants
 */
function parseFoodPage(html: string): Meal[] {
  const $ = cheerio.load(html);
  const meals: Meal[] = [];

  // Find the table rows in the food table
  $("table tbody tr").each((_, row) => {
    const $row = $(row);
    const cells = $row.find("td");
    
    if (cells.length < 4) return; // Skip incomplete rows

    // Extract restaurant name from the 3rd cell
    const restaurantCell = $(cells[2]);
    const restaurantName = restaurantCell.text().trim();
    
    // Check if this is one of our target restaurants
    const isTargetRestaurant = TARGET_RESTAURANTS.some(
      target => restaurantName.toLowerCase().includes(target.toLowerCase()) ||
                target.toLowerCase().includes(restaurantName.toLowerCase())
    );
    
    if (!isTargetRestaurant) return;

    // Match to exact target restaurant name
    const matchedRestaurant = TARGET_RESTAURANTS.find(
      target => restaurantName.toLowerCase().includes(target.toLowerCase()) ||
                target.toLowerCase().includes(restaurantName.toLowerCase())
    );
    
    if (!matchedRestaurant) return;

    // Extract meal name from 1st cell
    const mealCell = $(cells[0]);
    const mealName = mealCell.find("div").first().contents().filter(function() {
      return this.type === "text";
    }).text().trim() || mealCell.text().split("Allergènes")[0].split("Provenance")[0].trim();

    // Check for vegetarian/vegan tags
    const cellHtml = mealCell.html() || "";
    const isVegetarian = cellHtml.includes("végétarien") || cellHtml.includes("Vegetarian");
    const isVegan = cellHtml.includes("vegan") || cellHtml.includes("Vegan");

    // Extract allergens
    const allergens: string[] = [];
    mealCell.find("emphasis").each((_, em) => {
      const text = $(em).text().trim().toLowerCase();
      // Skip common non-allergen words
      if (text && 
          !text.includes("végétarien") && 
          !text.includes("vegan") && 
          !text.includes("provenance") &&
          !text.includes("allergènes")) {
        allergens.push(text);
      }
    });

    // Extract price from 4th cell
    const priceCell = $(cells[3]);
    const priceText = priceCell.text();
    const studentPrice = parseStudentPrice(priceText);

    // Clean up meal name
    let cleanName = mealName
      .replace(/Vegetarian|végétarien|vegan|Vegan/gi, "")
      .replace(/Allergènes?:?.*/gi, "")
      .replace(/Provenance:?.*/gi, "")
      .replace(/\s+/g, " ")
      .trim();

    // Skip empty or very short names
    if (cleanName.length < 3) return;

    meals.push({
      name: cleanName,
      restaurant: matchedRestaurant,
      studentPrice,
      isVegetarian,
      isVegan,
      allergens: [...new Set(allergens)], // Remove duplicates
    });
  });

  return meals;
}

/**
 * Get the date string in YYYY-MM-DD format (Zurich timezone)
 */
function formatDate(date: Date): string {
  // Use Zurich timezone to avoid UTC conversion issues at midnight
  return date.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' }); // en-CA gives YYYY-MM-DD format
}

/**
 * Get the day name in French (Zurich timezone)
 */
function getDayName(date: Date): string {
  return date.toLocaleDateString('fr-CH', { 
    timeZone: 'Europe/Zurich', 
    weekday: 'long' 
  }).toLowerCase();
}

/**
 * Get the Monday of the current week (Zurich timezone)
 */
function getMonday(date: Date): Date {
  // Create date in Zurich timezone
  const zurichStr = date.toLocaleDateString('en-US', { timeZone: 'Europe/Zurich' });
  const d = new Date(zurichStr);
  d.setHours(12, 0, 0, 0); // Set to noon to avoid timezone edge cases
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  d.setDate(diff);
  return d;
}

/**
 * Scrape menu for a specific date
 */
export async function scrapeDailyMenu(date: Date): Promise<DailyMenu> {
  const dateStr = formatDate(date);
  const html = await fetchFoodPage(dateStr);
  const meals = parseFoodPage(html);
  
  return {
    date: dateStr,
    dayName: getDayName(date),
    meals,
  };
}

/**
 * Scrape menu for the entire week (Monday to Friday)
 */
export async function scrapeWeeklyMenu(): Promise<WeeklyMenu> {
  const monday = getMonday(new Date());
  const days: DailyMenu[] = [];
  
  // Scrape Monday to Friday
  for (let i = 0; i < 5; i++) {
    const date = new Date(monday);
    date.setDate(monday.getDate() + i);
    
    try {
      const dailyMenu = await scrapeDailyMenu(date);
      days.push(dailyMenu);
    } catch (error) {
      console.warn(`Failed to scrape menu for ${formatDate(date)}:`, error);
      // Add empty day on failure
      days.push({
        date: formatDate(date),
        dayName: getDayName(date),
        meals: [],
      });
    }
  }
  
  return {
    weekStart: formatDate(monday),
    days,
    lastSync: new Date(),
  };
}

/**
 * Format menu data for LLM context
 */
export function formatMenuForContext(weeklyMenu: WeeklyMenu): string {
  const lines: string[] = [];
  
  lines.push("🍽️ MENUS DES RESTAURANTS EPFL (semaine courante)");
  lines.push("Restaurants: Alpine, Arcadie, Esplanade, Native, Ornithorynque, Piano\n");
  
  for (const day of weeklyMenu.days) {
    if (day.meals.length === 0) continue;
    
    const capitalizedDay = day.dayName.charAt(0).toUpperCase() + day.dayName.slice(1);
    lines.push(`📅 ${capitalizedDay} (${day.date}):`);
    
    // Group by restaurant
    const byRestaurant = new Map<string, Meal[]>();
    for (const meal of day.meals) {
      const key = meal.restaurant;
      if (!byRestaurant.has(key)) byRestaurant.set(key, []);
      byRestaurant.get(key)!.push(meal);
    }
    
    for (const [restaurant, meals] of byRestaurant) {
      lines.push(`\n  🏪 ${restaurant}:`);
      for (const meal of meals) {
        const tags: string[] = [];
        if (meal.isVegan) tags.push("🌱 vegan");
        else if (meal.isVegetarian) tags.push("🥬 végétarien");
        
        const price = meal.studentPrice 
          ? `${meal.studentPrice.toFixed(2)} CHF` 
          : "prix non disponible";
        
        const tagStr = tags.length > 0 ? ` [${tags.join(", ")}]` : "";
        lines.push(`    • ${meal.name}${tagStr} — ${price}`);
      }
    }
    
    lines.push(""); // Empty line between days
  }
  
  return lines.join("\n");
}

/**
 * Find the cheapest meal for a given day
 */
export function findCheapestMeal(dailyMenu: DailyMenu): Meal | null {
  const mealsWithPrice = dailyMenu.meals.filter(m => m.studentPrice !== null);
  if (mealsWithPrice.length === 0) return null;
  
  return mealsWithPrice.reduce((cheapest, current) => 
    (current.studentPrice! < cheapest.studentPrice!) ? current : cheapest
  );
}

/**
 * Find vegetarian/vegan meals for a given day
 */
export function findVeggieMeals(dailyMenu: DailyMenu, veganOnly: boolean = false): Meal[] {
  if (veganOnly) {
    return dailyMenu.meals.filter(m => m.isVegan);
  }
  return dailyMenu.meals.filter(m => m.isVegetarian || m.isVegan);
}

/**
 * Find meals at a specific restaurant
 */
export function findMealsByRestaurant(dailyMenu: DailyMenu, restaurant: string): Meal[] {
  const normalizedInput = restaurant.toLowerCase().trim();
  
  // Check aliases first
  const targetRestaurant = RESTAURANT_ALIASES[normalizedInput];
  if (targetRestaurant) {
    return dailyMenu.meals.filter(m => m.restaurant === targetRestaurant);
  }
  
  // Fuzzy match
  return dailyMenu.meals.filter(m => 
    m.restaurant.toLowerCase().includes(normalizedInput) ||
    normalizedInput.includes(m.restaurant.toLowerCase())
  );
}

