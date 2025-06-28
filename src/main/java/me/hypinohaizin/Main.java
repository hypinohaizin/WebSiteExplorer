package me.hypinohaizin;

import me.hypinohaizin.utils.*;
import me.hypinohaizin.managers.ScanManager;
import me.hypinohaizin.commands.Commands;

import java.io.File;
import java.util.Scanner;

public class Main {
    private static ConfigUtil.Config config;
    private static final File appDir = FileUtil.getAppDir();

    public static void main(String[] args) {
        config = ConfigUtil.loadConfig(appDir);
        ScanManager scanManager = new ScanManager(config);
        Commands commandManager = new Commands(config, scanManager, appDir);

        Scanner scanner = new Scanner(System.in);
        System.out.println(WSE.WSE_NAME + " 起動中... (help でコマンド一覧)");

        while (true) {
            System.out.print("WSE > ");
            String input = scanner.nextLine();
            if (!commandManager.execute(input)) {
                break;
            }
        }
    }
}
