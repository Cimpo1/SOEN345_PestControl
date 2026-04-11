import { createNativeStackNavigator } from "@react-navigation/native-stack";
import AdminEventsScreen from "../screens/AdminEventsScreen";
import AdminEventFormScreen from "../screens/AdminEventFormScreen";

export type AdminEventsStackParamList = {
  AdminEventsList: { flashMessage?: string } | undefined;
  AdminCreateEvent: undefined;
  AdminEditEvent: { eventId: number };
};

const Stack = createNativeStackNavigator<AdminEventsStackParamList>();

export default function AdminEventsStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="AdminEventsList"
        component={AdminEventsScreen}
        options={{
          headerShown: false,
          title: "Admin Events",
        }}
      />
      <Stack.Screen
        name="AdminCreateEvent"
        component={AdminEventFormScreen}
        options={{ title: "Create Event" }}
      />
      <Stack.Screen
        name="AdminEditEvent"
        component={AdminEventFormScreen}
        options={{ title: "Edit Event" }}
      />
    </Stack.Navigator>
  );
}
