import { useCallback, useEffect, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  RefreshControl,
  ScrollView,
  Text,
  View,
} from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuth } from "../context/AuthContext";
import {
  fetchCurrentReservations,
  fetchEventById,
  reserveEvent,
  type EventItem,
  type ReservationItem,
} from "../services/eventsApi";
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
  const { token } = useAuth();
  const { eventId } = route.params;
  const [event, setEvent] = useState<EventItem | null>(null);
  const [currentReservation, setCurrentReservation] =
    useState<ReservationItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [reserving, setReserving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reservationError, setReservationError] = useState<string | null>(null);

  const loadCurrentReservation = async () => {
    if (!token) {
      setCurrentReservation(null);
      return;
    }

    const result = await fetchCurrentReservations(token);
    if (result.ok && result.data) {
      const reservation = result.data.find(
        (item) => item.event.eventId === eventId,
      );
      setCurrentReservation(reservation || null);
      return;
    }

    setCurrentReservation(null);
  };

  const loadEventDetails = useCallback(
    async (isRefresh: boolean) => {
      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError(null);

      const result = await fetchEventById(eventId);

      if (result.ok && result.data) {
        setEvent(result.data);
        await loadCurrentReservation();
      } else {
        setError(result.error || "Failed to load event details.");
      }

      if (isRefresh) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    },
    [eventId, token],
  );

  useEffect(() => {
    void loadEventDetails(false);
  }, [loadEventDetails]);

  useFocusEffect(
    useCallback(() => {
      void loadCurrentReservation();
    }, [eventId, token]),
  );

  const isPast =
    !!event &&
    (event.status === "PAST" ||
      new Date(event.endDateTime).getTime() <= Date.now());
  const isCancelled = event?.status === "CANCELLED";
  const alreadyRegistered = !!currentReservation;

  const reserveDisabled =
    isPast || isCancelled || alreadyRegistered || reserving;

  const onReserve = async () => {
    if (!token || reserveDisabled) {
      return;
    }

    setReservationError(null);
    setReserving(true);

    const result = await reserveEvent(token, eventId);
    setReserving(false);

    if (result.ok && result.data) {
      setCurrentReservation(result.data);
      return;
    }

    setReservationError(result.error || "Unable to reserve this event.");
  };

  const onRefresh = () => {
    if (refreshing || loading) {
      return;
    }

    void loadEventDetails(true);
  };

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
    <SafeAreaView style={styles.screen} edges={["bottom"]}>
      <ScrollView
        contentContainerStyle={styles.container}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor="#d88b4b"
          />
        }
      >
        <Text style={styles.title}>{event.title}</Text>

        <View style={styles.section}>
          <Text style={styles.label}>Date & Time</Text>
          <Text style={styles.value}>
            {formatDateTime(event.startDateTime)}
          </Text>
          <Text style={styles.value}>{formatDateTime(event.endDateTime)}</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Category</Text>
          <Text style={styles.value}>
            {formatCategoryLabel(event.category)}
          </Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Status</Text>
          <Text style={styles.value}>{event.status}</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Price</Text>
          <Text style={styles.value}>
            ${Number(event.basePrice).toFixed(2)}
          </Text>
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

        <View style={styles.section}>
          <Pressable
            style={[
              styles.reserveButton,
              reserveDisabled && styles.reserveButtonDisabled,
            ]}
            onPress={onReserve}
            disabled={reserveDisabled}
          >
            <Text style={styles.reserveButtonText}>
              {reserving ? "Reserving..." : "Reserve"}
            </Text>
          </Pressable>

          {alreadyRegistered && (
            <Text style={styles.registeredText}>
              You are registered for this event.
            </Text>
          )}

          {!alreadyRegistered && isPast && (
            <Text style={styles.infoText}>This event has already passed.</Text>
          )}

          {!alreadyRegistered && isCancelled && (
            <Text style={styles.infoText}>This event is cancelled.</Text>
          )}

          {!!reservationError && (
            <Text style={styles.errorText}>{reservationError}</Text>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
