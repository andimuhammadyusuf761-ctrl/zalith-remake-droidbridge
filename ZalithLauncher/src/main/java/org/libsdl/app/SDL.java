package org.libsdl.app;

import android.content.Context;

/**
 * Stub for org.libsdl.app.SDL.
 * The real SDL Java bridge is loaded from the SDL native library at runtime.
 * This stub allows the DroidBridge mod-compat code to compile; actual
 * controller initialisation only runs when the SDL .so is present on-device.
 */
public final class SDL {
    private SDL() {}

    public static void loadLibrary(String libraryName, Context context) {
        try {
            System.loadLibrary(libraryName);
        } catch (UnsatisfiedLinkError ignored) {
            // SDL library not bundled — controller compat path will be skipped at runtime
        }
    }

    public static void setupJNI() {
        // no-op stub
    }

    public static void initialize() {
        // no-op stub
    }

    public static void setContext(Context context) {
        // no-op stub
    }
}
