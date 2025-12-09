// functions/src/__tests__/epflFoodScraper.test.ts
import { describe, expect, test } from "@jest/globals";
import {
  TARGET_RESTAURANTS,
  RESTAURANT_ALIASES,
  findCheapestMeal,
  findVeggieMeals,
  findMealsByRestaurant,
  type DailyMenu,
} from "../food/epflFoodScraper";

describe("EPFL Food Scraper", () => {
  describe("TARGET_RESTAURANTS", () => {
    test("contains all 6 target restaurants", () => {
      expect(TARGET_RESTAURANTS).toHaveLength(6);
      expect(TARGET_RESTAURANTS).toContain("Alpine");
      expect(TARGET_RESTAURANTS).toContain("Arcadie");
      expect(TARGET_RESTAURANTS).toContain("Esplanade");
      expect(TARGET_RESTAURANTS).toContain("Native-Restauration végétale - Bar à café");
      expect(TARGET_RESTAURANTS).toContain("Ornithorynque");
      expect(TARGET_RESTAURANTS).toContain("Piano");
    });
  });

  describe("RESTAURANT_ALIASES", () => {
    test("maps common aliases to restaurant names", () => {
      expect(RESTAURANT_ALIASES["alpine"]).toBe("Alpine");
      expect(RESTAURANT_ALIASES["orni"]).toBe("Ornithorynque");
      expect(RESTAURANT_ALIASES["native"]).toBe("Native-Restauration végétale - Bar à café");
      expect(RESTAURANT_ALIASES["piano"]).toBe("Piano");
    });
  });

  describe("findCheapestMeal", () => {
    const mockMenu: DailyMenu = {
      date: "2025-12-01",
      dayName: "lundi",
      meals: [
        { name: "Pasta", restaurant: "Alpine", studentPrice: 8.50, isVegetarian: true, isVegan: false, allergens: [] },
        { name: "Chicken", restaurant: "Arcadie", studentPrice: 12.00, isVegetarian: false, isVegan: false, allergens: [] },
        { name: "Salad", restaurant: "Piano", studentPrice: 7.10, isVegetarian: true, isVegan: true, allergens: [] },
        { name: "Special", restaurant: "Esplanade", studentPrice: null, isVegetarian: false, isVegan: false, allergens: [] },
      ],
    };

    test("finds the cheapest meal with a price", () => {
      const cheapest = findCheapestMeal(mockMenu);
      expect(cheapest).not.toBeNull();
      expect(cheapest!.name).toBe("Salad");
      expect(cheapest!.studentPrice).toBe(7.10);
    });

    test("ignores meals without prices", () => {
      const cheapest = findCheapestMeal(mockMenu);
      expect(cheapest!.name).not.toBe("Special");
    });

    test("returns null for empty menu", () => {
      const emptyMenu: DailyMenu = { date: "2025-12-01", dayName: "lundi", meals: [] };
      const cheapest = findCheapestMeal(emptyMenu);
      expect(cheapest).toBeNull();
    });
  });

  describe("findVeggieMeals", () => {
    const mockMenu: DailyMenu = {
      date: "2025-12-01",
      dayName: "lundi",
      meals: [
        { name: "Pasta", restaurant: "Alpine", studentPrice: 8.50, isVegetarian: true, isVegan: false, allergens: [] },
        { name: "Chicken", restaurant: "Arcadie", studentPrice: 12.00, isVegetarian: false, isVegan: false, allergens: [] },
        { name: "Vegan Bowl", restaurant: "Native-Restauration végétale - Bar à café", studentPrice: 11.00, isVegetarian: true, isVegan: true, allergens: [] },
        { name: "Salad", restaurant: "Piano", studentPrice: 7.10, isVegetarian: true, isVegan: false, allergens: [] },
      ],
    };

    test("finds all vegetarian meals (including vegan)", () => {
      const veggie = findVeggieMeals(mockMenu, false);
      expect(veggie).toHaveLength(3);
      expect(veggie.map(m => m.name)).toContain("Pasta");
      expect(veggie.map(m => m.name)).toContain("Vegan Bowl");
      expect(veggie.map(m => m.name)).toContain("Salad");
    });

    test("finds only vegan meals when veganOnly is true", () => {
      const vegan = findVeggieMeals(mockMenu, true);
      expect(vegan).toHaveLength(1);
      expect(vegan[0].name).toBe("Vegan Bowl");
    });

    test("returns empty array when no veggie meals", () => {
      const meatMenu: DailyMenu = {
        date: "2025-12-01",
        dayName: "lundi",
        meals: [
          { name: "Steak", restaurant: "Alpine", studentPrice: 15.00, isVegetarian: false, isVegan: false, allergens: [] },
        ],
      };
      const veggie = findVeggieMeals(meatMenu);
      expect(veggie).toHaveLength(0);
    });
  });

  describe("findMealsByRestaurant", () => {
    const mockMenu: DailyMenu = {
      date: "2025-12-01",
      dayName: "lundi",
      meals: [
        { name: "Pasta", restaurant: "Alpine", studentPrice: 8.50, isVegetarian: true, isVegan: false, allergens: [] },
        { name: "Grill", restaurant: "Alpine", studentPrice: 14.00, isVegetarian: false, isVegan: false, allergens: [] },
        { name: "Fish", restaurant: "Arcadie", studentPrice: 12.00, isVegetarian: false, isVegan: false, allergens: [] },
      ],
    };

    test("finds meals by exact restaurant name", () => {
      const meals = findMealsByRestaurant(mockMenu, "Alpine");
      expect(meals).toHaveLength(2);
    });

    test("finds meals using alias", () => {
      const meals = findMealsByRestaurant(mockMenu, "alpine");
      expect(meals).toHaveLength(2);
    });

    test("finds meals with partial match", () => {
      const meals = findMealsByRestaurant(mockMenu, "Arcadie");
      expect(meals).toHaveLength(1);
      expect(meals[0].name).toBe("Fish");
    });

    test("returns empty for unknown restaurant", () => {
      const meals = findMealsByRestaurant(mockMenu, "Unknown");
      expect(meals).toHaveLength(0);
    });
  });
});

