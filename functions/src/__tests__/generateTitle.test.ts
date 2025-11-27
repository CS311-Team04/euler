// Test generateTitleCore with mocked LLM
import { describe, it, expect, jest, beforeEach } from '@jest/globals';

describe('generateTitleCore', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    process.env.APERTUS_MODEL_ID = 'gpt-4';
  });

  it('should generate a concise title from question', async () => {
    const mockChatResponse = {
      choices: [{
        message: {
          content: 'EPFL Bachelor Programs'
        }
      }]
    };

    // Create mock client
    const mockClient = {
      chat: {
        completions: {
          create: (jest.fn() as any).mockResolvedValue(mockChatResponse)
        }
      }
    } as any;

    const { generateTitleCore } = await import('../index');

    const result = await generateTitleCore({
      question: 'What bachelor programs does EPFL offer in computer science?',
      client: mockClient
    });

    expect(result.title).toBeTruthy();
    expect(result.title.length).toBeLessThan(60);
    expect(result.title).not.toMatch(/["']$/); // No trailing quotes
  });

  it('should clean up quotes and extra whitespace', async () => {
    const mockChatResponse = {
      choices: [{
        message: {
          content: '  "EPFL   Programs"  '
        }
      }]
    };

    const mockClient = {
      chat: {
        completions: {
          create: (jest.fn() as any).mockResolvedValue(mockChatResponse)
        }
      }
    } as any;

    const { generateTitleCore } = await import('../index');

    const result = await generateTitleCore({
      question: 'Test question',
      client: mockClient
    });

    expect(result.title).toBe('EPFL Programs'); // Quotes should be stripped
    // Note: If this fails, check the regex in generateTitleCore
  });

  it('should return default title for empty response', async () => {
    const mockChatResponse = {
      choices: [{
        message: {
          content: ''
        }
      }]
    };

    const mockClient = {
      chat: {
        completions: {
          create: (jest.fn() as any).mockResolvedValue(mockChatResponse)
        }
      }
    } as any;

    const { generateTitleCore } = await import('../index');

    const result = await generateTitleCore({
      question: 'Test question',
      client: mockClient
    });

    expect(result.title).toBe('New conversation');
  });

  it('should truncate very long titles', async () => {
    const longTitle = 'This is an extremely long title that exceeds the maximum allowed length of sixty characters';
    
    const mockChatResponse = {
      choices: [{
        message: {
          content: longTitle
        }
      }]
    };

    const mockClient = {
      chat: {
        completions: {
          create: (jest.fn() as any).mockResolvedValue(mockChatResponse)
        }
      }
    } as any;

    const { generateTitleCore } = await import('../index');

    const result = await generateTitleCore({
      question: 'Test question',
      client: mockClient
    });

    expect(result.title.length).toBeLessThanOrEqual(60);
  });
});
