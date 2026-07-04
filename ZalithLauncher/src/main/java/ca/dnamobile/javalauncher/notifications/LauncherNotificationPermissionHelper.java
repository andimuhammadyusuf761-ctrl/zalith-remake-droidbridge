package ca.dnamobile.javalauncher.notifications;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/** Handles requesting the POST_NOTIFICATIONS permission on Android 13+. */
public final class LauncherNotificationPermissionHelper {
    public static final int REQUEST_CODE = 1001;

    private static final String PREFS = "launcher_notification_prefs";
    private static final String KEY_BG_INSTALL_NOTIFICATIONS = "bg_install_notifications_enabled";

    private LauncherNotificationPermissionHelper() {}

    /** Returns true if POST_NOTIFICATIONS permission is granted (or not needed on <API 33). */
    public static boolean hasPermission(@NonNull Activity activity) {
        return hasPostNotificationsPermission(activity);
    }

    /** Returns true if POST_NOTIFICATIONS permission is granted (or not needed on <API 33). */
    public static boolean hasPostNotificationsPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the POST_NOTIFICATIONS permission using an {@link ActivityResultLauncher}.
     * The launcher must be registered with {@code Manifest.permission.POST_NOTIFICATIONS}.
     */
    public static void requestPostNotificationsPermission(
            @NonNull ActivityResultLauncher<String> launcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** Request the POST_NOTIFICATIONS permission if not already granted (legacy path). */
    public static void requestIfNeeded(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (!hasPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE);
        }
    }

    /** Enable or disable background-install notifications in app preferences. */
    public static void setBackgroundInstallNotificationsEnabled(
            @NonNull Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BG_INSTALL_NOTIFICATIONS, enabled)
                .apply();
    }

    /** Returns true if background-install notifications have been enabled by the user. */
    public static boolean isBackgroundInstallNotificationsEnabled(@NonNull Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_BG_INSTALL_NOTIFICATIONS, false);
    }

    /**
     * Returns true if a runtime POST_NOTIFICATIONS permission request is required
     * (Android 13 / API 33 and above).
     */
    public static boolean requiresRuntimePermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    /** Open the system notification settings screen for this app. */
    public static Intent openAppNotificationSettings(@NonNull Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        }
        return intent;
    }
}
