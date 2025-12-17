// functions/src/features/food.ts

import * as logger from "firebase-functions/logger";
import admin from "firebase-admin";
import { db } from "../core/firebase";
import {
  scrapeWeeklyMenu,
  formatMenuForContext,
  findCheapestMeal,
  findVeggieMeals,
  findMealsByRestaurant,
  type DailyMenu,
  type Meal,
  TARGET_RESTAURANTS,
} from "../food/epflFoodScraper";

/**
 * Get the current week's Monday date string (Zurich timezone)
 */
function getCurrentWeekStart(): string {
  const zurichStr = new Date().toLocaleDateString('en-US', { timeZone: 'Europe/Zurich' });
  const now = new Date(zurichStr);
  now.setHours(12, 0, 0, 0); // Avoid timezone edge cases
  const day = now.getDay();
  const diff = now.getDate() - day + (day === 0 ? -6 : 1);
  const monday = new Date(now);
  monday.setDate(diff);
  return monday.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' }); // YYYY-MM-DD
}

/**
 * Get today's date string in YYYY-MM-DD (Zurich timezone)
 */
function getTodayDateStr(): string {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' });
}

/**
 * Get tomorrow's date string in YYYY-MM-DD (Zurich timezone)
 */
function getTomorrowDateStr(): string {
  const now = new Date();
  const zurichStr = now.toLocaleDateString('en-US', { timeZone: 'Europe/Zurich' });
  const tomorrow = new Date(zurichStr);
  tomorrow.setDate(tomorrow.getDate() + 1);
  return tomorrow.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' });
}

/**
 * Sync weekly food menus from EPFL website
 * Stores in a global collection (not per-user since menus are the same for everyone)
 */
export async function syncEpflFoodMenusCore(): Promise<{ success: boolean; mealCount: number; days: number }> {
  logger.info("syncEpflFoodMenus.starting");
  
  const weeklyMenu = await scrapeWeeklyMenu();
  
  const totalMeals = weeklyMenu.days.reduce((sum, day) => sum + day.meals.length, 0);
  
  // Store in Firestore - global collection
  const menuRef = db.collection('epflMenus').doc(weeklyMenu.weekStart);
  await menuRef.set({
    weekStart: weeklyMenu.weekStart,
    days: weeklyMenu.days.map(day => ({
      date: day.date,
      dayName: day.dayName,
      meals: day.meals.map(meal => ({
        name: meal.name,
        restaurant: meal.restaurant,
        studentPrice: meal.studentPrice,
        isVegetarian: meal.isVegetarian,
        isVegan: meal.isVegan,
        allergens: meal.allergens,
      })),
    })),
    lastSync: admin.firestore.FieldValue.serverTimestamp(),
    mealCount: totalMeals,
  });
  
  logger.info("syncEpflFoodMenus.success", { 
    weekStart: weeklyMenu.weekStart,
    mealCount: totalMeals,
    days: weeklyMenu.days.length,
  });
  
  return {
    success: true,
    mealCount: totalMeals,
    days: weeklyMenu.days.length,
  };
}

/**
 * Retrieve food menu context for LLM
 * Returns formatted string for the current week
 */
export async function getFoodContextCore(): Promise<string> {
  const weekStart = getCurrentWeekStart();
  const menuDoc = await db.collection('epflMenus').doc(weekStart).get();
  
  if (!menuDoc.exists) {
    // Try to sync if no data exists
    try {
      await syncEpflFoodMenusCore();
      const newDoc = await db.collection('epflMenus').doc(weekStart).get();
      if (newDoc.exists) {
        const data = newDoc.data() as any;
        return formatMenuForContext(data);
      }
    } catch (e) {
      logger.warn("getFoodContext.syncFailed", { error: String(e) });
    }
    return "No menu available for this week. Data has not been synced yet.";
  }
  
  const data = menuDoc.data() as any;
  return formatMenuForContext(data);
}

