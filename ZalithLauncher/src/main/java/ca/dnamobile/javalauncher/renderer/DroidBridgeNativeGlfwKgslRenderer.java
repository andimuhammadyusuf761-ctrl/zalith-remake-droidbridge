package ca.dnamobile.javalauncher.renderer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native GLFW renderer backed by the Qualcomm KGSL (Adreno) GPU driver.
 *
 * This is a stub that satisfies the Renderers list; the actual renderer
 * implementation is loaded from native code at runtime.
 */
public final class DroidBridgeNativeGlfwKgslRenderer implements RendererInterface {

    @NonNull
    @Override
    public String getRendererId() {
        return "gl4es_114_kgsl";
    }

    @NonNull
    @Override
    public String getUniqueIdentifier() {
        return "droidbridge_kgsl";
    }

    @NonNull
    @Override
    public String getRendererName() {
        return "Native GLFW (KGSL/Adreno)";
    }

    @NonNull
    @Override
    public String getRendererDescription() {
        return "Hardware OpenGL via Qualcomm KGSL driver. Best performance on Adreno GPUs.";
    }

    @NonNull
    @Override
    public Map<String, String> getRendererEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("POJAV_RENDERER", "gl4es_114");
        env.put("POJAV_VSYNC_IN_ZINK", "0");
        return env;
    }

    @NonNull
    @Override
    public List<String> getDlopenLibrary() {
        return Collections.emptyList();
    }

    @NonNull
    @Override
    public String getRendererLibrary() {
        return "libgl4es_114.so";
    }

    @Nullable
    @Override
    public String getRendererEGL() {
        return null;
    }
}
