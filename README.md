# SOEN 345 - Team Pest Control
## Team Members:
- Sebastian Bulgac
- David Cimpoiasu
- Kevin Ung
- Dmitrii Cazacu

## Project Overview: Event Control
This project is a cloud-based ticket reservation mobile application that allows users to view and reserve tickets for different events. The application is built using React Native and Expo for the frontend, and it communicates with a backend server to manage ticket reservations. This project features user authentication, event browsing, ticket reservation, as well as extensive testing to ensure reliability and performance.

## How to Run: 
**There are two ways to run the application:**
### Run Hosted Application:
Run the entire application that is hosted on a Linux server using the following QR code and scanning it with the Expo Go app on your mobile device:

  ![QR Code](./docs/Hosted_App_QR_Code.png)

### Run Locally:
#### Backend (Spring Boot Java Application):
1. Install Java JDK 21 or higher.
2. Navigate to the backend directory and run the following command to start the server:
   ```
   mvn spring-boot:run
   ```
> Note: If you are running the backend locally, you will need to include a `secrets.properties` file in the `src/main/resources` directory with the following content:
```
# secrets.properties
DB_PASSWORD=your_database_password
MAIL_HOST=your_mail_host
MAIL_PORT=your_mail_port
MAIL_USERNAME=your_mail_username
MAIL_PASSWORD=your_mail_password
MAIL_FROM=your_mail_from_address
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=false
MAIL_SMTP_SSL_ENABLE=true
MAIL_SSL_TRUST=your_mail_ssl_trust
MAIL_SSL_CHECK_SERVER_IDENTITY=false
```


#### Frontend Mobile Application (React Native with Expo):
1. Install Node.js and npm.
2. Navigate to the mobile directory and run the following command to install dependencies:
   ```
   npm install
   ```
3. Start the Expo development server:
   ```
    npm run start
    ```
4. Run the application on your device or emulator:
   - (a) Use the **Expo Go app** on your mobile device to scan the QR code displayed in the terminal or browser to run the application on your device.
   - (b) Run the application on an Android emulator. Follow the instructions in the `run_android_emulator.md` file for detailed steps.

> Note: Before running the frontend, ensure that the backend server is running and that the `EXPO_PUBLIC_BACKEND_IP` variable in the `.env` file is set to the correct IP address of your backend server. Notes below provide instructions on how to find your local IP address.

> Ensure that your mobile device and backend server are on the same network for the application to communicate properly.


## Finding Your Local IP Address:
### Windows:
1. Open Command Prompt and run the following command:
   ```
   ipconfig
   ```
2. Look for the "IPv4 Address" under your active network connection. This is your local IP address.
3. Update the `EXPO_PUBLIC_BACKEND_IP` variable in the `.env` file with this IP address to look like this:
   ```
   EXPO_PUBLIC_BACKEND_IP=192.168.x.x
   ```
### macOS/Linux:
1. Open Terminal and run the following command:
   ```
   ifconfig
   ```
2. Look for the "inet" address under your active network connection (usually `en0` or `wlan0`). This is your local IP address.
3. Update the `EXPO_PUBLIC_BACKEND_IP` variable in the `.env` file with this IP address to look like this:
   ```
   EXPO_PUBLIC_BACKEND_IP=192.168.x.x
   ```

## Testing:
### Backend Testing:
1. Navigate to the backend directory and run the following command to execute unit tests:
   ```
   mvn test
   ```

