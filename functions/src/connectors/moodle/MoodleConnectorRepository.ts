import { MoodleConnectorConfig } from "./MoodleConnectorModel";

/**
    * Repository for managing Moodle connector configurations in Firestore.
 */
export class MoodleConnectorRepository {
  private readonly collectionName = "connectors_moodle";

  constructor(private readonly db: FirebaseFirestore.Firestore) {}

  // Helper to get document reference for a user
  private docRef(userId: string) {
    return this.db.collection(this.collectionName).doc(userId);
  }

  // Retrieves the Moodle connector configuration for a user
  async getConfig(userId: string): Promise<MoodleConnectorConfig | null> {
    const snap = await this.docRef(userId).get();
    if (!snap.exists) {
      return null;
    }
    return snap.data() as MoodleConnectorConfig;
  }

  async saveConfig(userId: string, config: MoodleConnectorConfig): Promise<void> {
    await this.docRef(userId).set(config, { merge: true });
  }

  async deleteConfig(userId: string): Promise<void> {
    await this.docRef(userId).delete();
  }
}