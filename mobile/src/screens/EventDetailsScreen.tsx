import { useEffect, useState } from "react";
import { ActivityIndicator, ScrollView, Text, View } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { fetchEventById, type EventItem } from "../services/eventsApi";
import type { EventsStackParamList } from "../navigation/EventsStack";
import { styles } from "./styles/EventDetailsScreen.styles";

type Props = NativeStackScreenProps<EventsStackParamList, "EventDetails">;

function formatDateTime(dateText: string) {
  const date = new Date(dateText);
  return date.toLocaleString();
}

function formatCategoryLabel(category: string) {
  return category.replace(/_/g, " ");
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
      <View testID="event-details-loading-state" style={styles.centerArea}>
        <ActivityIndicator
          testID="event-details-loading-indicator"
          size="large"
          color="#d88b4b"
        />
      </View>
    );
  }

  if (error) {
    return (
      <View testID="event-details-error-state" style={styles.centerArea}>
        <Text testID="event-details-error-message" style={styles.errorText}>
          {error}
        </Text>
      </View>
    );
  }

  if (!event) {
    return (
      <View testID="event-details-not-found-state" style={styles.centerArea}>
        <Text testID="event-details-not-found-message" style={styles.errorText}>
          Event was not found.
        </Text>
      </View>
    );
  }

  return (
    <ScrollView
      testID="event-details-screen"
      contentContainerStyle={styles.container}
    >
      <Text testID="event-details-title" style={styles.title}>
        {event.title}
      </Text>

      <View style={styles.section}>
        <Text style={styles.label}>Date & Time</Text>
        <Text style={styles.value}>{formatDateTime(event.startDateTime)}</Text>
        <Text style={styles.value}>{formatDateTime(event.endDateTime)}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>Category</Text>
        <Text style={styles.value}>{formatCategoryLabel(event.category)}</Text>
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
