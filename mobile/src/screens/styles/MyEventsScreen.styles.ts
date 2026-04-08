import { StyleSheet } from "react-native";

export const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: "#f5f4f0",
        paddingHorizontal: 16,
        paddingTop: 12,
    },
    headerContent: {
        width: "100%",
    },
    title: {
        fontSize: 28,
        fontWeight: "800",
        color: "#2f2b27",
        marginBottom: 10,
    },
    searchInput: {
        borderWidth: 1,
        borderColor: "#d8d3c7",
        borderRadius: 12,
        paddingHorizontal: 14,
        paddingVertical: 12,
        fontSize: 15,
        backgroundColor: "#fffdf8",
        color: "#2f2b27",
        marginBottom: 12,
    },
    filtersCard: {
        backgroundColor: "#fff",
        borderRadius: 14,
        borderWidth: 1,
        borderColor: "#e8e1d3",
        padding: 12,
        marginBottom: 12,
        gap: 10,
    },
    filtersTitle: {
        fontWeight: "700",
        color: "#2f2b27",
        fontSize: 16,
    },
    filtersSubtitle: {
        fontWeight: "700",
        color: "#2f2b27",
        fontSize: 13,
        marginTop: 2,
    },
    filtersHeader: {
        flexDirection: "row",
        justifyContent: "space-between",
        alignItems: "center",
    },
    filtersArrow: {
        color: "#7a5d42",
        fontSize: 18,
        fontWeight: "700",
        width: 20,
        textAlign: "center",
    },
    filterInput: {
        borderWidth: 1,
        borderColor: "#d8d3c7",
        borderRadius: 10,
        paddingHorizontal: 12,
        paddingVertical: 10,
        fontSize: 14,
        backgroundColor: "#fffdf8",
        color: "#2f2b27",
    },
    rowInputs: {
        flexDirection: "row",
        gap: 8,
    },
    halfInput: {
        flex: 1,
    },
    categoriesWrap: {
        flexDirection: "row",
        flexWrap: "wrap",
        gap: 8,
    },
    categoryChip: {
        borderWidth: 1,
        borderColor: "#d8d3c7",
        borderRadius: 999,
        paddingHorizontal: 10,
        paddingVertical: 6,
        backgroundColor: "#fffdf8",
    },
    categoryChipSelected: {
        borderColor: "#d88b4b",
        backgroundColor: "#fde8d6",
    },
    categoryChipText: {
        color: "#5b5147",
        fontWeight: "600",
        fontSize: 12,
    },
    categoryChipTextSelected: {
        color: "#8a4b1c",
    },
    actionsRow: {
        flexDirection: "row",
        gap: 8,
    },
    applyButton: {
        backgroundColor: "#d88b4b",
        borderRadius: 10,
        paddingVertical: 10,
        paddingHorizontal: 12,
    },
    applyButtonText: {
        color: "#fff",
        fontWeight: "700",
    },
    resetButton: {
        borderRadius: 10,
        borderWidth: 1,
        borderColor: "#cdb9a2",
        paddingVertical: 10,
        paddingHorizontal: 12,
        backgroundColor: "#fff",
    },
    resetButtonText: {
        color: "#7a5d42",
        fontWeight: "700",
    },
    listContent: {
        paddingBottom: 24,
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
    rowTop: {
        flexDirection: "row",
        justifyContent: "space-between",
        alignItems: "center",
        gap: 10,
    },
    cardTitle: {
        flex: 1,
        color: "#2f2b27",
        fontWeight: "800",
        fontSize: 16,
    },
    badge: {
        borderRadius: 999,
        paddingHorizontal: 10,
        paddingVertical: 4,
    },
    registeredBadge: {
        backgroundColor: "#fde8d6",
    },
    passedBadge: {
        backgroundColor: "#d9dde3",
    },
    cancelledBadge: {
        backgroundColor: "#f8d8d2",
    },
    badgeText: {
        color: "#2f2b27",
        fontSize: 11,
        fontWeight: "700",
    },
    meta: {
        color: "#5b5147",
        fontSize: 13,
    },
    centerArea: {
        paddingTop: 30,
        alignItems: "center",
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
