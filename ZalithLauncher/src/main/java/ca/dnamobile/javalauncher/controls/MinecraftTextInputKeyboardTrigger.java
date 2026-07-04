package ca.dnamobile.javalauncher.controls;

/**
 * Detects whether a Minecraft screen that accepts text input is currently
 * open (e.g. the chat screen, command input, sign editor).
 *
 * This stub always returns false; the full implementation will poll the
 * JNI bridge for the active Minecraft screen class name.
 */
public final class MinecraftTextInputKeyboardTrigger {

    private MinecraftTextInputKeyboardTrigger() {}

    /**
     * Returns true if a Minecraft screen that shows the chat / text-input
     * field is currently active.
     *
     * When true, the overlay keyboard should be surfaced automatically.
     */
    public static boolean isMinecraftChatScreenOpen() {
        // TODO: query the JNI bridge for the active screen class
        return false;
    }
}
