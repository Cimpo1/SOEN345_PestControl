import { useCallback, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Pressable,
  RefreshControl,
  ScrollView,
  Text,
  View,
} from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuth } from "../context/AuthContext";
import {
  cancelReservation,
  fetchInteractedReservations,
  type ReservationItem,
} from "../services/eventsApi";
import { styles } from "./styles/ReservationDetailsScreen.styles";

type Props = {
  navigation: {
    goBack: () => void;
  };
  route: {
    params: {
      reservationId: number;
    };
  };
};

function formatDateTime(dateText: string) {
  const date = new Date(dateText);
  return date.toLocaleString();
}

export default function ReservationDetailsScreen({ navigation, route }: Props) {
  const { token } = useAuth();
  const { reservationId } = route.params;
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [reservation, setReservation] = useState<ReservationItem | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadReservation = useCallback(
    async (isRefresh: boolean) => {
      if (!token) {
        setError("You must be signed in.");
        if (isRefresh) {
          setRefreshing(false);
        } else {
          setLoading(false);
        }
        return;
      }

      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);

      const result = await fetchInteractedReservations(token);
      if (!result.ok || !result.data) {
        setError(result.error || "Failed to load reservation details.");
        if (isRefresh) {
          setRefreshing(false);
        } else {
          setLoading(false);
        }
        return;
      }

      const current = result.data.find(
        (item) => item.reservationId === reservationId,
      );
      if (!current) {
        setError("Reservation not found.");
        if (isRefresh) {
          setRefreshing(false);
        } else {
          setLoading(false);
        }
        return;
      }

      setReservation(current);
      if (isRefresh) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    },
    [reservationId, token],
  );

  useFocusEffect(
    useCallback(() => {
      void loadReservation(false);
    }, [loadReservation]),
  );

  const onRefresh = () => {
    if (loading || refreshing) {
      return;
    }

    void loadReservation(true);
  };

  const canCancel = useMemo(() => {
    if (!reservation) {
      return false;
    }

    return reservation.reservationStatus !== "CANCELLED";
  }, [reservation]);

  const onCancelReservation = async () => {
    if (!token || !reservation || submitting) {
      return;
    }

    setSubmitting(true);
    const result = await cancelReservation(token, reservation.reservationId);
    setSubmitting(false);

    if (result.ok) {
      Alert.alert(
        "Reservation cancelled",
        "You have been unregistered from this event.",
        [
          {
            text: "OK",
            onPress: () => navigation.goBack(),
          },
        ],
      );
      return;
    }

    Alert.alert("Unable to cancel", result.error || "Please try again.");
  };

  if (loading) {
    return (
      <View style={styles.centerArea}>
        <ActivityIndicator size="large" color="#d88b4b" />
      </View>
    );
  }

  if (error || !reservation) {
    return (
      <View style={styles.centerArea}>
        <Text style={styles.errorText}>
          {error || "Reservation unavailable."}
        </Text>
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
        <Text style={styles.title}>{reservation.event.title}</Text>

        <View style={styles.section}>
          <Text style={styles.label}>Reservation</Text>
          <Text style={styles.value}>ID #{reservation.reservationId}</Text>
          <Text style={styles.value}>
            Status: {reservation.interactionStatus}
          </Text>
          <Text style={styles.value}>
            Tickets: {reservation.ticketCount}
          </Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Date & Time</Text>
          <Text style={styles.value}>
            {formatDateTime(reservation.event.startDateTime)}
          </Text>
          <Text style={styles.value}>
            {formatDateTime(reservation.event.endDateTime)}
          </Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Location</Text>
          <Text style={styles.value}>{reservation.event.location.name}</Text>
          <Text style={styles.value}>
            {reservation.event.location.addressLine}
          </Text>
          <Text style={styles.value}>
            {reservation.event.location.city},{" "}
            {reservation.event.location.province}{" "}
            {reservation.event.location.postalCode}
          </Text>
        </View>

        <Pressable
          style={[
            styles.cancelButton,
            (!canCancel || submitting) && styles.cancelButtonDisabled,
          ]}
          onPress={onCancelReservation}
          disabled={!canCancel || submitting}
        >
          <Text style={styles.cancelButtonText}>
            {submitting ? "Cancelling..." : "Unregister"}
          </Text>
        </Pressable>
      </ScrollView>
    </SafeAreaView>
  );
}
