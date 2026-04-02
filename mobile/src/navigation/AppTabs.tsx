import { Ionicons } from "@expo/vector-icons";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import HomeScreen from "../screens/HomeScreen";
import EventsStack from "./EventsStack";

export type AppTabsParamList = {
  Home: undefined;
  Events: undefined;
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
            route.name === "Events" ? "calendar-outline" : "home-outline";
          return <Ionicons name={iconName} size={size} color={color} />;
        },
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} />
      <Tab.Screen name="Events" component={EventsStack} />
    </Tab.Navigator>
  );
}
