import { useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from "react-native";
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

function toInputDateText(value: string) {
  return value;
}

export default function AdminEventFormScreen({ navigation, route }: Props) {
  const { token } = useAuth();
  const isEdit = route.name === "AdminEditEvent";
  const eventId = isEdit ? route.params.eventId : null;

  const [loading, setLoading] = useState(isEdit);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [startDateTime, setStartDateTime] = useState("");
  const [endDateTime, setEndDateTime] = useState("");
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
      setStartDateTime(toInputDateText(event.startDateTime));
      setEndDateTime(toInputDateText(event.endDateTime));
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

    if (!startDateTime.trim() || !endDateTime.trim()) {
      setError("Start and end date/time are required.");
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
      startDateTime: startDateTime.trim(),
      endDateTime: endDateTime.trim(),
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

        <Text style={styles.label}>Start DateTime (ISO)</Text>
        <TextInput
          style={styles.input}
          value={startDateTime}
          onChangeText={setStartDateTime}
          placeholder="2026-07-01T19:00:00Z"
          placeholderTextColor="#8a8177"
          autoCapitalize="none"
        />

        <Text style={styles.label}>End DateTime (ISO)</Text>
        <TextInput
          style={styles.input}
          value={endDateTime}
          onChangeText={setEndDateTime}
          placeholder="2026-07-01T22:00:00Z"
          placeholderTextColor="#8a8177"
          autoCapitalize="none"
        />

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
      </ScrollView>
    </SafeAreaView>
  );
}
