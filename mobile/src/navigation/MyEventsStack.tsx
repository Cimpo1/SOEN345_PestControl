import { createNativeStackNavigator } from "@react-navigation/native-stack";
import MyEventsScreen from "../screens/MyEventsScreen";
import ReservationDetailsScreen from "../screens/ReservationDetailsScreen";

export type MyEventsStackParamList = {
  MyEventsMain: undefined;
  ReservationDetails: { reservationId: number };
};

const Stack = createNativeStackNavigator<MyEventsStackParamList>();

export default function MyEventsStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="MyEventsMain"
        component={MyEventsScreen}
        options={{
          headerShown: false,
          title: "My Events",
        }}
      />
      <Stack.Screen
        name="ReservationDetails"
        component={ReservationDetailsScreen}
        options={{
          title: "Reservation Details",
          statusBarTranslucent: false,
        }}
      />
    </Stack.Navigator>
  );
}
