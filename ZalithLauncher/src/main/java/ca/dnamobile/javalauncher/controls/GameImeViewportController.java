package ca.dnamobile.javalauncher.controls;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Controls the system IME viewport inset while the in-game keyboard is open.
 *
 * On a normal Android app, the IME pushes the window up.  In-game we handle
 * the layout ourselves (the game viewport should not shift), so we track the
 * "active" input view and clear the system inset when it is detached.
 *
 * This stub is sufficient for compilation; the full implementation will hook
 * into WindowInsetsController / ViewCompat inset APIs.
 */
public final class GameImeViewportController {

    @Nullable
    private static View activeInputView;

    private GameImeViewportController() {}

    /**
     * Attach a new input view as the active IME target.
     * The viewport inset produced by the system IME will be suppressed while
     * this view is active.
     *
     * @param inputView The view that will receive keyboard input.
     * @param anchor    An optional anchor view used for inset measurement.
     */
    public static void attach(@NonNull View inputView, @Nullable View anchor) {
        activeInputView = inputView;
        // TODO: request system IME inset suppression via WindowInsetsController
    }

    /**
     * Detach the currently-active input view.
     *
     * @param clearInset If true, also call {@link #clearImeViewportInset(View)} on the anchor.
     */
    public static void detachActive(boolean clearInset) {
        activeInputView = null;
        // TODO: release IME inset suppression
    }

    /**
     * Explicitly clear any IME-driven viewport inset for the given anchor view.
     * Call this after the keyboard is dismissed to ensure the game viewport
     * returns to its normal size.
     *
     * @param anchor The view whose inset should be cleared.
     */
    public static void clearImeViewportInset(@Nullable View anchor) {
        // TODO: apply zero inset via ViewCompat / WindowInsetsController
    }

    /** Returns the currently-active input view, or null if none. */
    @Nullable
    public static View getActiveInputView() {
        return activeInputView;
    }
}
