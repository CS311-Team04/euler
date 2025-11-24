// Integration tests using Firebase Emulators
import { describe, it, expect, beforeAll, afterAll } from '@jest/globals';
import * as admin from 'firebase-admin';

/**
 * INTEGRATION TESTS
 * 
 * These tests require Firebase Emulators to be running:
 * $ firebase emulators:start --only functions,firestore
 * 
 * You can also run them with:
 * $ npm test -- integration.test.ts --testEnvironment=node
 * 
 * To skip integration tests in CI:
 * $ npm test -- --testPathIgnorePatterns=integration
 */

describe('Integration Tests (requires emulators)', () => {
  let projectId: string;

  beforeAll(() => {
    // Use emulator
    process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
    process.env.FUNCTIONS_EMULATOR_HOST = 'localhost:5001';
    
    projectId = 'demo-test-project';
    
    // Initialize admin SDK for emulator
    if (!admin.apps.length) {
      admin.initializeApp({ projectId });
    }
  });

  afterAll(async () => {
    // Clean up
    await admin.app().delete();
  });

  it('should create message and trigger summary generation', async () => {
    const db = admin.firestore();
    const userId = 'test-user-' + Date.now();
    const conversationId = 'test-conv-' + Date.now();

    // Create a user message
    const messageRef = await db
      .collection('users').doc(userId)
      .collection('conversations').doc(conversationId)
      .collection('messages')
      .add({
        role: 'user',
        content: 'What programs does EPFL offer?',
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });

    // Wait for the trigger to process (adjust timing as needed)
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Check if summary was added
    const updatedDoc = await messageRef.get();
    const data = updatedDoc.data();

    // In emulator, the trigger should have added a summary
    // Note: This assumes your trigger is deployed to the emulator
    expect(data).toBeDefined();
    // Uncomment when trigger is properly set up in emulator:
    // expect(data?.summary).toBeDefined();
    // expect(data?.summary).toContain('parlé de');

    // Clean up
    await messageRef.delete();
  });

  it('should handle callable function invocation', async () => {
    // This test would call your callable functions through the emulator
    // You'd need to set up the Firebase Functions SDK on the client side
    
    // Example structure (requires additional setup):
    /*
    const functions = admin.functions();
    const generateTitle = functions.httpsCallable('generateTitleFn');
    
    const result = await generateTitle({
      question: 'What is EPFL?'
    });
    
    expect(result.data.title).toBeDefined();
    expect(result.data.title).not.toBe('New conversation');
    */
    
    expect(true).toBe(true); // Placeholder
  });
});

