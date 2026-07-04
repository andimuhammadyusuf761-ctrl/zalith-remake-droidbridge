package ca.dnamobile.javalauncher.skin;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Uploads a custom skin PNG to the Minecraft profile API for a Microsoft account.
 * All methods throw on failure; callers should run on a background thread.
 */
public final class MicrosoftSkinUploader {
    private MicrosoftSkinUploader() {}

    /**
     * Upload a skin to the Minecraft Services API.
     *
     * @param minecraftAccessToken Valid Minecraft access token for the account.
     * @param skinFile             PNG file to upload.
     * @param model                Skin model (classic or slim).
     * @throws Exception on network failure or non-200 HTTP response.
     */
    public static void uploadSkin(@NonNull String minecraftAccessToken,
                                  @NonNull File skinFile,
                                  @NonNull SkinModelType model) throws Exception {
        URL url = new URL("https://api.minecraftservices.com/minecraft/profile/skins");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + minecraftAccessToken);
        String boundary = "JavaLauncherBoundary_" + System.currentTimeMillis();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream out = conn.getOutputStream()) {
            // variant part
            byte[] variantPart = (
                "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"variant\"\r\n\r\n"
                + model.id + "\r\n"
            ).getBytes("UTF-8");
            out.write(variantPart);

            // file part header
            byte[] fileHeader = (
                "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n"
            ).getBytes("UTF-8");
            out.write(fileHeader);

            // file content
            try (FileInputStream fis = new FileInputStream(skinFile)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1) out.write(buf, 0, n);
            }

            // final boundary
            out.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
        }

        int code = conn.getResponseCode();
        conn.disconnect();
        if (code != 200 && code != 204) {
            throw new Exception("Skin upload failed: HTTP " + code);
        }
    }

    /**
     * Callback variant for upload — fire and forget from a background thread.
     */
    public interface Callback {
        void onSuccess();
        void onFailure(@NonNull String reason);
    }

    /** Async helper for callers that can't throw. */
    public static void uploadSkinAsync(@NonNull String minecraftAccessToken,
                                       @NonNull File skinFile,
                                       @NonNull SkinModelType model,
                                       @NonNull Callback callback) {
        new Thread(() -> {
            try {
                uploadSkin(minecraftAccessToken, skinFile, model);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onFailure(e.getMessage() != null ? e.getMessage()
                        : e.getClass().getSimpleName());
            }
        }, "SkinUploaderThread").start();
    }
}
