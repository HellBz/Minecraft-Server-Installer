package de.hellbz.MinecraftServerInstaller.Utils;

import de.hellbz.MinecraftServerInstaller.MinecraftServerInstaller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Logger;

import static de.hellbz.MinecraftServerInstaller.Data.Transform.printTable;

public class InstallerUI {

    private static final Logger logger = LoggerUtility.getLogger(InstallerUI.class);

    // Method to select an installer from the list
    public static MinecraftServerInstaller selectInstaller(List<MinecraftServerInstaller> installers) {
        logger.info("Available installers:");
        List<String> installerNames = new ArrayList<>();
        for (MinecraftServerInstaller installer : installers) {
            installerNames.add(installer.getInstallerName());
        }
        printTable(installerNames);

        logger.info("Select an installer (1-" + installers.size() + "): ");
        Scanner scanner = new Scanner(System.in);
        int installerChoice;
        try {
            installerChoice = scanner.nextInt();
        } catch (NoSuchElementException e) {
            installerChoice = 1; // Default choice
            logger.warning("No input provided, using default installer: " + installerChoice);
        }
        return installers.get(installerChoice - 1);
    }

    // Method to select a version from the installer
    public static String selectVersion(MinecraftServerInstaller installer) {
        List<String> availableVersions = Arrays.asList(installer.getAvailableVersions());
        logger.info("Available versions:");
        printTable(availableVersions, 4, "[", "]");

        logger.info("Select a version (1-" + availableVersions.size() + "): ");
        Scanner scanner = new Scanner(System.in);
        int versionChoice;
        try {
            versionChoice = scanner.nextInt();
        } catch (NoSuchElementException e) {
            versionChoice = 1; // Default choice
            logger.warning("No input provided, using default version: " + versionChoice);
        }
        return availableVersions.get(versionChoice - 1);
    }

    // Method to select a subversion if available
    public static String selectSubVersion(MinecraftServerInstaller installer, String version) {
        List<String> availableSubVersions = Arrays.asList(installer.getAvailableSubVersions(version));
        if (!availableSubVersions.isEmpty()) {
            logger.info("Available subversions:");
            printTable(availableSubVersions, 4, "[", "]");

            logger.info("Select a subversion (1-" + availableSubVersions.size() + "): ");
            Scanner scanner = new Scanner(System.in);
            int subVersionChoice;
            try {
                subVersionChoice = scanner.nextInt();
            } catch (NoSuchElementException e) {
                subVersionChoice = 1; // Default choice
                logger.warning("No input provided, using default subVersion: " + subVersionChoice);
            }
            return availableSubVersions.get(subVersionChoice - 1);
        } else {
            return null;
        }
    }

    // Method to select auto-update option
    public static boolean selectAutoUpdate() {
        logger.info("Enable auto-update? (yes/no): ");
        Scanner scanner = new Scanner(System.in);
        String autoUpdateChoice;
        try {
            autoUpdateChoice = scanner.next();
        } catch (NoSuchElementException e) {
            autoUpdateChoice = "no"; // Default choice
            logger.warning("No input provided, using default choice: " + autoUpdateChoice);
        }
        return autoUpdateChoice.equalsIgnoreCase("yes");
    }
}
