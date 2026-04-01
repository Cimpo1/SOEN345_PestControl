import { useRef, useState } from "react";
import {
  Animated,
  Image,
  TouchableOpacity,
  Text,
  View,
  TextInput,
  StyleSheet,
} from "react-native";
import { useAuth } from "../context/AuthContext";
import { loginUser, registerUser } from "../services/authApi";

type Tab = "signin" | "signup";

interface FormData {
  fullName: string;
  contact: string;
  email: string;
  phoneNumber: string;
  password: string;
}

export default function AuthScreen() {
  const { login } = useAuth();
  const [showSuccess, setShowSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const successAnim = useRef(new Animated.Value(0)).current;
  const [tab, setTab] = useState<Tab>("signin");
  const [form, setForm] = useState<FormData>({
    fullName: "",
    contact: "",
    email: "",
    phoneNumber: "",
    password: "",
  });

  const playRegistrationSuccess = () => {
    setShowSuccess(true);
    successAnim.setValue(0);

    Animated.sequence([
      Animated.timing(successAnim, {
        toValue: 1,
        duration: 260,
        useNativeDriver: true,
      }),
      Animated.delay(900),
      Animated.timing(successAnim, {
        toValue: 0,
        duration: 220,
        useNativeDriver: true,
      }),
    ]).start(() => {
      setShowSuccess(false);
    });
  };

  const handleSubmit = async () => {
    const { fullName, contact, email, phoneNumber, password } = form;
    setErrorMessage(null);

    if (tab === "signup") {
      const phoneRegex: RegExp = /^\d{3}-\d{3}-\d{4}$/;
      const emailRegex: RegExp =
        /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
      if (!emailRegex.test(email) && !phoneRegex.test(phoneNumber)) {
        setErrorMessage("Please enter a valid email and phone number.");
        return;
      }

      const result = await registerUser({
        fullName,
        email,
        phoneNumber,
        password,
      });

      if (result.ok) {
        playRegistrationSuccess();
        setTab("signin");
        setForm((prev) => ({
          ...prev,
          fullName: "",
          email: "",
          phoneNumber: "",
          contact: email,
          password: "",
        }));
      } else {
        setErrorMessage(
          result.error || "Registration failed. Please try again.",
        );
      }

      return;
    }

    const isEmail = contact.includes("@");

    const result = await loginUser({
      email: isEmail ? contact : null,
      phoneNumber: isEmail ? null : contact,
      password,
    });

    if (result.ok && result.data?.token) {
      login(result.data.token);
      return;
    }

    setErrorMessage(result.error || "Login failed. Please try again.");
  };

  const successScale = successAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [0.92, 1],
  });

  return (
    <View style={styles.page}>
      <View style={styles.card}>
        <Image
          source={require("../../assets/favicon.png")}
          style={{ width: 64, height: 64, alignSelf: "center" }}
        />
        <View style={styles.tabs}>
          {(["signin", "signup"] as Tab[]).map((t) => (
            <TouchableOpacity
              key={t}
              onPress={() => setTab(t)}
              style={[styles.tab, tab === t && styles.activeTab]}
            >
              <Text style={[styles.tabText, tab === t && styles.activeTabText]}>
                {t === "signin" ? "Sign In" : "Sign Up"}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {tab === "signup" && (
          <>
            <TextInput
              placeholder="Name"
              placeholderTextColor="#666"
              value={form.fullName}
              onChangeText={(val) => setForm({ ...form, fullName: val })}
              style={styles.input}
            />
            <TextInput
              placeholder="Email"
              placeholderTextColor="#666"
              value={form.email}
              onChangeText={(val) => setForm({ ...form, email: val })}
              style={styles.input}
            />
            <TextInput
              placeholder="Phone number"
              placeholderTextColor="#666"
              value={form.phoneNumber}
              onChangeText={(val) => setForm({ ...form, phoneNumber: val })}
              style={styles.input}
            />
          </>
        )}

        {tab === "signin" && (
          <TextInput
            placeholder="Email or phone number"
            placeholderTextColor="#666"
            value={form.contact}
            onChangeText={(val) => setForm({ ...form, contact: val })}
            style={styles.input}
          />
        )}

        <TextInput
          placeholder="Password"
          placeholderTextColor="#666"
          secureTextEntry
          value={form.password}
          onChangeText={(val) => setForm({ ...form, password: val })}
          style={styles.input}
        />
        {!!errorMessage && <Text style={styles.errorText}>{errorMessage}</Text>}
        <TouchableOpacity onPress={handleSubmit} style={styles.btn}>
          <Text style={styles.btnText}>
            {tab === "signin" ? "Sign In" : "Create Account"}
          </Text>
        </TouchableOpacity>
      </View>

      {showSuccess && (
        <View pointerEvents="none" style={styles.successOverlay}>
          <Animated.View
            style={[
              styles.successCard,
              { opacity: successAnim, transform: [{ scale: successScale }] },
            ]}
          >
            <Text style={styles.successTitle}>Registration successful</Text>
            <Text style={styles.successSubtitle}>You can now sign in.</Text>
          </Animated.View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  page: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#0f0f0f",
  },
  card: {
    backgroundColor: "#1a1a1a",
    borderWidth: 1,
    borderColor: "#2e2e2e",
    borderRadius: 12,
    padding: 36,
    width: 340,
    gap: 14,
  },
  tabs: {
    flexDirection: "row",
    borderBottomWidth: 1,
    borderBottomColor: "#2e2e2e",
    marginBottom: 6,
  },
  tab: { flex: 1, paddingVertical: 10, alignItems: "center" },
  activeTab: { borderBottomWidth: 2, borderBottomColor: "#c8a97e" },
  tabText: { color: "#666", fontSize: 14, letterSpacing: 0.4 },
  activeTabText: { color: "#f0e6d3" },
  input: {
    backgroundColor: "#111",
    borderWidth: 1,
    borderColor: "#2e2e2e",
    borderRadius: 8,
    color: "#f0e6d3",
    fontSize: 14,
    paddingVertical: 11,
    paddingHorizontal: 14,
  },
  errorText: {
    color: "#ff9f9f",
    fontSize: 13,
  },
  btn: {
    marginTop: 4,
    backgroundColor: "#c8a97e",
    borderRadius: 8,
    paddingVertical: 13,
    alignItems: "center",
  },
  btnText: {
    color: "#0f0f0f",
    fontSize: 14,
    fontWeight: "700",
    letterSpacing: 0.5,
  },
  successOverlay: {
    ...StyleSheet.absoluteFillObject,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(15, 15, 15, 0.35)",
  },
  successCard: {
    backgroundColor: "#183022",
    borderWidth: 1,
    borderColor: "#2e5a3f",
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 18,
    minWidth: 240,
    alignItems: "center",
  },
  successTitle: {
    color: "#b9f5cc",
    fontSize: 15,
    fontWeight: "700",
    marginBottom: 2,
  },
  successSubtitle: {
    color: "#ddf7e4",
    fontSize: 13,
  },
});
