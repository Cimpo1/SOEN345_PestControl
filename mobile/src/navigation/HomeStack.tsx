import { createNativeStackNavigator } from "@react-navigation/native-stack";
import HomeScreen from "../screens/HomeScreen";
import ReservationDetailsScreen from "../screens/ReservationDetailsScreen";

export type HomeStackParamList = {
  HomeMain: undefined;
  ReservationDetails: { reservationId: number };
};

const Stack = createNativeStackNavigator<HomeStackParamList>();

export default function HomeStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="HomeMain"
        component={HomeScreen}
        options={{
          headerShown: false,
          title: "Home",
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
