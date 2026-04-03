export interface SignupPayload {
  fullName: string;
  email: string;
  phoneNumber: string;
  password: string;
}

export interface LoginPayload {
  email: string | null;
  phoneNumber: string | null;
  password: string;
}

export interface AuthUser {
  userId: number;
  fullName: string;
  email: string | null;
  phoneNumber: string | null;
  userRole: string;
}

interface AuthApiResponse<T = unknown> {
  ok: boolean;
  status: number;
  data?: T;
  error?: string;
}

function getBaseUrl() {
  const backendIp = process.env.EXPO_PUBLIC_BACKEND_IP;
  return backendIp ? `http://${backendIp}:8080` : "http://localhost:8080";
}

export async function registerUser(
  payload: SignupPayload,
): Promise<AuthApiResponse> {
  try {
    const response = await fetch(getBaseUrl() + "/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      return { ok: true, status: response.status };
    }

    const rawError = await response.text();
    let friendlyMessage = "Registration failed. Please try again.";

    try {
      const parsedError = JSON.parse(rawError);
      if (typeof parsedError?.message === "string") {
        friendlyMessage = parsedError.message;
      }
    } catch {
      friendlyMessage = rawError || friendlyMessage;
    }

    return {
      ok: false,
      status: response.status,
      error: friendlyMessage,
    };
  } catch {
    return {
      ok: false,
      status: 0,
      error: "Network error. Check server connection and try again.",
    };
  }
}

export async function loginUser(
  payload: LoginPayload,
): Promise<AuthApiResponse<{ token: string; user: AuthUser }>> {
  try {
    const response = await fetch(getBaseUrl() + "/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      const data = await response.json();
      return { ok: true, status: response.status, data };
    }

    if (response.status === 400) {
      return { ok: false, status: 400, error: "Missing credentials." };
    }

    if (response.status === 401) {
      return { ok: false, status: 401, error: "Invalid credentials." };
    }

    return {
      ok: false,
      status: response.status,
      error: "Login failed. Please try again.",
    };
  } catch {
    return {
      ok: false,
      status: 0,
      error: "Network error. Check server connection and try again.",
    };
  }
}