package com.alisajjadfatmi.phonepad;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import java.util.List;
import java.util.Locale;

/**
 * Owns Bluetooth HID while the controls are not visible.
 *
 * Android automatically unregisters a Bluetooth HID Device application when it is no longer
 * foreground. Keeping the controller in this connected-device foreground service lets the phone
 * remain a keyboard and mouse while another app is open or the display is off.
 */
public final class PhonePadHidService extends Service implements HidDeviceController.Listener {
    static final String PREFERENCES = "phonepad_preferences";
    static final String PREF_HOST_ADDRESS = "preferred_host_address";

    private static final String CHANNEL_ID = "phonepad_hid_connection";
    private static final int NOTIFICATION_ID = 2401;

    private final LocalBinder binder = new LocalBinder();
    private HidDeviceController controller;
    private HidDeviceController.Listener uiListener;
    private String latestStatus = "Starting Bluetooth keyboard and mouse…";

    public final class LocalBinder extends Binder {
        PhonePadHidService getService() {
            return PhonePadHidService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startAsConnectedDeviceService();

        if (!hasBluetoothPermission()) {
            latestStatus = "Nearby Devices permission is required to keep PhonePad active.";
            updateNotification();
            stopSelf();
            return;
        }

        controller = new HidDeviceController(this, this);
        restorePreferredHost();
        controller.openProfile();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        uiListener = null;
        if (controller != null) {
            controller.close();
            controller = null;
        }
        super.onDestroy();
    }

    HidDeviceController controller() {
        return controller;
    }

    String latestStatus() {
        return latestStatus;
    }

    void setUiListener(HidDeviceController.Listener listener) {
        uiListener = listener;
        if (listener != null) {
            listener.onStatusChanged(latestStatus);
            listener.onStateChanged();
        }
    }

    @Override
    public void onStatusChanged(String message) {
        latestStatus = message;
        updateNotification();
        HidDeviceController.Listener listener = uiListener;
        if (listener != null) {
            listener.onStatusChanged(message);
        }
    }

    @Override
    public void onStateChanged() {
        updateNotification();
        HidDeviceController.Listener listener = uiListener;
        if (listener != null) {
            listener.onStateChanged();
        }
    }

    private boolean hasBluetoothPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
                == PackageManager.PERMISSION_GRANTED);
    }

    @SuppressLint("MissingPermission")
    private void restorePreferredHost() {
        if (controller == null) {
            return;
        }
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        String preferredAddress = preferences.getString(PREF_HOST_ADDRESS, null);
        List<BluetoothDevice> devices = controller.bondedDevices();
        BluetoothDevice fallbackLaptop = null;
        for (BluetoothDevice device : devices) {
            String address = safeAddress(device);
            if (preferredAddress != null && preferredAddress.equalsIgnoreCase(address)) {
                controller.setPreferredHost(device);
                return;
            }
            String name = device.getName();
            if (name != null && name.toUpperCase(Locale.ROOT).contains("ALISLAPTOP")) {
                fallbackLaptop = device;
            }
        }
        if (fallbackLaptop != null) {
            controller.setPreferredHost(fallbackLaptop);
        }
    }

    @SuppressLint("MissingPermission")
    private String safeAddress(BluetoothDevice device) {
        try {
            return device.getAddress();
        } catch (SecurityException exception) {
            return "";
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Bluetooth keyboard and mouse",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Keeps PhonePad connected when its controls are not on screen.");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void startAsConnectedDeviceService() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String detail = latestStatus;
        if (controller != null && controller.isConnected()) {
            detail = "Connected to " + controller.connectedHostName() + " · keyboard and mouse active";
        } else if (controller != null && controller.isRegistered()) {
            detail = "Keyboard and mouse active · waiting for the computer";
        }

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_phonepad_tile)
                .setContentTitle("PhonePad is active")
                .setContentText(detail)
                .setStyle(new Notification.BigTextStyle().bigText(detail))
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .build();
    }
}
