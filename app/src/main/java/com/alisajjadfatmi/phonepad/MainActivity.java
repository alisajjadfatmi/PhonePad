package com.alisajjadfatmi.phonepad;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.InputType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity implements HidDeviceController.Listener {
    private static final int REQUEST_BLUETOOTH = 1001;
    private static final int REQUEST_DISCOVERABLE = 1002;
    private static final int BACKGROUND = Color.rgb(11, 16, 32);
    private static final int SURFACE = Color.rgb(21, 28, 49);
    private static final int PRIMARY = Color.rgb(112, 165, 255);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(182, 192, 216);
    private static final String PREFERENCES = "phonepad_preferences";
    private static final String PREF_MODE = "control_mode";
    private static final String PREF_SENSITIVITY = "pointer_sensitivity";
    private static final String PREF_NATURAL_SCROLL = "natural_scroll";
    private static final String PREF_HAPTICS = "touchpad_haptics";
    private static final String PREF_LEFT_HANDED = "left_handed_primary";
    private static final String PREF_HOST_ADDRESS = "preferred_host_address";

    private HidDeviceController controller;
    private TextView statusText;
    private TextView capabilityText;
    private Button permissionButton;
    private Button registerButton;
    private Button connectButton;
    private Button discoverableButton;
    private Button refreshButton;
    private Spinner deviceSpinner;
    private LinearLayout permissionCard;
    private LinearLayout registerCard;
    private LinearLayout connectCard;
    private FrameLayout controlHost;
    private LinearLayout touchpadPanel;
    private LinearLayout phoneKeyboardPanel;
    private LinearLayout pcKeyboardPanel;
    private LinearLayout presenterPanel;
    private Button touchpadTab;
    private Button phoneKeyboardTab;
    private Button pcKeyboardTab;
    private Button presenterTab;
    private TouchpadView touchpadView;
    private PhoneKeyboardEditText phoneKeyboardInput;
    private final List<View> connectionRequiredViews = new ArrayList<>();
    private final List<Button> modifierButtons = new ArrayList<>();
    private int activeModifiers;
    private int selectedMode;
    private int sensitivityIndex = 1;
    private boolean naturalScroll;
    private boolean hapticsEnabled = true;
    private boolean leftHanded;
    private String preferredHostAddress;
    private SharedPreferences preferences;
    private final List<BluetoothDevice> devices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        selectedMode = Math.max(0, Math.min(3, preferences.getInt(PREF_MODE, 0)));
        sensitivityIndex = Math.max(0, Math.min(2, preferences.getInt(PREF_SENSITIVITY, 1)));
        naturalScroll = preferences.getBoolean(PREF_NATURAL_SCROLL, false);
        hapticsEnabled = preferences.getBoolean(PREF_HAPTICS, true);
        leftHanded = preferences.getBoolean(PREF_LEFT_HANDED, false);
        preferredHostAddress = preferences.getString(PREF_HOST_ADDRESS, null);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        TextView eyebrow = label("PHONEPAD · PRIVATE BLUETOOTH CONTROL", 12, PRIMARY);
        eyebrow.setLetterSpacing(0.12f);
        content.addView(eyebrow);

        TextView title = label("Turn this phone into your PC keyboard and mouse", 28, TEXT);
        title.setPadding(0, dp(8), 0, dp(8));
        content.addView(title);

        TextView intro = label(
                "Touchpad, five-button mouse, Samsung Keyboard, full PC keyboard, shortcuts, scrolling and media controls.",
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

        permissionCard = card();
        permissionCard.addView(sectionTitle("1 · Bluetooth access"));
        permissionCard.addView(sectionBody("Grant Nearby Devices permission and keep Bluetooth turned on."));
        permissionButton = actionButton("Allow Bluetooth access");
        permissionButton.setOnClickListener(view -> requestBluetoothPermission());
        permissionCard.addView(permissionButton, buttonParams());
        content.addView(permissionCard, cardParams());

        registerCard = card();
        registerCard.addView(sectionTitle("2 · Register keyboard + mouse"));
        registerCard.addView(sectionBody("Android should advertise PhonePad as one composite input device."));
        registerButton = actionButton("Register PhonePad");
        registerButton.setOnClickListener(view -> controller.registerApp());
        registerCard.addView(registerButton, buttonParams());
        content.addView(registerCard, cardParams());

        connectCard = card();
        connectCard.addView(sectionTitle("3 · Connect to the paired laptop"));
        connectCard.addView(sectionBody(
                "For the first HID connection, pair while PhonePad is registered so Windows discovers the keyboard and mouse service."
        ));
        discoverableButton = secondaryButton("Make phone discoverable for fresh pairing");
        discoverableButton.setOnClickListener(view -> makeDiscoverable());
        connectCard.addView(discoverableButton, buttonParams());
        deviceSpinner = new Spinner(this);
        deviceSpinner.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams spinnerParams = matchWrap();
        spinnerParams.setMargins(0, dp(12), 0, dp(8));
        connectCard.addView(deviceSpinner, spinnerParams);
        refreshButton = secondaryButton("Refresh paired computers");
        refreshButton.setOnClickListener(view -> {
            refreshBondedDevices();
            refreshUi();
            onStatusChanged("Refreshed the paired-device list.");
        });
        connectCard.addView(refreshButton, buttonParams());
        connectButton = actionButton("Connect as input device");
        connectButton.setOnClickListener(view -> connectSelectedDevice());
        connectCard.addView(connectButton, buttonParams());
        content.addView(connectCard, cardParams());

        content.addView(buildControlDeck(), cardParams());

        TextView privacy = sectionBody("Offline by design · No internet permission · No analytics · No account");
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(8), 0, 0);
        content.addView(privacy);
        return scrollView;
    }

    private LinearLayout buildControlDeck() {
        LinearLayout deck = card();
        deck.addView(sectionTitle("4 · Controls"));
        deck.addView(sectionBody("Switch between touchpad, Samsung Keyboard, PC keys and presentation controls."));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        touchpadTab = compactButton("Pad");
        phoneKeyboardTab = compactButton("Phone");
        pcKeyboardTab = compactButton("PC keys");
        presenterTab = compactButton("Present");
        touchpadTab.setOnClickListener(view -> showMode(0));
        phoneKeyboardTab.setOnClickListener(view -> showMode(1));
        pcKeyboardTab.setOnClickListener(view -> showMode(2));
        presenterTab.setOnClickListener(view -> showMode(3));
        tabs.addView(touchpadTab, weightedCompactParams());
        tabs.addView(phoneKeyboardTab, weightedCompactParams());
        tabs.addView(pcKeyboardTab, weightedCompactParams());
        tabs.addView(presenterTab, weightedCompactParams());
        LinearLayout.LayoutParams tabsParams = matchWrap();
        tabsParams.setMargins(0, dp(10), 0, dp(12));
        deck.addView(tabs, tabsParams);

        controlHost = new FrameLayout(this);
        touchpadPanel = buildTouchpadPanel();
        phoneKeyboardPanel = buildPhoneKeyboardPanel();
        pcKeyboardPanel = buildPcKeyboardPanel();
        presenterPanel = buildPresenterPanel();
        controlHost.addView(touchpadPanel, frameWrapParams());
        controlHost.addView(phoneKeyboardPanel, frameWrapParams());
        controlHost.addView(pcKeyboardPanel, frameWrapParams());
        controlHost.addView(presenterPanel, frameWrapParams());
        deck.addView(controlHost, matchWrap());
        showMode(selectedMode);
        return deck;
    }

    private LinearLayout buildTouchpadPanel() {
        LinearLayout panel = column();
        touchpadView = new TouchpadView(this);
        float[] sensitivityValues = {0.85f, 1.35f, 1.9f};
        String[] sensitivityLabels = {"slow", "normal", "fast"};
        touchpadView.setSensitivity(sensitivityValues[sensitivityIndex]);
        touchpadView.setNaturalScroll(naturalScroll);
        touchpadView.setHapticsEnabled(hapticsEnabled);
        touchpadView.setLeftHanded(leftHanded);
        touchpadView.setListener(new TouchpadView.Listener() {
            @Override
            public void onPointerMove(int dx, int dy) {
                controller.movePointer(dx, dy);
            }

            @Override
            public void onScroll(int vertical, int horizontal) {
                controller.scroll(vertical, horizontal);
            }

            @Override
            public void onButtonDown(int button) {
                controller.mouseButtonDown(button);
            }

            @Override
            public void onButtonUp(int button) {
                controller.mouseButtonUp(button);
            }

            @Override
            public void onButtonClick(int button) {
                controller.mouseClick(button);
            }
        });
        connectionRequiredViews.add(touchpadView);
        LinearLayout.LayoutParams padParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(410)
        );
        panel.addView(touchpadView, padParams);

        LinearLayout primaryButtons = new LinearLayout(this);
        primaryButtons.setOrientation(LinearLayout.HORIZONTAL);
        primaryButtons.addView(mouseButton("Left click", HidDeviceController.MOUSE_LEFT), weightedButtonParams());
        primaryButtons.addView(mouseButton("Right click", HidDeviceController.MOUSE_RIGHT), weightedButtonParams());
        panel.addView(primaryButtons, rowParams());

        LinearLayout extraButtons = new LinearLayout(this);
        extraButtons.setOrientation(LinearLayout.HORIZONTAL);
        extraButtons.addView(mouseButton("Back", HidDeviceController.MOUSE_BACK), weightedButtonParams());
        extraButtons.addView(mouseButton("Middle", HidDeviceController.MOUSE_MIDDLE), weightedButtonParams());
        extraButtons.addView(mouseButton("Forward", HidDeviceController.MOUSE_FORWARD), weightedButtonParams());
        panel.addView(extraButtons, rowParams());

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.HORIZONTAL);
        Button speed = compactButton("Speed: " + sensitivityLabels[sensitivityIndex]);
        speed.setOnClickListener(view -> {
            sensitivityIndex = (sensitivityIndex + 1) % sensitivityValues.length;
            touchpadView.setSensitivity(sensitivityValues[sensitivityIndex]);
            speed.setText("Speed: " + sensitivityLabels[sensitivityIndex]);
            preferences.edit().putInt(PREF_SENSITIVITY, sensitivityIndex).apply();
        });
        Button scrollDirection = compactButton(naturalScroll ? "Scroll: natural" : "Scroll: standard");
        scrollDirection.setOnClickListener(view -> {
            naturalScroll = !naturalScroll;
            touchpadView.setNaturalScroll(naturalScroll);
            scrollDirection.setText(naturalScroll ? "Scroll: natural" : "Scroll: standard");
            preferences.edit().putBoolean(PREF_NATURAL_SCROLL, naturalScroll).apply();
        });
        settings.addView(speed, weightedCompactParams());
        settings.addView(scrollDirection, weightedCompactParams());
        panel.addView(settings, rowParams());

        LinearLayout accessibility = new LinearLayout(this);
        accessibility.setOrientation(LinearLayout.HORIZONTAL);
        Button haptics = compactButton(hapticsEnabled ? "Haptics: on" : "Haptics: off");
        haptics.setOnClickListener(view -> {
            hapticsEnabled = !hapticsEnabled;
            touchpadView.setHapticsEnabled(hapticsEnabled);
            haptics.setText(hapticsEnabled ? "Haptics: on" : "Haptics: off");
            preferences.edit().putBoolean(PREF_HAPTICS, hapticsEnabled).apply();
        });
        Button primarySide = compactButton(leftHanded ? "Primary: right" : "Primary: left");
        primarySide.setOnClickListener(view -> {
            leftHanded = !leftHanded;
            touchpadView.setLeftHanded(leftHanded);
            primarySide.setText(leftHanded ? "Primary: right" : "Primary: left");
            preferences.edit().putBoolean(PREF_LEFT_HANDED, leftHanded).apply();
        });
        accessibility.addView(haptics, weightedCompactParams());
        accessibility.addView(primarySide, weightedCompactParams());
        panel.addView(accessibility, rowParams());
        return panel;
    }

    private LinearLayout buildPhoneKeyboardPanel() {
        LinearLayout panel = column();
        panel.addView(sectionBody(
                "Use Samsung Keyboard like a physical Bluetooth keyboard. Windows owns the text and cursor; this box only captures the current word."
        ));

        phoneKeyboardInput = new PhoneKeyboardEditText(this);
        phoneKeyboardInput.setTextColor(Color.rgb(12, 20, 36));
        phoneKeyboardInput.setHintTextColor(Color.rgb(92, 103, 126));
        phoneKeyboardInput.setTextSize(18);
        phoneKeyboardInput.setHint("Tap here — typing appears on Windows…");
        phoneKeyboardInput.setGravity(Gravity.TOP | Gravity.START);
        phoneKeyboardInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        phoneKeyboardInput.setBackground(roundRect(Color.WHITE, 14, PRIMARY, 1));
        phoneKeyboardInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        phoneKeyboardInput.setMinLines(3);
        phoneKeyboardInput.setPhoneKeyboardListener(new PhoneKeyboardEditText.Listener() {
            @Override
            public void onTypedText(CharSequence text) {
                controller.typeText(text);
            }

            @Override
            public void onBackspace(int count) {
                controller.backspace(count);
            }

            @Override
            public void onDelete(int count) {
                controller.deleteForward(count);
            }

            @Override
            public void onEnter() {
                controller.keyTap(0, HidKeyMap.KEY_ENTER);
            }

            @Override
            public void onTab() {
                controller.keyTap(0, HidKeyMap.KEY_TAB);
            }
        });
        connectionRequiredViews.add(phoneKeyboardInput);
        LinearLayout.LayoutParams inputParams = matchWrap();
        inputParams.setMargins(0, dp(8), 0, dp(8));
        panel.addView(phoneKeyboardInput, inputParams);

        LinearLayout inputActions = new LinearLayout(this);
        inputActions.setOrientation(LinearLayout.HORIZONTAL);
        Button openKeyboard = actionButton("Open Samsung Keyboard");
        openKeyboard.setOnClickListener(view -> {
            phoneKeyboardInput.requestFocus();
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.showSoftInput(phoneKeyboardInput, InputMethodManager.SHOW_IMPLICIT);
        });
        connectionRequiredViews.add(openKeyboard);
        Button clear = secondaryButton("Reset keyboard");
        clear.setOnClickListener(view -> phoneKeyboardInput.resetKeyboardCapture());
        inputActions.addView(openKeyboard, weightedButtonParams());
        inputActions.addView(clear, weightedButtonParams());
        panel.addView(inputActions, rowParams());

        Button sendEnter = secondaryButton("Send Enter ↵");
        sendEnter.setOnClickListener(view -> controller.keyTap(0, HidKeyMap.KEY_ENTER));
        connectionRequiredViews.add(sendEnter);
        panel.addView(sendEnter, buttonParams());

        panel.addView(sectionBody("Windows shortcuts"));
        panel.addView(buildShortcutStrip());
        panel.addView(sectionBody("Media controls"));
        panel.addView(buildMediaStrip());
        return panel;
    }

    private LinearLayout buildPcKeyboardPanel() {
        LinearLayout panel = column();
        panel.addView(sectionBody("Modifiers stay active until tapped again. Rows scroll sideways on smaller screens."));

        List<KeySpec> functionKeys = new ArrayList<>();
        functionKeys.add(new KeySpec("Esc", HidKeyMap.KEY_ESCAPE, 64));
        for (int index = 0; index < 12; index++) {
            functionKeys.add(new KeySpec("F" + (index + 1), HidKeyMap.KEY_F1 + index, 58));
        }
        functionKeys.add(new KeySpec("PrtSc", HidKeyMap.KEY_PRINT_SCREEN, 72));
        functionKeys.add(new KeySpec("Del", HidKeyMap.KEY_DELETE, 64));
        panel.addView(buildKeyRow(functionKeys.toArray(new KeySpec[0])));

        panel.addView(buildKeyRow(
                new KeySpec("`  ~", 0x35),
                new KeySpec("1  !", 0x1E), new KeySpec("2  @", 0x1F),
                new KeySpec("3  #", 0x20), new KeySpec("4  $", 0x21),
                new KeySpec("5  %", 0x22), new KeySpec("6  ^", 0x23),
                new KeySpec("7  &", 0x24), new KeySpec("8  *", 0x25),
                new KeySpec("9  (", 0x26), new KeySpec("0  )", 0x27),
                new KeySpec("-  _", 0x2D), new KeySpec("=  +", 0x2E),
                new KeySpec("Backspace", HidKeyMap.KEY_BACKSPACE, 112)
        ));
        panel.addView(buildKeyRow(
                new KeySpec("Tab", HidKeyMap.KEY_TAB, 78),
                letter("Q"), letter("W"), letter("E"), letter("R"), letter("T"),
                letter("Y"), letter("U"), letter("I"), letter("O"), letter("P"),
                new KeySpec("[  {", 0x2F), new KeySpec("]  }", 0x30),
                new KeySpec("\\  |", 0x31)
        ));
        panel.addView(buildKeyRow(
                new KeySpec("Caps", HidKeyMap.KEY_CAPS_LOCK, 86),
                letter("A"), letter("S"), letter("D"), letter("F"), letter("G"),
                letter("H"), letter("J"), letter("K"), letter("L"),
                new KeySpec(";  :", 0x33), new KeySpec("'  \"", 0x34),
                new KeySpec("Enter", HidKeyMap.KEY_ENTER, 108)
        ));
        panel.addView(buildKeyRow(
                new KeySpec("Shift", -HidKeyMap.MOD_SHIFT, 98),
                letter("Z"), letter("X"), letter("C"), letter("V"), letter("B"),
                letter("N"), letter("M"), new KeySpec(",  <", 0x36),
                new KeySpec(".  >", 0x37), new KeySpec("/  ?", 0x38),
                new KeySpec("↑", HidKeyMap.KEY_UP, 62)
        ));
        panel.addView(buildKeyRow(
                new KeySpec("Ctrl", -HidKeyMap.MOD_CTRL, 82),
                new KeySpec("Win", -HidKeyMap.MOD_GUI, 78),
                new KeySpec("Alt", -HidKeyMap.MOD_ALT, 78),
                new KeySpec("Space", HidKeyMap.KEY_SPACE, 240),
                new KeySpec("Alt", -HidKeyMap.MOD_ALT, 78),
                new KeySpec("←", HidKeyMap.KEY_LEFT, 62),
                new KeySpec("↓", HidKeyMap.KEY_DOWN, 62),
                new KeySpec("→", HidKeyMap.KEY_RIGHT, 62)
        ));
        panel.addView(buildKeyRow(
                new KeySpec("Ins", HidKeyMap.KEY_INSERT),
                new KeySpec("Home", HidKeyMap.KEY_HOME, 74),
                new KeySpec("PgUp", HidKeyMap.KEY_PAGE_UP, 74),
                new KeySpec("End", HidKeyMap.KEY_END, 74),
                new KeySpec("PgDn", HidKeyMap.KEY_PAGE_DOWN, 74),
                new KeySpec("Delete", HidKeyMap.KEY_DELETE, 82)
        ));
        panel.addView(sectionBody("Media controls"));
        panel.addView(buildMediaStrip());
        return panel;
    }

    private LinearLayout buildPresenterPanel() {
        LinearLayout panel = column();
        panel.addView(sectionBody(
                "Large presentation controls for PowerPoint, Google Slides and other apps that accept standard keyboard shortcuts."
        ));

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.addView(
                presentationButton("← Previous", 0, HidKeyMap.KEY_LEFT),
                weightedButtonParams()
        );
        navigation.addView(
                presentationButton("Next →", 0, HidKeyMap.KEY_RIGHT),
                weightedButtonParams()
        );
        panel.addView(navigation, rowParams());

        LinearLayout startControls = new LinearLayout(this);
        startControls.setOrientation(LinearLayout.HORIZONTAL);
        startControls.addView(
                presentationButton("Start · F5", 0, HidKeyMap.KEY_F1 + 4),
                weightedButtonParams()
        );
        startControls.addView(
                presentationButton("From current", HidKeyMap.MOD_SHIFT, HidKeyMap.KEY_F1 + 4),
                weightedButtonParams()
        );
        panel.addView(startControls, rowParams());

        LinearLayout screenControls = new LinearLayout(this);
        screenControls.setOrientation(LinearLayout.HORIZONTAL);
        screenControls.addView(
                presentationButton("Black screen · B", 0, HidKeyMap.letterUsage('b')),
                weightedButtonParams()
        );
        screenControls.addView(
                presentationButton("White screen · W", 0, HidKeyMap.letterUsage('w')),
                weightedButtonParams()
        );
        panel.addView(screenControls, rowParams());

        Button endPresentation = presentationButton("End presentation · Esc", 0, HidKeyMap.KEY_ESCAPE);
        panel.addView(endPresentation, buttonParams());
        panel.addView(sectionBody("Media and volume"));
        panel.addView(buildMediaStrip());
        return panel;
    }

    private HorizontalScrollView buildShortcutStrip() {
        HorizontalScrollView scroll = horizontalStrip();
        LinearLayout row = horizontalRow();
        row.addView(shortcutButton("Copy", HidKeyMap.MOD_CTRL, HidKeyMap.letterUsage('c')));
        row.addView(shortcutButton("Paste", HidKeyMap.MOD_CTRL, HidKeyMap.letterUsage('v')));
        row.addView(shortcutButton("Cut", HidKeyMap.MOD_CTRL, HidKeyMap.letterUsage('x')));
        row.addView(shortcutButton("Undo", HidKeyMap.MOD_CTRL, HidKeyMap.letterUsage('z')));
        row.addView(shortcutButton("Redo", HidKeyMap.MOD_CTRL, HidKeyMap.letterUsage('y')));
        row.addView(shortcutButton("Select all", HidKeyMap.MOD_CTRL, HidKeyMap.letterUsage('a')));
        row.addView(shortcutButton("Switch app", HidKeyMap.MOD_ALT, HidKeyMap.KEY_TAB));
        row.addView(shortcutButton("Close", HidKeyMap.MOD_ALT, HidKeyMap.KEY_F1 + 3));
        row.addView(shortcutButton("Search", HidKeyMap.MOD_GUI, HidKeyMap.letterUsage('s')));
        scroll.addView(row);
        return scroll;
    }

    private HorizontalScrollView buildMediaStrip() {
        HorizontalScrollView scroll = horizontalStrip();
        LinearLayout row = horizontalRow();
        row.addView(consumerButton("Previous", HidKeyMap.CONSUMER_PREVIOUS));
        row.addView(consumerButton("Play / pause", HidKeyMap.CONSUMER_PLAY_PAUSE));
        row.addView(consumerButton("Next", HidKeyMap.CONSUMER_NEXT));
        row.addView(consumerButton("Mute", HidKeyMap.CONSUMER_MUTE));
        row.addView(consumerButton("Volume −", HidKeyMap.CONSUMER_VOLUME_DOWN));
        row.addView(consumerButton("Volume +", HidKeyMap.CONSUMER_VOLUME_UP));
        row.addView(consumerButton("Brightness −", HidKeyMap.CONSUMER_BRIGHTNESS_DOWN));
        row.addView(consumerButton("Brightness +", HidKeyMap.CONSUMER_BRIGHTNESS_UP));
        scroll.addView(row);
        return scroll;
    }

    private HorizontalScrollView buildKeyRow(KeySpec... keys) {
        HorizontalScrollView scroll = horizontalStrip();
        LinearLayout row = horizontalRow();
        for (KeySpec key : keys) {
            Button button;
            if (key.usage < 0) {
                button = modifierButton(key.label, -key.usage);
            } else {
                button = compactButton(key.label);
                button.setOnClickListener(view -> controller.keyTap(activeModifiers, key.usage));
                connectionRequiredViews.add(button);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(key.widthDp), dp(52));
            params.setMargins(dp(3), dp(4), dp(3), dp(4));
            row.addView(button, params);
        }
        scroll.addView(row);
        return scroll;
    }

    private Button mouseButton(String label, int buttonMask) {
        Button button = secondaryButton(label);
        button.setOnClickListener(view -> controller.mouseClick(buttonMask));
        connectionRequiredViews.add(button);
        return button;
    }

    private Button presentationButton(String label, int modifiers, int usage) {
        Button button = secondaryButton(label);
        button.setOnClickListener(view -> controller.keyTap(modifiers, usage));
        connectionRequiredViews.add(button);
        return button;
    }

    private Button shortcutButton(String label, int modifiers, int usage) {
        Button button = compactButton(label);
        button.setOnClickListener(view -> controller.keyTap(modifiers, usage));
        connectionRequiredViews.add(button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(100), dp(50));
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private Button consumerButton(String label, int usage) {
        Button button = compactButton(label);
        button.setOnClickListener(view -> controller.consumerTap(usage));
        connectionRequiredViews.add(button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(112), dp(50));
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private Button modifierButton(String label, int modifier) {
        Button button = compactButton(label);
        button.setOnClickListener(view -> {
            activeModifiers ^= modifier;
            refreshModifierButtons();
        });
        button.setTag(modifier);
        modifierButtons.add(button);
        connectionRequiredViews.add(button);
        return button;
    }

    private void refreshModifierButtons() {
        for (Button button : modifierButtons) {
            int modifier = (int) button.getTag();
            boolean active = (activeModifiers & modifier) != 0;
            button.setBackground(active
                    ? roundRect(PRIMARY, 12, PRIMARY, 0)
                    : roundRect(Color.rgb(35, 46, 76), 12, Color.rgb(75, 95, 135), 1));
            button.setTextColor(active ? Color.rgb(6, 17, 38) : TEXT);
        }
    }

    private void showMode(int mode) {
        selectedMode = Math.max(0, Math.min(3, mode));
        if (touchpadPanel == null) {
            return;
        }
        touchpadPanel.setVisibility(selectedMode == 0 ? View.VISIBLE : View.GONE);
        phoneKeyboardPanel.setVisibility(selectedMode == 1 ? View.VISIBLE : View.GONE);
        pcKeyboardPanel.setVisibility(selectedMode == 2 ? View.VISIBLE : View.GONE);
        presenterPanel.setVisibility(selectedMode == 3 ? View.VISIBLE : View.GONE);
        styleTab(touchpadTab, selectedMode == 0);
        styleTab(phoneKeyboardTab, selectedMode == 1);
        styleTab(pcKeyboardTab, selectedMode == 2);
        styleTab(presenterTab, selectedMode == 3);
        preferences.edit().putInt(PREF_MODE, selectedMode).apply();
    }

    private void styleTab(Button button, boolean selected) {
        button.setBackground(selected
                ? roundRect(PRIMARY, 12, PRIMARY, 0)
                : roundRect(Color.rgb(35, 46, 76), 12, Color.rgb(75, 95, 135), 1));
        button.setTextColor(selected ? Color.rgb(6, 17, 38) : TEXT);
    }

    private HorizontalScrollView horizontalStrip() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);
        return scroll;
    }

    private LinearLayout horizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private KeySpec letter(String label) {
        return new KeySpec(label, HidKeyMap.letterUsage(label.charAt(0)));
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
                || (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[] {
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            }, REQUEST_BLUETOOTH);
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
        boolean granted = grantResults.length == permissions.length;
        for (int result : grantResults) {
            granted = granted && result == PackageManager.PERMISSION_GRANTED;
        }
        if (granted) {
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
        int preferredIndex = -1;
        int laptopFallbackIndex = 0;
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
            if (name.toUpperCase(Locale.ROOT).contains("ALISLAPTOP")) {
                laptopFallbackIndex = index;
            }
            if (preferredHostAddress != null
                    && preferredHostAddress.equalsIgnoreCase(safeAddress(device))) {
                preferredIndex = index;
            }
        }
        if (names.isEmpty()) {
            names.add("No paired computers found");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                names
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                styleDeviceChoice(view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                styleDeviceChoice(view);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(adapter);
        if (!devices.isEmpty()) {
            int selection = preferredIndex >= 0 ? preferredIndex : laptopFallbackIndex;
            deviceSpinner.setSelection(selection);
            controller.setPreferredHost(devices.get(selection));
        }
    }

    private String safeAddress(BluetoothDevice device) {
        try {
            return device.getAddress();
        } catch (SecurityException exception) {
            return "";
        }
    }

    private void styleDeviceChoice(TextView view) {
        view.setTextColor(Color.rgb(12, 20, 36));
        view.setTextSize(16);
        view.setBackgroundColor(Color.WHITE);
        view.setPadding(dp(16), dp(12), dp(16), dp(12));
    }

    private void connectSelectedDevice() {
        int position = deviceSpinner.getSelectedItemPosition();
        if (position < 0 || position >= devices.size()) {
            onStatusChanged("No paired computer is available. Pair one in Samsung Bluetooth settings first.");
            return;
        }
        BluetoothDevice selectedDevice = devices.get(position);
        preferredHostAddress = safeAddress(selectedDevice);
        preferences.edit().putString(PREF_HOST_ADDRESS, preferredHostAddress).apply();
        controller.setPreferredHost(selectedDevice);
        controller.connect(selectedDevice);
    }

    private void makeDiscoverable() {
        if (!controller.isRegistered()) {
            onStatusChanged("Register PhonePad before starting fresh pairing.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermission();
            return;
        }
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        try {
            startActivityForResult(intent, REQUEST_DISCOVERABLE);
        } catch (SecurityException exception) {
            onStatusChanged("Android blocked discoverability. Allow Nearby Devices and try again.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_DISCOVERABLE) {
            return;
        }
        if (resultCode > 0) {
            if (!controller.isRegistered()) {
                controller.registerApp();
            }
            onStatusChanged("PhonePad is discoverable for " + resultCode
                    + " seconds. Add it from Windows Bluetooth settings now.");
        } else {
            onStatusChanged("Discoverability was cancelled.");
        }
        refreshUi();
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
        boolean registrationPending = controller.isRegistrationPending();
        registerButton.setEnabled(permission && proxy && !registered && !registrationPending);
        registerButton.setText(registered
                ? "Keyboard + mouse registered"
                : registrationPending ? "Registering keyboard + mouse…" : "Register PhonePad");
        discoverableButton.setEnabled(permission && registered);
        refreshButton.setEnabled(permission);
        connectButton.setEnabled(registered && !devices.isEmpty() && !connected);
        connectButton.setText(connected
                ? "Connected to " + controller.connectedHostName()
                : "Connect as input device");
        permissionCard.setVisibility(connected ? View.GONE : View.VISIBLE);
        registerCard.setVisibility(connected ? View.GONE : View.VISIBLE);
        connectCard.setVisibility(connected ? View.GONE : View.VISIBLE);
        for (View view : connectionRequiredViews) {
            view.setEnabled(connected);
        }
        if (controlHost != null) {
            controlHost.setAlpha(connected ? 1f : 0.48f);
        }
        capabilityText.setText(proxy
                ? "HID profile: available ✓"
                : "HID profile: waiting for Android");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controller != null && hasBluetoothPermission()) {
            refreshBondedDevices();
            refreshUi();
        }
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

    private Button compactButton(String text) {
        Button button = secondaryButton(text);
        button.setTextSize(13);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(10), dp(6), dp(10), dp(6));
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

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams weightedCompactParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, 0);
        return params;
    }

    private FrameLayout.LayoutParams frameWrapParams() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class KeySpec {
        final String label;
        final int usage;
        final int widthDp;

        KeySpec(String label, int usage) {
            this(label, usage, 64);
        }

        KeySpec(String label, int usage, int widthDp) {
            this.label = label;
            this.usage = usage;
            this.widthDp = widthDp;
        }
    }
}
