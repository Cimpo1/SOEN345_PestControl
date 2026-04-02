import { createContext, useContext, useState, ReactNode } from "react";

type Screen = "Auth" | "Home";

interface AuthContextType {
  token: string | null;
  currentScreen: Screen;
  login: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

const isE2ePreAuthEnabled = process.env.EXPO_PUBLIC_E2E_PREAUTH === "1";

function getInitialToken() {
  return isE2ePreAuthEnabled ? "e2e-token" : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(getInitialToken);
  const [currentScreen, setScreen] = useState<Screen>(
    isE2ePreAuthEnabled ? "Home" : "Auth",
  );

  const login = (newToken: string) => {
    setToken(newToken);
    setScreen("Home");
  };

  const logout = () => {
    setToken(null);
    setScreen("Auth");
  };

  return (
    <AuthContext.Provider value={{ token, currentScreen, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }
  return ctx;
}