/**
 * Get meals for a specific day (today, tomorrow, or a date string)
 */
export async function getMealsForDay(daySpecifier: "today" | "tomorrow" | string): Promise<DailyMenu | null> {
  const weekStart = getCurrentWeekStart();
  const menuDoc = await db.collection('epflMenus').doc(weekStart).get();
  
  if (!menuDoc.exists) {
    // Try to sync
    try {
      await syncEpflFoodMenusCore();
    } catch (e) {
      return null;
    }
    const newDoc = await db.collection('epflMenus').doc(weekStart).get();
    if (!newDoc.exists) return null;
  }
  
  const data = menuDoc.data() as any;
  
  let targetDate: string;
  if (daySpecifier === "today") {
    targetDate = getTodayDateStr();
  } else if (daySpecifier === "tomorrow") {
    targetDate = getTomorrowDateStr();
  } else {
    targetDate = daySpecifier;
  }
  
  const dayData = data.days?.find((d: any) => d.date === targetDate);
  if (!dayData) return null;
  
  return {
    date: dayData.date,
    dayName: dayData.dayName,
    meals: dayData.meals || [],
  };
}

/**
 * Detect which day the user is asking about from their question
 * Returns: { dayIndex: 0-4 for Mon-Fri, dayLabel: localized label, isEnglish: language flag }
 */
function detectDayFromQuestion(q: string): { dayIndex: number | null; dayLabel: string; isEnglish: boolean } {
  const today = new Date();
  const zurichStr = today.toLocaleDateString('en-US', { timeZone: 'Europe/Zurich' });
  const zurichToday = new Date(zurichStr);
  const todayDayOfWeek = zurichToday.getDay(); // 0=Sun, 1=Mon, ..., 6=Sat
  
  // Detect language - check for English patterns
  const isEnglish = /\b(what['']?s|lunch|dinner|breakfast|today|tomorrow|monday|tuesday|wednesday|thursday|friday|meal|food|cafeteria|canteen|eat|cheap|vegetarian|vegan)\b/i.test(q);
  
  // Day name mappings (French and English)
  const dayMappings: Array<{ patterns: string[]; dayOfWeek: number; labelFr: string; labelEn: string }> = [
    { patterns: ["lundi", "monday"], dayOfWeek: 1, labelFr: "lundi", labelEn: "Monday" },
    { patterns: ["mardi", "tuesday"], dayOfWeek: 2, labelFr: "mardi", labelEn: "Tuesday" },
    { patterns: ["mercredi", "wednesday"], dayOfWeek: 3, labelFr: "mercredi", labelEn: "Wednesday" },
    { patterns: ["jeudi", "thursday"], dayOfWeek: 4, labelFr: "jeudi", labelEn: "Thursday" },
    { patterns: ["vendredi", "friday"], dayOfWeek: 5, labelFr: "vendredi", labelEn: "Friday" },
  ];
  
  // Check for explicit day names first
  for (const { patterns, dayOfWeek, labelFr, labelEn } of dayMappings) {
    if (patterns.some(p => q.includes(p))) {
      // Calculate index (0=Mon, 4=Fri)
      return { dayIndex: dayOfWeek - 1, dayLabel: isEnglish ? labelEn : labelFr, isEnglish };
    }
  }
  
  // Check for "demain" / "tomorrow"
  if (q.includes("demain") || q.includes("tomorrow")) {
    const tomorrowDayOfWeek = (todayDayOfWeek + 1) % 7;
    if (tomorrowDayOfWeek >= 1 && tomorrowDayOfWeek <= 5) {
      return { dayIndex: tomorrowDayOfWeek - 1, dayLabel: isEnglish ? "tomorrow" : "demain", isEnglish };
    }
    return { dayIndex: null, dayLabel: isEnglish ? "tomorrow" : "demain", isEnglish }; // Weekend
  }
  
  // Default to today
  if (todayDayOfWeek >= 1 && todayDayOfWeek <= 5) {
    return { dayIndex: todayDayOfWeek - 1, dayLabel: isEnglish ? "today" : "aujourd'hui", isEnglish };
  }
  return { dayIndex: null, dayLabel: isEnglish ? "today" : "aujourd'hui", isEnglish }; // Weekend
}

