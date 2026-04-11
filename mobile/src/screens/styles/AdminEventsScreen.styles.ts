import { StyleSheet } from "react-native";

export const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: "#f5f4f0",
    },
    container: {
        flex: 1,
    },
    listContent: {
        paddingHorizontal: 16,
        paddingTop: 8,
        paddingBottom: 24,
        gap: 10,
    },
    heading: {
        fontSize: 28,
        fontWeight: "800",
        color: "#2f2b27",
        marginBottom: 12,
    },
    createButton: {
        backgroundColor: "#d88b4b",
        borderRadius: 10,
        paddingVertical: 10,
        paddingHorizontal: 14,
        alignSelf: "flex-start",
        marginBottom: 10,
    },
    createButtonText: {
        color: "#ffffff",
        fontWeight: "700",
    },
    flashText: {
        color: "#0f7a34",
        fontWeight: "600",
        marginBottom: 8,
    },
    sectionBlock: {
        gap: 10,
    },
    sectionHeader: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        paddingVertical: 4,
    },
    sectionHeaderTextWrap: {
        flexDirection: "row",
        alignItems: "baseline",
        gap: 10,
    },
    sectionTitle: {
        marginTop: 10,
        marginBottom: 4,
        fontSize: 18,
        fontWeight: "800",
        color: "#433b33",
    },
    sectionCount: {
        color: "#7a5d42",
        fontSize: 12,
        fontWeight: "700",
    },
    sectionChevron: {
        color: "#7a5d42",
        fontSize: 22,
        fontWeight: "800",
        paddingHorizontal: 4,
    },
    sectionContent: {
        gap: 10,
    },
    sectionItemSpacing: {
        gap: 10,
    },
    emptySectionText: {
        color: "#6f7a86",
        fontSize: 14,
        marginTop: 2,
        marginBottom: 4,
    },
    eventCard: {
        backgroundColor: "#fff",
        borderWidth: 1,
        borderColor: "#e8e1d3",
        borderRadius: 12,
        padding: 14,
        gap: 4,
    },
    eventTitle: {
        color: "#2f2b27",
        fontWeight: "800",
        fontSize: 16,
    },
    eventMeta: {
        color: "#5b5147",
        fontSize: 13,
    },
    statusText: {
        color: "#6a4234",
        fontWeight: "700",
        fontSize: 12,
        marginTop: 4,
    },
    actionsRow: {
        flexDirection: "row",
        gap: 8,
        marginTop: 8,
    },
    editButton: {
        backgroundColor: "#3f6e9e",
        borderRadius: 8,
        paddingHorizontal: 10,
        paddingVertical: 8,
    },
    editButtonText: {
        color: "#fff",
        fontWeight: "700",
        fontSize: 12,
    },
    cancelButton: {
        backgroundColor: "#a74633",
        borderRadius: 8,
        paddingHorizontal: 10,
        paddingVertical: 8,
    },
    cancelButtonText: {
        color: "#fff",
        fontWeight: "700",
        fontSize: 12,
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
    },
});
