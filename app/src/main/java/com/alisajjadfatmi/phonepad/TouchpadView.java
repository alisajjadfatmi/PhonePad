package com.alisajjadfatmi.phonepad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

final class TouchpadView extends View {
    interface Listener {
        void onPointerMove(int dx, int dy);

        void onScroll(int vertical, int horizontal);

        void onButtonDown(int button);

        void onButtonUp(int button);

        void onButtonClick(int button);
    }

    private static final long DOUBLE_TAP_MS = 320;
    private static final long LONG_PRESS_MS = 520;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final float touchSlop;

    private Listener listener;
    private float downX;
    private float downY;
    private float lastX;
    private float lastY;
    private float lastCenterX;
    private float lastCenterY;
    private float scrollAccumulatorX;
    private float scrollAccumulatorY;
    private long downTime;
    private long lastTapTime;
    private float lastTapX;
    private float lastTapY;
    private int maximumPointers;
    private boolean moved;
    private boolean dragHeld;
    private float sensitivity = 1.35f;
    private boolean naturalScroll;

    TouchpadView(Context context) {
        this(context, null);
    }

    TouchpadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        backgroundPaint.setColor(Color.rgb(19, 27, 48));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1.5f));
        borderPaint.setColor(Color.rgb(83, 110, 162));
        textPaint.setColor(Color.rgb(182, 192, 216));
        textPaint.setTextSize(dp(15));
        textPaint.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
        setFocusable(true);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    void setNaturalScroll(boolean naturalScroll) {
        this.naturalScroll = naturalScroll;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        bounds.set(dp(1), dp(1), getWidth() - dp(1), getHeight() - dp(1));
        float radius = dp(22);
        canvas.drawRoundRect(bounds, radius, radius, backgroundPaint);
        canvas.drawRoundRect(bounds, radius, radius, borderPaint);
        float center = getWidth() / 2f;
        canvas.drawText("Move · tap to click · two fingers to scroll", center, getHeight() / 2f - dp(4), textPaint);
        canvas.drawText("Two-finger tap: right click · double-tap + drag", center, getHeight() / 2f + dp(24), textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (listener == null) {
            return false;
        }
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                beginGesture(event);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                maximumPointers = Math.max(maximumPointers, event.getPointerCount());
                updateCenter(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                handleMove(event);
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                maximumPointers = Math.max(maximumPointers, event.getPointerCount());
                return true;
            case MotionEvent.ACTION_UP:
                finishGesture(event);
                getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (dragHeld) {
                    listener.onButtonUp(HidDeviceController.MOUSE_LEFT);
                }
                dragHeld = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return true;
        }
    }

    private void beginGesture(MotionEvent event) {
        downX = lastX = event.getX();
        downY = lastY = event.getY();
        downTime = SystemClock.uptimeMillis();
        maximumPointers = 1;
        moved = false;
        scrollAccumulatorX = 0;
        scrollAccumulatorY = 0;

        long sinceLastTap = downTime - lastTapTime;
        float tapDistance = distance(downX, downY, lastTapX, lastTapY);
        dragHeld = sinceLastTap <= DOUBLE_TAP_MS && tapDistance <= dp(48);
        if (dragHeld) {
            listener.onButtonDown(HidDeviceController.MOUSE_LEFT);
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void handleMove(MotionEvent event) {
        maximumPointers = Math.max(maximumPointers, event.getPointerCount());
        if (event.getPointerCount() >= 2) {
            float centerX = averageX(event);
            float centerY = averageY(event);
            if (lastCenterX != 0 || lastCenterY != 0) {
                scrollAccumulatorX += centerX - lastCenterX;
                scrollAccumulatorY += centerY - lastCenterY;
                int threshold = Math.max(1, Math.round(dp(14)));
                int horizontal = (int) (scrollAccumulatorX / threshold);
                int vertical = (int) (scrollAccumulatorY / threshold);
                if (horizontal != 0 || vertical != 0) {
                    int direction = naturalScroll ? 1 : -1;
                    listener.onScroll(vertical * direction, horizontal * direction);
                    scrollAccumulatorX -= horizontal * threshold;
                    scrollAccumulatorY -= vertical * threshold;
                    moved = true;
                }
            }
            lastCenterX = centerX;
            lastCenterY = centerY;
            return;
        }

        lastCenterX = 0;
        lastCenterY = 0;
        float rawDx = event.getX() - lastX;
        float rawDy = event.getY() - lastY;
        if (distance(event.getX(), event.getY(), downX, downY) > touchSlop) {
            moved = true;
        }
        float speed = (float) Math.hypot(rawDx, rawDy);
        float acceleration = 1f + Math.min(0.8f, speed / dp(28));
        int dx = Math.round(rawDx * sensitivity * acceleration);
        int dy = Math.round(rawDy * sensitivity * acceleration);
        if (dx != 0 || dy != 0) {
            listener.onPointerMove(dx, dy);
        }
        lastX = event.getX();
        lastY = event.getY();
    }

    private void finishGesture(MotionEvent event) {
        long duration = SystemClock.uptimeMillis() - downTime;
        if (dragHeld) {
            listener.onButtonUp(HidDeviceController.MOUSE_LEFT);
            dragHeld = false;
            lastTapTime = 0;
            return;
        }
        if (!moved && maximumPointers >= 3) {
            listener.onButtonClick(HidDeviceController.MOUSE_MIDDLE);
            hapticClick();
            lastTapTime = 0;
        } else if (!moved && maximumPointers == 2) {
            listener.onButtonClick(HidDeviceController.MOUSE_RIGHT);
            hapticClick();
            lastTapTime = 0;
        } else if (!moved && duration >= LONG_PRESS_MS) {
            listener.onButtonClick(HidDeviceController.MOUSE_RIGHT);
            hapticClick();
            lastTapTime = 0;
        } else if (!moved) {
            listener.onButtonClick(HidDeviceController.MOUSE_LEFT);
            hapticClick();
            lastTapTime = SystemClock.uptimeMillis();
            lastTapX = event.getX();
            lastTapY = event.getY();
        }
    }

    private void updateCenter(MotionEvent event) {
        lastCenterX = averageX(event);
        lastCenterY = averageY(event);
    }

    private static float averageX(MotionEvent event) {
        float total = 0;
        for (int index = 0; index < event.getPointerCount(); index++) {
            total += event.getX(index);
        }
        return total / event.getPointerCount();
    }

    private static float averageY(MotionEvent event) {
        float total = 0;
        for (int index = 0; index < event.getPointerCount(); index++) {
            total += event.getY(index);
        }
        return total / event.getPointerCount();
    }

    private void hapticClick() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x1 - x2, y1 - y2);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
