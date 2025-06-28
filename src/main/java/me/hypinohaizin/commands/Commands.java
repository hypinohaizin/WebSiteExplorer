package me.hypinohaizin.commands;

import me.hypinohaizin.WSE;
import me.hypinohaizin.managers.ScanManager;
import me.hypinohaizin.utils.ConfigUtil;
import me.hypinohaizin.utils.FileUtil;

import java.io.*;
import java.net.URL;

public class Commands {
    private final ConfigUtil.Config config;
    private final ScanManager scanManager;
    private final File appDir;

    public Commands(ConfigUtil.Config config, ScanManager scanManager, File appDir) {
        this.config = config;
        this.scanManager = scanManager;
        this.appDir = appDir;
    }

    public boolean execute(String input) {
        if (input.startsWith("scan ")) {
            String url = input.substring(5).trim();
            scanManager.scanWebsite(url);
        } else if (input.startsWith("dl ") || input.startsWith("download ")) {
            String url = input.startsWith("dl ") ? input.substring(3).trim() : input.substring(9).trim();
            downloadFile(url);
        } else if (input.equals("config")) {
            System.out.println("現在のdelay: " + config.scanDelayMs + "ms");
            System.out.print("新しいdelay(ms)を入力: ");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                config.scanDelayMs = Integer.parseInt(br.readLine());
                ConfigUtil.saveConfig(config, appDir);
            } catch (NumberFormatException e) {
                System.out.println("数値を入力してください。");
            } catch (IOException ignored) {}
        } else if (input.equals("help")) {
            printHelp();
        } else if (input.equals("exit")) {
            return false;
        } else {
            System.out.println("コマンドが不正です。help で一覧を表示します。");
        }
        return true;
    }

    private void downloadFile(String url) {
        File outDir = FileUtil.getDownloadDir();
        try (InputStream in = new URL(url).openStream()) {
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) fileName = "index.html";
            File outFile = new File(outDir, fileName);
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                System.out.println(outFile.getAbsolutePath() + " にダウンロードしました。");
            }
        } catch (Exception e) {
            System.out.println("ダウンロード失敗: " + e.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("--- " + WSE.WSE_NAME + " コマンド一覧 ---");
        System.out.println("help             ... このヘルプを表示");
        System.out.println("scan <url>       ... 指定URLをスキャン（内部/外部/拡張子/変更日時/作成日時表示）");
        System.out.println("dl <url>         ... 指定ファイルをダウンロード");
        System.out.println("download <url>   ... 上と同じ");
        System.out.println("config           ... スキャン時のdelay(ms)を変更");
        System.out.println("exit             ... 終了");
    }
}
