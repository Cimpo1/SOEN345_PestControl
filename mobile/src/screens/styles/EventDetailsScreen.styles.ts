import { StyleSheet } from "react-native";

export const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#f5f4f0",
  },
  container: {
    padding: 16,
    backgroundColor: "#f5f4f0",
    flexGrow: 1,
    gap: 12,
    width: "100%",
  },
  title: {
    fontSize: 28,
    fontWeight: "800",
    color: "#2f2b27",
  },
  section: {
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#e8e1d3",
    borderRadius: 12,
    padding: 12,
    gap: 4,
    width: "100%",
  },
  label: {
    color: "#8a4b1c",
    fontSize: 12,
    fontWeight: "700",
    textTransform: "uppercase",
  },
  value: {
    color: "#2f2b27",
    fontSize: 14,
  },
  centerArea: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f5f4f0",
    paddingHorizontal: 16,
  },
  errorText: {
    color: "#b04832",
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
  },
  reserveButton: {
    backgroundColor: "#d88b4b",
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: "center",
  },
  reserveButtonDisabled: {
    backgroundColor: "#cbb7a3",
  },
  reserveButtonText: {
    color: "#fff",
    fontWeight: "800",
    fontSize: 15,
  },
  registeredText: {
    color: "#2f2b27",
    fontSize: 13,
    fontWeight: "700",
  },
  infoText: {
    color: "#6f7a86",
    fontSize: 13,
    fontWeight: "600",
  },
});