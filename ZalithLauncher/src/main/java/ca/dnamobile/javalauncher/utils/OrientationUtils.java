package ca.dnamobile.javalauncher.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;

import androidx.annotation.NonNull;

import ca.dnamobile.javalauncher.settings.LauncherPreferences;

public final class OrientationUtils {

    public static void apply(@NonNull Activity activity) {
        apply(activity, LauncherPreferences.getAppOrientationMode(activity));
    }

    public static void apply(@NonNull Activity activity, @NonNull String mode) {
        switch (mode) {
            case LauncherPreferences.APP_ORIENTATION_LANDSCAPE:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
            case LauncherPreferences.APP_ORIENTATION_PORTRAIT:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                break;
            default:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
        }
    }

    private OrientationUtils() {}
}
