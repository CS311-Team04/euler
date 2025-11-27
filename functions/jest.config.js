/**
    * @file Jest configuration for the project.
    * This configuration sets up Jest to work with TypeScript using ts-jest.
    * It specifies the test environment as Node.js and defines the pattern
    * for locating test files within the project.
    */
/** @type {import('jest').Config} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  testMatch: ["**/test/**/*.test.ts"],
};