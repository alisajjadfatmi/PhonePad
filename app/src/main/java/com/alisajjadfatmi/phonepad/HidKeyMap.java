package com.alisajjadfatmi.phonepad;

final class HidKeyMap {
    static final int MOD_CTRL = 0x01;
    static final int MOD_SHIFT = 0x02;
    static final int MOD_ALT = 0x04;
    static final int MOD_GUI = 0x08;

    static final int KEY_ENTER = 0x28;
    static final int KEY_ESCAPE = 0x29;
    static final int KEY_BACKSPACE = 0x2A;
    static final int KEY_TAB = 0x2B;
    static final int KEY_SPACE = 0x2C;
    static final int KEY_CAPS_LOCK = 0x39;
    static final int KEY_F1 = 0x3A;
    static final int KEY_PRINT_SCREEN = 0x46;
    static final int KEY_INSERT = 0x49;
    static final int KEY_HOME = 0x4A;
    static final int KEY_PAGE_UP = 0x4B;
    static final int KEY_DELETE = 0x4C;
    static final int KEY_END = 0x4D;
    static final int KEY_PAGE_DOWN = 0x4E;
    static final int KEY_RIGHT = 0x4F;
    static final int KEY_LEFT = 0x50;
    static final int KEY_DOWN = 0x51;
    static final int KEY_UP = 0x52;

    static final int CONSUMER_PLAY_PAUSE = 0x00CD;
    static final int CONSUMER_MUTE = 0x00E2;
    static final int CONSUMER_VOLUME_UP = 0x00E9;
    static final int CONSUMER_VOLUME_DOWN = 0x00EA;
    static final int CONSUMER_NEXT = 0x00B5;
    static final int CONSUMER_PREVIOUS = 0x00B6;
    static final int CONSUMER_BRIGHTNESS_UP = 0x006F;
    static final int CONSUMER_BRIGHTNESS_DOWN = 0x0070;

    private HidKeyMap() {
    }

    static int letterUsage(char letter) {
        char lower = Character.toLowerCase(letter);
        return lower >= 'a' && lower <= 'z' ? 0x04 + lower - 'a' : 0;
    }

    static boolean isPrintableTextCharacter(char character) {
        return character >= 0x20
                && character <= 0x7E
                && forCharacter(character) != null;
    }

    static KeyStroke forCharacter(char character) {
        if (character >= 'a' && character <= 'z') {
            return new KeyStroke(0, 0x04 + character - 'a');
        }
        if (character >= 'A' && character <= 'Z') {
            return new KeyStroke(MOD_SHIFT, 0x04 + character - 'A');
        }
        if (character >= '1' && character <= '9') {
            return new KeyStroke(0, 0x1E + character - '1');
        }
        if (character == '0') {
            return new KeyStroke(0, 0x27);
        }
        switch (character) {
            case ' ': return new KeyStroke(0, 0x2C);
            case '\n': return new KeyStroke(0, KEY_ENTER);
            case '\t': return new KeyStroke(0, KEY_TAB);
            case '-': return new KeyStroke(0, 0x2D);
            case '_': return new KeyStroke(MOD_SHIFT, 0x2D);
            case '=': return new KeyStroke(0, 0x2E);
            case '+': return new KeyStroke(MOD_SHIFT, 0x2E);
            case '[': return new KeyStroke(0, 0x2F);
            case '{': return new KeyStroke(MOD_SHIFT, 0x2F);
            case ']': return new KeyStroke(0, 0x30);
            case '}': return new KeyStroke(MOD_SHIFT, 0x30);
            case '\\': return new KeyStroke(0, 0x31);
            case '|': return new KeyStroke(MOD_SHIFT, 0x31);
            case ';': return new KeyStroke(0, 0x33);
            case ':': return new KeyStroke(MOD_SHIFT, 0x33);
            case '\'': return new KeyStroke(0, 0x34);
            case '"': return new KeyStroke(MOD_SHIFT, 0x34);
            case '`': return new KeyStroke(0, 0x35);
            case '~': return new KeyStroke(MOD_SHIFT, 0x35);
            case ',': return new KeyStroke(0, 0x36);
            case '<': return new KeyStroke(MOD_SHIFT, 0x36);
            case '.': return new KeyStroke(0, 0x37);
            case '>': return new KeyStroke(MOD_SHIFT, 0x37);
            case '/': return new KeyStroke(0, 0x38);
            case '?': return new KeyStroke(MOD_SHIFT, 0x38);
            case '!': return new KeyStroke(MOD_SHIFT, 0x1E);
            case '@': return new KeyStroke(MOD_SHIFT, 0x1F);
            case '#': return new KeyStroke(MOD_SHIFT, 0x20);
            case '$': return new KeyStroke(MOD_SHIFT, 0x21);
            case '%': return new KeyStroke(MOD_SHIFT, 0x22);
            case '^': return new KeyStroke(MOD_SHIFT, 0x23);
            case '&': return new KeyStroke(MOD_SHIFT, 0x24);
            case '*': return new KeyStroke(MOD_SHIFT, 0x25);
            case '(': return new KeyStroke(MOD_SHIFT, 0x26);
            case ')': return new KeyStroke(MOD_SHIFT, 0x27);
            default: return null;
        }
    }

    static final class KeyStroke {
        final int modifier;
        final int usage;

        KeyStroke(int modifier, int usage) {
            this.modifier = modifier;
            this.usage = usage;
        }
    }
}
