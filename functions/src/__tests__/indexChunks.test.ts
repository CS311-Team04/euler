// Test indexChunksCore with mocked external dependencies
import { describe, it, expect, jest, beforeEach, afterEach } from '@jest/globals';

// Mock fetch globally
global.fetch = jest.fn() as jest.MockedFunction<typeof fetch>;

// You would import this from your actual file
// For demo purposes, we'll show the testing pattern

describe('indexChunksCore', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Set up environment variables
    process.env.EMBED_BASE_URL = 'https://api.jina.ai/v1';
    process.env.EMBED_API_KEY = 'test-key';
    process.env.EMBED_MODEL_ID = 'jina-embeddings-v3';
    process.env.QDRANT_URL = 'https://qdrant.example.com';
    process.env.QDRANT_API_KEY = 'qdrant-key';
    process.env.QDRANT_COLLECTION = 'test-collection';
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should return zero count for empty chunks', async () => {
    // Import after mocks are set up
    const { indexChunksCore } = await import('../index');
    
    const result = await indexChunksCore({ chunks: [] });
    
    expect(result).toEqual({ count: 0, dim: 0 });
    expect(fetch).not.toHaveBeenCalled();
  });

  it('should embed chunks and upsert to Qdrant', async () => {
    const mockEmbeddings = [[0.1, 0.2, 0.3], [0.4, 0.5, 0.6]];
    
    // Mock embedding API response
    (fetch as jest.MockedFunction<typeof fetch>)
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: async () => ({
            data: mockEmbeddings.map(embedding => ({ embedding }))
          })
        } as Response)
      )
      // Mock Qdrant collection check
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: async () => ({ result: { status: 'ok' } })
        } as Response)
      )
      // Mock Qdrant upsert
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: async () => ({ result: { status: 'ok' } })
        } as Response)
      );

    const { indexChunksCore } = await import('../index');

    const chunks = [
      { id: '1', text: 'Test chunk 1', title: 'Title 1', url: 'https://example.com/1' },
      { id: '2', text: 'Test chunk 2', title: 'Title 2' }
    ];

    const result = await indexChunksCore({ chunks });

    expect(result.count).toBe(2);
    expect(result.dim).toBe(3);
    
    // Verify embedding API was called
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/embeddings'),
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Authorization': 'Bearer test-key',
          'Content-Type': 'application/json',
        })
      })
    );

    // Verify Qdrant upsert was called
    const qdrantCalls = (fetch as jest.MockedFunction<typeof fetch>).mock.calls.filter(
      call => call[0]?.toString().includes('qdrant')
    );
    expect(qdrantCalls.length).toBeGreaterThan(0);
  });

  it('should handle embedding API errors gracefully', async () => {
    (fetch as jest.MockedFunction<typeof fetch>)
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: false,
          status: 500,
          text: async () => 'Internal server error'
        } as Response)
      );

    const { indexChunksCore } = await import('../index');

    const chunks = [
      { id: '1', text: 'Test chunk' }
    ];

    await expect(indexChunksCore({ chunks })).rejects.toThrow();
  });

  it('should sanitize and truncate long texts', async () => {
    const longText = 'a'.repeat(10000); // Exceeds MAX_CHARS (8000)
    
    (fetch as jest.MockedFunction<typeof fetch>)
      .mockImplementation(() =>
        Promise.resolve({
          ok: true,
          json: async () => ({
            data: [{ embedding: [0.1, 0.2] }]
          })
        } as Response)
      );

    const { indexChunksCore } = await import('../index');

    const chunks = [{ id: '1', text: longText }];
    
    await indexChunksCore({ chunks });

    // Check that the embedding request truncated the text
    const embedCall = (fetch as jest.MockedFunction<typeof fetch>).mock.calls[0];
    const requestBody = JSON.parse(embedCall[1]?.body as string);
    expect(requestBody.input[0].length).toBeLessThanOrEqual(8000);
  });
});

