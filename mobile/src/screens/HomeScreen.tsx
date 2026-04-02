import { Text, View, StyleSheet, Button } from "react-native";
import { StatusBar } from "expo-status-bar";
import { useAuth } from "../context/AuthContext";

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

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#f5f4f0",
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
    gap: 10,
  },
  title: {
    fontSize: 28,
    fontWeight: "800",
    color: "#2f2b27",
  },
  subtitle: {
    fontSize: 14,
    color: "#5b5147",
    textAlign: "center",
  },
});
