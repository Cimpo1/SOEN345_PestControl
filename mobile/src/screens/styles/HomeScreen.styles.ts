import { StyleSheet } from "react-native";

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#f5f4f0",
    paddingHorizontal: 16,
    paddingTop: 12,
    gap: 8,
  },
  title: {
    fontSize: 28,
    fontWeight: "800",
    color: "#2f2b27",
  },
  subtitle: {
    fontSize: 14,
    color: "#5b5147",
  },
  list: {
    flex: 1,
    width: "100%",
  },
  listContent: {
    paddingVertical: 8,
    gap: 10,
  },
  card: {
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#e8e1d3",
    borderRadius: 12,
    padding: 14,
    gap: 4,
  },
  cardTitle: {
    color: "#2f2b27",
    fontWeight: "800",
    fontSize: 16,
  },
  cardMeta: {
    color: "#5b5147",
    fontSize: 13,
  },
  logoutButton: {
    marginBottom: 16,
    backgroundColor: "#2f2b27",
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: "center",
  },
  logoutButtonText: {
    color: "#fff",
    fontSize: 14,
    fontWeight: "800",
  },
  centerArea: {
    alignItems: "center",
    paddingTop: 24,
  },
  emptyText: {
    color: "#6f7a86",
    fontSize: 14,
  },
  errorText: {
    color: "#b04832",
    fontSize: 13,
    fontWeight: "600",
    textAlign: "center",
  },
});