package ca.dnamobile.javalauncher.input;

import android.app.Activity;

import androidx.annotation.NonNull;

/**
 * Stub for exporting / importing gamepad mapping profiles.
 * Full implementation TODO when file-transfer UI is built.
 */
public final class GamepadMappingTransferActivity {

    private GamepadMappingTransferActivity() {}

    /**
     * Start the export flow for the given profile key.
     * The user will be shown a file-saver UI (TODO).
     */
    public static void startExport(@NonNull Activity activity, @NonNull String profileKey) {
        // TODO: launch SAF document-creation intent and serialize the profile
        android.widget.Toast.makeText(activity,
                "Export not yet implemented", android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * Start the import flow for the given profile key.
     * The user will be shown a file-picker UI (TODO).
     */
    public static void startImport(@NonNull Activity activity, @NonNull String profileKey) {
        // TODO: launch SAF document-open intent and deserialize the profile
        android.widget.Toast.makeText(activity,
                "Import not yet implemented", android.widget.Toast.LENGTH_SHORT).show();
    }
}
