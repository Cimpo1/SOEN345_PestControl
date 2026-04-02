import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  Text,
  View,
} from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuth } from "../context/AuthContext";
import {
  fetchInteractedReservations,
  type ReservationItem,
} from "../services/eventsApi";
import type { MyEventsStackParamList } from "../navigation/MyEventsStack";
import EventFilters from "../components/EventFilters";
import { styles } from "./styles/MyEventsScreen.styles";

type Props = NativeStackScreenProps<MyEventsStackParamList, "MyEventsMain">;

const CATEGORY_OPTIONS = [
  "CONCERT",
  "SPORTS",
  "ART_THEATER",
  "COMEDY",
  "FAMILY",
] as const;

type CategoryOption = (typeof CATEGORY_OPTIONS)[number];
type InteractionStatus = "REGISTERED" | "PASSED" | "CANCELLED";

const STATUS_OPTIONS: InteractionStatus[] = [
  "REGISTERED",
  "PASSED",
  "CANCELLED",
];

function formatDateTime(dateText: string) {
  const date = new Date(dateText);
  return date.toLocaleString();
}

function formatCategoryLabel(category: string) {
  return category.replace(/_/g, " ");
}

function isIsoDate(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value.trim());
}

export default function MyEventsScreen({ navigation }: Props) {
  const { token } = useAuth();
  const [items, setItems] = useState<ReservationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [locationInput, setLocationInput] = useState("");
  const [startDateInput, setStartDateInput] = useState("");
  const [endDateInput, setEndDateInput] = useState("");
  const [selectedCategories, setSelectedCategories] = useState<
    CategoryOption[]
  >([]);
  const [appliedFilters, setAppliedFilters] = useState({
    location: "",
    startDate: "",
    endDate: "",
    categories: [] as CategoryOption[],
    statuses: [] as InteractionStatus[],
  });
  const [selectedStatuses, setSelectedStatuses] = useState<InteractionStatus[]>(
    [],
  );
  const [filterError, setFilterError] = useState<string | null>(null);
  const [filtersOpen, setFiltersOpen] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchInput.trim().toLowerCase());
    }, 300);

    return () => clearTimeout(timer);
  }, [searchInput]);

  const loadInteracted = useCallback(async () => {
    if (!token) {
      setItems([]);
      return;
    }

    setLoading(true);
    setError(null);

    const result = await fetchInteractedReservations(token);
    if (result.ok && result.data) {
      setItems(result.data);
    } else {
      setItems([]);
      setError(result.error || "Failed to load your events.");
    }

    setLoading(false);
  }, [token]);

  useFocusEffect(
    useCallback(() => {
      void loadInteracted();
    }, [loadInteracted]),
  );

  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      const title = item.event.title.toLowerCase();

      const location = [
        item.event.location.name,
        item.event.location.city,
        item.event.location.province,
      ]
        .join(" ")
        .toLowerCase();

      const eventDate = new Date(item.event.startDateTime);
      const eventCategory = item.event.category as CategoryOption;

      const matchesSearch = !debouncedSearch || title.includes(debouncedSearch);
      const matchesLocation =
        !appliedFilters.location ||
        location.includes(appliedFilters.location.toLowerCase());

      const matchesStartDate =
        !appliedFilters.startDate ||
        eventDate >= new Date(`${appliedFilters.startDate}T00:00:00`);
      const matchesEndDate =
        !appliedFilters.endDate ||
        eventDate <= new Date(`${appliedFilters.endDate}T23:59:59`);

      const matchesCategory =
        appliedFilters.categories.length === 0 ||
        appliedFilters.categories.includes(eventCategory);
      const matchesStatus =
        appliedFilters.statuses.length === 0 ||
        appliedFilters.statuses.includes(item.interactionStatus);

      return (
        matchesSearch &&
        matchesLocation &&
        matchesStartDate &&
        matchesEndDate &&
        matchesCategory &&
        matchesStatus
      );
    });
  }, [items, debouncedSearch, appliedFilters]);

  const onToggleCategory = (category: CategoryOption) => {
    setSelectedCategories((prev) =>
      prev.includes(category)
        ? prev.filter((item) => item !== category)
        : [...prev, category],
    );
  };

  const onToggleStatus = (status: InteractionStatus) => {
    setSelectedStatuses((prev) =>
      prev.includes(status)
        ? prev.filter((item) => item !== status)
        : [...prev, status],
    );
  };

  const onApplyFilters = () => {
    setFilterError(null);

    const start = startDateInput.trim();
    const end = endDateInput.trim();
    const location = locationInput.trim();

    if (start && !isIsoDate(start)) {
      setFilterError("Start date must use YYYY-MM-DD.");
      return;
    }
    if (end && !isIsoDate(end)) {
      setFilterError("End date must use YYYY-MM-DD.");
      return;
    }

    setAppliedFilters({
      location,
      startDate: start,
      endDate: end,
      categories: selectedCategories,
      statuses: selectedStatuses,
    });
  };

  const onResetFilters = () => {
    setSearchInput("");
    setDebouncedSearch("");
    setLocationInput("");
    setStartDateInput("");
    setEndDateInput("");
    setSelectedCategories([]);
    setAppliedFilters({
      location: "",
      startDate: "",
      endDate: "",
      categories: [],
      statuses: [],
    });
    setSelectedStatuses([]);
    setFilterError(null);
  };

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <FlatList
        ListHeaderComponent={
          <View style={styles.headerContent}>
            <Text style={styles.title}>My Events</Text>

            <EventFilters
              searchValue={searchInput}
              onSearchChange={setSearchInput}
              filtersOpen={filtersOpen}
              onToggleOpen={() => setFiltersOpen((prev) => !prev)}
              locationValue={locationInput}
              onLocationChange={setLocationInput}
              startDateValue={startDateInput}
              onStartDateChange={setStartDateInput}
              endDateValue={endDateInput}
              onEndDateChange={setEndDateInput}
              categoryOptions={CATEGORY_OPTIONS}
              selectedCategories={selectedCategories}
              onToggleCategory={onToggleCategory}
              statusTitle="Reservation Status"
              statusOptions={STATUS_OPTIONS}
              selectedStatuses={selectedStatuses}
              onToggleStatus={onToggleStatus}
              filterError={filterError}
              onApply={onApplyFilters}
              onReset={onResetFilters}
              formatLabel={formatCategoryLabel}
            />
          </View>
        }
        data={filteredItems}
        keyExtractor={(item) => item.reservationId.toString()}
        contentContainerStyle={styles.listContent}
        onRefresh={loadInteracted}
        refreshing={loading}
        renderItem={({ item }) => (
          <Pressable
            style={styles.card}
            onPress={() =>
              navigation.navigate("ReservationDetails", {
                reservationId: item.reservationId,
              })
            }
          >
            <View style={styles.rowTop}>
              <Text style={styles.cardTitle}>{item.event.title}</Text>
              <View
                style={[
                  styles.badge,
                  item.interactionStatus === "CANCELLED"
                    ? styles.cancelledBadge
                    : item.interactionStatus === "PASSED"
                      ? styles.passedBadge
                      : styles.registeredBadge,
                ]}
              >
                <Text style={styles.badgeText}>{item.interactionStatus}</Text>
              </View>
            </View>

            <Text style={styles.meta}>
              {formatDateTime(item.event.startDateTime)} -{" "}
              {formatDateTime(item.event.endDateTime)}
            </Text>
            <Text style={styles.meta}>
              {item.event.location.name} - {item.event.location.city}
            </Text>
            <Text style={styles.meta}>{item.event.location.addressLine}</Text>
            <Text style={styles.meta}>
              {item.event.location.province} {item.event.location.postalCode}
            </Text>
            <Text style={styles.meta}>
              {formatCategoryLabel(item.event.category)} - $
              {Number(item.event.basePrice).toFixed(2)}
            </Text>
          </Pressable>
        )}
        ListEmptyComponent={
          <View style={styles.centerArea}>
            {loading ? (
              <ActivityIndicator size="large" color="#d88b4b" />
            ) : error ? (
              <Text style={styles.errorText}>{error}</Text>
            ) : (
              <Text style={styles.emptyText}>
                No events match your search/filters.
              </Text>
            )}
          </View>
        }
      />
    </SafeAreaView>
  );
}
