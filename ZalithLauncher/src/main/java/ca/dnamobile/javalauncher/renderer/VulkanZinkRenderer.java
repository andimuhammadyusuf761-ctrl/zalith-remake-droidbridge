/*
 * Copyright (c) 2026 DNA Mobile Applications.
 * All rights reserved.
 *
 * This file is DroidBridge project code.
 * It is not part of Minecraft and does not grant rights to Minecraft,
 * Mojang, Microsoft, PojavLauncher, Zalith Launcher, or any third-party project.
 *
 * Files written entirely by DNA Mobile Applications are proprietary unless
 * a file header or separate license notice states otherwise.
 */

package ca.dnamobile.javalauncher.renderer;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VulkanZinkRenderer implements RendererInterface {
    @NonNull
    @Override
    public String getRendererId() {
        return "vulkan_zink";
    }

    @NonNull
    @Override
    public String getUniqueIdentifier() {
        return "0fa435e2-46df-45c9-906c-b29606aaef00";
    }

    @NonNull
    @Override
    public String getRendererName() {
        return "Vulkan Zink";
    }

    @NonNull
    @Override
    public String getRendererDescription() {
        return "Vulkan-backed OpenGL renderer. Useful for newer Minecraft versions when the device supports Vulkan.";
    }

    @NonNull
    @Override
    public Map<String, String> getRendererEnv() {
        LinkedHashMap<String, String> env = new LinkedHashMap<>();

        // ── GL version advertisement ──────────────────────────────────────
        env.put("MESA_GL_VERSION_OVERRIDE", "4.6");
        env.put("MESA_GLSL_VERSION_OVERRIDE", "460");

        // ── Zink / Gallium backend ────────────────────────────────────────
        env.put("GALLIUM_DRIVER", "zink");
        env.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");

        // ── Vulkan descriptor strategy (lazy = fewer updates per frame) ───
        env.put("ZINK_DESCRIPTORS", "lazy");

        // ── Error checking bypass (safe for stable releases) ──────────────
        env.put("MESA_NO_ERROR", "1");

        // ── Shader / GLSL cache — eliminates first-frame stutters ─────────
        env.put("MESA_SHADER_CACHE_DISABLE", "false");
        env.put("MESA_GLSL_CACHE_DISABLE", "false");

        // ── Mesa GL threading: async command stream, reduces CPU stall ────
        env.put("mesa_glthread", "true");
        env.put("MESA_GLTHREAD_INIT_BUFFER_SIZE", "512");

        // ── Disable VSync at the driver level for maximum FPS ─────────────
        env.put("vblank_mode", "0");

        // ── Library path ──────────────────────────────────────────────────
        env.put("LIB_MESA_NAME", getRendererLibrary());

        // ── GL4ES compatibility flags ─────────────────────────────────────
        env.put("LIBGL_ES", "3");
        env.put("LIBGL_NOERROR", "1");
        env.put("LIBGL_NORMALIZE", "1");
        env.put("LIBGL_MIPMAP", "3");
        env.put("LIBGL_NOINTOVLHACK", "1");

        // ── VBO / VAO / geometry pipeline ─────────────────────────────────
        // LIBGL_USEVBO=1 enables Vertex Buffer Objects — batch geometry on GPU
        env.put("LIBGL_USEVBO", "1");
        // LIBGL_VAOEXT=1 enables Vertex Array Object extension
        env.put("LIBGL_VAOEXT", "1");
        // LIBGL_NOBATCH=0: enable draw-call batching (0 = batch on)
        env.put("LIBGL_NOBATCH", "0");
        // LIBGL_FASTMATH=1: allow fast (approximate) math in the GL driver
        env.put("LIBGL_FASTMATH", "1");

        // ── GLSL extension compatibility ──────────────────────────────────
        env.put("allow_higher_compat_version", "true");
        env.put("allow_glsl_extension_directive_midshader", "true");
        env.put("force_glsl_extensions_warn", "true");

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
        return "libOSMesa_8.so";
    }

    @Override
    public String getRendererEGL() {
        return getRendererLibrary();
    }
}
