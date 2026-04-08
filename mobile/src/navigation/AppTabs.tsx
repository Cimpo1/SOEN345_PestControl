import { Ionicons } from "@expo/vector-icons";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import type { NavigatorScreenParams } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import HomeStack, { type HomeStackParamList } from "./HomeStack";
import EventsStack, { type EventsStackParamList } from "./EventsStack";
import MyEventsStack, { type MyEventsStackParamList } from "./MyEventsStack";
import AdminEventsStack, {
  type AdminEventsStackParamList,
} from "./AdminEventsStack";

export type AppTabsParamList = {
  Home: NavigatorScreenParams<HomeStackParamList>;
  Events: NavigatorScreenParams<EventsStackParamList>;
  MyEvents: NavigatorScreenParams<MyEventsStackParamList>;
  AdminEvents: NavigatorScreenParams<AdminEventsStackParamList>;
};

const Tab = createBottomTabNavigator<AppTabsParamList>();

export default function AppTabs() {
  const { user } = useAuth();
  const isAdmin = user?.userRole === "ADMIN";

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: "#d88b4b",
        tabBarInactiveTintColor: "#7f7f7f",
        tabBarStyle: {
          borderTopColor: "#e6e6e6",
          paddingBottom: 6,
          paddingTop: 6,
          height: 64,
        },
        tabBarIcon: ({ color, size }) => {
          const iconName =
            route.name === "Events"
              ? "calendar-outline"
              : route.name === "MyEvents"
                ? "bookmark-outline"
                : route.name === "AdminEvents"
                  ? "settings-outline"
                  : "home-outline";
          return <Ionicons name={iconName} size={size} color={color} />;
        },
      })}
    >
      <Tab.Screen
        name="Home"
        component={HomeStack}
        options={{ title: "Home" }}
      />
      <Tab.Screen name="Events" component={EventsStack} />
      <Tab.Screen
        name="MyEvents"
        component={MyEventsStack}
        options={{ title: "My Events" }}
      />
      {isAdmin && (
        <Tab.Screen
          name="AdminEvents"
          component={AdminEventsStack}
          options={{ title: "Admin" }}
        />
      )}
    </Tab.Navigator>
  );
}
