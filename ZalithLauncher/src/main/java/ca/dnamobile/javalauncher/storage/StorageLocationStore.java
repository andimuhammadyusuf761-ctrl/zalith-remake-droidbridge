package ca.dnamobile.javalauncher.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/** Persists the chosen game data storage location for DroidBridge. */
public final class StorageLocationStore {
    private static final String PREFS = "storage_location";
    private static final String KEY_PATH = "custom_path";
    private static final String KEY_LOCATION_ID = "location_id";

    /** Location ID representing the default (app-managed) storage root. */
    public static final String DEFAULT_LOCATION_ID = "default";

    private final SharedPreferences prefs;
    private final Context context;

    public StorageLocationStore(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Returns the user-chosen storage root, or the app's default external files dir. */
    @NonNull
    public File getStorageRoot() {
        String path = prefs.getString(KEY_PATH, null);
        if (path != null) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        File def = context.getExternalFilesDir(null);
        return def != null ? def : context.getFilesDir();
    }

    public void setStorageRoot(@NonNull File root) {
        prefs.edit().putString(KEY_PATH, root.getAbsolutePath()).apply();
    }

    public void clearCustomPath() {
        prefs.edit().remove(KEY_PATH).apply();
    }

    /** Returns the currently selected location ID. */
    @NonNull
    public String getSelectedLocationId() {
        return prefs.getString(KEY_LOCATION_ID, DEFAULT_LOCATION_ID);
    }

    /** Persist the selected location ID. */
    public void setSelectedLocationId(@NonNull String id) {
        prefs.edit().putString(KEY_LOCATION_ID, id).apply();
    }

    /**
     * Returns the user-chosen launcher home directory as a static helper
     * (does not require an instance).
     */
    @NonNull
    public static File getSelectedLauncherHome(@NonNull Context context) {
        return new StorageLocationStore(context).getStorageRoot();
    }

    // ------------------------------------------------------------------
    // Multi-location management (used by StorageLocationDialog/Adapter)
    // ------------------------------------------------------------------

    /**
     * Returns all configured storage locations.  Currently only the default location
     * is returned; custom locations can be added by extending this class.
     */
    @NonNull
    public static java.util.List<ca.dnamobile.javalauncher.storage.StorageLocation>
    getLocations(@NonNull Context context) {
        // Build a single-entry list with the current storage root
        StorageLocationStore store = new StorageLocationStore(context);
        String rootPath = store.getStorageRoot().getAbsolutePath();
        ca.dnamobile.javalauncher.storage.StorageLocation loc =
                new ca.dnamobile.javalauncher.storage.StorageLocation(
                        DEFAULT_LOCATION_ID,
                        "Default Storage",
                        rootPath,
                        null,          // uriString
                        true           // defaultLocation
                );
        java.util.List<ca.dnamobile.javalauncher.storage.StorageLocation> list = new java.util.ArrayList<>();
        list.add(loc);
        return list;
    }

    /** Static variant of {@link #setSelectedLocationId(String)} (Context overload). */
    public static void setSelectedLocationId(
            @NonNull Context context, @NonNull String locationId) {
        new StorageLocationStore(context).setSelectedLocationId(locationId);
    }

    /** Static variant of {@link #getSelectedLocationId()} (Context overload). */
    @NonNull
    public static String getSelectedLocationId(@NonNull Context context) {
        return new StorageLocationStore(context).getSelectedLocationId();
    }
}
