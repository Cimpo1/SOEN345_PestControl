import { NavigationContainer } from "@react-navigation/native";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { AuthProvider, useAuth } from "../context/AuthContext";
import AuthScreen from "../screens/AuthScreen";
import AppTabs from "../navigation/AppTabs";

function Navigator() {
  const { token } = useAuth();

  if (!token) {
    return <AuthScreen />;
  }

  return (
    <NavigationContainer>
      <AppTabs />
    </NavigationContainer>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <Navigator />
      </AuthProvider>
    </SafeAreaProvider>
  );
}
