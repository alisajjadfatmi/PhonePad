package com.alisajjadfatmi.phonepad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

final class PhoneKeyboardEditText extends EditText {
    interface Listener {
        void onCommittedText(CharSequence text);

        void onBackspace(int count);

        void onEnter();
    }

    private Listener listener;

    PhoneKeyboardEditText(Context context) {
        super(context);
    }

    PhoneKeyboardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setPhoneKeyboardListener(Listener listener) {
        this.listener = listener;
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
            public boolean commitText(CharSequence text, int newCursorPosition) {
                if (listener != null && text != null && text.length() > 0) {
                    listener.onCommittedText(text);
                }
                return super.commitText(text, newCursorPosition);
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                if (listener != null && beforeLength > 0) {
                    listener.onBackspace(beforeLength);
                }
                return super.deleteSurroundingText(beforeLength, afterLength);
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && listener != null) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        listener.onBackspace(1);
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        listener.onEnter();
                    }
                }
                return super.sendKeyEvent(event);
            }

            @Override
            public boolean performEditorAction(int editorAction) {
                if (listener != null) {
                    listener.onEnter();
                }
                return true;
            }
        };
    }
}
