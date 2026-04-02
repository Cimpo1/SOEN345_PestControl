import { createNativeStackNavigator } from "@react-navigation/native-stack";
import EventsListScreen from "../screens/EventsListScreen";
import EventDetailsScreen from "../screens/EventDetailsScreen";

export type EventsStackParamList = {
  EventsList: undefined;
  EventDetails: { eventId: number };
};

const Stack = createNativeStackNavigator<EventsStackParamList>();

export default function EventsStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="EventsList"
        component={EventsListScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen
        name="EventDetails"
        component={EventDetailsScreen}
        options={{ title: "Event Details" }}
      />
    </Stack.Navigator>
  );
}
