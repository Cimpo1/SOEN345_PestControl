import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Pressable,
  Text,
  View,
} from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { SafeAreaView } from "react-native-safe-area-context";
import { useFocusEffect } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import {
  cancelEvent,
  fetchAdminEvents,
  type EventItem,
} from "../services/eventsApi";
import type { AdminEventsStackParamList } from "../navigation/AdminEventsStack";
import { styles } from "./styles/AdminEventsScreen.styles";

type Props = NativeStackScreenProps<
  AdminEventsStackParamList,
  "AdminEventsList"
>;

type EventListItem =
  | { type: "section"; key: string; title: string }
  | { type: "event"; key: string; event: EventItem };

type SectionKey = "available" | "passed" | "cancelled";

type SectionState = Record<SectionKey, boolean>;

function formatDateTime(dateText: string) {
  const date = new Date(dateText);
  return date.toLocaleString();
}

function sortEventsByDate(events: EventItem[]) {
  return [...events].sort(
    (a, b) =>
      new Date(b.startDateTime).getTime() - new Date(a.startDateTime).getTime(),
  );
}

export default function AdminEventsScreen({ navigation, route }: Props) {
  const { token } = useAuth();
  const [events, setEvents] = useState<EventItem[]>([]);
  const [sectionsOpen, setSectionsOpen] = useState<SectionState>({
    available: true,
    passed: true,
    cancelled: true,
  });
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [flashMessage, setFlashMessage] = useState<string | null>(null);

  const loadEvents = useCallback(
    async (isPullToRefresh: boolean) => {
      if (!token) {
        setError("Authentication required.");
        setEvents([]);
        return;
      }

      if (isPullToRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError(null);
      const result = await fetchAdminEvents(token);

      if (result.ok && result.data) {
        setEvents(result.data);
      } else {
        setEvents([]);
        setError(result.error || "Failed to load admin events.");
      }

      if (isPullToRefresh) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    },
    [token],
  );

  useEffect(() => {
    void loadEvents(false);
  }, [loadEvents]);

  useFocusEffect(
    useCallback(() => {
      if (route.params?.flashMessage) {
        setFlashMessage(route.params.flashMessage);
        navigation.setParams({ flashMessage: undefined });
      }
      void loadEvents(false);
    }, [route.params?.flashMessage, navigation, loadEvents]),
  );

  useEffect(() => {
    if (!flashMessage) {
      return;
    }

    const timeout = setTimeout(() => {
      setFlashMessage(null);
    }, 3000);

    return () => clearTimeout(timeout);
  }, [flashMessage]);

  const items = useMemo(() => {
    const available = sortEventsByDate(
      events.filter((event) => event.status === "SCHEDULED"),
    );
    const passed = sortEventsByDate(
      events.filter((event) => event.status === "PAST"),
    );
    const cancelled = sortEventsByDate(
      events.filter((event) => event.status === "CANCELLED"),
    );

    const list: EventListItem[] = [];
    list.push({
      type: "section",
      key: "section-available",
      title: "Available",
    });
    available.forEach((event) =>
      list.push({
        type: "event",
        key: `event-${event.eventId}`,
        event,
      }),
    );

    list.push({ type: "section", key: "section-passed", title: "Passed" });
    passed.forEach((event) =>
      list.push({
        type: "event",
        key: `event-${event.eventId}`,
        event,
      }),
    );

    list.push({
      type: "section",
      key: "section-cancelled",
      title: "Cancelled",
    });
    cancelled.forEach((event) =>
      list.push({
        type: "event",
        key: `event-${event.eventId}`,
        event,
      }),
    );

    return list;
  }, [events]);

  const groupedSections = useMemo(
    () => [
      {
        key: "available" as const,
        title: "Available",
        events: sortEventsByDate(
          events.filter((event) => event.status === "SCHEDULED"),
        ),
      },
      {
        key: "passed" as const,
        title: "Passed",
        events: sortEventsByDate(
          events.filter((event) => event.status === "PAST"),
        ),
      },
      {
        key: "cancelled" as const,
        title: "Cancelled",
        events: sortEventsByDate(
          events.filter((event) => event.status === "CANCELLED"),
        ),
      },
    ],
    [events],
  );

  const onRefresh = () => {
    if (loading || refreshing) {
      return;
    }
    void loadEvents(true);
  };

  const onCancelEvent = (event: EventItem) => {
    if (!token) {
      return;
    }

    Alert.alert("Cancel Event", "Do you really want to cancel this event?", [
      { text: "No", style: "cancel" },
      {
        text: "Yes",
        style: "destructive",
        onPress: async () => {
          const result = await cancelEvent(token, event.eventId);
          if (!result.ok) {
            Alert.alert("Error", result.error || "Unable to cancel event.");
            return;
          }

          setFlashMessage("Event cancellation was successful.");
          void loadEvents(false);
        },
      },
    ]);
  };

  const toggleSection = (sectionKey: SectionKey) => {
    setSectionsOpen((current) => ({
      ...current,
      [sectionKey]: !current[sectionKey],
    }));
  };

  const renderEventCard = (event: EventItem) => {
    const isAvailable = event.status === "SCHEDULED";

    return (
      <View style={styles.eventCard}>
        <Text style={styles.eventTitle}>{event.title}</Text>
        <Text style={styles.eventMeta}>
          {formatDateTime(event.startDateTime)}
        </Text>
        <Text style={styles.eventMeta}>
          {event.location.name} - {event.location.city}
        </Text>
        <Text style={styles.statusText}>Status: {event.status}</Text>

        {isAvailable && (
          <View style={styles.actionsRow}>
            <Pressable
              style={styles.editButton}
              onPress={() =>
                navigation.navigate("AdminEditEvent", {
                  eventId: event.eventId,
                })
              }
            >
              <Text style={styles.editButtonText}>Edit Event</Text>
            </Pressable>
            <Pressable
              style={styles.cancelButton}
              onPress={() => onCancelEvent(event)}
            >
              <Text style={styles.cancelButtonText}>Cancel Event</Text>
            </Pressable>
          </View>
        )}
      </View>
    );
  };

  const renderSection = (section: {
    key: SectionKey;
    title: string;
    events: EventItem[];
  }) => {
    const isOpen = sectionsOpen[section.key];

    return (
      <View style={styles.sectionBlock}>
        <Pressable
          style={styles.sectionHeader}
          onPress={() => toggleSection(section.key)}
        >
          <View style={styles.sectionHeaderTextWrap}>
            <Text style={styles.sectionTitle}>{section.title}</Text>
            <Text style={styles.sectionCount}>
              {section.events.length} events
            </Text>
          </View>
          <Text style={styles.sectionChevron}>{isOpen ? "▾" : "▸"}</Text>
        </Pressable>

        {isOpen && (
          <View style={styles.sectionContent}>
            {section.events.length > 0 ? (
              section.events.map((event) => (
                <View key={event.eventId} style={styles.sectionItemSpacing}>
                  {renderEventCard(event)}
                </View>
              ))
            ) : (
              <Text style={styles.emptySectionText}>
                No events in this section.
              </Text>
            )}
          </View>
        )}
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.safeArea} edges={["top"]}>
      <View style={styles.container}>
        <FlatList
          data={groupedSections}
          keyExtractor={(item) => item.key}
          renderItem={({ item }) => renderSection(item)}
          contentContainerStyle={styles.listContent}
          refreshing={refreshing}
          onRefresh={onRefresh}
          ListHeaderComponent={
            <View>
              <Text style={styles.heading}>Administrator Events</Text>
              <Pressable
                style={styles.createButton}
                onPress={() => navigation.navigate("AdminCreateEvent")}
              >
                <Text style={styles.createButtonText}>Create Event</Text>
              </Pressable>
              {!!flashMessage && (
                <Text style={styles.flashText}>{flashMessage}</Text>
              )}
            </View>
          }
          ListEmptyComponent={
            <View style={styles.centerArea}>
              {loading ? (
                <ActivityIndicator size="large" color="#d88b4b" />
              ) : error ? (
                <Text style={styles.errorText}>{error}</Text>
              ) : (
                <Text style={styles.emptyText}>No events found.</Text>
              )}
            </View>
          }
        />
      </View>
    </SafeAreaView>
  );
}