/**
 * Extract max price from question like "moins de 10 chf" or "under 15 francs"
 */
function extractMaxPrice(q: string): number | null {
  // Match patterns like "moins de 10", "moins de 10 chf", "under 15", "< 12"
  const patterns = [
    /moins\s+de\s+(\d+(?:[.,]\d+)?)/i,
    /under\s+(\d+(?:[.,]\d+)?)/i,
    /en\s+dessous\s+de\s+(\d+(?:[.,]\d+)?)/i,
    /<\s*(\d+(?:[.,]\d+)?)/,
    /max(?:imum)?\s+(\d+(?:[.,]\d+)?)/i,
  ];
  
  for (const pattern of patterns) {
    const match = q.match(pattern);
    if (match) {
      return parseFloat(match[1].replace(",", "."));
    }
  }
  return null;
}

/**
 * Filter meals by max price
 */
function filterMealsByPrice(meals: Meal[], maxPrice: number): Meal[] {
  return meals.filter(m => m.studentPrice !== null && m.studentPrice <= maxPrice);
}

/**
 * Answer a food-related question using the food data
 * Responds in the same language the user uses (French or English)
 */
export async function answerFoodQuestionCore(question: string): Promise<{
  reply: string;
  source_type: "food";
  meals?: Meal[];
}> {
  const q = question.toLowerCase();
  
  // Determine which day the user is asking about (includes language detection)
  const { dayIndex, dayLabel, isEnglish } = detectDayFromQuestion(q);
  
  // Get the weekly menu
  const weekStart = getCurrentWeekStart();
  const menuDoc = await db.collection('epflMenus').doc(weekStart).get();
  
  if (!menuDoc.exists) {
    try {
      await syncEpflFoodMenusCore();
    } catch (e) {
      return {
        reply: isEnglish 
          ? `I don't have menu data. Please try again later.`
          : `Je n'ai pas de données sur les menus. Veuillez réessayer plus tard.`,
        source_type: "food",
      };
    }
  }
  
  const data = (await db.collection('epflMenus').doc(weekStart).get()).data() as any;
  
  if (!data?.days || dayIndex === null || dayIndex >= data.days.length) {
    return {
      reply: isEnglish
        ? `I don't have menu data for ${dayLabel}. Menus are available Monday through Friday.`
        : `Je n'ai pas de données sur les menus pour ${dayLabel}. Les menus sont disponibles du lundi au vendredi.`,
      source_type: "food",
    };
  }
  
  const dayData = data.days[dayIndex];
  const dailyMenu: DailyMenu = {
    date: dayData.date,
    dayName: dayData.dayName,
    meals: dayData.meals || [],
  };
  
  if (dailyMenu.meals.length === 0) {
    return {
      reply: isEnglish
        ? `No menu available for ${dayLabel} (${dailyMenu.date}).`
        : `Aucun menu disponible pour ${dayLabel} (${dailyMenu.date}).`,
      source_type: "food",
    };
  }
  
  // Extract price filter if present
  const maxPrice = extractMaxPrice(q);
  
  // Format day label with capitalization
  const formattedDayLabel = dayLabel.charAt(0).toUpperCase() + dayLabel.slice(1);
  
  // Check for specific restaurant query
  const restaurantMatch = TARGET_RESTAURANTS.find(r => 
    q.includes(r.toLowerCase()) || 
    q.includes(r.toLowerCase().split("-")[0].trim()) ||
    (r === "Ornithorynque" && q.includes("orni")) ||
    (r === "Native-Restauration végétale - Bar à café" && q.includes("native"))
  );
  
  if (restaurantMatch) {
    let meals = findMealsByRestaurant(dailyMenu, restaurantMatch);
    if (maxPrice !== null) {
      meals = filterMealsByPrice(meals, maxPrice);
    }
    
    if (meals.length === 0) {
      const priceNote = maxPrice !== null 
        ? (isEnglish ? ` under ${maxPrice} CHF` : ` à moins de ${maxPrice} CHF`)
        : "";
      return {
        reply: isEnglish
          ? `No dishes found${priceNote} at ${restaurantMatch} ${dayLabel}.`
          : `Aucun plat trouvé${priceNote} pour ${restaurantMatch} ${dayLabel}.`,
        source_type: "food",
      };
    }
    
    const mealsList = meals.map(m => {
      const tags: string[] = [];
      if (m.isVegan) tags.push("🌱 vegan");
      else if (m.isVegetarian) tags.push(isEnglish ? "🥬 vegetarian" : "🥬 végétarien");
      const price = m.studentPrice ? ` — **${m.studentPrice.toFixed(2)} CHF**` : "";
      const tagStr = tags.length > 0 ? ` *${tags.join(", ")}*` : "";
      return `- ${m.name}${tagStr}${price}`;
    }).join("\n");
    
    const priceNote = maxPrice !== null ? ` (< **${maxPrice} CHF**)` : "";
    return {
      reply: isEnglish
        ? `### ${formattedDayLabel} at **${restaurantMatch}**${priceNote}\n\n${mealsList}`
        : `### ${formattedDayLabel} à **${restaurantMatch}**${priceNote}\n\n${mealsList}`,
      source_type: "food",
      meals,
    };
  }
  
  // Check for vegetarian/vegan query
  const isVeganQuery = q.includes("vegan");
  const isVeggieQuery = q.includes("végé") || q.includes("veggie") || q.includes("végétarien") || q.includes("vegetarian") || isVeganQuery;
  
  if (isVeggieQuery) {
    let veggieMeals = findVeggieMeals(dailyMenu, isVeganQuery);
    if (maxPrice !== null) {
      veggieMeals = filterMealsByPrice(veggieMeals, maxPrice);
    }
    
    if (veggieMeals.length === 0) {
      const type = isVeganQuery ? "vegan" : (isEnglish ? "vegetarian" : "végétarien");
      const priceNote = maxPrice !== null 
        ? (isEnglish ? ` under ${maxPrice} CHF` : ` à moins de ${maxPrice} CHF`)
        : "";
      return {
        reply: isEnglish
          ? `No ${type} dishes${priceNote} found for ${dayLabel}.`
          : `Aucun plat ${type}${priceNote} trouvé pour ${dayLabel}.`,
        source_type: "food",
      };
    }
    
    const type = isVeganQuery ? "vegan" : (isEnglish ? "vegetarian" : "végétarien");
    const mealsList = veggieMeals.map(m => {
      const tag = m.isVegan ? "🌱" : "🥬";
      const price = m.studentPrice ? ` — **${m.studentPrice.toFixed(2)} CHF**` : "";
      return `- ${tag} **${m.name}** *at ${m.restaurant}*${price}`;
    }).join("\n");
    
    const priceNote = maxPrice !== null ? ` (< **${maxPrice} CHF**)` : "";
    const typeLabel = type.charAt(0).toUpperCase() + type.slice(1);
    return {
      reply: isEnglish
        ? `### ${typeLabel} dishes — ${formattedDayLabel}${priceNote}\n\n${mealsList}`
        : `### Plats ${type}s — ${formattedDayLabel}${priceNote}\n\n${mealsList}`,
      source_type: "food",
      meals: veggieMeals,
    };
  }
  
  // Check for price-based query (meals under X CHF)
  if (maxPrice !== null) {
    const affordableMeals = filterMealsByPrice(dailyMenu.meals, maxPrice);
    
    if (affordableMeals.length === 0) {
      return {
        reply: isEnglish
          ? `No dishes under ${maxPrice} CHF found for ${dayLabel}.`
          : `Aucun plat à moins de ${maxPrice} CHF trouvé pour ${dayLabel}.`,
        source_type: "food",
      };
    }
    
    // Sort by price
    affordableMeals.sort((a, b) => (a.studentPrice ?? 999) - (b.studentPrice ?? 999));
    
    const mealsList = affordableMeals.map(m => {
      const tags: string[] = [];
      if (m.isVegan) tags.push("🌱");
      else if (m.isVegetarian) tags.push("🥬");
      const tagStr = tags.length > 0 ? ` ${tags.join(" ")}` : "";
      return `- **${m.name}**${tagStr} *at ${m.restaurant}* — **${m.studentPrice?.toFixed(2)} CHF**`;
    }).join("\n");
    
    return {
      reply: isEnglish
        ? `### Dishes under **${maxPrice} CHF** — ${formattedDayLabel}\n\n${mealsList}`
        : `### Plats à moins de **${maxPrice} CHF** — ${formattedDayLabel}\n\n${mealsList}`,
      source_type: "food",
      meals: affordableMeals,
    };
  }
  
  // Check for cheapest meal query (le moins cher)
  if (q.includes("moins cher") || q.includes("cheapest") || q.includes("cheap")) {
    const cheapest = findCheapestMeal(dailyMenu);
    if (!cheapest) {
      return {
        reply: isEnglish
          ? `No price data available for ${dayLabel}.`
          : `Pas de données de prix disponibles pour ${dayLabel}.`,
        source_type: "food",
      };
    }
    
    const tags: string[] = [];
    if (cheapest.isVegan) tags.push("🌱 vegan");
    else if (cheapest.isVegetarian) tags.push(isEnglish ? "🥬 vegetarian" : "🥬 végétarien");
    const tagStr = tags.length > 0 ? ` *${tags.join(", ")}*` : "";
    
    return {
      reply: isEnglish
        ? `💰 **Cheapest dish ${dayLabel}:**\n\n**${cheapest.name}**${tagStr}\n- 📍 *${cheapest.restaurant}*\n- 💵 **${cheapest.studentPrice?.toFixed(2)} CHF** (student price)`
        : `💰 **Plat le moins cher ${dayLabel}:**\n\n**${cheapest.name}**${tagStr}\n- 📍 *${cheapest.restaurant}*\n- 💵 **${cheapest.studentPrice?.toFixed(2)} CHF** (prix étudiant)`,
      source_type: "food",
      meals: [cheapest],
    };
  }
  
  // General "what's to eat" query - summarize the day
  const byRestaurant = new Map<string, Meal[]>();
  for (const meal of dailyMenu.meals) {
    if (!byRestaurant.has(meal.restaurant)) byRestaurant.set(meal.restaurant, []);
    byRestaurant.get(meal.restaurant)!.push(meal);
  }
  
  const lines: string[] = [`## 🍽️ ${formattedDayLabel} (${dailyMenu.date})\n`];
  for (const [restaurant, meals] of byRestaurant) {
    lines.push(`### 🏪 ${restaurant}\n`);
    for (const meal of meals) {
      const tags: string[] = [];
      if (meal.isVegan) tags.push("🌱");
      else if (meal.isVegetarian) tags.push("🥬");
      const price = meal.studentPrice ? ` — **${meal.studentPrice.toFixed(2)} CHF**` : "";
      const tagStr = tags.length > 0 ? ` ${tags.join(" ")}` : "";
      lines.push(`- ${meal.name}${tagStr}${price}`);
    }
    lines.push(""); // Add spacing between restaurants
  }
  
  return {
    reply: lines.join("\n"),
    source_type: "food",
    meals: dailyMenu.meals,
  };
}

