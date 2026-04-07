import { useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from "react-native";
import DateTimePicker, {
  type DateTimePickerEvent,
} from "@react-native-community/datetimepicker";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuth } from "../context/AuthContext";
import {
  createEvent,
  fetchEventById,
  updateEvent,
  type EventLocationInput,
  type UpsertEventPayload,
} from "../services/eventsApi";
import type { AdminEventsStackParamList } from "../navigation/AdminEventsStack";
import { styles } from "./styles/AdminEventFormScreen.styles";

type Props =
  | NativeStackScreenProps<AdminEventsStackParamList, "AdminCreateEvent">
  | NativeStackScreenProps<AdminEventsStackParamList, "AdminEditEvent">;

const CATEGORY_OPTIONS = [
  "CONCERT",
  "SPORTS",
  "ART_THEATER",
  "COMEDY",
  "FAMILY",
] as const;

type PickerTarget = {
  field: "start" | "end";
  mode: "date" | "time";
};

function parseApiDate(value: string) {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function formatSelectedDateTime(value: Date | null) {
  if (!value) {
    return "Not selected";
  }
  return value.toLocaleString();
}

function toOffsetDateTimeString(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  const hours = String(value.getHours()).padStart(2, "0");
  const minutes = String(value.getMinutes()).padStart(2, "0");
  const seconds = String(value.getSeconds()).padStart(2, "0");

  const offsetMinutes = -value.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? "+" : "-";
  const absoluteOffset = Math.abs(offsetMinutes);
  const offsetHours = String(Math.floor(absoluteOffset / 60)).padStart(2, "0");
  const remainingOffsetMinutes = String(absoluteOffset % 60).padStart(2, "0");

  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}${sign}${offsetHours}:${remainingOffsetMinutes}`;
}

function withUpdatedDatePart(base: Date, picked: Date) {
  const updated = new Date(base);
  updated.setFullYear(
    picked.getFullYear(),
    picked.getMonth(),
    picked.getDate(),
  );
  return updated;
}

function withUpdatedTimePart(base: Date, picked: Date) {
  const updated = new Date(base);
  updated.setHours(picked.getHours(), picked.getMinutes(), 0, 0);
  return updated;
}

export default function AdminEventFormScreen({ navigation, route }: Props) {
  const { token } = useAuth();
  const isEdit = route.name === "AdminEditEvent";
  const eventId = isEdit ? route.params.eventId : null;

  const [loading, setLoading] = useState(isEdit);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [startDateTime, setStartDateTime] = useState<Date | null>(null);
  const [endDateTime, setEndDateTime] = useState<Date | null>(null);
  const [activePicker, setActivePicker] = useState<PickerTarget | null>(null);
  const [pickerDraftDate, setPickerDraftDate] = useState<Date>(new Date());
  const [category, setCategory] = useState<string>("CONCERT");
  const [basePrice, setBasePrice] = useState("");

  const [locationMode, setLocationMode] = useState<"existing" | "new">(
    "existing",
  );
  const [locationId, setLocationId] = useState("");
  const [locationName, setLocationName] = useState("");
  const [locationAddressLine, setLocationAddressLine] = useState("");
  const [locationCity, setLocationCity] = useState("");
  const [locationProvince, setLocationProvince] = useState("");
  const [locationPostalCode, setLocationPostalCode] = useState("");

  useEffect(() => {
    if (!isEdit || !eventId || !token) {
      return;
    }

    let mounted = true;

    const loadEvent = async () => {
      setLoading(true);
      setError(null);

      const result = await fetchEventById(eventId);
      if (!mounted) {
        return;
      }

      if (!result.ok || !result.data) {
        setError(result.error || "Failed to load event.");
        setLoading(false);
        return;
      }

      const event = result.data;
      setTitle(event.title);
      setStartDateTime(parseApiDate(event.startDateTime));
      setEndDateTime(parseApiDate(event.endDateTime));
      setCategory(event.category);
      setBasePrice(String(event.basePrice));
      setLocationMode("existing");
      setLocationId(String(event.location.locationId));
      setLoading(false);
    };

    void loadEvent();

    return () => {
      mounted = false;
    };
  }, [isEdit, eventId, token]);

  const categoryChips = useMemo(
    () =>
      CATEGORY_OPTIONS.map((option) => {
        const selected = option === category;
        return (
          <Pressable
            key={option}
            style={[
              styles.categoryChip,
              selected && styles.categoryChipSelected,
            ]}
            onPress={() => setCategory(option)}
          >
            <Text
              style={[
                styles.categoryChipText,
                selected && styles.categoryChipTextSelected,
              ]}
            >
              {option.replaceAll("_", " ")}
            </Text>
          </Pressable>
        );
      }),
    [category],
  );

  const buildLocationInput = (): {
    locationId?: number;
    location?: EventLocationInput;
  } | null => {
    if (locationMode === "existing") {
      const parsedLocationId = Number(locationId);
      if (!Number.isInteger(parsedLocationId) || parsedLocationId <= 0) {
        setError("Location ID must be a valid positive integer.");
        return null;
      }

      return { locationId: parsedLocationId };
    }

    if (
      !locationName.trim() ||
      !locationAddressLine.trim() ||
      !locationCity.trim() ||
      !locationProvince.trim() ||
      !locationPostalCode.trim()
    ) {
      setError(
        "All location fields are required when creating a new location.",
      );
      return null;
    }

    return {
      location: {
        name: locationName.trim(),
        addressLine: locationAddressLine.trim(),
        city: locationCity.trim(),
        province: locationProvince.trim(),
        postalCode: locationPostalCode.trim(),
      },
    };
  };

  const onOpenPicker = (field: "start" | "end", mode: "date" | "time") => {
    const currentValue = field === "start" ? startDateTime : endDateTime;
    setPickerDraftDate(currentValue ?? new Date());
    setActivePicker({ field, mode });
  };

  const onPickerChange = (event: DateTimePickerEvent, selected?: Date) => {
    if (!activePicker) {
      return;
    }

    if (event.type === "dismissed" || !selected) {
      setActivePicker(null);
      return;
    }

    setPickerDraftDate(selected);

    if (Platform.OS === "ios") {
      return;
    }

    const currentValue =
      activePicker.field === "start" ? startDateTime : endDateTime;
    const baseValue = currentValue ?? new Date();
    const nextValue =
      activePicker.mode === "date"
        ? withUpdatedDatePart(baseValue, selected)
        : withUpdatedTimePart(baseValue, selected);

    if (activePicker.field === "start") {
      setStartDateTime(nextValue);
    } else {
      setEndDateTime(nextValue);
    }

    setActivePicker(null);
  };

  const onConfirmPicker = () => {
    if (!activePicker) {
      return;
    }

    const currentValue =
      activePicker.field === "start" ? startDateTime : endDateTime;
    const baseValue = currentValue ?? new Date();
    const nextValue =
      activePicker.mode === "date"
        ? withUpdatedDatePart(baseValue, pickerDraftDate)
        : withUpdatedTimePart(baseValue, pickerDraftDate);

    if (activePicker.field === "start") {
      setStartDateTime(nextValue);
    } else {
      setEndDateTime(nextValue);
    }

    setActivePicker(null);
  };

  const onSubmit = async () => {
    if (!token) {
      setError("Authentication required.");
      return;
    }

    setError(null);

    const parsedPrice = Number(basePrice);
    if (!title.trim()) {
      setError("Title is required.");
      return;
    }

    if (!startDateTime || !endDateTime) {
      setError("Start and end date/time are required.");
      return;
    }

    if (endDateTime.getTime() <= startDateTime.getTime()) {
      setError("End date/time must be after start date/time.");
      return;
    }

    if (!Number.isFinite(parsedPrice) || parsedPrice <= 0) {
      setError("Base price must be greater than 0.");
      return;
    }

    const locationPayload = buildLocationInput();
    if (!locationPayload) {
      return;
    }

    const payload: UpsertEventPayload = {
      title: title.trim(),
      startDateTime: toOffsetDateTimeString(startDateTime),
      endDateTime: toOffsetDateTimeString(endDateTime),
      category,
      basePrice: parsedPrice,
      ...locationPayload,
    };

    setSubmitting(true);

    const result =
      isEdit && eventId
        ? await updateEvent(token, eventId, payload)
        : await createEvent(token, payload);

    setSubmitting(false);

    if (!result.ok) {
      setError(result.error || "Unable to save event.");
      return;
    }

    navigation.navigate("AdminEventsList", {
      flashMessage: isEdit
        ? "Event editing was successful."
        : "Event creation was successful.",
    });
  };

  const onCancel = () => {
    navigation.navigate("AdminEventsList", {
      flashMessage: isEdit
        ? "Event editing was cancelled."
        : "Event creation was cancelled.",
    });
  };

  if (loading) {
    return (
      <View style={styles.centerArea}>
        <ActivityIndicator size="large" color="#d88b4b" />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.heading}>
          {isEdit ? "Edit Event" : "Create Event"}
        </Text>

        <Text style={styles.label}>Title</Text>
        <TextInput
          style={styles.input}
          value={title}
          onChangeText={setTitle}
          placeholder="Event title"
          placeholderTextColor="#8a8177"
        />

        <Text style={styles.label}>Start Date & Time</Text>
        <View style={styles.dateTimeValueBox}>
          <Text style={styles.dateTimeValueText}>
            {formatSelectedDateTime(startDateTime)}
          </Text>
        </View>
        <View style={styles.actionsRow}>
          <Pressable
            style={styles.modeButton}
            onPress={() => onOpenPicker("start", "date")}
          >
            <Text style={styles.modeButtonText}>Pick Start Date</Text>
          </Pressable>
          <Pressable
            style={styles.modeButton}
            onPress={() => onOpenPicker("start", "time")}
          >
            <Text style={styles.modeButtonText}>Pick Start Time</Text>
          </Pressable>
        </View>

        <Text style={styles.label}>End Date & Time</Text>
        <View style={styles.dateTimeValueBox}>
          <Text style={styles.dateTimeValueText}>
            {formatSelectedDateTime(endDateTime)}
          </Text>
        </View>
        <View style={styles.actionsRow}>
          <Pressable
            style={styles.modeButton}
            onPress={() => onOpenPicker("end", "date")}
          >
            <Text style={styles.modeButtonText}>Pick End Date</Text>
          </Pressable>
          <Pressable
            style={styles.modeButton}
            onPress={() => onOpenPicker("end", "time")}
          >
            <Text style={styles.modeButtonText}>Pick End Time</Text>
          </Pressable>
        </View>

        <Text style={styles.label}>Category</Text>
        <View style={styles.categoriesWrap}>{categoryChips}</View>

        <Text style={styles.label}>Base Price</Text>
        <TextInput
          style={styles.input}
          value={basePrice}
          onChangeText={setBasePrice}
          placeholder="59.99"
          placeholderTextColor="#8a8177"
          keyboardType="decimal-pad"
        />

        <Text style={styles.label}>Location Mode</Text>
        <View style={styles.actionsRow}>
          <Pressable
            style={[
              styles.modeButton,
              locationMode === "existing" && styles.modeButtonSelected,
            ]}
            onPress={() => setLocationMode("existing")}
          >
            <Text
              style={[
                styles.modeButtonText,
                locationMode === "existing" && styles.modeButtonTextSelected,
              ]}
            >
              Existing Location
            </Text>
          </Pressable>
          <Pressable
            style={[
              styles.modeButton,
              locationMode === "new" && styles.modeButtonSelected,
            ]}
            onPress={() => setLocationMode("new")}
          >
            <Text
              style={[
                styles.modeButtonText,
                locationMode === "new" && styles.modeButtonTextSelected,
              ]}
            >
              New Location
            </Text>
          </Pressable>
        </View>

        {locationMode === "existing" ? (
          <>
            <Text style={styles.label}>Location ID</Text>
            <TextInput
              style={styles.input}
              value={locationId}
              onChangeText={setLocationId}
              placeholder="1"
              placeholderTextColor="#8a8177"
              keyboardType="number-pad"
            />
          </>
        ) : (
          <>
            <Text style={styles.label}>Location Name</Text>
            <TextInput
              style={styles.input}
              value={locationName}
              onChangeText={setLocationName}
              placeholder="Bell Centre"
              placeholderTextColor="#8a8177"
            />

            <Text style={styles.label}>Address Line</Text>
            <TextInput
              style={styles.input}
              value={locationAddressLine}
              onChangeText={setLocationAddressLine}
              placeholder="1 Arena Way"
              placeholderTextColor="#8a8177"
            />

            <Text style={styles.label}>City</Text>
            <TextInput
              style={styles.input}
              value={locationCity}
              onChangeText={setLocationCity}
              placeholder="Montreal"
              placeholderTextColor="#8a8177"
            />

            <Text style={styles.label}>Province</Text>
            <TextInput
              style={styles.input}
              value={locationProvince}
              onChangeText={setLocationProvince}
              placeholder="QC"
              placeholderTextColor="#8a8177"
            />

            <Text style={styles.label}>Postal Code</Text>
            <TextInput
              style={styles.input}
              value={locationPostalCode}
              onChangeText={setLocationPostalCode}
              placeholder="H1A1A1"
              placeholderTextColor="#8a8177"
            />
          </>
        )}

        {!!error && <Text style={styles.errorText}>{error}</Text>}

        <View style={styles.actionsRow}>
          <Pressable
            style={[styles.submitButton, submitting && styles.buttonDisabled]}
            onPress={onSubmit}
            disabled={submitting}
          >
            <Text style={styles.submitButtonText}>
              {submitting ? "Saving..." : "Submit"}
            </Text>
          </Pressable>
          <Pressable
            style={styles.cancelButton}
            onPress={onCancel}
            disabled={submitting}
          >
            <Text style={styles.cancelButtonText}>Cancel</Text>
          </Pressable>
        </View>

        <Modal
          visible={!!activePicker && Platform.OS === "ios"}
          transparent
          animationType="fade"
          onRequestClose={() => setActivePicker(null)}
        >
          <View style={styles.pickerModalBackdrop}>
            <View style={styles.pickerModalCard}>
              <Text style={styles.pickerModalTitle}>
                {activePicker?.field === "start" ? "Start" : "End"}{" "}
                {activePicker?.mode === "date" ? "Date" : "Time"}
              </Text>

              {activePicker && (
                <DateTimePicker
                  value={pickerDraftDate}
                  mode={activePicker.mode}
                  is24Hour={false}
                  display="spinner"
                  onChange={onPickerChange}
                />
              )}

              <View style={styles.pickerModalActions}>
                <Pressable
                  style={styles.cancelButton}
                  onPress={() => setActivePicker(null)}
                >
                  <Text style={styles.cancelButtonText}>Cancel</Text>
                </Pressable>
                <Pressable
                  style={styles.submitButton}
                  onPress={onConfirmPicker}
                >
                  <Text style={styles.submitButtonText}>Done</Text>
                </Pressable>
              </View>
            </View>
          </View>
        </Modal>

        {activePicker && Platform.OS !== "ios" && (
          <DateTimePicker
            value={pickerDraftDate}
            mode={activePicker.mode}
            is24Hour={false}
            display="default"
            onChange={onPickerChange}
          />
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
