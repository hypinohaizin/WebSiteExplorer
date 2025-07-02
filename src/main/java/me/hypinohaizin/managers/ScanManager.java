package me.hypinohaizin.managers;

import me.hypinohaizin.utils.FileUtil;
import me.hypinohaizin.utils.ConfigUtil;
import me.hypinohaizin.WSE;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScanManager {
    private final ConfigUtil.Config config;
    private final Map<String, List<String>> extFileList = new HashMap<>(); // 拡張子別URLリスト

    public ScanManager(ConfigUtil.Config config) {
        this.config = config;
    }

    public List<String> getFilesByExtension(String ext) {
        return extFileList.getOrDefault(ext, Collections.emptyList());
    }

    public void scanWebsite(String url) {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> extCount = new HashMap<>();
        extFileList.clear(); // 新規スキャンごとにクリア

        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            File saveDir = FileUtil.getScanDir(url);
            Document doc;
            String baseHost;
            try {
                doc = Jsoup.connect(url).get();
                baseHost = new URL(url).getHost();
            } catch (IOException e) {
                System.out.println("【エラーコード:E01】接続失敗: " + e.getMessage());
                return;
            } catch (Exception e) {
                System.out.println("【エラーコード:E99】不明な接続例外: " + e.getMessage());
                return;
            }

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            sb.append("WSE.WSE_NAME: ").append(WSE.WSE_NAME).append("\n");
            sb.append("WSE.WSE_VERSION: ").append(WSE.WSE_VERSION).append("\n");
            sb.append("Scan-Date: ").append(now).append("\n\n");

            sb.append("タイトル: ").append(doc.title()).append("\n");

            sb.append("リンク一覧:\n");
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String linkUrl = link.absUrl("href");
                if (linkUrl.isEmpty() ||
                        linkUrl.startsWith("javascript:") ||
                        linkUrl.startsWith("tel:")) continue;

                boolean isExternal = isExternal(linkUrl, baseHost);
                String mark = isExternal ? "[外部]" : "[内部]";
                String[] dates = getUrlDates(linkUrl);
                String cliOut = String.format(
                        "[SCAN][a]%s %s -> %s [最終変更: %s] [作成: %s]",
                        mark, link.text(), linkUrl, dates[0], dates[1]
                );
                System.out.println(cliOut);
                sb.append(String.format(
                        "  [a]%s %s -> %s [最終変更: %s] [作成: %s]\n",
                        mark, link.text(), linkUrl, dates[0], dates[1]
                ));
                addExtCount(linkUrl, extCount);
                Thread.sleep(config.scanDelayMs);
            }

            sb.append("画像一覧:\n");
            Elements images = doc.select("img[src]");
            for (Element img : images) {
                String imgUrl = img.absUrl("src");
                if (imgUrl.isEmpty()) continue;
                boolean isExternal = isExternal(imgUrl, baseHost);
                String mark = isExternal ? "[外部]" : "[内部]";
                String[] dates = getUrlDates(imgUrl);
                String cliOut = String.format(
                        "[SCAN][img]%s %s [最終変更: %s] [作成: %s]",
                        mark, imgUrl, dates[0], dates[1]
                );
                System.out.println(cliOut);
                sb.append(String.format(
                        "  [img]%s %s [最終変更: %s] [作成: %s]\n",
                        mark, imgUrl, dates[0], dates[1]
                ));
                addExtCount(imgUrl, extCount);
                Thread.sleep(config.scanDelayMs);
            }

            sb.append("JavaScript一覧:\n");
            Elements scripts = doc.select("script[src]");
            for (Element script : scripts) {
                String scriptUrl = script.absUrl("src");
                if (scriptUrl.isEmpty()) continue;
                boolean isExternal = isExternal(scriptUrl, baseHost);
                String mark = isExternal ? "[外部]" : "[内部]";
                String[] dates = getUrlDates(scriptUrl);
                String cliOut = String.format(
                        "[SCAN][js]%s %s [最終変更: %s] [作成: %s]",
                        mark, scriptUrl, dates[0], dates[1]
                );
                System.out.println(cliOut);
                sb.append(String.format(
                        "  [js]%s %s [最終変更: %s] [作成: %s]\n",
                        mark, scriptUrl, dates[0], dates[1]
                ));
                addExtCount(scriptUrl, extCount);
                Thread.sleep(config.scanDelayMs);
            }

            sb.append("\nファイル拡張子集計:\n");
            if (extCount.isEmpty()) {
                sb.append("  取得なし\n");
            } else {
                extCount.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(e -> sb.append("  .").append(e.getKey())
                                .append(": ").append(e.getValue()).append("件\n"));
            }

            File outFile = new File(saveDir, "structure.txt");
            try (Writer w = new FileWriter(outFile)) {
                w.write(sb.toString());
            } catch (IOException e) {
                System.out.println("【エラーコード:E02】ファイル保存失敗: " + e.getMessage());
                return;
            }
            System.out.println("スキャン結果を " + outFile.getAbsolutePath() + " に保存しました。\n");
            System.out.println(sb.toString());
        } catch (InterruptedException e) {
            System.out.println("【エラーコード:E03】スリープ中断: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("【エラーコード:E99】予期せぬエラー: " + e.getMessage());
        }
    }

    private void addExtCount(String url, Map<String, Integer> extCount) {
        String ext = getExtension(url);
        if (!ext.isEmpty()) {
            extCount.put(ext, extCount.getOrDefault(ext, 0) + 1);
            extFileList.computeIfAbsent(ext, k -> new ArrayList<>()).add(url); // URLリスト追加
        }
    }

    private String getExtension(String url) {
        try {
            String path = new URL(url).getPath();
            int lastDot = path.lastIndexOf('.');
            if (lastDot != -1 && lastDot < path.length() - 1) {
                String ext = path.substring(lastDot + 1).toLowerCase(Locale.ROOT);
                if (ext.matches("[a-zA-Z0-9]+")) return ext;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private boolean isExternal(String targetUrl, String baseHost) {
        String targetRoot = extractRootDomain(targetUrl);
        String baseRoot = extractRootDomain(baseHost);
        if (targetRoot.isEmpty() || baseRoot.isEmpty()) return true;
        return !targetRoot.equalsIgnoreCase(baseRoot);
    }

    private String extractRootDomain(String urlOrHost) {
        try {
            String host = urlOrHost;
            if (host.startsWith("http")) host = new URL(urlOrHost).getHost();
            String[] parts = host.split("\\.");
            if (parts.length < 2) return host;
            String suffix2 = parts[parts.length - 2] + "." + parts[parts.length - 1];
            if (parts.length >= 3) {
                String suffix3 = parts[parts.length - 3] + "." + suffix2;
                if (suffix2.equals("co.jp") || suffix2.equals("ac.jp") || suffix2.equals("or.jp")) {
                    return suffix3;
                }
            }
            return suffix2;
        } catch (Exception e) {
            return "";
        }
    }

    private String[] getUrlDates(String url) {
        String[] result = new String[] { "-", "-" };
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            String lastMod = conn.getHeaderField("Last-Modified");
            String creation = conn.getHeaderField("Creation-Date");
            result[0] = lastMod != null ? lastMod : "-";
            result[1] = creation != null ? creation : "-";
            conn.disconnect();
        } catch (Exception ignored) {}
        return result;
    }
}
