package ca.dnamobile.javalauncher.launcher;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;

import ca.dnamobile.javalauncher.feature.log.Logging;
import net.kdt.pojavlaunch.Architecture;

/**
 * Runtime compatibility checks — determines which JRE version and flags
 * are compatible with the current device and Minecraft version.
 */
public final class RuntimeCompat {
    private static final String TAG = "RuntimeCompat";

    /** Version identifier written to the internal runtime marker file. */
    public static final String PATCH_ID = "db1";

    private RuntimeCompat() {}

    /** Returns the major Java version expected for a bundled runtime name (e.g. "Internal-8" → 8). */
    public static int javaMajorForRuntimeName(@NonNull String runtimeName) {
        if (runtimeName.contains("-21")) return 21;
        if (runtimeName.contains("-17")) return 17;
        if (runtimeName.contains("-11")) return 11;
        return 8; // default / "Internal-8"
    }

    /**
     * Returns true if the runtime for the given name is sufficiently installed
     * for the given Java major version to launch successfully.
     */
    public static boolean isRuntimeInstalledForJava(@NonNull String runtimeName,
                                                     @NonNull File runtimeHome,
                                                     int javaMajor) {
        if (!runtimeHome.exists()) return false;
        // Minimal check: at least the native library dir must exist
        File lib = new File(runtimeHome, "lib");
        return lib.exists();
    }

    /** Human-readable description of the current install state of a runtime. */
    @NonNull
    public static String describeRuntimeState(@NonNull String runtimeName, @NonNull File runtimeHome) {
        if (!runtimeHome.exists()) return "not installed";
        File lib = new File(runtimeHome, "lib");
        if (!lib.exists()) return "lib missing";
        return "ok";
    }

    /** Returns true if the device supports Generational ZGC (requires JRE 21+, 64-bit). */
    public static boolean supportsGenerationalZGC(@NonNull Context context) {
        boolean is64Bit = Architecture.is64BitsDevice();
        Logging.i(TAG, "64-bit device: " + is64Bit);
        return is64Bit;
    }

    /** Returns the recommended minimum heap size in MB for this device. */
    public static int recommendedMinHeapMb(@NonNull Context context) {
        android.app.ActivityManager am =
                (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return 512;
        long totalMb = am.getMemoryClass();
        return (int) Math.max(512, Math.min(totalMb * 4, 2048));
    }
}
