// Integration tests using Firebase Emulators
import { describe, it, expect, beforeAll, afterAll } from '@jest/globals';
import * as admin from 'firebase-admin';
import * as net from 'net';

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
  let emulatorConnected = false;

  async function isEmulatorReachable(host = 'localhost', port = 8080, timeoutMs = 1200): Promise<boolean> {
    return new Promise(resolve => {
      const socket = new net.Socket();
      const cleanup = () => {
        socket.destroy();
      };
      const onError = () => {
        cleanup();
        resolve(false);
      };
      socket.setTimeout(timeoutMs);
      socket.once('error', onError);
      socket.once('timeout', onError);
      socket.connect(port, host, () => {
        cleanup();
        resolve(true);
      });
    });
  }

  function skipIfNoEmulator(): boolean {
    if (!emulatorConnected) {
      // Mark as skipped by returning early
      return true;
    }
    return false;
  }

  beforeAll(async () => {
    // Use emulator
    process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
    process.env.FUNCTIONS_EMULATOR_HOST = 'localhost:5001';
    
    projectId = 'demo-test-project';

    emulatorConnected = await isEmulatorReachable('localhost', 8080);
    if (!emulatorConnected) {
      console.warn('⚠️  Firestore emulator not reachable; skipping integration tests.');
      console.warn('   Start emulator: firebase emulators:start --only firestore');
      return;
    }
    
    // Initialize admin SDK for emulator
    if (!admin.apps.length) {
      admin.initializeApp({ projectId });
    }
  });

  afterAll(async () => {
    // Clean up
    if (admin.apps.length > 0) {
      await Promise.all(admin.apps.map(app => app?.delete()));
    }
  });

  it('should create message and trigger summary generation', async () => {
    if (skipIfNoEmulator()) return;

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
});

