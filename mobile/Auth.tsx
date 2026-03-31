import { useState } from "react";
import {
  Image,
  TouchableOpacity,
  Text,
  View,
  TextInput,
  StyleSheet,
} from "react-native";
import { useAuth } from "./context/AuthContext";

type Tab = "signin" | "signup";

interface FormData {
  fullName: string;
  contact: string;
  email: string;
  phoneNumber: string;
  password: string;
}

export default function AuthPage() {
  const { login } = useAuth();
  const [tab, setTab] = useState<Tab>("signin");
  const [form, setForm] = useState<FormData>({
    fullName: "",
    contact: "",
    email: "",
    phoneNumber: "",
    password: "",
  });

  const handleSubmit = async () => {
    const url = "http://10.0.2.2:8080";
    const { fullName, contact, email, phoneNumber, password } = form;

    if (tab === "signup") {
      const phoneRegex: RegExp = /^\d{3}-\d{3}-\d{4}$/;
      const emailRegex: RegExp = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
      if (!emailRegex.test(email) && !phoneRegex.test(phoneNumber)) {
        console.log("Phone number and Email are invalid");
        return;
      }
      const body = {
        fullName,
        email,
        phoneNumber,
        password,
      };

      try {
        const response = await fetch(url + "/auth/register", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });

        if (response.ok) {
          console.log("Registration successful");
        } else {
          const errorText = await response.text();
          console.error("Registration failed:", errorText);
        }
      } catch (error) {
        console.error("Network error:", error);
      }
    } else {
      const { contact, password } = form;
      const isEmail = contact.includes("@");


      const body = {
        email: isEmail ? contact : null,
        phoneNumber: isEmail ? null : contact,
        password,
      };

      try {
        const response = await fetch(url + "/auth/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });

        if (response.ok) {
          const { token, user } = await response.json();
          console.log("Login successful. Token:", token, "User:", user);
          login(token);

        } else if (response.status === 400) {
          console.error("Login failed: missing credentials");
        } else if (response.status === 401) {
          console.error("Login failed: invalid credentials");
        } else {
          console.error("Login failed: unexpected error", response.status);
        }
      } catch (error) {
        console.error("Network error:", error);
      }
    }
  };

  return (
    <View style={styles.page}>
      <View style={styles.card}>
        <Image
          source={require("./assets/favicon.png")}
          style={{ width: 64, height: 64, alignSelf: "center" }}
        />
        //Tab Switch
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
        //Entries
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
        <TouchableOpacity onPress={handleSubmit} style={styles.btn}>
          <Text style={styles.btnText}>
            {tab === "signin" ? "Sign In" : "Create Account"}
          </Text>
        </TouchableOpacity>
      </View>
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
});


