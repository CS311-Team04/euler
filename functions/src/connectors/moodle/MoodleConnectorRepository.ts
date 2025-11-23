import { MoodleConnectorConfig } from "./MoodleConnectorModel";

export class MoodleConnectorRepository {
  private readonly collectionName = "connectors_moodle";

  constructor(private readonly db: FirebaseFirestore.Firestore) {}

  private docRef(userId: string) {
    return this.db.collection(this.collectionName).doc(userId);
  }

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