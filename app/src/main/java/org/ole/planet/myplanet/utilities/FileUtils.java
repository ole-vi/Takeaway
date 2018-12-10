package org.ole.planet.myplanet.utilities;

import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;

public class FileUtils {
    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            return availableBlocks * blockSize;
        } else {
            return 0;
        }
    }

    public static String getFileExtension(String address) {
        if (TextUtils.isEmpty(address))
            return "";
        String filenameArray[] = address.split("\\.");
        return filenameArray[filenameArray.length - 1];
    }

}
