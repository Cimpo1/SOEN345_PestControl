import { useState } from "react";
import { Image, TouchableOpacity, Text, View, TextInput, StyleSheet } from 'react-native';

type Tab = "signin" | "signup";

interface FormData {
  name: string;
  contact: string;
  password: string;
}

export default function AuthPage() {
  const [tab, setTab] = useState<Tab>("signin");
  const [form, setForm] = useState<FormData>({ name: "", contact: "", password: "" });

  const handleSubmit = () => {
    const { name, contact, password } = form;
    if (tab === "signup") console.log("Sign up:", { name, contact, password });
    else console.log("Sign in:", { contact, password });
  };

  return (
    <View style={styles.page}>
      <View style={styles.card}>
        <Image source={require('./assets/favicon.png')} style={{ width: 64, height: 64, alignSelf: 'center' }} />

         //Tab Switch
        <View style={styles.tabs}>
          {(["signin", "signup"] as Tab[]).map((t) => (
            <TouchableOpacity key={t} onPress={() => setTab(t)} style={[styles.tab, tab === t && styles.activeTab]}>
              <Text style={[styles.tabText, tab === t && styles.activeTabText]}>
                {t === "signin" ? "Sign In" : "Sign Up"}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        //Entries
        {tab === "signup" && (
          <TextInput
            placeholder="Name"
            placeholderTextColor="#666"
            value={form.name}
            onChangeText={(val) => setForm({ ...form, name: val })}
            style={styles.input}
          />
        )}
        <TextInput
          placeholder="Email or phone number"
          placeholderTextColor="#666"
          value={form.contact}
          onChangeText={(val) => setForm({ ...form, contact: val })}
          style={styles.input}
        />
        <TextInput
          placeholder="Password"
          placeholderTextColor="#666"
          secureTextEntry
          value={form.password}
          onChangeText={(val) => setForm({ ...form, password: val })}
          style={styles.input}
        />

        <TouchableOpacity onPress={handleSubmit} style={styles.btn}>
          <Text style={styles.btnText}>{tab === "signin" ? "Sign In" : "Create Account"}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, alignItems: "center", justifyContent: "center", backgroundColor: "#0f0f0f" },
  card: { backgroundColor: "#1a1a1a", borderWidth: 1, borderColor: "#2e2e2e", borderRadius: 12, padding: 36, width: 340, gap: 14 },
  tabs: { flexDirection: "row", borderBottomWidth: 1, borderBottomColor: "#2e2e2e", marginBottom: 6 },
  tab: { flex: 1, paddingVertical: 10, alignItems: "center" },
  activeTab: { borderBottomWidth: 2, borderBottomColor: "#c8a97e" },
  tabText: { color: "#666", fontSize: 14, letterSpacing: 0.4 },
  activeTabText: { color: "#f0e6d3" },
  input: { backgroundColor: "#111", borderWidth: 1, borderColor: "#2e2e2e", borderRadius: 8, color: "#f0e6d3", fontSize: 14, paddingVertical: 11, paddingHorizontal: 14 },
  btn: { marginTop: 4, backgroundColor: "#c8a97e", borderRadius: 8, paddingVertical: 13, alignItems: "center" },
  btnText: { color: "#0f0f0f", fontSize: 14, fontWeight: "700", letterSpacing: 0.5 },
});