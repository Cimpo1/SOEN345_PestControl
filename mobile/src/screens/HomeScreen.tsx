import { Button, Text, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { useAuth } from "../context/AuthContext";
import { styles } from "./styles/HomeScreen.styles";

export default function HomeScreen() {
  const { logout } = useAuth();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Welcome</Text>
      <Text style={styles.subtitle}>
        Tap the Events icon below to browse events.
      </Text>

      <Button title="Logout" onPress={logout} />

      <StatusBar style="auto" />
    </View>
  );
}
