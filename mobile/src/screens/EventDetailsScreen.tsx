import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { fetchEventById, type EventItem } from "../services/eventsApi";
import type { EventsStackParamList } from "../navigation/EventsStack";

type Props = NativeStackScreenProps<EventsStackParamList, "EventDetails">;

function formatDateTime(dateText: string) {
  const date = new Date(dateText);
  return date.toLocaleString();
}

export default function EventDetailsScreen({ route }: Props) {
  const { eventId } = route.params;
  const [event, setEvent] = useState<EventItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const loadEvent = async () => {
      setLoading(true);
      setError(null);

      const result = await fetchEventById(eventId);

      if (!mounted) {
        return;
      }

      if (result.ok && result.data) {
        setEvent(result.data);
      } else {
        setError(result.error || "Failed to load event details.");
      }

      setLoading(false);
    };

    void loadEvent();

    return () => {
      mounted = false;
    };
  }, [eventId]);

  if (loading) {
    return (
      <View style={styles.centerArea}>
        <ActivityIndicator size="large" color="#d88b4b" />
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.centerArea}>
        <Text style={styles.errorText}>{error}</Text>
      </View>
    );
  }

  if (!event) {
    return (
      <View style={styles.centerArea}>
        <Text style={styles.errorText}>Event was not found.</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>{event.title}</Text>

      <View style={styles.section}>
        <Text style={styles.label}>Date & Time</Text>
        <Text style={styles.value}>{formatDateTime(event.startDateTime)}</Text>
        <Text style={styles.value}>{formatDateTime(event.endDateTime)}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>Category</Text>
        <Text style={styles.value}>{event.category}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>Status</Text>
        <Text style={styles.value}>{event.status}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>Price</Text>
        <Text style={styles.value}>${Number(event.basePrice).toFixed(2)}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>Location</Text>
        <Text style={styles.value}>{event.location.name}</Text>
        <Text style={styles.value}>{event.location.addressLine}</Text>
        <Text style={styles.value}>
          {event.location.city}, {event.location.province}{" "}
          {event.location.postalCode}
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: "#f5f4f0",
    flexGrow: 1,
    gap: 12,
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
});
