import { StyleSheet } from "react-native";

export const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: "#f5f4f0",
    },
    container: {
        padding: 16,
        gap: 8,
        paddingBottom: 30,
    },
    heading: {
        fontSize: 26,
        fontWeight: "800",
        color: "#2f2b27",
        marginBottom: 8,
    },
    label: {
        fontWeight: "700",
        color: "#4a4138",
        marginTop: 4,
    },
    input: {
        borderWidth: 1,
        borderColor: "#d8d3c7",
        borderRadius: 10,
        paddingHorizontal: 12,
        paddingVertical: 10,
        fontSize: 14,
        backgroundColor: "#fffdf8",
        color: "#2f2b27",
    },
    dateTimeValueBox: {
        borderWidth: 1,
        borderColor: "#d8d3c7",
        borderRadius: 10,
        paddingHorizontal: 12,
        paddingVertical: 10,
        backgroundColor: "#fffdf8",
    },
    dateTimeValueText: {
        color: "#2f2b27",
        fontSize: 14,
    },
    categoriesWrap: {
        flexDirection: "row",
        flexWrap: "wrap",
        gap: 8,
        marginBottom: 4,
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
        marginTop: 8,
    },
    modeButton: {
        borderRadius: 8,
        borderWidth: 1,
        borderColor: "#cdb9a2",
        backgroundColor: "#fff",
        paddingHorizontal: 10,
        paddingVertical: 8,
    },
    modeButtonSelected: {
        borderColor: "#d88b4b",
        backgroundColor: "#fde8d6",
    },
    modeButtonText: {
        color: "#7a5d42",
        fontWeight: "700",
        fontSize: 12,
    },
    modeButtonTextSelected: {
        color: "#8a4b1c",
    },
    submitButton: {
        backgroundColor: "#d88b4b",
        borderRadius: 10,
        paddingVertical: 11,
        paddingHorizontal: 14,
    },
    submitButtonText: {
        color: "#fff",
        fontWeight: "700",
    },
    cancelButton: {
        borderRadius: 10,
        borderWidth: 1,
        borderColor: "#cdb9a2",
        paddingVertical: 11,
        paddingHorizontal: 14,
        backgroundColor: "#fff",
    },
    cancelButtonText: {
        color: "#7a5d42",
        fontWeight: "700",
    },
    buttonDisabled: {
        opacity: 0.6,
    },
    errorText: {
        color: "#b04832",
        fontSize: 13,
        fontWeight: "600",
        marginTop: 6,
    },
    centerArea: {
        flex: 1,
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: "#f5f4f0",
    },
    pickerModalBackdrop: {
        flex: 1,
        backgroundColor: "rgba(0, 0, 0, 0.35)",
        justifyContent: "center",
        paddingHorizontal: 20,
    },
    pickerModalCard: {
        backgroundColor: "#fffdf8",
        borderRadius: 14,
        paddingHorizontal: 12,
        paddingTop: 14,
        paddingBottom: 12,
        borderWidth: 1,
        borderColor: "#e2dbcf",
    },
    pickerModalTitle: {
        fontSize: 16,
        fontWeight: "800",
        color: "#2f2b27",
        marginBottom: 4,
        textAlign: "center",
    },
    pickerModalActions: {
        flexDirection: "row",
        justifyContent: "flex-end",
        gap: 8,
        marginTop: 4,
    },
});
