import { describe, it, expect, jest, beforeEach, afterEach } from '@jest/globals';

import { EdConnectorService } from "../src/connectors/ed/EdConnectorService";
import { EdConnectorRepository } from "../src/connectors/ed/EdConnectorRepository";
import {
  EdConnectorConfig,
  ED_CONNECTOR_ERROR_CODES,
} from "../src/connectors/ed/EdConnectorModel";
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
  let testConnectionMock: any;
  let postThreadMock: any;
  let getCoursesMock: any;

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
    postThreadMock = jest.fn();
    getCoursesMock = jest.fn();
    MockedEdDiscussionClient.mockImplementation(
      () =>
        ({
          testConnection: testConnectionMock,
          postThread: postThreadMock,
          getCourses: getCoursesMock,
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

    expect(result).toEqual(existing);
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
    expect(savedConfig.lastError).toBe(ED_CONNECTOR_ERROR_CODES.INVALID_CREDENTIALS);

    expect(result.status).toBe("error");
    expect(result.lastError).toBe(ED_CONNECTOR_ERROR_CODES.INVALID_CREDENTIALS);
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
      lastError: ED_CONNECTOR_ERROR_CODES.TEST_FAILED,
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
    expect(savedConfig.lastError).toBe(ED_CONNECTOR_ERROR_CODES.TEST_FAILED);

    expect(result.status).toBe("error");
  });

  it("postThread uses decrypted token and returns thread info", async () => {
    const existing: EdConnectorConfig = {
      status: "connected",
      baseUrl: "https://custom.ed/api",
      apiKeyEncrypted: encrypt("secret-token"),
      lastTestAt: "2025-01-01T00:00:00.000Z",
      lastError: null,
    };
    repo.getConfig.mockResolvedValueOnce(existing);
    postThreadMock.mockResolvedValueOnce({
      id: 99,
      course_id: 1153,
      number: 12,
      title: "Thread title",
    });

    const result = await service.postThread("user-1", {
      title: "Hello",
      body: "World",
      courseId: 1153,
    });

    expect(MockedEdDiscussionClient).toHaveBeenCalledWith(
      "https://custom.ed/api",
      "secret-token"
    );
    expect(postThreadMock).toHaveBeenCalledWith(1153, expect.any(Object));
    expect(result.threadId).toBe(99);
    expect(result.courseId).toBe(1153);
    expect(result.threadNumber).toBe(12);
  });

  it("postThread throws when connector missing", async () => {
    repo.getConfig.mockResolvedValueOnce(null);

    await expect(
      service.postThread("user-1", { title: "T", body: "B", courseId: 1153 })
    ).rejects.toThrow("ED connector is not connected");
  });

  it("postThread throws when courseId is missing", async () => {
    const existing: EdConnectorConfig = {
      status: "connected",
      baseUrl: "https://custom.ed/api",
      apiKeyEncrypted: encrypt("secret-token"),
    };
    repo.getConfig.mockResolvedValueOnce(existing);

    await expect(
      service.postThread("user-1", { title: "T", body: "B" })
    ).rejects.toThrow("courseId is required");
  });

  it("postThread passes isAnonymous to client", async () => {
    const existing: EdConnectorConfig = {
      status: "connected",
      baseUrl: "https://custom.ed/api",
      apiKeyEncrypted: encrypt("secret-token"),
    };
    repo.getConfig.mockResolvedValueOnce(existing);
    postThreadMock.mockResolvedValueOnce({
      id: 99,
      course_id: 1153,
      number: 12,
    });

    await service.postThread("user-1", {
      title: "Hello",
      body: "World",
      courseId: 1153,
      isAnonymous: true,
    });

    const payload = postThreadMock.mock.calls[0][1];
    expect(payload.isAnonymous).toBe(true);
  });

  it("postThread defaults isAnonymous to false", async () => {
    const existing: EdConnectorConfig = {
      status: "connected",
      baseUrl: "https://custom.ed/api",
      apiKeyEncrypted: encrypt("secret-token"),
    };
    repo.getConfig.mockResolvedValueOnce(existing);
    postThreadMock.mockResolvedValueOnce({
      id: 99,
      course_id: 1153,
      number: 12,
    });

    await service.postThread("user-1", {
      title: "Hello",
      body: "World",
      courseId: 1153,
    });

    const payload = postThreadMock.mock.calls[0][1];
    expect(payload.isAnonymous).toBe(false);
  });

  it("postThread builds paragraph XML with breaks and escaping", async () => {
    const existing: EdConnectorConfig = {
      status: "connected",
      baseUrl: "https://custom.ed/api",
      apiKeyEncrypted: encrypt("secret-token"),
    };
    repo.getConfig.mockResolvedValueOnce(existing);
    postThreadMock.mockResolvedValueOnce({});

    await service.postThread("user-1", {
      title: "T",
      body: "Hello\nworld & <tag>",
    });

    const payload = postThreadMock.mock.calls[0][1];
    expect(payload.content).toBe(
      '<document version="2.0"><paragraph>Hello<br/>world &amp; &lt;tag&gt;</paragraph></document>'
    );
  });

  it("postThread splits paragraphs on blank lines", async () => {
    const existing: EdConnectorConfig = {
      status: "connected",
      baseUrl: "https://custom.ed/api",
      apiKeyEncrypted: encrypt("secret-token"),
    };
    repo.getConfig.mockResolvedValueOnce(existing);
    postThreadMock.mockResolvedValueOnce({});

    await service.postThread("user-1", {
      title: "T",
      body: "P1 line1\nP1 line2\n\nP2",
    });

    const payload = postThreadMock.mock.calls[0][1];
    expect(payload.content).toBe(
      '<document version="2.0"><paragraph>P1 line1<br/>P1 line2</paragraph><paragraph>P2</paragraph></document>'
    );
  });
});
