
import crypto from "crypto";

/**
 * Symmetric encryption helper for connector secrets (API tokens, etc.).
 *
 * It uses AES-256-GCM with a key derived from CONNECTORS_SECRET_KEY.
 * The output format is: base64( iv || authTag || ciphertext )
 *
 * Make sure to set CONNECTORS_SECRET_KEY in your environment (e.g. .env, functions config).
 */

const ALGORITHM = "aes-256-gcm";
const KEY_ENV_VAR = "CONNECTORS_SECRET_KEY";

// Derive a 32-byte key from the env var using SHA-256
const rawKey = process.env[KEY_ENV_VAR];

if (!rawKey) {
  // You can also choose to log a warning instead of throwing,
  // but failing fast is usually safer for secrets.
  throw new Error(
    `Missing ${KEY_ENV_VAR} environment variable for secret encryption`
  );
}

const KEY = crypto.createHash("sha256").update(rawKey).digest(); // 32 bytes

/**
 * Encrypts a secret string (e.g. API token) into a base64 string.
 */
export function encryptSecret(plain: string): string {
  const iv = crypto.randomBytes(12); // recommended IV size for GCM
  const cipher = crypto.createCipheriv(ALGORITHM, KEY, iv);

  const ciphertext = Buffer.concat([
    cipher.update(plain, "utf8"),
    cipher.final(),
  ]);

  const authTag = cipher.getAuthTag();

  // Concatenate iv || authTag || ciphertext, then base64-encode
  const packed = Buffer.concat([iv, authTag, ciphertext]);
  return packed.toString("base64");
}

/**
 * Decrypts a base64-encoded secret produced by encryptSecret.
 */
export function decryptSecret(encoded: string): string {
  try {
    const packed = Buffer.from(encoded, "base64");

    // iv = first 12 bytes, authTag = next 16 bytes, rest = ciphertext
    const iv = packed.subarray(0, 12);
    const authTag = packed.subarray(12, 28);
    const ciphertext = packed.subarray(28);

    const decipher = crypto.createDecipheriv(ALGORITHM, KEY, iv);
    decipher.setAuthTag(authTag);

    const decrypted = Buffer.concat([
      decipher.update(ciphertext),
      decipher.final(),
    ]);

    return decrypted.toString("utf8");
  } catch (err) {
    // If anything goes wrong (bad base64, wrong key, tampered data, etc.)
    throw new Error("Failed to decrypt secret");
  }
}
