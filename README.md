# Offline Bluetooth Messaging App

An Android application that enables two devices to send and receive text messages via Bluetooth without requiring internet connectivity. This app demonstrates peer-to-peer communication using Android's Bluetooth API.

## Features

- **Device Discovery**: Scan for and discover nearby Bluetooth-enabled devices
- **Bi-directional Communication**: Both devices can act as server or client
- **Real-time Messaging**: Send and receive text messages instantly
- **Permission Management**: Handles both legacy and Android 12+ Bluetooth permissions
- **User-friendly Interface**: Clean and intuitive messaging interface
- **Connection Status**: Real-time connection status updates
- **Device Management**: View discovered devices and connection history

## Requirements

- Android API Level 21+ (Android 5.0 Lollipop)
- Bluetooth-enabled device
- Location permissions (for device discovery on older Android versions)
- Bluetooth permissions (for Android 12+)

## Permissions Required

The app requires the following permissions:

### For Android 12 and above:
- `BLUETOOTH_SCAN` - To discover nearby devices
- `BLUETOOTH_ADVERTISE` - To make device discoverable
- `BLUETOOTH_CONNECT` - To connect to devices

### For Android 11 and below:
- `BLUETOOTH` - Basic Bluetooth functionality
- `BLUETOOTH_ADMIN` - Advanced Bluetooth operations
- `ACCESS_COARSE_LOCATION` - Required for device discovery
- `ACCESS_FINE_LOCATION` - Enhanced location accuracy

## How to Use

1. **Enable Bluetooth**: The app will prompt to enable Bluetooth if disabled
2. **Grant Permissions**: Allow required Bluetooth and location permissions
3. **Make Discoverable**: Tap "Make Discoverable" to allow other devices to find you
4. **Scan for Devices**: Tap "Scan for Devices" to find nearby Bluetooth devices
5. **Connect**: Tap on a discovered device to establish connection
6. **Start Messaging**: Once connected, type messages and tap send

## Technical Architecture

### Main Components

- **MainActivity**: Core activity handling UI and Bluetooth operations
- **DeviceListAdapter**: RecyclerView adapter for displaying discovered devices
- **MessageListAdapter**: RecyclerView adapter for displaying chat messages
- **Message**: Data class for message objects

### Bluetooth Threading

The app uses three types of threads for Bluetooth operations:

1. **AcceptThread**: Server mode - listens for incoming connections
2. **ConnectThread**: Client mode - initiates connections to other devices
3. **ConnectedThread**: Data transfer - handles sending/receiving messages

### Key Features Implementation

#### Permission Handling
```java
private boolean hasBluetoothPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ permissions
        return checkPermissions(BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT);
    } else {
        // Legacy permissions
        return checkPermissions(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION);
    }
}
```

#### Device Discovery
```java
private void startDeviceDiscovery() {
    if (bluetoothAdapter.isDiscovering()) {
        bluetoothAdapter.cancelDiscovery();
    }
    bluetoothAdapter.startDiscovery();
}
```

#### Message Handling
```java
private void sendMessage() {
    String message = messageInput.getText().toString().trim();
    if (message.isEmpty() || !isConnected) return;
    
    connectedThread.write(message);
    addMessage("You", message);
}
```

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Build and run on a physical Android device (Bluetooth simulation not available in emulator)

```bash
git clone https://github.com/alinapradhan/Offline-Bluetooth-Messaging-App.git
cd Offline-Bluetooth-Messaging-App
./gradlew assembleDebug
```

## Testing

To test the application:

1. Install the APK on two Android devices
2. Enable Bluetooth on both devices
3. Run the app on both devices
4. Make one device discoverable
5. Use the other device to scan and connect
6. Start exchanging messages

## Limitations

- Bluetooth range is typically 10-30 meters depending on device class
- Only one-to-one communication (not group chat)
- Messages are not persistent (cleared when app is closed)
- Requires physical devices for testing (emulator doesn't support Bluetooth)

## Future Enhancements

- File transfer capability
- Group messaging support
- Message persistence using local database
- Enhanced UI with themes and customization
- Audio message support
- Encryption for secure communication

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Troubleshooting

### Common Issues

1. **Connection Failed**: Ensure both devices have Bluetooth enabled and are within range
2. **Permission Denied**: Grant all required permissions in device settings
3. **Device Not Found**: Make sure the target device is discoverable
4. **Messages Not Sending**: Check connection status and try reconnecting
 
### Debug Tips

- Check Android Studio logcat for detailed error messages
- Ensure both devices are running the app
- Verify Bluetooth is enabled and working on both devices
- Test with devices from different manufacturers for compatibility
