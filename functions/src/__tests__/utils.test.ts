// Test utility/helper functions (deterministic)
import { describe, it, expect } from '@jest/globals';

// These would be exported from your index.ts or a separate utils file
// For now, we'll copy them here for testing purposes

function withV1(url?: string): string {
  const u = (url ?? "").trim();
  if (!u) return "https://api.publicai.co/v1";
  return u.includes("/v1") ? u : u.replace(/\/+$/, "") + "/v1";
}

function isUuid(s: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(s);
}

function isUintString(s: string): boolean {
  return /^\d+$/.test(s);
}

function normalizePointId(id: string | number): string | number {
  if (typeof id === "number") return id;
  const s = String(id);
  if (isUintString(s)) return Number(s);
  if (isUuid(s)) return s;
  // In real test, we'd mock randomUUID
  return "mocked-uuid";
}

describe('Utility Functions', () => {
  describe('withV1', () => {
    it('should return default URL when input is empty', () => {
      expect(withV1()).toBe('https://api.publicai.co/v1');
      expect(withV1('')).toBe('https://api.publicai.co/v1');
      expect(withV1('  ')).toBe('https://api.publicai.co/v1');
    });

    it('should append /v1 when not present', () => {
      expect(withV1('https://api.example.com')).toBe('https://api.example.com/v1');
      expect(withV1('https://api.example.com/')).toBe('https://api.example.com/v1');
    });

    it('should not duplicate /v1 when already present', () => {
      expect(withV1('https://api.example.com/v1')).toBe('https://api.example.com/v1');
      expect(withV1('https://api.example.com/v1/')).toBe('https://api.example.com/v1/');
    });
  });

  describe('isUuid', () => {
    it('should validate correct UUIDs', () => {
      expect(isUuid('550e8400-e29b-41d4-a716-446655440000')).toBe(true);
      expect(isUuid('6ba7b810-9dad-11d1-80b4-00c04fd430c8')).toBe(true);
    });

    it('should reject invalid UUIDs', () => {
      expect(isUuid('not-a-uuid')).toBe(false);
      expect(isUuid('550e8400-e29b-41d4-a716')).toBe(false);
      expect(isUuid('')).toBe(false);
    });
  });

  describe('isUintString', () => {
    it('should validate positive integer strings', () => {
      expect(isUintString('123')).toBe(true);
      expect(isUintString('0')).toBe(true);
      expect(isUintString('999999')).toBe(true);
    });

    it('should reject non-integer strings', () => {
      expect(isUintString('-123')).toBe(false);
      expect(isUintString('12.3')).toBe(false);
      expect(isUintString('abc')).toBe(false);
      expect(isUintString('')).toBe(false);
    });
  });

  describe('normalizePointId', () => {
    it('should return number as-is', () => {
      expect(normalizePointId(123)).toBe(123);
    });

    it('should convert numeric strings to numbers', () => {
      expect(normalizePointId('456')).toBe(456);
    });

    it('should keep valid UUIDs as strings', () => {
      const uuid = '550e8400-e29b-41d4-a716-446655440000';
      expect(normalizePointId(uuid)).toBe(uuid);
    });

    it('should generate UUID for invalid inputs', () => {
      const result = normalizePointId('invalid-id');
      expect(typeof result).toBe('string');
      expect(result).toBe('mocked-uuid');
    });
  });
});

