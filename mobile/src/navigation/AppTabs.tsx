import { Ionicons } from "@expo/vector-icons";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import type { NavigatorScreenParams } from "@react-navigation/native";
import HomeStack, { type HomeStackParamList } from "./HomeStack";
import EventsStack, { type EventsStackParamList } from "./EventsStack";
import MyEventsStack, { type MyEventsStackParamList } from "./MyEventsStack";

export type AppTabsParamList = {
  Home: NavigatorScreenParams<HomeStackParamList>;
  Events: NavigatorScreenParams<EventsStackParamList>;
  MyEvents: NavigatorScreenParams<MyEventsStackParamList>;
};

const Tab = createBottomTabNavigator<AppTabsParamList>();

export default function AppTabs() {
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
    </Tab.Navigator>
  );
}
