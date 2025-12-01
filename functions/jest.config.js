module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/src'],
  testMatch: ['**/__tests__/**/*.test.ts'],
  collectCoverageFrom: [
    'src/**/*.ts',
    '!src/**/*.test.ts',
    '!src/**/__tests__/**',
    '!src/**/__mocks__/**'
  ],
  coverageDirectory: 'coverage',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  transform: {
    '^.+\\.(ts|tsx)$': [
      'ts-jest',
      {
        tsconfig: '<rootDir>/tsconfig.json'
      }
    ]
  },
  // Setup file to run before tests
  setupFilesAfterEnv: ['<rootDir>/src/__tests__/setup.ts'],
  // Don't ignore integration tests - we'll control this with npm scripts
  testPathIgnorePatterns: [
    '/node_modules/',
    '/lib/'
  ],
  // Clear mocks between tests
  clearMocks: true,
  // Reset mocks between tests
  resetMocks: false,
  // Don't restore mocks (keep our manual mocks)
  restoreMocks: false,
  // Timeout for async tests (integration tests need more time)
  testTimeout: 30000
};
