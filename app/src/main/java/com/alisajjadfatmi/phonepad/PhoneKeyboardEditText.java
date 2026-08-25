package com.alisajjadfatmi.phonepad;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * A transient bridge between an Android soft keyboard and Bluetooth HID.
 *
 * <p>This view deliberately does not mirror the document on the computer. It
 * tracks only the IME's current composing text, emits the corresponding key
 * presses, and clears its local buffer after a commit. Windows remains the
 * owner of the document, selection and caret just as it does for a physical
 * Bluetooth keyboard.</p>
 */
final class PhoneKeyboardEditText extends EditText {
    interface Listener {
        void onTypedText(CharSequence text);

        void onBackspace(int count);

        void onDelete(int count);

        void onEnter();

        void onTab();
    }

    private Listener listener;
    private String sentComposition = "";
    private long editGeneration;

    PhoneKeyboardEditText(Context context) {
        super(context);
    }

    PhoneKeyboardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setPhoneKeyboardListener(Listener listener) {
        this.listener = listener;
    }

    void resetKeyboardCapture() {
        editGeneration++;
        sentComposition = "";
        Editable editable = getText();
        if (editable != null) {
            editable.clear();
        }
        InputMethodManager manager = getContext().getSystemService(InputMethodManager.class);
        if (manager != null) {
            manager.restartInput(this);
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection base = super.onCreateInputConnection(outAttrs);
        if (base == null) {
            return null;
        }
        outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        return new InputConnectionWrapper(base, true) {
            @Override
            public boolean setComposingText(CharSequence text, int newCursorPosition) {
                editGeneration++;
                updateSentComposition(printableTextOnly(text));
                return super.setComposingText(text, newCursorPosition);
            }

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                editGeneration++;
                forwardCommittedText(text);
                boolean handled = super.commitText(text, newCursorPosition);
                scheduleVisualBufferClear();
                return handled;
            }

            @Override
            public boolean finishComposingText() {
                editGeneration++;
                sentComposition = "";
                boolean handled = super.finishComposingText();
                scheduleVisualBufferClear();
                return handled;
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                if (listener != null) {
                    if (beforeLength > 0) {
                        listener.onBackspace(beforeLength);
                        removeFromSentComposition(beforeLength);
                    }
                    if (afterLength > 0) {
                        listener.onDelete(afterLength);
                    }
                }
                return super.deleteSurroundingText(beforeLength, afterLength);
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                return event.getAction() == KeyEvent.ACTION_DOWN && forwardKeyDown(event)
                        || super.sendKeyEvent(event);
            }

            @Override
            public boolean performEditorAction(int editorAction) {
                if (listener == null) {
                    return super.performEditorAction(editorAction);
                }
                sentComposition = "";
                listener.onEnter();
                scheduleVisualBufferClear();
                return true;
            }
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return forwardKeyDown(event) || super.onKeyDown(keyCode, event);
    }

    private boolean forwardKeyDown(KeyEvent event) {
        if (listener == null) {
            return false;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DEL:
                listener.onBackspace(1);
                removeFromSentComposition(1);
                return true;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                listener.onDelete(1);
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                sentComposition = "";
                listener.onEnter();
                scheduleVisualBufferClear();
                return true;
            case KeyEvent.KEYCODE_TAB:
                sentComposition = "";
                listener.onTab();
                scheduleVisualBufferClear();
                return true;
            default:
                int unicodeCharacter = event.getUnicodeChar();
                if (unicodeCharacter <= Character.MAX_VALUE
                        && HidKeyMap.isPrintableTextCharacter((char) unicodeCharacter)) {
                    listener.onTypedText(String.valueOf((char) unicodeCharacter));
                    return true;
                }
                return false;
        }
    }

    private void forwardCommittedText(CharSequence text) {
        String value = text == null ? "" : text.toString();
        StringBuilder printableSegment = new StringBuilder();
        boolean sawControlKey = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\r' || character == '\n' || character == '\t') {
                if (printableSegment.length() > 0) {
                    updateSentComposition(printableSegment.toString());
                }
                printableSegment.setLength(0);
                sentComposition = "";
                sawControlKey = true;
                if (listener != null) {
                    if (character == '\t') {
                        listener.onTab();
                    } else if (character == '\r'
                            && index + 1 < value.length()
                            && value.charAt(index + 1) == '\n') {
                        listener.onEnter();
                        index++;
                    } else {
                        listener.onEnter();
                    }
                }
            } else if (HidKeyMap.isPrintableTextCharacter(character)) {
                printableSegment.append(character);
            }
        }
        if (printableSegment.length() > 0 || !sawControlKey) {
            updateSentComposition(printableSegment.toString());
        }
        sentComposition = "";
    }

    private void updateSentComposition(String newComposition) {
        if (listener == null) {
            sentComposition = newComposition;
            return;
        }

        int commonPrefix = 0;
        int maximumPrefix = Math.min(sentComposition.length(), newComposition.length());
        while (commonPrefix < maximumPrefix
                && sentComposition.charAt(commonPrefix) == newComposition.charAt(commonPrefix)) {
            commonPrefix++;
        }

        int replacedCharacters = sentComposition.length() - commonPrefix;
        if (replacedCharacters > 0) {
            listener.onBackspace(replacedCharacters);
        }
        if (commonPrefix < newComposition.length()) {
            listener.onTypedText(newComposition.substring(commonPrefix));
        }
        sentComposition = newComposition;
    }

    private void removeFromSentComposition(int count) {
        int remainingLength = Math.max(0, sentComposition.length() - count);
        sentComposition = sentComposition.substring(0, remainingLength);
    }

    private static String printableTextOnly(CharSequence text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (HidKeyMap.isPrintableTextCharacter(character)) {
                result.append(character);
            }
        }
        return result.toString();
    }

    private void scheduleVisualBufferClear() {
        long expectedGeneration = editGeneration;
        post(() -> {
            if (expectedGeneration != editGeneration || !sentComposition.isEmpty()) {
                return;
            }
            Editable editable = getText();
            if (editable != null) {
                editable.clear();
            }
        });
    }
}
