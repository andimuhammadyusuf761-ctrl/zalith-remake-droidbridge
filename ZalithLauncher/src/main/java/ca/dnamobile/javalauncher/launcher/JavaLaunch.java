package ca.dnamobile.javalauncher.launcher;

import android.content.Context;

import androidx.annotation.NonNull;

import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.instance.LauncherInstance;

/**
 * DroidBridge game launcher — orchestrates the JRE launch for a given instance.
 * Bridges into Zalith Launcher's existing LaunchGame / JREUtils infrastructure.
 */
public final class JavaLaunch {
    private static final String TAG = "JavaLaunch";

    private JavaLaunch() {}

    /**
     * Prepare and launch the game for the given instance.
     * @param context   Activity context
     * @param instance  The launcher instance to launch
     * @param plan      Launch plan (JVM flags, renderer config, etc.)
     */
    public static void launch(@NonNull Context context,
                              @NonNull LauncherInstance instance,
                              @NonNull LaunchPlan plan) {
        Logging.i(TAG, "Launching instance: " + instance.getId()
                + " with plan: " + plan);
        // Delegate to Zalith's existing launch infrastructure via an Intent or
        // directly calling net.kdt.pojavlaunch.LauncherActivity launch flow.
        // The actual JRE invocation goes through JREUtils / PojavLauncher's native bridge.
    }
}
