package com.alisajjadfatmi.phonepad;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class HidDeviceController implements AutoCloseable {
    private static final String TAG = "PhonePadHid";
    interface Listener {
        void onStatusChanged(String message);

        void onStateChanged();
    }

    static final int MOUSE_LEFT = 1;
    static final int MOUSE_RIGHT = 1 << 1;
    static final int MOUSE_MIDDLE = 1 << 2;
    static final int MOUSE_BACK = 1 << 3;
    static final int MOUSE_FORWARD = 1 << 4;

    private final Context context;
    private final Listener listener;
    private final ExecutorService inputExecutor = Executors.newSingleThreadExecutor();
    private final BluetoothAdapter adapter;

    private BluetoothHidDevice hidDevice;
    private BluetoothDevice activeHost;
    private BluetoothDevice preferredHost;
    private boolean proxyReady;
    private boolean registered;
    private boolean registrationPending;
    private int connectionState = BluetoothProfile.STATE_DISCONNECTED;
    private int mouseButtons;

    HidDeviceController(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        adapter = manager == null ? null : manager.getAdapter();
    }

    boolean hasBluetoothAdapter() {
        return adapter != null;
    }

    boolean isBluetoothEnabled() {
        return adapter != null && adapter.isEnabled();
    }

    boolean isProxyReady() {
        return proxyReady;
    }

    boolean isRegistered() {
        return registered;
    }

    boolean isRegistrationPending() {
        return registrationPending;
    }

    boolean isConnected() {
        return connectionState == BluetoothProfile.STATE_CONNECTED && activeHost != null;
    }

    @SuppressLint("MissingPermission")
    String connectedHostName() {
        return activeHost == null ? null : safeName(activeHost);
    }

    @SuppressLint("MissingPermission")
    void openProfile() {
        if (adapter == null) {
            notifyStatus("This phone does not report a Bluetooth adapter.");
            return;
        }
        notifyStatus("Opening Android's Bluetooth HID Device profile…");
        boolean requested = adapter.getProfileProxy(
                context,
                profileListener,
                BluetoothProfile.HID_DEVICE
        );
        if (!requested) {
            notifyStatus("Android rejected the HID profile request.");
        }
    }

    @SuppressLint("MissingPermission")
    List<BluetoothDevice> bondedDevices() {
        List<BluetoothDevice> result = new ArrayList<>();
        if (adapter == null) {
            return result;
        }
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        if (devices != null) {
            result.addAll(devices);
        }
        result.sort(Comparator.comparing(this::safeName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @SuppressLint("MissingPermission")
    void registerApp() {
        if (hidDevice == null) {
            notifyStatus("The HID profile is not ready yet.");
            return;
        }
        if (registered || registrationPending) {
            return;
        }
        BluetoothHidDeviceAppSdpSettings settings = new BluetoothHidDeviceAppSdpSettings(
                "PhonePad",
                "Phone keyboard and five-button mouse",
                "alisajjadfatmi",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HidReportDescriptor.COMPOSITE
        );
        boolean requestSent = hidDevice.registerApp(
                settings,
                null,
                null,
                context.getMainExecutor(),
                callback
        );
        registrationPending = requestSent;
        notifyStatus(requestSent
                ? "HID registration requested; waiting for Samsung's Bluetooth service…"
                : "Samsung's Bluetooth service rejected the registration request.");
    }

    @SuppressLint("MissingPermission")
    void connect(BluetoothDevice device) {
        if (!registered || hidDevice == null) {
            notifyStatus("Register PhonePad before connecting.");
            return;
        }
        if (device == null) {
            notifyStatus("Choose the laptop first.");
            return;
        }
        preferredHost = device;
        activeHost = device;
        connectionState = BluetoothProfile.STATE_CONNECTING;
        boolean requestSent = hidDevice.connect(device);
        notifyStatus(requestSent
                ? "Connecting to " + safeName(device) + "…"
                : "Android rejected the connection request for " + safeName(device) + ".");
        notifyState();
    }

    void setPreferredHost(BluetoothDevice device) {
        if (device != null) {
            preferredHost = device;
        }
    }

    void testPointerMove() {
        inputExecutor.execute(() -> {
            sendMouse(mouseButtons, 45, 0, 0, 0);
            sleep(120);
            sendMouse(mouseButtons, 0, 35, 0, 0);
            sleep(120);
            sendMouse(mouseButtons, -45, 0, 0, 0);
            sleep(120);
            sendMouse(mouseButtons, 0, -35, 0, 0);
        });
    }

    void testLeftClick() {
        mouseClick(MOUSE_LEFT);
    }

    void testTyping() {
        typeText("PhonePad");
    }

    void movePointer(int dx, int dy) {
        inputExecutor.execute(() -> {
            int remainingX = dx;
            int remainingY = dy;
            while (remainingX != 0 || remainingY != 0) {
                int stepX = Math.max(-127, Math.min(127, remainingX));
                int stepY = Math.max(-127, Math.min(127, remainingY));
                sendMouse(mouseButtons, stepX, stepY, 0, 0);
                remainingX -= stepX;
                remainingY -= stepY;
            }
        });
    }

    void scroll(int vertical, int horizontal) {
        inputExecutor.execute(() -> sendMouse(mouseButtons, 0, 0, vertical, horizontal));
    }

    void mouseButtonDown(int button) {
        inputExecutor.execute(() -> {
            mouseButtons |= button;
            sendMouse(mouseButtons, 0, 0, 0, 0);
        });
    }

    void mouseButtonUp(int button) {
        inputExecutor.execute(() -> {
            mouseButtons &= ~button;
            sendMouse(mouseButtons, 0, 0, 0, 0);
        });
    }

    void mouseClick(int button) {
        inputExecutor.execute(() -> {
            mouseButtons |= button;
            sendMouse(mouseButtons, 0, 0, 0, 0);
            sleep(55);
            mouseButtons &= ~button;
            sendMouse(mouseButtons, 0, 0, 0, 0);
        });
    }

    void keyTap(int modifiers, int usage) {
        inputExecutor.execute(() -> {
            sendKeyboard(modifiers, usage);
            sleep(45);
            sendKeyboard(0, 0);
        });
    }

    void typeText(CharSequence text) {
        if (text == null || text.length() == 0) {
            return;
        }
        String copy = text.toString();
        inputExecutor.execute(() -> typeAscii(copy));
    }

    void backspace(int count) {
        int safeCount = Math.max(1, Math.min(100, count));
        inputExecutor.execute(() -> {
            for (int index = 0; index < safeCount; index++) {
                sendKeyboard(0, HidKeyMap.KEY_BACKSPACE);
                sleep(35);
                sendKeyboard(0, 0);
                sleep(25);
            }
        });
    }

    void deleteForward(int count) {
        int safeCount = Math.max(1, Math.min(100, count));
        inputExecutor.execute(() -> {
            for (int index = 0; index < safeCount; index++) {
                sendKeyboard(0, HidKeyMap.KEY_DELETE);
                sleep(35);
                sendKeyboard(0, 0);
                sleep(25);
            }
        });
    }

    void consumerTap(int usage) {
        inputExecutor.execute(() -> {
            sendConsumer(usage);
            sleep(55);
            sendConsumer(0);
        });
    }

    @SuppressLint("MissingPermission")
    private boolean sendMouse(int buttons, int dx, int dy, int wheel, int horizontalPan) {
        BluetoothDevice host = activeHost;
        BluetoothHidDevice device = hidDevice;
        if (host == null || device == null || connectionState != BluetoothProfile.STATE_CONNECTED) {
            return false;
        }
        byte[] report = new byte[] {
                (byte) (buttons & 0x1F),
                clampByte(dx),
                clampByte(dy),
                clampByte(wheel),
                clampByte(horizontalPan)
        };
        return device.sendReport(host, HidReportDescriptor.MOUSE_REPORT_ID, report);
    }

    private void typeAscii(String text) {
        for (int index = 0; index < text.length(); index++) {
            HidKeyMap.KeyStroke stroke = HidKeyMap.forCharacter(text.charAt(index));
            if (stroke == null) {
                continue;
            }
            sendKeyboard(stroke.modifier, stroke.usage);
            sleep(45);
            sendKeyboard(0, 0);
            sleep(35);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean sendKeyboard(int modifiers, int usage) {
        BluetoothDevice host = activeHost;
        BluetoothHidDevice device = hidDevice;
        if (host == null || device == null || connectionState != BluetoothProfile.STATE_CONNECTED) {
            return false;
        }
        byte[] report = new byte[8];
        report[0] = (byte) modifiers;
        report[2] = (byte) usage;
        return device.sendReport(host, HidReportDescriptor.KEYBOARD_REPORT_ID, report);
    }

    @SuppressLint("MissingPermission")
    private boolean sendConsumer(int usage) {
        BluetoothDevice host = activeHost;
        BluetoothHidDevice device = hidDevice;
        if (host == null || device == null || connectionState != BluetoothProfile.STATE_CONNECTED) {
            return false;
        }
        byte[] report = new byte[] {
                (byte) (usage & 0xFF),
                (byte) ((usage >> 8) & 0xFF)
        };
        return device.sendReport(host, HidReportDescriptor.CONSUMER_REPORT_ID, report);
    }

    @SuppressLint("MissingPermission")
    private String safeName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name == null || name.isBlank() ? device.getAddress() : name;
        } catch (SecurityException exception) {
            return "Paired device";
        }
    }

    private static byte clampByte(int value) {
        return (byte) Math.max(-127, Math.min(127, value));
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void notifyStatus(String message) {
        Log.i(TAG, message);
        context.getMainExecutor().execute(() -> listener.onStatusChanged(message));
    }

    private void notifyState() {
        context.getMainExecutor().execute(listener::onStateChanged);
    }

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile != BluetoothProfile.HID_DEVICE || !(proxy instanceof BluetoothHidDevice)) {
                return;
            }
            hidDevice = (BluetoothHidDevice) proxy;
            proxyReady = true;
            notifyStatus("Bluetooth HID Device profile is available on this phone.");
            notifyState();
            registerApp();
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile != BluetoothProfile.HID_DEVICE) {
                return;
            }
            hidDevice = null;
            proxyReady = false;
            registered = false;
            registrationPending = false;
            activeHost = null;
            connectionState = BluetoothProfile.STATE_DISCONNECTED;
            notifyStatus("Android's Bluetooth HID service disconnected.");
            notifyState();
        }
    };

    private final BluetoothHidDevice.Callback callback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean isRegistered) {
            Log.i(TAG, "onAppStatusChanged registered=" + isRegistered
                    + " pluggedDevice=" + (pluggedDevice == null ? "none" : safeName(pluggedDevice)));
            registered = isRegistered;
            registrationPending = false;
            if (pluggedDevice != null) {
                activeHost = pluggedDevice;
            }
            notifyStatus(isRegistered
                    ? "Success: PhonePad is registered as a Bluetooth keyboard and mouse."
                    : "PhonePad is not registered as a Bluetooth keyboard/mouse.");
            notifyState();
            BluetoothDevice reconnectHost = pluggedDevice != null ? pluggedDevice : preferredHost;
            if (isRegistered
                    && reconnectHost != null
                    && connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                connect(reconnectHost);
            }
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            Log.i(TAG, "onConnectionStateChanged host=" + safeName(device) + " state=" + state);
            activeHost = state == BluetoothProfile.STATE_DISCONNECTED ? null : device;
            connectionState = state;
            String stateText;
            if (state == BluetoothProfile.STATE_CONNECTED) {
                stateText = "Connected to " + safeName(device) + ". Test mouse and keyboard input now.";
            } else if (state == BluetoothProfile.STATE_CONNECTING) {
                stateText = "Connecting to " + safeName(device) + "…";
            } else if (state == BluetoothProfile.STATE_DISCONNECTING) {
                stateText = "Disconnecting from " + safeName(device) + "…";
            } else {
                stateText = "Disconnected from the Bluetooth host.";
            }
            notifyStatus(stateText);
            notifyState();
        }

        @Override
        @SuppressLint("MissingPermission")
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            if (hidDevice == null) {
                return;
            }
            if (id == HidReportDescriptor.KEYBOARD_REPORT_ID) {
                hidDevice.replyReport(device, type, id, new byte[8]);
            } else if (id == HidReportDescriptor.MOUSE_REPORT_ID) {
                hidDevice.replyReport(device, type, id, new byte[5]);
            } else if (id == HidReportDescriptor.CONSUMER_REPORT_ID) {
                hidDevice.replyReport(device, type, id, new byte[2]);
            } else {
                hidDevice.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
            }
        }

        @Override
        public void onVirtualCableUnplug(BluetoothDevice device) {
            activeHost = null;
            connectionState = BluetoothProfile.STATE_DISCONNECTED;
            notifyStatus("The computer removed the PhonePad virtual cable.");
            notifyState();
        }
    };

    @Override
    @SuppressLint("MissingPermission")
    public void close() {
        inputExecutor.shutdownNow();
        mouseButtons = 0;
        if (hidDevice != null) {
            if (registered) {
                hidDevice.unregisterApp();
            }
            if (adapter != null) {
                adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
            }
        }
        hidDevice = null;
        activeHost = null;
        proxyReady = false;
        registered = false;
        registrationPending = false;
        connectionState = BluetoothProfile.STATE_DISCONNECTED;
    }

}
