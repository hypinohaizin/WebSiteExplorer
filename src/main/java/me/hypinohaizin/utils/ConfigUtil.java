package me.hypinohaizin.utils;

import com.google.gson.Gson;
import java.io.*;

public class ConfigUtil {
    public static class Config {
        public int scanDelayMs = 500;
    }

    public static Config loadConfig(File dir) {
        File file = new File(dir, "config.json");
        Gson gson = new Gson();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                return gson.fromJson(reader, Config.class);
            } catch (Exception ignored) {}
        }
        Config config = new Config();
        saveConfig(config, dir);
        return config;
    }

    public static void saveConfig(Config config, File dir) {
        File file = new File(dir, "config.json");
        try (Writer writer = new FileWriter(file)) {
            new Gson().toJson(config, writer);
        } catch (Exception ignored) {}
    }
}
