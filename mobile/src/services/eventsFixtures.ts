import type { EventItem, EventFilters } from "./eventsApi";

type EventsFixtureMode = "some" | "many" | "empty";

const BASE_EVENTS: EventItem[] = [
  {
    eventId: 1,
    title: "Montreal Jazz Night",
    startDateTime: "2026-04-10T19:00:00.000Z",
    endDateTime: "2026-04-10T22:00:00.000Z",
    category: "CONCERT",
    basePrice: 45,
    status: "SCHEDULED",
    location: {
      locationId: 101,
      name: "Bell Centre",
      addressLine: "1909 Av. des Canadiens-de-Montreal",
      city: "Montreal",
      province: "QC",
      postalCode: "H4B5G0",
    },
  },
  {
    eventId: 2,
    title: "Laval Family Expo",
    startDateTime: "2026-04-12T16:00:00.000Z",
    endDateTime: "2026-04-12T19:00:00.000Z",
    category: "FAMILY",
    basePrice: 18,
    status: "SCHEDULED",
    location: {
      locationId: 102,
      name: "Place Bell",
      addressLine: "1950 Rue Claude-Gagne",
      city: "Laval",
      province: "QC",
      postalCode: "H7N0E4",
    },
  },
  {
    eventId: 3,
    title: "Stand-up Weekend",
    startDateTime: "2026-04-14T20:00:00.000Z",
    endDateTime: "2026-04-14T22:30:00.000Z",
    category: "COMEDY",
    basePrice: 35,
    status: "SCHEDULED",
    location: {
      locationId: 103,
      name: "Olympia",
      addressLine: "1004 Rue Sainte-Catherine E",
      city: "Montreal",
      province: "QC",
      postalCode: "H2L2G2",
    },
  },
];

export function isEventsMockEnabled() {
  return process.env.EXPO_PUBLIC_E2E_MOCK_EVENTS === "1";
}

export function getEventsFixtureMode(): EventsFixtureMode {
  const rawMode = (process.env.EXPO_PUBLIC_E2E_EVENTS_MODE || "some").toLowerCase();

  if (rawMode === "many" || rawMode === "empty") {
    return rawMode;
  }

  return "some";
}

export function getEventsFixtureData(mode: EventsFixtureMode): EventItem[] {
  if (mode === "empty") {
    return [];
  }

  if (mode === "many") {
    return Array.from({ length: 30 }, (_, index) => {
      const itemNumber = index + 1;
      const day = ((index % 25) + 1).toString().padStart(2, "0");

      return {
        eventId: 1000 + itemNumber,
        title: `Fixture Event ${itemNumber}`,
        startDateTime: `2026-05-${day}T18:00:00.000Z`,
        endDateTime: `2026-05-${day}T20:30:00.000Z`,
        category: ["CONCERT", "SPORTS", "ART_THEATER", "COMEDY", "FAMILY"][
          index % 5
        ],
        basePrice: 20 + (index % 8) * 5,
        status: "SCHEDULED",
        location: {
          locationId: 300 + itemNumber,
          name: `Venue ${itemNumber}`,
          addressLine: `${100 + itemNumber} Main Street`,
          city: index % 2 === 0 ? "Montreal" : "Quebec City",
          province: "QC",
          postalCode: "H1A1A1",
        },
      };
    });
  }

  return [...BASE_EVENTS];
}

export function filterFixtureEvents(events: EventItem[], filters: EventFilters) {
  return events.filter((event) => {
    if (filters.title) {
      const query = filters.title.toLowerCase();
      if (!event.title.toLowerCase().includes(query)) {
        return false;
      }
    }

    if (filters.location) {
      const query = filters.location.toLowerCase();
      const haystack = [event.location.name, event.location.city, event.location.province]
        .join(" ")
        .toLowerCase();

      if (!haystack.includes(query)) {
        return false;
      }
    }

    if (filters.categories && filters.categories.length > 0) {
      if (!filters.categories.includes(event.category)) {
        return false;
      }
    }

    if (filters.startDate) {
      const eventDate = event.startDateTime.slice(0, 10);
      if (eventDate < filters.startDate) {
        return false;
      }
    }

    if (filters.endDate) {
      const eventDate = event.startDateTime.slice(0, 10);
      if (eventDate > filters.endDate) {
        return false;
      }
    }

    return true;
  });
}
