/*
 * DroidBridge integration stub — LauncherTheme.
 * Bridges DroidBridge's theme system into Zalith Launcher's existing AppTheme.
 */
package ca.dnamobile.javalauncher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.movtery.zalithlauncher.R;

/**
 * Handles DroidBridge-compatible theme application.
 * Delegates to Zalith Launcher's existing AppTheme while supporting
 * DroidBridge's rainbow/accent theme variants.
 */
public final class LauncherTheme {
    private static final String PREFS = "launcher_preferences";
    private static final String KEY_THEME = "launcher_theme";

    private LauncherTheme() {}

    /** Apply the launcher theme to an activity before setContentView. */
    public static void apply(Activity activity) {
        // Apply the DroidBridge dark Teal Ocean theme. This overrides the old
        // Zalith Aurora light theme for all DroidBridge-namespaced activities.
        activity.setTheme(R.style.DroidBridgeSettingsTheme);
    }

    /** Apply a rainbow animated background if the rainbow theme is active. */
    public static void applyRainbowBackgroundIfNeeded(Activity activity) {
        String theme = getTheme(activity);
        if ("rainbow".equals(theme)) {
            // Rainbow background: DroidBridge animates a gradient. For now we
            // apply the aurora background drawable as a compatible fallback.
            activity.getWindow().getDecorView()
                    .setBackgroundResource(R.drawable.bg_aurora_glow);
        }
    }

    public static String getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, "light");
    }

    public static void setTheme(Context context, String theme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, theme)
                .apply();
    }
}
