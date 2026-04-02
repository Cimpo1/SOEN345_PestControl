describe("AT-4 All Events Page (US-2.1)", () => {
  it("Given signed in user, Events icon is visible", async () => {
    await expect(element(by.id("tab-events"))).toBeVisible();
  });

  it("When Events icon is clicked, Events page becomes visible", async () => {
    await element(by.id("tab-events")).tap();
    await expect(element(by.id("events-list-screen"))).toBeVisible();
  });

  it("When Events page is visible and events exist, list appears", async () => {
    await element(by.id("tab-events")).tap();

    await waitFor(element(by.id("events-list")))
      .toBeVisible()
      .withTimeout(15000);

    await waitFor(element(by.id("event-card-1001")))
      .toBeVisible()
      .withTimeout(15000);
  });

  it("When many events exist, user can scroll through events", async () => {
    await element(by.id("tab-events")).tap();

    await waitFor(element(by.id("event-card-1020")))
      .toBeVisible()
      .whileElement(by.id("events-list"))
      .scroll(400, "down");
  });

  it("When no events match, empty-state message is shown", async () => {
    await element(by.id("tab-events")).tap();

    await element(by.id("events-search-input")).replaceText("zzzz-no-events");

    await waitFor(element(by.id("events-empty-message")))
      .toBeVisible()
      .withTimeout(15000);
  });
});
