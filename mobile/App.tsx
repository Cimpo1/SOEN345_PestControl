import { AuthProvider, useAuth } from "./context/AuthContext";
import AuthPage from "./Auth";
import HomePage from "./Home";

function Navigator() {
  const { currentScreen } = useAuth();

  switch (currentScreen) {
    case "Home": return <HomePage />;
    default:     return <AuthPage />;
  }
}

export default function App() {
  return (
    <AuthProvider>
      <Navigator />
    </AuthProvider>
  );
}
