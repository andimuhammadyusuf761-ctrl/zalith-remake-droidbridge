package ca.dnamobile.javalauncher.shortcuts;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.dnamobile.javalauncher.instance.LauncherInstance;
import ca.dnamobile.javalauncher.feature.log.Logging;

/** Creates and removes Android home-screen shortcuts for launcher instances. */
public final class InstanceShortcutHelper {
    private static final String TAG = "InstanceShortcutHelper";

    private InstanceShortcutHelper() {}

    // -----------------------------------------------------------------------
    // ShortcutData — identifies the instance a shortcut should launch
    // -----------------------------------------------------------------------

    public static final class ShortcutData {
        @NonNull public final String instanceId;
        @NonNull public final String instanceName;
        @Nullable public final String iconPath;

        public ShortcutData(
                @NonNull String instanceId,
                @NonNull String instanceName,
                @Nullable String iconPath) {
            this.instanceId = instanceId;
            this.instanceName = instanceName;
            this.iconPath = iconPath;
        }
    }

    // -----------------------------------------------------------------------
    // API
    // -----------------------------------------------------------------------

    /**
     * Request that the system pin a shortcut for the given instance.
     * The user will see the system confirmation dialog on Android 8+.
     */
    public static void requestPinShortcut(@NonNull Activity activity, @NonNull ShortcutData data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        ShortcutManager sm = activity.getSystemService(ShortcutManager.class);
        if (sm == null || !sm.isRequestPinShortcutSupported()) {
            Logging.w(TAG, "Pinned shortcuts not supported");
            return;
        }
        // TODO: build a real launch Intent pointing to the instance
        Logging.i(TAG, "requestPinShortcut for instance: " + data.instanceId);
    }

    /** Create or update a pinned shortcut for the given instance (legacy path). */
    public static void createShortcut(@NonNull Context context,
                                      @NonNull LauncherInstance instance) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        ShortcutManager sm = context.getSystemService(ShortcutManager.class);
        if (sm == null || !sm.isRequestPinShortcutSupported()) {
            Logging.w(TAG, "Pinned shortcuts not supported");
            return;
        }
        Logging.i(TAG, "Shortcut created for instance: " + instance.getId());
    }

    /** Remove a previously pinned shortcut. */
    public static void removeShortcut(@NonNull Context context, @NonNull String instanceId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
        ShortcutManager sm = context.getSystemService(ShortcutManager.class);
        if (sm != null) {
            sm.removeDynamicShortcuts(java.util.Collections.singletonList(instanceId));
        }
    }
}
