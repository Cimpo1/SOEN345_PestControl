module.exports = {
  testRunner: {
    args: {
      $0: "jest",
      config: "e2e/jest.config.js",
      _: ["e2e/**/*.e2e.ts"],
    },
    jest: {
      setupTimeout: 180000,
    },
  },
  behavior: {
    init: {
      exposeGlobals: true,
    },
  },
  apps: {
    "android.debug": {
      type: "android.apk",
      binaryPath: "android/app/build/outputs/apk/debug/app-debug.apk",
      testBinaryPath:
        "android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk",
      build: "node e2e/build-android.js",
      reversePorts: [8081],
    },
  },
  devices: {
    emulator: {
      type: "android.emulator",
      device: {
        avdName: "Pixel_6_API_34",
      },
    },
  },
  configurations: {
    "android.emu.debug": {
      device: "emulator",
      app: "android.debug",
    },
  },
};
