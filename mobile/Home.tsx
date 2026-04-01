import { useAuth } from "./context/AuthContext";
import {
  TouchableOpacity,
  Text,
  View,
  StyleSheet,
  Button
} from "react-native";
import { StatusBar } from "expo-status-bar";

export default function HomePage() {
  const {token, logout } = useAuth();
  return (
    <View style={styles.container}>
      <Text>Navigate your app!</Text>

      <Button
        title="Logout"
        onPress={logout}
      />

      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
    alignItems: "center",
    justifyContent: "center",
  },
});