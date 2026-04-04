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

export interface ReservationItem {
  reservationId: number;
  reservationStatus: string;
  interactionStatus: "REGISTERED" | "PASSED" | "CANCELLED";
  creationDate: string;
  ticketCount: number;
  event: EventItem;
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

function authHeaders(token: string) {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };
}

export async function reserveEvent(
  token: string,
  eventId: number,
  quantity: number,
): Promise<ApiResponse<ReservationItem>> {
  try {
    const response = await fetch(`${getBaseUrl()}/reservations`, {
      method: "POST",
      headers: authHeaders(token),
      body: JSON.stringify({ eventId, quantity }),
    });

    if (response.ok) {
      const data = (await response.json()) as ReservationItem;
      return { ok: true, status: response.status, data };
    }

    const rawError = await response.text();
    return {
      ok: false,
      status: response.status,
      error: rawError || "Failed to reserve event.",
    };
  } catch {
    return {
      ok: false,
      status: 0,
      error: "Network error. Check server connection and try again.",
    };
  }
}

export async function fetchCurrentReservations(
  token: string,
): Promise<ApiResponse<ReservationItem[]>> {
  try {
    const response = await fetch(`${getBaseUrl()}/reservations/current`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      const data = (await response.json()) as ReservationItem[];
      return { ok: true, status: response.status, data };
    }

    const rawError = await response.text();
    return {
      ok: false,
      status: response.status,
      error: rawError || "Failed to load reservations.",
    };
  } catch {
    return {
      ok: false,
      status: 0,
      error: "Network error. Check server connection and try again.",
    };
  }
}

export async function fetchInteractedReservations(
  token: string,
): Promise<ApiResponse<ReservationItem[]>> {
  try {
    const response = await fetch(`${getBaseUrl()}/reservations/interacted`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      const data = (await response.json()) as ReservationItem[];
      return { ok: true, status: response.status, data };
    }

    const rawError = await response.text();
    return {
      ok: false,
      status: response.status,
      error: rawError || "Failed to load interacted events.",
    };
  } catch {
    return {
      ok: false,
      status: 0,
      error: "Network error. Check server connection and try again.",
    };
  }
}

export async function cancelReservation(
  token: string,
  reservationId: number,
): Promise<ApiResponse<ReservationItem>> {
  try {
    const response = await fetch(`${getBaseUrl()}/reservations/${reservationId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      const data = (await response.json()) as ReservationItem;
      return { ok: true, status: response.status, data };
    }

    const rawError = await response.text();
    return {
      ok: false,
      status: response.status,
      error: rawError || "Failed to cancel reservation.",
    };
  } catch {
    return {
      ok: false,
      status: 0,
      error: "Network error. Check server connection and try again.",
    };
  }
}