package ca.dnamobile.javalauncher.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * A view that presents a send-key keyboard UI for the in-game touch controls overlay.
 *
 * Users pick a sequence of GLFW key codes and tap "Send" to dispatch them to the game.
 * This is a stub layout: the real implementation uses a custom keyboard drawn on a Canvas.
 */
public class TouchKeySenderKeyboardView extends FrameLayout {

    /** Callbacks for the overlay to respond to user actions. */
    public interface Listener {
        /** User wants to dismiss the keyboard panel. */
        void onCloseRequested();

        /** User confirmed a set of key codes to send to the game. */
        void onSendRequested(@NonNull List<Integer> keyCodes);
    }

    @Nullable private Listener listener;

    public TouchKeySenderKeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TouchKeySenderKeyboardView(@NonNull Context context,
                                      @Nullable AttributeSet attrs,
                                      int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /** Convenience constructor used when creating programmatically with a listener. */
    public TouchKeySenderKeyboardView(@NonNull Context context, @NonNull Listener listener) {
        super(context);
        this.listener = listener;
        init();
    }

    private void init() {
        // TODO: inflate the key-sender layout or build it programmatically
        setBackgroundColor(0xFF1A1A1A);
    }

    /** Attach (or replace) the event listener. */
    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }
}
