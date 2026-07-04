package ca.dnamobile.javalauncher.renderer;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * Helpers for Mesa-based OpenGL renderers (Zink / Turnip).
 *
 * All methods are stubs; full implementation will be wired to the
 * Mesa library bundled in DroidBridge's native assets.
 */
public final class DroidBridgeMesaSupport {

    /** Library name used for EGL when the Mesa renderer is active. */
    public static final String LIB_EGL_MESA = "libEGL_mesa.so";

    private DroidBridgeMesaSupport() {}

    /**
     * Returns true if the given renderer represents a Mesa Zink + Turnip configuration.
     * Accepts both a {@link RendererInterface} and its string identifier.
     */
    public static boolean isMesaZinkTurnipRenderer(@NonNull RendererInterface renderer) {
        String combined = (renderer.getRendererId() + " " + renderer.getRendererName()
                + " " + renderer.getRendererLibrary()).toLowerCase(java.util.Locale.ROOT);
        return combined.contains("zink") || combined.contains("turnip") || combined.contains("mesa");
    }

    /** String-accepting overload kept for call sites that pass an identifier directly. */
    public static boolean isMesaZinkTurnipRenderer(@NonNull String rendererString) {
        String lower = rendererString.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("zink") || lower.contains("turnip") || lower.contains("mesa");
    }

    /**
     * Apply any surface/window workaround required by the Zink renderer.
     */
    public static void applyZinkSurfaceWorkaround(@NonNull RendererInterface renderer) {
        // TODO: apply ANativeWindow / EGL surface workaround for Zink
    }

    /** String overload. */
    public static void applyZinkSurfaceWorkaround(@NonNull String rendererString) {
        // no-op stub
    }

    /**
     * Inject Turnip-specific environment variables required by Mesa.
     */
    public static void applyZinkTurnipEnvironment(
            @NonNull Context context,
            @NonNull RendererInterface renderer,
            @NonNull Map<String, String> env) {
        env.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
        env.put("TU_DEBUG", "noconform");
        env.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");
    }

    /** String overload. */
    public static void applyZinkTurnipEnvironment(
            @NonNull Context context,
            @NonNull String rendererString,
            @NonNull Map<String, String> env) {
        env.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
        env.put("TU_DEBUG", "noconform");
        env.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");
    }
}
