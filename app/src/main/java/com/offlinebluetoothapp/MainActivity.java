package com.offlinebluetoothapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "BluetoothMessaging";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int REQUEST_DISCOVERABLE = 3;
    private static final String APP_NAME = "BluetoothMessaging";
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    
    // UI Components
    private TextView statusText;
    private Button scanButton;
    private Button discoverableButton;
    private Button sendButton;
    private EditText messageInput;
    private RecyclerView devicesRecyclerView;
    private RecyclerView messagesRecyclerView;
    
    // Adapters
    private DeviceListAdapter deviceAdapter;
    private MessageListAdapter messageAdapter;
    
    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDevice> discoveredDevices;
    private List<Message> messages;
    
    // Connection threads
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    
    // Handler for UI updates
    private Handler mainHandler;
    
    // Connection state
    private boolean isConnected = false;
    private BluetoothDevice connectedDevice = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupRecyclerViews();
        initializeBluetooth();
        setupClickListeners();
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Check and request permissions
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
        } else {
            enableBluetooth();
        }
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.status_text);
        scanButton = findViewById(R.id.scan_button);
        discoverableButton = findViewById(R.id.discoverable_button);
        sendButton = findViewById(R.id.send_button);
        messageInput = findViewById(R.id.message_input);
        devicesRecyclerView = findViewById(R.id.devices_recycler_view);
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
    }
    
    private void setupRecyclerViews() {
        discoveredDevices = new ArrayList<>();
        messages = new ArrayList<>();
        
        deviceAdapter = new DeviceListAdapter(discoveredDevices, this::connectToDevice);
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        devicesRecyclerView.setAdapter(deviceAdapter);
        
        messageAdapter = new MessageListAdapter(messages);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);
    }
    
    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if (bluetoothAdapter == null) {
            statusText.setText(R.string.bluetooth_not_supported);
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }
    
    private void setupClickListeners() {
        scanButton.setOnClickListener(v -> startDeviceDiscovery());
        discoverableButton.setOnClickListener(v -> makeDiscoverable());
        sendButton.setOnClickListener(v -> sendMessage());
    }
    
    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestBluetoothPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[] {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            permissions = new String[] {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
        
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
    }
    
    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            onBluetoothEnabled();
        }
    }
    
    private void onBluetoothEnabled() {
        statusText.setText("Bluetooth enabled - Ready to connect");
        startAcceptThread();
    }
    
    private void startDeviceDiscovery() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
        
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        
        discoveredDevices.clear();
        deviceAdapter.notifyDataSetChanged();
        
        statusText.setText(R.string.discovering_devices);
        bluetoothAdapter.startDiscovery();
    }
    
    private void makeDiscoverable() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
        
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
    }
    
    private void connectToDevice(BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        
        connectThread = new ConnectThread(device);
        connectThread.start();
        
        statusText.setText(getString(R.string.connecting) + " " + device.getName());
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty() || !isConnected || connectedThread == null) {
            return;
        }
        
        connectedThread.write(message);
        addMessage("You", message);
        messageInput.setText("");
    }
    
    private void addMessage(String sender, String content) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        messages.add(new Message(sender, content, timestamp));
        
        mainHandler.post(() -> {
            messageAdapter.notifyItemInserted(messages.size() - 1);
            messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
        });
    }
    
    // BroadcastReceiver for Bluetooth events
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device);
                    deviceAdapter.notifyItemInserted(discoveredDevices.size() - 1);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                statusText.setText(getString(R.string.device_discovery_finished) + 
                                 " (" + discoveredDevices.size() + " devices found)");
            }
        }
    };
    
    // Accept Thread - Server mode
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }
        
        public void run() {
            BluetoothSocket socket = null;
            
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }
                
                if (socket != null) {
                    manageMyConnectedSocket(socket);
                    break;
                }
            }
        }
        
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
    
    // Connect Thread - Client mode
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }
        
        public void run() {
            bluetoothAdapter.cancelDiscovery();
            
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    mmSocket.connect();
                }
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                
                mainHandler.post(() -> {
                    statusText.setText(R.string.connection_failed);
                    Toast.makeText(MainActivity.this, R.string.connection_failed, Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            manageMyConnectedSocket(mmSocket);
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
    
    // Connected Thread - Data transfer
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;
        
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input/output streams", e);
            }
            
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        
        public void run() {
            mmBuffer = new byte[1024];
            int numBytes;
            
            while (true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    String receivedMessage = new String(mmBuffer, 0, numBytes);
                    
                    String deviceName = "Remote";
                    if (connectedDevice != null) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            deviceName = connectedDevice.getName();
                        }
                        if (deviceName == null) deviceName = "Remote";
                    }
                    
                    addMessage(deviceName, receivedMessage);
                    
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    
                    mainHandler.post(() -> {
                        statusText.setText(R.string.disconnected);
                        isConnected = false;
                        connectedDevice = null;
                        sendButton.setEnabled(false);
                    });
                    break;
                }
            }
        }
        
        public void write(String message) {
            try {
                mmOutStream.write(message.getBytes());
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
            }
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
    
    private void manageMyConnectedSocket(BluetoothSocket socket) {
        connectedDevice = socket.getRemoteDevice();
        isConnected = true;
        
        String deviceName = "Remote Device";
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            deviceName = connectedDevice.getName();
        }
        if (deviceName == null) deviceName = "Remote Device";
        
        final String finalDeviceName = deviceName;
        
        mainHandler.post(() -> {
            statusText.setText(getString(R.string.connected) + " to " + finalDeviceName);
            sendButton.setEnabled(true);
            Toast.makeText(MainActivity.this, getString(R.string.connected) + " to " + finalDeviceName, Toast.LENGTH_SHORT).show();
        });
        
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }
    
    private void startAcceptThread() {
        if (acceptThread != null) {
            acceptThread.cancel();
        }
        
        acceptThread = new AcceptThread();
        acceptThread.start();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    onBluetoothEnabled();
                } else {
                    statusText.setText(R.string.bluetooth_disabled);
                    Toast.makeText(this, R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
                }
                break;
                
            case REQUEST_DISCOVERABLE:
                if (resultCode > 0) {
                    statusText.setText("Device is discoverable for " + resultCode + " seconds");
                    Toast.makeText(this, "Device is now discoverable", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                enableBluetooth();
            } else {
                statusText.setText(R.string.permission_denied);
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
        }
        
        if (acceptThread != null) {
            acceptThread.cancel();
        }
        
        if (connectThread != null) {
            connectThread.cancel();
        }
        
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }
}