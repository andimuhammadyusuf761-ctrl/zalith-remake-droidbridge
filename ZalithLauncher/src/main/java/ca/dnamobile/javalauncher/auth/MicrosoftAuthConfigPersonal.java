package ca.dnamobile.javalauncher.auth;

/**
 * Microsoft OAuth configuration for DroidBridge's personal account sign-in flow.
 *
 * To enable Microsoft sign-in, replace CLIENT_ID with a real Azure application
 * (client) ID that has the XboxLive.signin and offline_access scopes granted.
 */
public final class MicrosoftAuthConfigPersonal {
    /**
     * Azure application (client) ID.
     * Replace with your own ID from https://portal.azure.com to enable sign-in.
     */
    public static final String CLIENT_ID = "00000000-0000-0000-0000-000000000000";

    /**
     * OAuth redirect URI registered for this client in Azure.
     * Must match the value configured in the Azure app registration.
     */
    public static final String REDIRECT_URI =
            "https://login.microsoftonline.com/common/oauth2/nativeclient";

    private MicrosoftAuthConfigPersonal() {}

    /**
     * Returns true if a real (non-placeholder) client ID has been configured.
     * The settings screen uses this to show a warning when sign-in is unconfigured.
     */
    public static boolean isConfigured() {
        return CLIENT_ID != null
                && !CLIENT_ID.isEmpty()
                && !CLIENT_ID.equals("00000000-0000-0000-0000-000000000000");
    }
}
