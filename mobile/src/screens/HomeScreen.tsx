import { useCallback, useRef, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  Text,
  View,
} from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuth } from "../context/AuthContext";
import {
  fetchCurrentReservations,
  type ReservationItem,
} from "../services/eventsApi";
import type { HomeStackParamList } from "../navigation/HomeStack";
import { styles } from "./styles/HomeScreen.styles";

type Props = NativeStackScreenProps<HomeStackParamList, "HomeMain">;

function formatDateTime(dateText: string) {
  const date = new Date(dateText);
  return date.toLocaleString();
}

export default function HomeScreen({ navigation }: Props) {
  const { logout, token, user } = useAuth();
  const listRef = useRef<FlatList<ReservationItem>>(null);
  const [reservations, setReservations] = useState<ReservationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadReservations = useCallback(async () => {
    if (!token) {
      setReservations([]);
      return;
    }

    setLoading(true);
    setError(null);

    const result = await fetchCurrentReservations(token);
    if (result.ok && result.data) {
      const now = Date.now();
      const upcomingReservations = result.data
        .filter((reservation) => {
          const startsAt = new Date(reservation.event.startDateTime).getTime();
          return Number.isFinite(startsAt) && startsAt > now;
        })
        .sort(
          (a, b) =>
            new Date(a.event.startDateTime).getTime() -
            new Date(b.event.startDateTime).getTime(),
        );

      setReservations(upcomingReservations);
    } else {
      setReservations([]);
      setError(result.error || "Unable to load current reservations.");
    }

    setLoading(false);
  }, [token]);

  useFocusEffect(
    useCallback(() => {
      listRef.current?.scrollToOffset({ offset: 0, animated: false });
      void loadReservations();
    }, [loadReservations]),
  );

  const onRefresh = useCallback(() => {
    if (loading) {
      return;
    }

    void loadReservations();
  }, [loading, loadReservations]);

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <Text style={styles.title}>
        Welcome{user ? `, ${user.fullName}` : ""}
      </Text>
      <Text style={styles.subtitle}>
        Your current reservations are listed below.
      </Text>

      <FlatList
        ref={listRef}
        data={reservations}
        keyExtractor={(item) => item.reservationId.toString()}
        style={styles.list}
        contentContainerStyle={styles.listContent}
        alwaysBounceVertical
        bounces
        overScrollMode="always"
        renderItem={({ item }) => (
          <Pressable
            style={styles.card}
            onPress={() =>
              navigation.navigate("ReservationDetails", {
                reservationId: item.reservationId,
              })
            }
          >
            <Text style={styles.cardTitle}>{item.event.title}</Text>
            <Text style={styles.cardMeta}>
              {formatDateTime(item.event.startDateTime)}
            </Text>
            <Text style={styles.cardMeta}>
              {item.event.location.name} - {item.event.location.city}
            </Text>
          </Pressable>
        )}
        refreshing={loading}
        onRefresh={onRefresh}
        ListEmptyComponent={
          <View style={styles.centerArea}>
            {loading ? (
              <ActivityIndicator size="large" color="#d88b4b" />
            ) : error ? (
              <Text style={styles.errorText}>{error}</Text>
            ) : (
              <Text style={styles.emptyText}>No active reservations yet.</Text>
            )}
          </View>
        }
      />

      <Pressable style={styles.logoutButton} onPress={logout}>
        <Text style={styles.logoutButtonText}>Logout</Text>
      </Pressable>
    </SafeAreaView>
  );
}
