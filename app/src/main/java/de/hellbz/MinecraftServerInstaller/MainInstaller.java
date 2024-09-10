package de.hellbz.MinecraftServerInstaller;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static de.hellbz.MinecraftServerInstaller.Data.Transform.printTable;
import static de.hellbz.MinecraftServerInstaller.MinecraftServerInstaller.*;
import de.hellbz.MinecraftServerInstaller.Utils.Config;
import de.hellbz.MinecraftServerInstaller.Utils.ConfigHandler;
import de.hellbz.MinecraftServerInstaller.Utils.LoggerUtility;

public class MainInstaller {

    public static void main(String[] args) {

        // Initialize LoggerUtility after the config is loaded
        Logger logger = LoggerUtility.getLogger(MainInstaller.class);

        // Explicitly load the config at the start
        try {
            ConfigHandler.loadConfig( Config.configFilePath );
        } catch (IOException e) {
            logger.severe("Error loading configuration: " + e.getMessage());
        }

        // Testen verschiedener Log-Einträge mit Farbstilen
        logger.info("Starting the MainInstaller...");

        try {
            // 1. Internal installers
            logger.info("Loading internal Installer-Modules...");
            List<MinecraftServerInstaller> installers = new ArrayList<>(loadInternalInstallers());

            // 2. Load external JARs and override internal installers if necessary
            logger.info("Loading external JARs from " + Config.modulesFolder + " ...");
            loadExternalJars( Config.modulesFolder, installers );

            // Check if any installers were loaded
            if (installers.isEmpty()) {
                logger.severe("No installers were loaded. Exiting.");
                System.exit(1);
            }

            Scanner scanner = new Scanner(System.in);

            // 3. Select installer
            logger.info("Available installers:");
            List<String> installerNames = new ArrayList<>();
            for (MinecraftServerInstaller installer : installers) {
                installerNames.add(installer.getInstallerName());
            }
            printTable(installerNames);  // Display installers in table format

            logger.info("Select an installer (1-" + installers.size() + "): ");
            int installerChoice;
            try {
                installerChoice = scanner.nextInt();
            } catch (NoSuchElementException e) {
                installerChoice = 1; // Default choice
                logger.warning("No input provided, using default installer: " + installerChoice);
            }
            MinecraftServerInstaller selectedInstaller = installers.get(installerChoice - 1);

            // 4. Select version
            List<String> availableVersions = Arrays.asList(selectedInstaller.getAvailableVersions());
            logger.info("Available versions:");
            printTable(availableVersions, 4, "[", "]");

            logger.info("Select a version (1-" + availableVersions.size() + "): ");
            int versionChoice;
            try {
                versionChoice = scanner.nextInt();
            } catch (NoSuchElementException e) {
                versionChoice = 1; // Default choice
                logger.warning("No input provided, using default version: " + versionChoice);
            }
            String selectedVersion = availableVersions.get(versionChoice - 1);

            // 5. Select subversion (if available)
            List<String> availableSubVersions = Arrays.asList(selectedInstaller.getAvailableSubVersions(selectedVersion));
            String selectedSubVersion = null;
            if (!availableSubVersions.isEmpty()) {
                logger.info("Available subversions:");
                printTable(availableSubVersions, 4, "[", "]");

                logger.info("Select a subversion (1-" + availableSubVersions.size() + "): ");
                int subVersionChoice;
                try {
                    subVersionChoice = scanner.nextInt();
                } catch (NoSuchElementException e) {
                    subVersionChoice = 1; // Default choice
                    logger.warning("No input provided, using default subVersion: " + subVersionChoice);
                }
                selectedSubVersion = availableSubVersions.get(subVersionChoice - 1);
            }

            // 6. Auto-update option (for future implementation)
            logger.info("Enable auto-update? (yes/no): ");
            String autoUpdateChoice;
            try {
                autoUpdateChoice = scanner.next();
            } catch (NoSuchElementException e) {
                autoUpdateChoice = "no"; // Default choice
                logger.warning("No input provided, using default choice: " + autoUpdateChoice);
            }
            boolean autoUpdate = autoUpdateChoice.equalsIgnoreCase("yes");

            // Start installation
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
            e.printStackTrace();  // Optionally log the stack trace for debugging
        } finally {
            logger.info("Installation process completed. Exiting.");
            System.exit(0);  // Ensure the program exits successfully
        }
    }
}
