package me.hypinohaizin.utils;

import java.io.File;

public class FileUtil {
    public static File getAppDir() {
        return new File(System.getProperty("user.dir"));
    }

    public static File getScanDir(String url) {
        String host = url.replaceAll("^https?://", "").replaceAll("[^a-zA-Z0-9.-]", "_");
        File dir = new File(getAppDir(), host + File.separator + "files");
        dir.mkdirs();
        return dir;
    }

    public static File getDownloadDir() {
        File dir = new File(getAppDir(), "downloadedfiles");
        dir.mkdirs();
        return dir;
    }
}
