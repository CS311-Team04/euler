import { Firestore } from "firebase-admin/firestore";
import { EdConnectorConfig } from "./EdConnectorModel";

/**
 * Repository responsible for persisting the ED connector configuration
 * for a given user in the database (e.g. Firestore).
 *
 * We store configs under:
 *   connectors_ed/{userId}
 */
export class EdConnectorRepository {
  private readonly collectionName = "connectors_ed";

  constructor(private readonly db: Firestore) {}

  private docRef(userId: string) {
    return this.db.collection(this.collectionName).doc(userId);
  }

  /**
   * Returns the stored connector config for the given user,
   * or null if no config exists yet.
   */
  async getConfig(userId: string): Promise<EdConnectorConfig | null> {
    const snap = await this.docRef(userId).get();
    if (!snap.exists) {
      return null;
    }
    return snap.data() as EdConnectorConfig;
  }

  /**
   * Saves (or updates) the connector config for the given user.
   * Uses merge to allow partial updates if needed.
   */
  async saveConfig(userId: string, config: Partial<EdConnectorConfig>): Promise<void> {
    await this.docRef(userId).set(config, { merge: true });
  }

  /**
   * Deletes the connector config for the given user.
   */
  async deleteConfig(userId: string): Promise<void> {
    await this.docRef(userId).delete();
  }
}
