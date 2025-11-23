/// <reference types="jest" />

import { EdConnectorService } from "../src/connectors/ed/EdConnectorService";
import { EdConnectorRepository } from "../src/connectors/ed/EdConnectorRepository";
import { EdConnectorConfig } from "../src/connectors/ed/EdConnectorModel";
import { EdDiscussionClient } from "../src/connectors/ed/EdDiscussionClient";

jest.mock("../src/connectors/ed/EdDiscussionClient");

const MockedEdDiscussionClient = EdDiscussionClient as jest.MockedClass<
  typeof EdDiscussionClient
>;

// Simple encrypt/decrypt helpers used in tests
const encrypt = (plain: string) => `enc:${plain}`;
const decrypt = (cipher: string) => cipher.replace(/^enc:/, "");

describe("EdConnectorService", () => {
  let repo: jest.Mocked<EdConnectorRepository>;
  let service: EdConnectorService;
  let testConnectionMock: jest.Mock;

  beforeEach(() => {
    // Mock repository
    repo = {
      getConfig: jest.fn(),
      saveConfig: jest.fn(),
      deleteConfig: jest.fn(),
    } as unknown as jest.Mocked<EdConnectorRepository>;

    // Mock ED client: each new EdDiscussionClient(...) will return
    // an object with a mocked testConnection() function.
    testConnectionMock = jest.fn();
    MockedEdDiscussionClient.mockImplementation(
      () =>
        ({
          testConnection: testConnectionMock,
        } as any)
    );

    service = new EdConnectorService(
      repo,
      encrypt,
      decrypt,
      "https://eu.edstem.org/api"
    );
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("getStatus returns not_connected when there is no config", async () => {
    repo.getConfig.mockResolvedValueOnce(null);

    const result = await service.getStatus("user-1");

    expect(repo.getConfig).toHaveBeenCalledWith("user-1");
    expect(result.status).toBe("not_connected");
  });

  it("getStatus returns existing config when it exists", async () => {
    const existing: EdConnectorConfig = {
      status: "connected",
      baseUrl: "https://eu.edstem.org/api",
      apiKeyEncrypted: "enc:token",
      lastTestAt: "2025-01-01T00:00:00.000Z",
      lastError: null,
    };
    repo.getConfig.mockResolvedValueOnce(existing);

    const result = await service.getStatus("user-1");

    expect(result).toBe(existing);
  });

  it("connect sets error status when testConnection() fails", async () => {
    testConnectionMock.mockResolvedValueOnce(false);

    const result = await service.connect("user-1", {
      apiToken: "my-token",
    });

    expect(MockedEdDiscussionClient).toHaveBeenCalledWith(
      "https://eu.edstem.org/api",
      "my-token"
    );
    expect(repo.saveConfig).toHaveBeenCalledTimes(1);

    const savedConfig = (repo.saveConfig as jest.Mock).mock.calls[0][1] as EdConnectorConfig;
    expect(savedConfig.status).toBe("error");
    expect(savedConfig.lastError).toBe("invalid_credentials");

    expect(result.status).toBe("error");
    expect(result.lastError).toBe("invalid_credentials");
  });

  it("connect stores the encrypted token and sets status to connected when testConnection() succeeds", async () => {
    testConnectionMock.mockResolvedValueOnce(true);

    const result = await service.connect("user-1", {
      apiToken: "my-token",
    });

    expect(MockedEdDiscussionClient).toHaveBeenCalledWith(
      "https://eu.edstem.org/api",
      "my-token"
    );
    expect(repo.saveConfig).toHaveBeenCalledTimes(1);

    const savedConfig = (repo.saveConfig as jest.Mock).mock.calls[0][1] as EdConnectorConfig;
    expect(savedConfig.status).toBe("connected");
    expect(savedConfig.apiKeyEncrypted).toBe("enc:my-token");
    expect(savedConfig.lastError).toBeNull();

    expect(result.status).toBe("connected");
  });

  it("disconnect calls deleteConfig on the repository", async () => {
    await service.disconnect("user-1");

    expect(repo.deleteConfig).toHaveBeenCalledWith("user-1");
  });

  it("test returns not_connected when there is no config or token", async () => {
    repo.getConfig.mockResolvedValueOnce(null);

    const result = await service.test("user-1");

    expect(result.status).toBe("not_connected");
  });

  it("test sets status to connected when testConnection() succeeds", async () => {
    const existing: EdConnectorConfig = {
      status: "error",
      baseUrl: "https://eu.edstem.org/api",
      apiKeyEncrypted: encrypt("my-token"),
      lastTestAt: "2025-01-01T00:00:00.000Z",
      lastError: "test_failed",
    };
    repo.getConfig.mockResolvedValueOnce(existing);
    testConnectionMock.mockResolvedValueOnce(true);

    const result = await service.test("user-1");

    expect(MockedEdDiscussionClient).toHaveBeenCalledWith(
      "https://eu.edstem.org/api",
      "my-token"
    );
    expect(repo.saveConfig).toHaveBeenCalledTimes(1);

    const savedConfig = (repo.saveConfig as jest.Mock).mock.calls[0][1] as EdConnectorConfig;
    expect(savedConfig.status).toBe("connected");
    expect(savedConfig.lastError).toBeNull();

    expect(result.status).toBe("connected");
  });

  it("test sets status to error when testConnection() fails", async () => {
    const existing: EdConnectorConfig = {
      status: "connected",
      baseUrl: "https://eu.edstem.org/api",
      apiKeyEncrypted: encrypt("my-token"),
      lastTestAt: "2025-01-01T00:00:00.000Z",
      lastError: null,
    };
    repo.getConfig.mockResolvedValueOnce(existing);
    testConnectionMock.mockResolvedValueOnce(false);

    const result = await service.test("user-1");

    expect(MockedEdDiscussionClient).toHaveBeenCalledWith(
      "https://eu.edstem.org/api",
      "my-token"
    );
    expect(repo.saveConfig).toHaveBeenCalledTimes(1);

    const savedConfig = (repo.saveConfig as jest.Mock).mock.calls[0][1] as EdConnectorConfig;
    expect(savedConfig.status).toBe("error");
    expect(savedConfig.lastError).toBe("test_failed");

    expect(result.status).toBe("error");
  });
});
