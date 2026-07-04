package ca.dnamobile.javalauncher.controls;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Dialog for picking a keyboard key to bind to a touch-control button. */
public final class TouchKeyPickerDialog {

    // ------------------------------------------------------------------
    // Virtual gamepad cursor key codes (outside GLFW/LWJGL range)
    // ------------------------------------------------------------------
    public static final int GAMEPAD_CURSOR_UP    = 10000;
    public static final int GAMEPAD_CURSOR_DOWN  = 10001;
    public static final int GAMEPAD_CURSOR_LEFT  = 10002;
    public static final int GAMEPAD_CURSOR_RIGHT = 10003;

    // ------------------------------------------------------------------
    // Listener interfaces
    // ------------------------------------------------------------------

    public interface OnKeyPickedListener {
        void onKeyPicked(int keyCode, @NonNull String keyLabel);
    }

    /** Simple callback for callers that only care about the picked key code. */
    public interface KeyPickedCallback {
        /** Return true to dismiss the dialog, false to keep it open. */
        boolean onKeyPicked(int keyCode);
    }

    // ------------------------------------------------------------------
    // Data models
    // ------------------------------------------------------------------

    /**
     * Unified picker entry type. Previously split into {@code ExtraKey} and {@code KeySpec};
     * both call sites now use this single class, returned by {@link #extraKey}.
     */
    public static final class KeySpec {
        @NonNull public final String label;
        public final int keyCode;
        public final float scale;

        KeySpec(@NonNull String label, int keyCode, float scale) {
            this.label = label;
            this.keyCode = keyCode;
            this.scale = scale;
        }
    }

    /**
     * Backward-compatible alias — retained so call sites that still reference
     * {@code ExtraKey} compile without changes.
     */
    public static final class ExtraKey extends Object {
        @NonNull public final String label;
        public final int keyCode;
        public final float scale;

        ExtraKey(@NonNull String label, int keyCode, float scale) {
            this.label = label;
            this.keyCode = keyCode;
            this.scale = scale;
        }
    }

    private TouchKeyPickerDialog() {}

    // ------------------------------------------------------------------
    // Factory helpers
    // ------------------------------------------------------------------

    /**
     * Create a picker entry.  Returns {@link KeySpec} so it can be added to both
     * {@code List<ExtraKey>} and {@code List<KeySpec>} call sites — {@link ExtraKey}
     * is retained as a separate supertype only for backward compatibility.
     */
    @NonNull
    public static KeySpec extraKey(@NonNull String label, int keyCode, float scale) {
        return new KeySpec(label, keyCode, scale);
    }

    /** Alias of {@link #extraKey}. */
    @NonNull
    public static KeySpec keySpec(@NonNull String label, int keyCode, float scale) {
        return new KeySpec(label, keyCode, scale);
    }

    // ------------------------------------------------------------------
    // Full showPicker variants
    // ------------------------------------------------------------------

    /**
     * Show the key-picker dialog with optional extra keys (e.g. gamepad cursor entries).
     * Signature used by the legacy touch-controls key-slot editor.
     */
    public static void showPicker(@NonNull Context context,
                                  @NonNull List<String> keyLabels,
                                  @NonNull int[] keyCodes,
                                  @NonNull List<ExtraKey> extraKeys,
                                  @Nullable OnKeyPickedListener listener) {
        List<String> allLabels = new ArrayList<>(keyLabels);
        List<Integer> allCodes = new ArrayList<>();
        for (int c : keyCodes) allCodes.add(c);
        for (ExtraKey ek : extraKeys) {
            allLabels.add(ek.label);
            allCodes.add(ek.keyCode);
        }
        showListDialog(context, "Pick a key", allLabels, allCodes, keyCode -> {
            if (listener != null) {
                String label = allLabels.get(allCodes.indexOf(keyCode));
                listener.onKeyPicked(keyCode, label);
            }
            return true;
        });
    }

    /**
     * Show the key-picker dialog with a title, message, and {@link KeySpec} extras.
     * Signature used by GamepadMappingDialog.
     */
    public static void showPicker(@NonNull Context context,
                                  @NonNull String title,
                                  @NonNull String message,
                                  @NonNull List<KeySpec> keySpecs,
                                  @Nullable KeyPickedCallback callback) {
        List<String> allLabels = new ArrayList<>();
        List<Integer> allCodes = new ArrayList<>();

        // Build label list from the KEY_OPTIONS table via TouchInputBinding
        TouchInputBinding.Option[] opts = TouchInputBinding.optionsForAction(
                ca.dnamobile.javalauncher.controls.TouchControlActions.KEY);
        for (TouchInputBinding.Option o : opts) {
            allLabels.add(o.label);
            allCodes.add(o.value);
        }

        for (KeySpec ks : keySpecs) {
            allLabels.add(ks.label);
            allCodes.add(ks.keyCode);
        }

        showListDialog(context, title, allLabels, allCodes, callback);
    }

    /**
     * Simple overload used by the touch-controls slot spinner.
     * Shows the full key list; calls {@code callback} with the chosen key code.
     *
     * @param slotIndex ignored — kept for call-site compatibility
     */
    public static void showPicker(@NonNull Context context,
                                  int slotIndex,
                                  @Nullable KeyPickedCallback callback) {
        List<String> allLabels = new ArrayList<>();
        List<Integer> allCodes = new ArrayList<>();

        TouchInputBinding.Option[] opts = TouchInputBinding.optionsForAction(
                ca.dnamobile.javalauncher.controls.TouchControlActions.KEY);
        for (TouchInputBinding.Option o : opts) {
            allLabels.add(o.label);
            allCodes.add(o.value);
        }

        showListDialog(context, "Pick a key", allLabels, allCodes, callback);
    }

    /** Show the key-picker dialog without extra keys. */
    public static void show(@NonNull Context context,
                            @NonNull List<String> keyLabels,
                            @NonNull int[] keyCodes,
                            @Nullable OnKeyPickedListener listener) {
        showPicker(context, keyLabels, keyCodes, new ArrayList<>(), listener);
    }

    // ------------------------------------------------------------------
    // Internal helper
    // ------------------------------------------------------------------

    private static void showListDialog(@NonNull Context context,
                                       @NonNull String title,
                                       @NonNull List<String> labels,
                                       @NonNull List<Integer> codes,
                                       @Nullable KeyPickedCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, android.R.layout.simple_list_item_1, labels);
        ListView list = new ListView(context);
        list.setAdapter(adapter);

        AlertDialog[] dialog = new AlertDialog[1];
        list.setOnItemClickListener((parent, view, position, id) -> {
            boolean dismiss = callback == null || callback.onKeyPicked(codes.get(position));
            if (dismiss && dialog[0] != null) dialog[0].dismiss();
        });

        builder.setView(list);
        builder.setNegativeButton("Cancel", null);
        dialog[0] = builder.show();
    }
}
