package com.alisajjadfatmi.phonepad;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

final class PhoneKeyboardEditText extends EditText {
    interface Listener {
        void onCommittedText(CharSequence text);

        void onBackspace(int count);

        void onEnter();
    }

    private Listener listener;
    private String forwardedText = "";
    private boolean suppressForwarding;

    PhoneKeyboardEditText(Context context) {
        super(context);
        initializeTextForwarding();
    }

    PhoneKeyboardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeTextForwarding();
    }

    void setPhoneKeyboardListener(Listener listener) {
        this.listener = listener;
    }

    void clearLocalText() {
        suppressForwarding = true;
        setText("");
        forwardedText = "";
        suppressForwarding = false;
    }

    private void initializeTextForwarding() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // Nothing to capture before the edit.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                // The final editable value is handled in afterTextChanged.
            }

            @Override
            public void afterTextChanged(Editable editable) {
                forwardTextDifference(editable == null ? "" : editable.toString());
            }
        });
    }

    private void forwardTextDifference(String currentText) {
        if (suppressForwarding) {
            return;
        }

        if (listener == null) {
            forwardedText = currentText;
            return;
        }

        int commonPrefix = 0;
        int maximumPrefix = Math.min(forwardedText.length(), currentText.length());
        while (commonPrefix < maximumPrefix
                && forwardedText.charAt(commonPrefix) == currentText.charAt(commonPrefix)) {
            commonPrefix++;
        }

        int removedCharacters = forwardedText.length() - commonPrefix;
        if (removedCharacters > 0) {
            listener.onBackspace(removedCharacters);
        }

        if (commonPrefix < currentText.length()) {
            listener.onCommittedText(currentText.substring(commonPrefix));
        }
        forwardedText = currentText;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection base = super.onCreateInputConnection(outAttrs);
        if (base == null) {
            return null;
        }
        outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        return base;
    }
}
