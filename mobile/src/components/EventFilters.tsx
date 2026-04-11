import { Pressable, Text, TextInput, View } from "react-native";
import { styles } from "./EventFilters.styles";

interface EventFiltersProps<
  CategoryType extends string,
  StatusType extends string,
> {
  searchValue: string;
  onSearchChange: (value: string) => void;
  filtersOpen: boolean;
  onToggleOpen: () => void;
  locationValue: string;
  onLocationChange: (value: string) => void;
  startDateValue: string;
  onStartDateChange: (value: string) => void;
  endDateValue: string;
  onEndDateChange: (value: string) => void;
  startDatePlaceholder?: string;
  endDatePlaceholder?: string;
  categoryOptions: readonly CategoryType[];
  selectedCategories: CategoryType[];
  onToggleCategory: (category: CategoryType) => void;
  statusTitle?: string;
  statusOptions?: readonly StatusType[];
  selectedStatuses?: StatusType[];
  onToggleStatus?: (status: StatusType) => void;
  filterError: string | null;
  onApply: () => void;
  onReset: () => void;
  formatLabel: (value: string) => string;
}

export default function EventFilters<
  CategoryType extends string,
  StatusType extends string,
>({
  searchValue,
  onSearchChange,
  filtersOpen,
  onToggleOpen,
  locationValue,
  onLocationChange,
  startDateValue,
  onStartDateChange,
  endDateValue,
  onEndDateChange,
  startDatePlaceholder,
  endDatePlaceholder,
  categoryOptions,
  selectedCategories,
  onToggleCategory,
  statusTitle,
  statusOptions,
  selectedStatuses,
  onToggleStatus,
  filterError,
  onApply,
  onReset,
  formatLabel,
}: EventFiltersProps<CategoryType, StatusType>) {
  return (
    <>
      <TextInput
        style={styles.searchInput}
        placeholder="Search by title"
        placeholderTextColor="#6f7a86"
        value={searchValue}
        onChangeText={onSearchChange}
      />

      <View style={styles.filtersCard}>
        <Pressable style={styles.filtersHeader} onPress={onToggleOpen}>
          <Text style={styles.filtersTitle}>Filters</Text>
          <Text style={styles.filtersArrow}>{filtersOpen ? "^" : "v"}</Text>
        </Pressable>

        {filtersOpen && (
          <>
            <TextInput
              style={styles.filterInput}
              placeholder="Location (name/city/province)"
              placeholderTextColor="#6f7a86"
              value={locationValue}
              onChangeText={onLocationChange}
            />

            <View style={styles.rowInputs}>
              <TextInput
                style={[styles.filterInput, styles.halfInput]}
                placeholder={startDatePlaceholder || "Start date"}
                placeholderTextColor="#6f7a86"
                value={startDateValue}
                onChangeText={onStartDateChange}
              />
              <TextInput
                style={[styles.filterInput, styles.halfInput]}
                placeholder={endDatePlaceholder || "End date"}
                placeholderTextColor="#6f7a86"
                value={endDateValue}
                onChangeText={onEndDateChange}
              />
            </View>

            <View style={styles.chipsWrap}>
              {categoryOptions.map((category) => {
                const selected = selectedCategories.includes(category);
                return (
                  <Pressable
                    key={category}
                    style={[styles.chip, selected && styles.chipSelected]}
                    onPress={() => onToggleCategory(category)}
                  >
                    <Text
                      style={[
                        styles.chipText,
                        selected && styles.chipTextSelected,
                      ]}
                    >
                      {formatLabel(category)}
                    </Text>
                  </Pressable>
                );
              })}
            </View>

            {statusTitle &&
              statusOptions &&
              selectedStatuses &&
              onToggleStatus && (
                <>
                  <Text style={styles.filtersSubtitle}>{statusTitle}</Text>
                  <View style={styles.chipsWrap}>
                    {statusOptions.map((status) => {
                      const selected = selectedStatuses.includes(status);
                      return (
                        <Pressable
                          key={status}
                          style={[styles.chip, selected && styles.chipSelected]}
                          onPress={() => onToggleStatus(status)}
                        >
                          <Text
                            style={[
                              styles.chipText,
                              selected && styles.chipTextSelected,
                            ]}
                          >
                            {formatLabel(status)}
                          </Text>
                        </Pressable>
                      );
                    })}
                  </View>
                </>
              )}

            {!!filterError && (
              <Text style={styles.errorText}>{filterError}</Text>
            )}

            <View style={styles.actionsRow}>
              <Pressable
                style={({ pressed }) => [
                  styles.applyButton,
                  pressed && styles.applyButtonPressed,
                ]}
                onPress={onApply}
              >
                <Text style={styles.applyButtonText}>Apply Filters</Text>
              </Pressable>
              <Pressable style={styles.resetButton} onPress={onReset}>
                <Text style={styles.resetButtonText}>Reset</Text>
              </Pressable>
            </View>
          </>
        )}
      </View>
    </>
  );
}
