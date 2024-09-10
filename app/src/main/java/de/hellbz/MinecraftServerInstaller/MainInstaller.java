package de.hellbz.MinecraftServerInstaller;

import de.hellbz.MinecraftServerInstaller.Utils.Config;
import de.hellbz.MinecraftServerInstaller.Utils.ConfigHandler;
import de.hellbz.MinecraftServerInstaller.Utils.LoggerUtility;
import de.hellbz.MinecraftServerInstaller.Utils.InstallerUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MainInstaller {

    public static void main(String[] args) {

        // Initialize LoggerUtility after the config is loaded
        Logger logger = LoggerUtility.getLogger(MainInstaller.class);


        String version = "v 1.2.45"; // Beispiel-Version
        String centeredVersion = centerText( version, 15);


        System.out.println("\n" +
                "   ________________  \n" +
                "  /               /| Created by HellBz \n" +
                " /_______________/ | GitHub:    https://github.com/HellBz\n" +
                "|   Minecraft   |  | Twitter:   https://x.com/HellBz\n" +
                "|     Server    |  | Discord:   https://discord.gg/tuzpmeZ\n" +
                "|   Installer   |  | Steam:     https://s.team/u/hellbz\n" +
        "|" + centeredVersion + "| /  Facebook:  https://fb.com/hellbz\n" +
                "|_______________|/   Instagram: https://instagram.com/h3llbz\n\n" +
                        "------------------------------------------------------------");


        // Load configuration at the start
        try {
            ConfigHandler.loadConfig(Config.configFilePath);
        } catch (IOException e) {
            logger.severe("Error loading configuration: " + e.getMessage());
        }

        logger.info("Starting the MainInstaller...");

        try {
            // 1. Load internal installers
            logger.info("Loading internal Installer-Modules...");
            List<MinecraftServerInstaller> installers = new ArrayList<>(MinecraftServerInstaller.loadInternalInstallers());

            // 2. Load external JARs
            logger.info("Loading external JARs from " + Config.modulesFolder + "...");
            MinecraftServerInstaller.loadExternalJars(Config.modulesFolder, installers);

            // Check if any installers were loaded
            if (installers.isEmpty()) {
                logger.severe("No installers were loaded. Exiting.");
                System.exit(1);
            }

            // 3. Installer selection
            MinecraftServerInstaller selectedInstaller = InstallerUI.selectInstaller(installers);

            // 4. Version selection
            String selectedVersion = InstallerUI.selectVersion(selectedInstaller);

            // 5. Subversion selection (if available)
            String selectedSubVersion = InstallerUI.selectSubVersion(selectedInstaller, selectedVersion);

            // 6. Auto-update selection
            boolean autoUpdate = InstallerUI.selectAutoUpdate();

            // 7. Start installation
            selectedInstaller.init();
            selectedInstaller.install(selectedVersion, selectedSubVersion);

            // Log auto-update option
            if (autoUpdate) {
                logger.info("Auto-update is enabled. (This is a future feature)");
            } else {
                logger.info("Auto-update is disabled.");
            }

        } catch (Exception e) {
            logger.severe("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            logger.info("Installation process completed. Exiting.");
            System.exit(0);
        }
    }

    public static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width); // Kürzt, wenn der Text zu lang ist
        }
        int paddingTotal = width - text.length();
        int paddingStart = (paddingTotal + 1) / 2; // +1 sorgt für mehr Leerzeichen vorn bei ungeraden Zahlen
        int paddingEnd = paddingTotal / 2;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < paddingStart; i++) {
            builder.append(" ");
        }
        builder.append(text);
        for (int i = 0; i < paddingEnd; i++) {
            builder.append(" ");
        }
        return builder.toString();
    }
}
