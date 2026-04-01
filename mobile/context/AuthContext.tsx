import { createContext, useContext, useState, ReactNode } from "react";

type Screen = "Auth" | "Home";

interface AuthContextType {
  token: string | null;
  currentScreen: Screen;
  login: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken]               = useState<string | null>(null);
  const [currentScreen, setScreen]      = useState<Screen>("Auth");

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
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}