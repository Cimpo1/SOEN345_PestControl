import { AuthProvider, useAuth } from "../context/AuthContext";
import AuthScreen from "../screens/AuthScreen";
import HomeScreen from "../screens/HomeScreen";

function Navigator() {
  const { currentScreen } = useAuth();

  switch (currentScreen) {
    case "Home":
      return <HomeScreen />;
    default:
      return <AuthScreen />;
  }
}

export default function App() {
  return (
    <AuthProvider>
      <Navigator />
    </AuthProvider>
  );
}
