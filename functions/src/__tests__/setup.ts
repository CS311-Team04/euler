// Jest setup file - runs before all tests
import { jest } from '@jest/globals';

// Set default timeout
jest.setTimeout(10000);

// Mock environment variables for all tests
process.env.OPENAI_API_KEY = 'test-openai-key';
process.env.OPENAI_BASE_URL = 'https://api.publicai.co';
process.env.APERTUS_MODEL_ID = 'gpt-4';
process.env.EMBED_BASE_URL = 'https://api.jina.ai/v1';
process.env.EMBED_API_KEY = 'test-embed-key';
process.env.EMBED_MODEL_ID = 'jina-embeddings-v3';
process.env.QDRANT_URL = 'https://qdrant.example.com';
process.env.QDRANT_API_KEY = 'test-qdrant-key';
process.env.QDRANT_COLLECTION = 'test-collection';
process.env.INDEX_API_KEY = 'test-index-key';

// Tell Jest to use the manual mock for OpenAI
// The manual mock is in src/__mocks__/openai.ts
jest.mock('openai');

// Suppress console output during tests (optional)
global.console = {
  ...console,
  // Uncomment to suppress logs during tests
  // log: jest.fn(),
  // info: jest.fn(),
  // warn: jest.fn(),
  // error: jest.fn(),
};

// Firebase Admin is NOT mocked globally
// Integration tests need real admin to connect to emulator
// Unit tests that need mocks should mock firebase-admin locally

// Reset all mocks after each test
afterEach(() => {
  jest.clearAllMocks();
});

