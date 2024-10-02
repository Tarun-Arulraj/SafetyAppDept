# SafetyApp

**SafetyApp** is a mobile application designed to ensure personal safety by allowing users to send emergency alerts to authorities with just a tap. The app features easy-to-use buttons corresponding to different emergency situations, and upon clicking any of these buttons, the user's live location is sent to the relevant authorities in real-time.

## Features

- **Emergency Buttons**: Users can select the type of emergency they are facing (e.g., medical, police, fire).
- **Live Location Sharing**: Once an emergency button is pressed, the app sends the user’s real-time location to pre-set emergency contacts or local authorities.
- **Quick and Easy Interface**: The app is designed with simplicity in mind to ensure that users can send alerts swiftly during emergencies.

## Installation

1. Clone this repository:
    ```bash
    git clone https://github.com/YourUsername/SafetyApp.git
    ```
2. Navigate to the project directory:
    ```bash
    cd SafetyApp
    ```
3. Open the project in Android Studio.

4. Sync Gradle files and build the project.

5. Run the application on an Android device or emulator.

## Requirements

- Android Studio
- Minimum Android SDK version 21 (Lollipop)
- Internet access for real-time location sharing
- Google Maps API key for location services

## Usage

1. Launch the app and ensure that location services are enabled.
2. In case of an emergency, press the relevant button for the type of assistance required (e.g., medical, fire, police).
3. The app will automatically detect and send your current location to the pre-configured emergency contacts or authorities.
4. Authorities or contacts will receive a notification with your live location, allowing them to respond quickly.

## Configuration

- Add your Google Maps API key in `google-services.json`.
- Pre-configure emergency contacts within the app's settings or integrate it with local authorities' systems for direct alerts.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/NewFeature`).
3. Commit your changes (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature/NewFeature`).
5. Open a Pull Request.
