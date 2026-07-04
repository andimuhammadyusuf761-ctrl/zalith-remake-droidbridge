package ca.dnamobile.javalauncher;

/**
 * Compatibility shim so DroidBridge code that imports
 * ca.dnamobile.javalauncher.BuildConfig still compiles when
 * merged into the Zalith launcher module.
 *
 * Real version info is forwarded to the generated
 * com.movtery.zalithlauncher.BuildConfig at runtime.
 */
public final class BuildConfig {
    private BuildConfig() {}

    public static final String VERSION_NAME =
            com.movtery.zalithlauncher.BuildConfig.VERSION_NAME;

    public static final int VERSION_CODE =
            com.movtery.zalithlauncher.BuildConfig.VERSION_CODE;

    public static final String APPLICATION_ID =
            com.movtery.zalithlauncher.BuildConfig.APPLICATION_ID;

    public static final boolean DEBUG =
            com.movtery.zalithlauncher.BuildConfig.DEBUG;
}
