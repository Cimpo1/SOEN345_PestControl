export interface EventLocation {
  locationId: number;
  name: string;
  addressLine: string;
  city: string;
  province: string;
  postalCode: string;
}

export interface EventItem {
  eventId: number;
  title: string;
  startDateTime: string;
  endDateTime: string;
  category: string;
  basePrice: number;
  status: string;
  location: EventLocation;
}

interface ApiResponse<T> {
  ok: boolean;
  status: number;
  data?: T;
  error?: string;
}

export interface EventFilters {
  title?: string;
  startDate?: string;
  endDate?: string;
  location?: string;
  categories?: string[];
}

function getBaseUrl() {
  const backendIp = process.env.EXPO_PUBLIC_BACKEND_IP;
  return backendIp ? `http://${backendIp}:8080` : "http://localhost:8080";
}

function buildEventsQuery(filters: EventFilters) {
  const params = new URLSearchParams();

  if (filters.title) {
    params.set("title", filters.title);
  }
  if (filters.startDate) {
    params.set("startDate", filters.startDate);
  }
  if (filters.endDate) {
    params.set("endDate", filters.endDate);
  }
  if (filters.location) {
    params.set("location", filters.location);
  }
  if (filters.categories && filters.categories.length > 0) {
    params.set("categories", filters.categories.join(","));
  }

  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function fetchEvents(
  filters: EventFilters,
): Promise<ApiResponse<EventItem[]>> {
  try {
    const response = await fetch(`${getBaseUrl()}/events${buildEventsQuery(filters)}`);

    if (response.ok) {
      const data = (await response.json()) as EventItem[];
      return { ok: true, status: response.status, data };
    }

    const rawError = await response.text();
    return {
      ok: false,
      status: response.status,
      error: rawError || "Failed to load events.",
    };
  } catch {
    return {
      ok: false,
      status: 0,
      error: "Network error. Check server connection and try again.",
    };
  }
}

export async function fetchEventById(
  eventId: number,
): Promise<ApiResponse<EventItem>> {
  try {
    const response = await fetch(`${getBaseUrl()}/events/${eventId}`);

    if (response.ok) {
      const data = (await response.json()) as EventItem;
      return { ok: true, status: response.status, data };
    }

    const rawError = await response.text();
    return {
      ok: false,
      status: response.status,
      error: rawError || "Failed to load event details.",
    };
  } catch {
    return {
      ok: false,
      status: 0,
      error: "Network error. Check server connection and try again.",
    };
  }
}