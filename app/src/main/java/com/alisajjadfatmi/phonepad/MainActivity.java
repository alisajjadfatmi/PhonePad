package com.alisajjadfatmi.phonepad;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity implements HidDeviceController.Listener {
    private static final int REQUEST_BLUETOOTH = 1001;
    private static final int BACKGROUND = Color.rgb(11, 16, 32);
    private static final int SURFACE = Color.rgb(21, 28, 49);
    private static final int PRIMARY = Color.rgb(112, 165, 255);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(182, 192, 216);

    private HidDeviceController controller;
    private TextView statusText;
    private TextView capabilityText;
    private Button permissionButton;
    private Button registerButton;
    private Button connectButton;
    private Button moveButton;
    private Button clickButton;
    private Button typeButton;
    private Spinner deviceSpinner;
    private final List<BluetoothDevice> devices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        setContentView(buildContent());
        controller = new HidDeviceController(this, this);
        refreshUi();
        if (hasBluetoothPermission()) {
            initializeBluetooth();
        } else {
            onStatusChanged("Allow Nearby Devices so PhonePad can register its keyboard and mouse service.");
        }
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);

        LinearLayout content = column();
        content.setPadding(dp(20), dp(28), dp(20), dp(32));
        scrollView.addView(content, matchWrap());

        TextView eyebrow = label("PHONEPAD · CAPABILITY TEST", 12, PRIMARY);
        eyebrow.setLetterSpacing(0.12f);
        content.addView(eyebrow);

        TextView title = label("Turn this phone into your PC keyboard and mouse", 28, TEXT);
        title.setPadding(0, dp(8), 0, dp(8));
        content.addView(title);

        TextView intro = label(
                "This first build verifies Samsung's native Bluetooth HID support before the full touchpad and keyboard are added.",
                15,
                MUTED
        );
        intro.setLineSpacing(0, 1.2f);
        content.addView(intro);

        statusText = label("Preparing Bluetooth…", 15, TEXT);
        statusText.setPadding(dp(16), dp(14), dp(16), dp(14));
        statusText.setBackground(roundRect(SURFACE, 16, PRIMARY, 1));
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.setMargins(0, dp(20), 0, dp(16));
        content.addView(statusText, statusParams);

        capabilityText = label("HID profile: checking", 14, MUTED);
        content.addView(capabilityText);

        LinearLayout permissionCard = card();
        permissionCard.addView(sectionTitle("1 · Bluetooth access"));
        permissionCard.addView(sectionBody("Grant Nearby Devices permission and keep Bluetooth turned on."));
        permissionButton = actionButton("Allow Bluetooth access");
        permissionButton.setOnClickListener(view -> requestBluetoothPermission());
        permissionCard.addView(permissionButton, buttonParams());
        content.addView(permissionCard, cardParams());

        LinearLayout registerCard = card();
        registerCard.addView(sectionTitle("2 · Register keyboard + mouse"));
        registerCard.addView(sectionBody("Android should advertise PhonePad as one composite input device."));
        registerButton = actionButton("Register PhonePad");
        registerButton.setOnClickListener(view -> controller.registerApp());
        registerCard.addView(registerButton, buttonParams());
        content.addView(registerCard, cardParams());

        LinearLayout connectCard = card();
        connectCard.addView(sectionTitle("3 · Connect to the paired laptop"));
        connectCard.addView(sectionBody("Choose ALISLAPTOP. The laptop and phone are already paired, so no scan is needed."));
        deviceSpinner = new Spinner(this);
        deviceSpinner.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams spinnerParams = matchWrap();
        spinnerParams.setMargins(0, dp(12), 0, dp(8));
        connectCard.addView(deviceSpinner, spinnerParams);
        connectButton = actionButton("Connect as input device");
        connectButton.setOnClickListener(view -> connectSelectedDevice());
        connectCard.addView(connectButton, buttonParams());
        content.addView(connectCard, cardParams());

        LinearLayout testCard = card();
        testCard.addView(sectionTitle("4 · Verify real input"));
        testCard.addView(sectionBody("Open Notepad on the laptop, then try each test."));
        moveButton = actionButton("Move pointer in a square");
        moveButton.setOnClickListener(view -> controller.testPointerMove());
        testCard.addView(moveButton, buttonParams());
        clickButton = secondaryButton("Send left click");
        clickButton.setOnClickListener(view -> controller.testLeftClick());
        testCard.addView(clickButton, buttonParams());
        typeButton = secondaryButton("Type “PhonePad”");
        typeButton.setOnClickListener(view -> controller.testTyping());
        testCard.addView(typeButton, buttonParams());
        content.addView(testCard, cardParams());

        TextView privacy = sectionBody("Offline by design · No internet permission · No analytics · No account");
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(8), 0, 0);
        content.addView(privacy);
        return scrollView;
    }

    private void initializeBluetooth() {
        if (!controller.hasBluetoothAdapter()) {
            onStatusChanged("No Bluetooth adapter was found on this phone.");
            return;
        }
        if (!controller.isBluetoothEnabled()) {
            onStatusChanged("Bluetooth is off. Turn it on, then return to PhonePad.");
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            return;
        }
        refreshBondedDevices();
        controller.openProfile();
    }

    private boolean hasBluetoothPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[] {Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH);
        } else {
            initializeBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_BLUETOOTH) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onStatusChanged("Bluetooth access granted.");
            initializeBluetooth();
        } else {
            onStatusChanged("Bluetooth access was denied. PhonePad cannot act as an input device without it.");
        }
        refreshUi();
    }

    private void refreshBondedDevices() {
        devices.clear();
        devices.addAll(controller.bondedDevices());
        List<String> names = new ArrayList<>();
        int laptopIndex = 0;
        for (int index = 0; index < devices.size(); index++) {
            BluetoothDevice device = devices.get(index);
            String name;
            try {
                name = device.getName();
                if (name == null || name.isBlank()) {
                    name = device.getAddress();
                }
            } catch (SecurityException exception) {
                name = "Paired device " + (index + 1);
            }
            names.add(name);
            if (name.toUpperCase().contains("ALISLAPTOP")) {
                laptopIndex = index;
            }
        }
        if (names.isEmpty()) {
            names.add("No paired computers found");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                names
        );
        deviceSpinner.setAdapter(adapter);
        if (!devices.isEmpty()) {
            deviceSpinner.setSelection(laptopIndex);
        }
    }

    private void connectSelectedDevice() {
        int position = deviceSpinner.getSelectedItemPosition();
        if (position < 0 || position >= devices.size()) {
            onStatusChanged("No paired laptop is available. Pair ALISLAPTOP in Samsung Bluetooth settings first.");
            return;
        }
        controller.connect(devices.get(position));
    }

    @Override
    public void onStatusChanged(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }

    @Override
    public void onStateChanged() {
        runOnUiThread(this::refreshUi);
    }

    private void refreshUi() {
        if (controller == null) {
            return;
        }
        boolean permission = hasBluetoothPermission();
        boolean proxy = controller.isProxyReady();
        boolean registered = controller.isRegistered();
        boolean connected = controller.isConnected();

        permissionButton.setEnabled(!permission);
        permissionButton.setText(permission ? "Bluetooth access granted" : "Allow Bluetooth access");
        registerButton.setEnabled(permission && proxy && !registered);
        registerButton.setText(registered ? "Keyboard + mouse registered" : "Register PhonePad");
        connectButton.setEnabled(registered && !devices.isEmpty() && !connected);
        connectButton.setText(connected
                ? "Connected to " + controller.connectedHostName()
                : "Connect as input device");
        moveButton.setEnabled(connected);
        clickButton.setEnabled(connected);
        typeButton.setEnabled(connected);
        capabilityText.setText(proxy
                ? "HID profile: available ✓"
                : "HID profile: waiting for Android");
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.close();
        }
        super.onDestroy();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout card() {
        LinearLayout layout = column();
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(roundRect(SURFACE, 18, SURFACE, 0));
        return layout;
    }

    private TextView sectionTitle(String text) {
        return label(text, 18, TEXT);
    }

    private TextView sectionBody(String text) {
        TextView view = label(text, 14, MUTED);
        view.setPadding(0, dp(6), 0, dp(4));
        view.setLineSpacing(0, 1.15f);
        return view;
    }

    private TextView label(String text, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.rgb(6, 17, 38));
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setBackground(roundRect(PRIMARY, 14, PRIMARY, 0));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(TEXT);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setBackground(roundRect(Color.rgb(35, 46, 76), 14, Color.rgb(75, 95, 135), 1));
        return button;
    }

    private GradientDrawable roundRect(int color, int radiusDp, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(12), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        params.setMargins(0, dp(10), 0, 0);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

