import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { fetchEvents, type EventItem } from "../services/eventsApi";
import type { EventsStackParamList } from "../navigation/EventsStack";
import { styles } from "./styles/EventsListScreen.styles";

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

function formatCategoryLabel(category: string) {
  return category.replace(/_/g, " ");
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
      testID={`event-card-${item.eventId}`}
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
        {formatCategoryLabel(item.category)} - $
        {Number(item.basePrice).toFixed(2)}
      </Text>
    </Pressable>
  );

  return (
    <SafeAreaView
      testID="events-list-screen"
      style={styles.safeArea}
      edges={["top"]}
    >
      <View style={styles.container}>
        <FlatList
          testID="events-list"
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
                testID="events-search-input"
                style={styles.searchInput}
                placeholder="Search by title"
                placeholderTextColor="#6f7a86"
                value={searchInput}
                onChangeText={setSearchInput}
              />

              <View style={styles.filtersCard}>
                <Pressable
                  testID="events-filters-toggle"
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
                      testID="events-location-input"
                      style={styles.filterInput}
                      placeholder="Location (name/city/province)"
                      placeholderTextColor="#6f7a86"
                      value={locationInput}
                      onChangeText={setLocationInput}
                    />

                    <View style={styles.rowInputs}>
                      <TextInput
                        testID="events-start-date-input"
                        style={[styles.filterInput, styles.halfInput]}
                        placeholder="Start date YYYY-MM-DD"
                        placeholderTextColor="#6f7a86"
                        value={startDateInput}
                        onChangeText={setStartDateInput}
                      />
                      <TextInput
                        testID="events-end-date-input"
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
                            testID={`events-category-${category.toLowerCase()}`}
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
                              {formatCategoryLabel(category)}
                            </Text>
                          </Pressable>
                        );
                      })}
                    </View>

                    {!!filterError && (
                      <Text testID="events-filter-error" style={styles.errorText}>
                        {filterError}
                      </Text>
                    )}

                    <View style={styles.actionsRow}>
                      <Pressable
                        testID="events-apply-filters"
                        style={styles.applyButton}
                        onPress={onApplyFilters}
                      >
                        <Text style={styles.applyButtonText}>
                          Apply Filters
                        </Text>
                      </Pressable>
                      <Pressable
                        testID="events-reset-filters"
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
                <ActivityIndicator
                  testID="events-loading-indicator"
                  size="large"
                  color="#d88b4b"
                />
              ) : error ? (
                <Text testID="events-list-error" style={styles.errorText}>
                  {error}
                </Text>
              ) : (
                <Text testID="events-empty-message" style={styles.emptyText}>
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
