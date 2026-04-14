import { useRef, useState } from "react";
import {
  Animated,
  Image,
  TextInput,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuth } from "../context/AuthContext";
import { loginUser, registerUser } from "../services/authApi";
import { styles } from "./styles/AuthScreen.styles";

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

      const trimmedEmail = email.trim();
      const trimmedPhoneNumber = phoneNumber.trim();
      const hasEmail = trimmedEmail.length > 0;
      const hasPhone = trimmedPhoneNumber.length > 0;

      if (!hasEmail && !hasPhone) {
        setErrorMessage("Please enter an email, a phone number, or both.");
        return;
      }

      if (hasEmail && !emailRegex.test(trimmedEmail)) {
        setErrorMessage("Please enter a valid email address.");
        return;
      }

      if (hasPhone && !phoneRegex.test(trimmedPhoneNumber)) {
        setErrorMessage(
          "Please enter a valid phone number (e.g., 123-456-7890).",
        );
        return;
      }

      const result = await registerUser({
        fullName,
        email: hasEmail ? trimmedEmail : null,
        phoneNumber: hasPhone ? trimmedPhoneNumber : null,
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
          contact: hasEmail ? trimmedEmail : trimmedPhoneNumber,
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

    if (result.ok && result.data?.token && result.data.user) {
      login(result.data.token, result.data.user);
      return;
    }

    setErrorMessage(result.error || "Login failed. Please try again.");
  };

  const successScale = successAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [0.92, 1],
  });

  return (
    <SafeAreaView style={styles.page} edges={["top", "bottom"]}>
      <View style={styles.card}>
        <Text style={styles.welcomeTitle}>
          Welcome to <Text style={styles.appNameHighlight}>Event Control</Text>
        </Text>
        <Text style={styles.welcomeSubtext}>by Pest Control</Text>
        <Image
          source={require("../../assets/EventControlLogo.png")}
          style={{ width: 96, height: 96, alignSelf: "center" }}
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
    </SafeAreaView>
  );
}
