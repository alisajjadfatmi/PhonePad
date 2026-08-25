package com.alisajjadfatmi.phonepad;

final class HidReportDescriptor {
    static final byte KEYBOARD_REPORT_ID = 1;
    static final byte MOUSE_REPORT_ID = 2;
    static final byte CONSUMER_REPORT_ID = 3;

    private HidReportDescriptor() {
    }

    /**
     * Composite HID descriptor with a boot-compatible six-key keyboard and a
     * five-button relative mouse with vertical and horizontal scrolling.
     */
    static final byte[] COMPOSITE = new byte[] {
            // Keyboard, report ID 1.
            0x05, 0x01,             // Usage Page (Generic Desktop)
            0x09, 0x06,             // Usage (Keyboard)
            (byte) 0xA1, 0x01,      // Collection (Application)
            (byte) 0x85, KEYBOARD_REPORT_ID,
            0x05, 0x07,             // Usage Page (Keyboard)
            0x19, (byte) 0xE0,
            0x29, (byte) 0xE7,
            0x15, 0x00,
            0x25, 0x01,
            0x75, 0x01,
            (byte) 0x95, 0x08,
            (byte) 0x81, 0x02,      // Modifier bitmap
            (byte) 0x95, 0x01,
            0x75, 0x08,
            (byte) 0x81, 0x01,      // Reserved byte
            0x05, 0x08,             // Usage Page (LEDs)
            0x19, 0x01,
            0x29, 0x05,
            (byte) 0x95, 0x05,
            0x75, 0x01,
            (byte) 0x91, 0x02,
            (byte) 0x95, 0x01,
            0x75, 0x03,
            (byte) 0x91, 0x01,
            0x05, 0x07,
            0x19, 0x00,
            0x29, 0x65,
            0x15, 0x00,
            0x25, 0x65,
            (byte) 0x95, 0x06,
            0x75, 0x08,
            (byte) 0x81, 0x00,      // Six key usages
            (byte) 0xC0,

            // Mouse, report ID 2.
            0x05, 0x01,
            0x09, 0x02,
            (byte) 0xA1, 0x01,
            (byte) 0x85, MOUSE_REPORT_ID,
            0x09, 0x01,
            (byte) 0xA1, 0x00,
            0x05, 0x09,             // Usage Page (Buttons)
            0x19, 0x01,
            0x29, 0x05,             // Five buttons
            0x15, 0x00,
            0x25, 0x01,
            (byte) 0x95, 0x05,
            0x75, 0x01,
            (byte) 0x81, 0x02,
            (byte) 0x95, 0x01,
            0x75, 0x03,
            (byte) 0x81, 0x01,      // Button padding
            0x05, 0x01,
            0x09, 0x30,             // X
            0x09, 0x31,             // Y
            0x09, 0x38,             // Wheel
            0x15, (byte) 0x81,
            0x25, 0x7F,
            0x75, 0x08,
            (byte) 0x95, 0x03,
            (byte) 0x81, 0x06,      // Relative axes
            0x05, 0x0C,             // Consumer page
            0x0A, 0x38, 0x02,       // AC Pan
            0x15, (byte) 0x81,
            0x25, 0x7F,
            0x75, 0x08,
            (byte) 0x95, 0x01,
            (byte) 0x81, 0x06,
            (byte) 0xC0,
            (byte) 0xC0,

            // Consumer controls, report ID 3. One 16-bit usage at a time.
            0x05, 0x0C,             // Usage Page (Consumer)
            0x09, 0x01,             // Usage (Consumer Control)
            (byte) 0xA1, 0x01,
            (byte) 0x85, CONSUMER_REPORT_ID,
            0x15, 0x00,
            0x26, (byte) 0xFF, 0x03,
            0x19, 0x00,
            0x2A, (byte) 0xFF, 0x03,
            0x75, 0x10,
            (byte) 0x95, 0x01,
            (byte) 0x81, 0x00,
            (byte) 0xC0
    };
}
