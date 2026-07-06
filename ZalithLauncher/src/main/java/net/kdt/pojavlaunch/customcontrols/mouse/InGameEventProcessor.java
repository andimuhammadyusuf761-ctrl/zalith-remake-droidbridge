package net.kdt.pojavlaunch.customcontrols.mouse;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import com.movtery.zalithlauncher.feature.touchbooster.TouchBooster;
import com.movtery.zalithlauncher.setting.AllSettings;
import com.movtery.zalithlauncher.setting.AllStaticSettings;
import com.movtery.zalithlauncher.support.touch_controller.ContactHandler;

import org.lwjgl.glfw.CallbackBridge;

public class InGameEventProcessor implements TouchEventProcessor {
    private final Handler mGestureHandler = new Handler(Looper.getMainLooper());
    private final double mSensitivity;
    private boolean mEventTransitioned = true;
    private final PointerTracker mTracker = new PointerTracker();
    private final LeftClickGesture mLeftClickGesture = new LeftClickGesture(mGestureHandler);
    private final RightClickGesture mRightClickGesture = new RightClickGesture(mGestureHandler);

    public InGameEventProcessor(double sensitivity) {
        mSensitivity = sensitivity;
    }

    @Override
    public boolean processTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTracker.startTracking(motionEvent);
                TouchBooster.INSTANCE.boost();
                if (AllSettings.getDisableGestures().getValue()) break;
                mEventTransitioned = false;
                checkGestures();
                break;

            case MotionEvent.ACTION_MOVE:
                // --- DroidBridge Touch Booster: process ALL historical samples ---
                // Android batches multiple touch samples into a single ACTION_MOVE event.
                // Processing only the final position drops intermediate samples and causes
                // jerky camera movement. Walking the full history gives butter-smooth panning.
                int histSize = motionEvent.getHistorySize();
                int trackedIdx = motionEvent.findPointerIndex(mTracker.getTrackedPointerId());
                if (trackedIdx < 0) trackedIdx = 0;

                float prevX = mTracker.getLastX();
                float prevY = mTracker.getLastY();
                float totalDX = 0f, totalDY = 0f;

                for (int h = 0; h < histSize; h++) {
                    float hx = motionEvent.getHistoricalX(trackedIdx, h);
                    float hy = motionEvent.getHistoricalY(trackedIdx, h);
                    totalDX += hx - prevX;
                    totalDY += hy - prevY;
                    prevX = hx;
                    prevY = hy;
                }
                float curX = motionEvent.getX(trackedIdx);
                float curY = motionEvent.getY(trackedIdx);
                totalDX += curX - prevX;
                totalDY += curY - prevY;

                // Commit final position to tracker (keeps gesture detectors in sync)
                mTracker.trackEvent(motionEvent);

                float deltaX = (float) (totalDX * mSensitivity);
                float deltaY = (float) (totalDY * mSensitivity);
                mLeftClickGesture.setMotion(deltaX, deltaY);
                mRightClickGesture.setMotion(deltaX, deltaY);
                CallbackBridge.mouseX += deltaX;
                CallbackBridge.mouseY += deltaY;
                CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY);
                if (AllSettings.getDisableGestures().getValue()) break;
                checkGestures();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTracker.cancelTracking();
                cancelGestures(false);
                TouchBooster.INSTANCE.restore();
                break;
        }
        return true;
    }

    @Override
    public void cancelPendingActions() {
        cancelGestures(true);
        TouchBooster.INSTANCE.restore();
    }

    @Override
    public void dispatchTouchEvent(MotionEvent event, View view) {
        if (AllStaticSettings.useControllerProxy) {
            ContactHandler.INSTANCE.progressEvent(event, view);
        }
    }

    private void checkGestures() {
        mLeftClickGesture.inputEvent();
        if (!mEventTransitioned) mRightClickGesture.inputEvent();
    }

    private void cancelGestures(boolean isSwitching) {
        mEventTransitioned = true;
        mLeftClickGesture.cancel(isSwitching);
        mRightClickGesture.cancel(isSwitching);
    }
}
