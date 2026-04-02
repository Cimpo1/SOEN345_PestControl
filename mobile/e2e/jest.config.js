module.exports = {
  testRunner: "jest-circus/runner",
  testTimeout: 180000,
  testEnvironment: "detox/runners/jest/testEnvironment",
  globalSetup: "detox/runners/jest/globalSetup",
  globalTeardown: "detox/runners/jest/globalTeardown",
  reporters: ["detox/runners/jest/reporter"],
  setupFilesAfterEnv: ["./jest.setup.js"],
  testMatch: ["**/*.e2e.ts"],
};
