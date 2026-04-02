import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { fetchEvents, type EventItem } from "../services/eventsApi";
import type { EventsStackParamList } from "../navigation/EventsStack";

type Props = NativeStackScreenProps<EventsStackParamList, "EventsList">;

const CATEGORY_OPTIONS = [
  "CONCERT",
  "SPORTS",
  "ART_THEATER",
  "COMEDY",
  "FAMILY",
] as const;

type CategoryOption = (typeof CATEGORY_OPTIONS)[number];

function formatDateTime(dateText: string) {
  const date = new Date(dateText);
  return date.toLocaleString();
}

function isIsoDate(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value.trim());
}

export default function EventsListScreen({ navigation }: Props) {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
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
  });

  const [filterError, setFilterError] = useState<string | null>(null);
  const [filtersOpen, setFiltersOpen] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchInput.trim());
    }, 300);

    return () => clearTimeout(timer);
  }, [searchInput]);

  const query = useMemo(
    () => ({
      title: debouncedSearch || undefined,
      location: appliedFilters.location || undefined,
      startDate: appliedFilters.startDate || undefined,
      endDate: appliedFilters.endDate || undefined,
      categories:
        appliedFilters.categories.length > 0
          ? [...appliedFilters.categories]
          : undefined,
    }),
    [debouncedSearch, appliedFilters],
  );

  const loadEvents = useCallback(
    async (isPullToRefresh: boolean) => {
      if (isPullToRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError(null);

      const result = await fetchEvents(query);

      if (result.ok && result.data) {
        setEvents(result.data);
      } else {
        setError(result.error || "Failed to load events.");
        setEvents([]);
      }

      if (isPullToRefresh) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    },
    [query],
  );

  useEffect(() => {
    void loadEvents(false);
  }, [loadEvents]);

  const onToggleCategory = (category: CategoryOption) => {
    setSelectedCategories((prev) =>
      prev.includes(category)
        ? prev.filter((item) => item !== category)
        : [...prev, category],
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
    });
  };

  const onResetFilters = () => {
    setLocationInput("");
    setStartDateInput("");
    setEndDateInput("");
    setSelectedCategories([]);
    setAppliedFilters({
      location: "",
      startDate: "",
      endDate: "",
      categories: [],
    });
    setFilterError(null);
  };

  const onRefresh = () => {
    if (loading || refreshing) {
      return;
    }
    void loadEvents(true);
  };

  const renderItem = ({ item }: { item: EventItem }) => (
    <Pressable
      style={styles.eventCard}
      onPress={() =>
        navigation.navigate("EventDetails", { eventId: item.eventId })
      }
    >
      <Text style={styles.eventTitle}>{item.title}</Text>
      <Text style={styles.eventMeta}>{formatDateTime(item.startDateTime)}</Text>
      <Text style={styles.eventMeta}>
        {item.location.name} - {item.location.city}
      </Text>
      <Text style={styles.eventMeta}>
        {item.category} - ${Number(item.basePrice).toFixed(2)}
      </Text>
    </Pressable>
  );

  return (
    <SafeAreaView style={styles.safeArea} edges={["top"]}>
      <View style={styles.container}>
        <FlatList
          data={events}
          keyExtractor={(item) => item.eventId.toString()}
          renderItem={renderItem}
          contentContainerStyle={styles.listContent}
          refreshing={refreshing}
          onRefresh={onRefresh}
          ListHeaderComponent={
            <View style={styles.headerContent}>
              <Text style={styles.heading}>Events</Text>

              <TextInput
                style={styles.searchInput}
                placeholder="Search by title"
                placeholderTextColor="#6f7a86"
                value={searchInput}
                onChangeText={setSearchInput}
              />

              <View style={styles.filtersCard}>
                <Pressable
                  style={styles.filtersHeader}
                  onPress={() => setFiltersOpen((prev) => !prev)}
                >
                  <Text style={styles.filtersTitle}>Filters</Text>
                  <Text style={styles.filtersArrow}>
                    {filtersOpen ? "^" : "v"}
                  </Text>
                </Pressable>

                {filtersOpen && (
                  <>
                    <TextInput
                      style={styles.filterInput}
                      placeholder="Location (name/city/province)"
                      placeholderTextColor="#6f7a86"
                      value={locationInput}
                      onChangeText={setLocationInput}
                    />

                    <View style={styles.rowInputs}>
                      <TextInput
                        style={[styles.filterInput, styles.halfInput]}
                        placeholder="Start date YYYY-MM-DD"
                        placeholderTextColor="#6f7a86"
                        value={startDateInput}
                        onChangeText={setStartDateInput}
                      />
                      <TextInput
                        style={[styles.filterInput, styles.halfInput]}
                        placeholder="End date YYYY-MM-DD"
                        placeholderTextColor="#6f7a86"
                        value={endDateInput}
                        onChangeText={setEndDateInput}
                      />
                    </View>

                    <View style={styles.categoriesWrap}>
                      {CATEGORY_OPTIONS.map((category) => {
                        const selected = selectedCategories.includes(category);
                        return (
                          <Pressable
                            key={category}
                            style={[
                              styles.categoryChip,
                              selected && styles.categoryChipSelected,
                            ]}
                            onPress={() => onToggleCategory(category)}
                          >
                            <Text
                              style={[
                                styles.categoryChipText,
                                selected && styles.categoryChipTextSelected,
                              ]}
                            >
                              {category}
                            </Text>
                          </Pressable>
                        );
                      })}
                    </View>

                    {!!filterError && (
                      <Text style={styles.errorText}>{filterError}</Text>
                    )}

                    <View style={styles.actionsRow}>
                      <Pressable
                        style={styles.applyButton}
                        onPress={onApplyFilters}
                      >
                        <Text style={styles.applyButtonText}>
                          Apply Filters
                        </Text>
                      </Pressable>
                      <Pressable
                        style={styles.resetButton}
                        onPress={onResetFilters}
                      >
                        <Text style={styles.resetButtonText}>Reset</Text>
                      </Pressable>
                    </View>
                  </>
                )}
              </View>
            </View>
          }
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
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#f5f4f0",
  },
  container: {
    flex: 1,
  },
  headerContent: {
    width: "100%",
  },
  heading: {
    fontSize: 28,
    fontWeight: "800",
    color: "#2f2b27",
    marginBottom: 12,
  },
  searchInput: {
    borderWidth: 1,
    borderColor: "#d8d3c7",
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    backgroundColor: "#fffdf8",
    color: "#2f2b27",
    marginBottom: 12,
  },
  filtersCard: {
    backgroundColor: "#fff",
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#e8e1d3",
    padding: 12,
    marginBottom: 12,
    gap: 10,
  },
  filtersTitle: {
    fontWeight: "700",
    color: "#2f2b27",
    fontSize: 16,
  },
  filtersHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  filtersArrow: {
    color: "#7a5d42",
    fontSize: 18,
    fontWeight: "700",
    width: 20,
    textAlign: "center",
  },
  filterInput: {
    borderWidth: 1,
    borderColor: "#d8d3c7",
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    backgroundColor: "#fffdf8",
    color: "#2f2b27",
  },
  rowInputs: {
    flexDirection: "row",
    gap: 8,
  },
  halfInput: {
    flex: 1,
  },
  categoriesWrap: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
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
  },
  applyButton: {
    backgroundColor: "#d88b4b",
    borderRadius: 10,
    paddingVertical: 10,
    paddingHorizontal: 12,
  },
  applyButtonText: {
    color: "#fff",
    fontWeight: "700",
  },
  resetButton: {
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "#cdb9a2",
    paddingVertical: 10,
    paddingHorizontal: 12,
    backgroundColor: "#fff",
  },
  resetButtonText: {
    color: "#7a5d42",
    fontWeight: "700",
  },
  listContent: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 24,
    gap: 10,
  },
  eventCard: {
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#e8e1d3",
    borderRadius: 12,
    padding: 14,
    gap: 4,
  },
  eventTitle: {
    color: "#2f2b27",
    fontWeight: "800",
    fontSize: 16,
  },
  eventMeta: {
    color: "#5b5147",
    fontSize: 13,
  },
  centerArea: {
    paddingTop: 30,
    alignItems: "center",
  },
  emptyText: {
    color: "#6f7a86",
    fontSize: 14,
  },
  errorText: {
    color: "#b04832",
    fontSize: 13,
    fontWeight: "600",
  },
});
