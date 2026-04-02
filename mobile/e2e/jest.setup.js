beforeAll(async () => {
  await device.launchApp({
    newInstance: true,
    delete: true,
    permissions: { notifications: "YES" },
  });
});

beforeEach(async () => {
  await device.reloadReactNative();
});
