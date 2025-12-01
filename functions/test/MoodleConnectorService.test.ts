import { MoodleConnectorService } from "../src/connectors/moodle/MoodleConnectorService";
import { MoodleConnectorRepository } from "../src/connectors/moodle/MoodleConnectorRepository";
import { MoodleClient } from "../src/connectors/moodle/MoodleClient";

jest.mock("../src/connectors/moodle/MoodleClient");

const MockedMoodleClient = MoodleClient as jest.MockedClass<typeof MoodleClient>;

describe("MoodleConnectorService", () => {
  const encrypt = (s: string) => `enc(${s})`;
  const decrypt = (s: string) => s.replace(/^enc\(/, "").replace(/\)$/, "");

  let repo: jest.Mocked<MoodleConnectorRepository>;
  let service: MoodleConnectorService;

  beforeEach(() => {
    repo = {
      getConfig: jest.fn(),
      saveConfig: jest.fn(),
      deleteConfig: jest.fn(),
    } as any;

    service = new MoodleConnectorService(repo, encrypt, decrypt);
    MockedMoodleClient.mockClear();
  });

  it("connect: valid token -> connected", async () => {
    MockedMoodleClient.prototype.testConnection = jest.fn().mockResolvedValue(true);

    const cfg = await service.connect("user1", {
      baseUrl: "https://example.moodlecloud.com",
      token: "ABC",
    });

    expect(cfg.status).toBe("connected");
    expect(cfg.tokenEncrypted).toBe("enc(ABC)");
    expect(repo.saveConfig).toHaveBeenCalledTimes(1);
  });

  it("connect: invalid token -> error", async () => {
    MockedMoodleClient.prototype.testConnection = jest.fn().mockResolvedValue(false);

    const cfg = await service.connect("user1", {
      baseUrl: "https://example.moodlecloud.com",
      token: "BAD",
    });

    expect(cfg.status).toBe("error");
    expect(cfg.lastError).toBe("invalid_credentials");
    expect(repo.saveConfig).toHaveBeenCalledTimes(1);
  });

  it("creates MoodleClient with decrypted token and calls testConnection", async () => {
      MockedMoodleClient.prototype.testConnection = jest.fn().mockResolvedValue(true);

      await service.connect("user1", {
        baseUrl: "https://example.moodlecloud.com",
        token: "ABC",
      });

      expect(MockedMoodleClient).toHaveBeenCalledTimes(1);
      expect(MockedMoodleClient).toHaveBeenCalledWith("https://example.moodlecloud.com", "ABC");
      expect(MockedMoodleClient.prototype.testConnection).toHaveBeenCalledTimes(1);
    });

  it("saves config with userId and encrypted token on success", async () => {
      MockedMoodleClient.prototype.testConnection = jest.fn().mockResolvedValue(true);

      const cfg = await service.connect("user1", {
        baseUrl: "https://example.moodlecloud.com",
        token: "ABC",
      });

      expect(repo.saveConfig).toHaveBeenCalledTimes(1);
      const callArgs = repo.saveConfig.mock.calls[0];
      expect(callArgs[0]).toBe("user1"); // userId param
      expect(callArgs[1].tokenEncrypted).toBe("enc(ABC)");
      expect(callArgs[1].status).toBe("connected");
      expect(cfg.status).toBe("connected");
    });

   it(" doesn't save config with userId and lastError on invalid token", async () => {
      MockedMoodleClient.prototype.testConnection = jest.fn().mockResolvedValue(false);

      const cfg = await service.connect("user1", {
        baseUrl: "https://example.moodlecloud.com",
        token: "BAD",
    });
      expect(repo.saveConfig).toHaveBeenCalledTimes(1);
      const callArgs = repo.saveConfig.mock.calls[0];
      expect(callArgs[0]).toBe("user1");
      expect(callArgs[1].tokenEncrypted).toBeUndefined();;
      expect(callArgs[1].status).toBe("error");
      expect(callArgs[1].lastError).toBe("invalid_credentials");
      expect(cfg.status).toBe("error");
    });
});
