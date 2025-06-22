package dev.cubie.CubeServerTool.Data;

import dev.cubie.CubeServerTool.CubeServerModule;
import dev.cubie.CubeServerTool.Utils.LoggerUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Logger;

import static dev.cubie.CubeServerTool.Data.DataTablePrinter.printTable;

public class InstallerUI {

    private static final Logger logger = LoggerUtility.getLogger(InstallerUI.class);

    // Method to select an installer from the list
    public static void selectInstaller(List<CubeServerModule> installers) {
        logger.info("Available installers:");
        List<String> installerNames = new ArrayList<>();
        for (CubeServerModule installer : installers) {
            // Debugging-Ausgabe
            // System.out.println("Installer: " + installer);
            // System.out.println("Installer Name: " + installer.getInstallerName());
            // System.out.println("Installer Class: " + installer.getClass().getSimpleName());
            // System.out.println("Installer Class Full Name: " + installer.getClass().getName());
            
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
        Config.selectedInstaller = installers.get(installerChoice - 1);
    }
    // Method to select the Version-Type from the installer
    public static void selectType() {
        List<String> availableTypes = Arrays.asList(Config.selectedInstaller.getAvailableTypes());
        if (!availableTypes.isEmpty()) {
            logger.info("Available Types:");
            printTable(availableTypes, 4, "[", "]");

            logger.info("Select a Type (1-" + availableTypes.size() + "): ");
            Scanner scanner = new Scanner(System.in);
            int subVersionChoice;
            try {
                subVersionChoice = scanner.nextInt();
            } catch (NoSuchElementException e) {
                subVersionChoice = 1; // Default choice
                logger.warning("No input provided, using default subVersion: " + subVersionChoice);
            }
            Config.selectedType = availableTypes.get(subVersionChoice - 1);
        }
    }

    // Method to select a version from the installer
    public static void selectVersion() {
        List<String> availableVersions = Arrays.asList(Config.selectedInstaller.getAvailableVersions());
        if (availableVersions.isEmpty()) {
            logger.severe("Keine Versionen verfügbar!");
            System.exit(1);
        }
        
        logger.info("Verfügbare Versionen (neueste zuerst):");
        printTable(availableVersions, 4, "[", "]");

        while (true) {
            logger.info("Wählen Sie eine Version (1-" + availableVersions.size() + ") oder geben Sie 'latest' oder '0' für die neueste Version ein: ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim().toLowerCase();
            
            // Leere Eingabe, 'latest' oder '0' wählt die neueste Version
            if (input.isEmpty() || input.equals("latest") || input.equals("0")) {
                Config.selectedVersion = availableVersions.get(0);
                logger.info("Verwende neueste Version: " + Config.selectedVersion);
                return;
            }
            
            // Versuche die Eingabe als Zahl zu parsen
            try {
                int versionChoice = Integer.parseInt(input);
                if (versionChoice >= 1 && versionChoice <= availableVersions.size()) {
                    Config.selectedVersion = availableVersions.get(versionChoice - 1);
                    logger.info("Ausgewählte Version: " + Config.selectedVersion);
                    return;
                } else {
                    logger.warning("Ungültige Auswahl. Bitte geben Sie eine Zahl zwischen 1 und " + availableVersions.size() + " ein.");
                }
            } catch (NumberFormatException e) {
                logger.warning("Ungültige Eingabe. Bitte geben Sie eine Zahl, 'latest' oder drücken Sie einfach Enter ein.");
            }
        }
    }

    // Method to select a subversion if available
    public static void selectSubVersion() {
        String[] subVersions = Config.selectedInstaller.getAvailableSubVersions();
        if (subVersions == null || subVersions.length == 0) {
            Config.selectedSubVersion = "";
            return;
        }
        
        List<String> availableSubVersions = Arrays.asList(subVersions);
        logger.info("Verfügbare Subversionen (neueste zuerst):");
        printTable(availableSubVersions, 4, "[", "]");

        while (true) {
            logger.info("Wählen Sie eine Subversion (1-" + availableSubVersions.size() + "), 'latest', '0' oder drücken Sie Enter für die neueste Version: ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim().toLowerCase();
            
            // Leere Eingabe, 'latest' oder '0' wählt die neueste Version
            if (input.isEmpty() || input.equals("latest") || input.equals("0")) {
                Config.selectedSubVersion = availableSubVersions.get(0);
                logger.info("Verwende neueste Subversion: " + Config.selectedSubVersion);
                return;
            }
            
            // Versuche die Eingabe als Zahl zu parsen
            try {
                int subVersionChoice = Integer.parseInt(input);
                if (subVersionChoice >= 1 && subVersionChoice <= availableSubVersions.size()) {
                    Config.selectedSubVersion = availableSubVersions.get(subVersionChoice - 1);
                    logger.info("Ausgewählte Subversion: " + Config.selectedSubVersion);
                    return;
                } else {
                    logger.warning("Ungültige Auswahl. Bitte geben Sie eine Zahl zwischen 1 und " + availableSubVersions.size() + " ein.");
                }
            } catch (NumberFormatException e) {
                logger.warning("Ungültige Eingabe. Bitte geben Sie eine Zahl, 'latest' oder drücken Sie einfach Enter ein.");
            }
        }
    }

    // Method to select auto-update option
    public static void selectAutoUpdate() {
        logger.info("Enable auto-update? (yes/no): ");
        Scanner scanner = new Scanner(System.in);
        String autoUpdateChoice;
        try {
            autoUpdateChoice = scanner.next();
        } catch (NoSuchElementException e) {
            autoUpdateChoice = "no"; // Default choice
            logger.warning("No input provided, using default choice: " + autoUpdateChoice);
        }
        Config.selectedAutoUpdate = autoUpdateChoice.equalsIgnoreCase("yes");
    }
}
