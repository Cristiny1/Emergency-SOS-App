package com.example.emergency_sos_app.network;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileHelper {
    public static File getFile(Context context, Uri uri) {
        if (uri == null) return null;
        try {
            File file = new File(context.getCacheDir(), "upload_file_" + System.currentTimeMillis());
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(file)) {
                if (inputStream == null) return null;
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }
}
