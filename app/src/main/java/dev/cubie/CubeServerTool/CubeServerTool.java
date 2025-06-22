package dev.cubie.CubeServerTool;

import dev.cubie.CubeServerTool.Data.Config;
import dev.cubie.CubeServerTool.Data.InstallerUI;
import dev.cubie.CubeServerTool.Utils.AutoInstallFileHandler;
import dev.cubie.CubeServerTool.Utils.ConfigHandler;
import dev.cubie.CubeServerTool.Utils.LoggerUtility;
import dev.cubie.CubeServerTool.Utils.InstallationManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

// https://chatgpt.com/c/69609c43-014f-47da-8556-cb95b05d6f38

public class CubeServerTool {

    public static void main(String[] args) {
        Logger logger = LoggerUtility.getLogger(CubeServerTool.class);

        try {
            // Zeige Willkommensbildschirm
            String cVersion = centerText(Config.appVersion, 15);
            System.out.println("\n" +
                    "   ________________  \n" +
                    "  /               /| Created by HellBz \n" +
                    " /_______________/ | GitHub:    https://github.com/HellBz\n" +
                    "|     Cube      |  | Twitter:   https://x.com/HellBz\n" +
                    "|    Server     |  | Discord:   https://discord.gg/tuzpmeZ\n" +
                    "|     Tool      |  | Steam:     https://s.team/u/hellbz\n" +
                    "|" + cVersion + "| /  Facebook:  https://fb.com/hellbz\n" +
                    "|_______________|/   Instagram: https://instagram.com/h3llbz\n\n" +
                    "------------------------------------------------------------");

            logger.info("Starte CubeServerTool...");

            // Lade die Konfiguration
            try {
                ConfigHandler.loadConfig();
            } catch (IOException e) {
                logger.severe("Fehler beim Laden der Konfiguration: " + e.getMessage());
                startInteractiveMode();
                return;
            }

            // Prüfe auf bestehende Installation
            if (InstallationManager.isInstalled()) {
                handleExistingInstallation(logger);
            } else {
                // Keine Installation gefunden, starte normalen Installationsprozess
                logger.info("Keine bestehende Installation gefunden.");
                startInteractiveMode();
            }
        } catch (Exception e) {
            logger.severe("Ein schwerwiegender Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void handleExistingInstallation(Logger logger) {
        try {
            InstallationManager.InstallationInfo existingInstall = InstallationManager.loadInstallationInfo();
            if (existingInstall == null) {
                logger.warning("Installationsdatei beschädigt. Starte im interaktiven Modus...");
                startInteractiveMode();
                return;
            }

            // Setze Konfiguration aus der Installation
            Config.selectedAutoUpdate = existingInstall.autoUpdate;
            Config.selectedInstaller = findMatchingInstaller(existingInstall);
            
            if (Config.selectedInstaller != null) {
                Config.selectedType = existingInstall.type;
                Config.selectedVersion = existingInstall.version;
                Config.selectedSubVersion = existingInstall.subVersion;
            }

            logger.info("Bestehende Installation gefunden: " +
                    existingInstall.installerName + " " +
                    existingInstall.version + " (" + existingInstall.type + ")");

            // Prüfe auf Updates, wenn Auto-Update aktiviert ist
            if (Config.selectedAutoUpdate && Config.selectedInstaller != null) {
                // Nur prüfen, wenn 'latest' als Version angegeben ist
                if ("latest".equalsIgnoreCase(existingInstall.version)) {
                    logger.info("Prüfe auf Updates (Version ist auf 'latest' gesetzt)...");
                    
                    try {
                        // Prüfe auf Hauptversions-Updates
                        String[] availableVersions = Config.selectedInstaller.getAvailableVersions();
                        
                        if (availableVersions != null && availableVersions.length > 0) {
                            String latestVersion = availableVersions[0]; // Erste Version sollte die neueste sein
                            logger.info(String.format("Aktuelle neueste Version: %s", latestVersion));
                            
                            // Setze die Version auf die neueste verfügbare Version
                            Config.selectedVersion = latestVersion;
                            
                            // Prüfe auf Subversion-Updates, falls vorhanden
                            String[] availableSubVersions = Config.selectedInstaller.getAvailableSubVersions();
                            if (availableSubVersions != null && availableSubVersions.length > 0) {
                                String latestSubVersion = availableSubVersions[0];
                                logger.info(String.format("Aktuelle neueste Subversion: %s", latestSubVersion));
                                Config.selectedSubVersion = latestSubVersion;
                            }
                            
                            // Speichere die aktuelle Version in versionCurrent, wenn sie noch nicht gesetzt ist
                            if (existingInstall.versionCurrent == null || existingInstall.versionCurrent.isEmpty()) {
                                existingInstall.versionCurrent = existingInstall.version;
                                existingInstall.subVersionCurrent = existingInstall.subVersion;
                                logger.info(String.format("Aktuelle Version gespeichert: %s (%s)", 
                                    existingInstall.versionCurrent, 
                                    existingInstall.subVersionCurrent != null ? existingInstall.subVersionCurrent : "keine"));
                                
                                // Speichere die aktualisierten Installationsinformationen
                                try {
                                    existingInstall.saveToFile(Config.installationFilePath);
                                    logger.info("Installationsinformationen wurden aktualisiert.");
                                } catch (IOException e) {
                                    logger.warning("Konnte Installationsinformationen nicht speichern: " + e.getMessage());
                                }
                            }
                            
                            // Hier könnte man ein automatisches Update durchführen
                        } else {
                            logger.warning("Konnte keine verfügbaren Versionen abrufen");
                        }
                    } catch (Exception e) {
                        logger.warning("Fehler bei der Update-Prüfung: " + e.getMessage());
                    }
                } else {
                    logger.info("Keine Update-Prüfung nötig - feste Version angegeben: " + existingInstall.version);
                }
            }

            // Starte den vorhandenen Server
            if (Config.selectedInstaller != null) {
                try {
                    Config.selectedInstaller.init();
                    Config.selectedInstaller.start();
                    return; // Erfolgreich gestartet
                } catch (Exception e) {
                    logger.severe("Fehler beim Starten der Installation: " + e.getMessage());
                }
            } else {
                logger.warning("Kein passender Installer für die vorhandene Installation gefunden.");
            }
        } catch (Exception e) {
            logger.severe("Fehler beim Verarbeiten der Installation: " + e.getMessage());
        }
        
        // Falls wir hier ankommen, ist etwas schiefgelaufen
        logger.info("Starte interaktiven Modus...");
        startInteractiveMode();
    }

    private static CubeServerModule findMatchingInstaller(InstallationManager.InstallationInfo info) {
        Logger logger = LoggerUtility.getLogger(CubeServerTool.class);
        logger.info("Suche nach passendem Installer für Installation");
        List<CubeServerModule> installers = new ArrayList<>(CubeServerModule.loadInternalInstallers());
        CubeServerModule.loadExternalJars(Config.modulesFolder, installers);
        
        for (CubeServerModule installer : installers) {
            if (installer.getInstallerName().equalsIgnoreCase(info.installerName)) {
                return installer;
            }
        }
        return null;
    }

    public static void startInteractiveMode() {
        Logger logger = LoggerUtility.getLogger(CubeServerTool.class);
        // Load internal installers
        logger.info("Lade interne Installer-Module...");
        List<CubeServerModule> installers = new ArrayList<>(CubeServerModule.loadInternalInstallers());

        // Load external JARs
        logger.info("Lade externe JARs aus " + Config.modulesFolder + "...");
        CubeServerModule.loadExternalJars(Config.modulesFolder, installers);

        // Check if any installers were loaded
        if (installers.isEmpty()) {
            logger.severe("Keine Installer gefunden. Beende Programm.");
            System.exit(1);
        }

        // Versuche Auto-Install
        try {
            logger.info("Prüfe auf automatische Installation...");
            AutoInstallFileHandler.processAutoInstall(installers, logger);
        } catch (Exception e) {
            logger.warning("Automatische Installation fehlgeschlagen: " + e.getMessage());
        }

        // Wenn kein Auto-Install erfolgte, nutze manuelle Auswahl
        if (Config.selectedInstaller == null) {
            logger.info("Starte interaktiven Installationsassistenten...");
            // 3. Installer selection
            InstallerUI.selectInstaller(installers);

            // 3.5. Version selection
            InstallerUI.selectType();

            // 4. Version selection
            InstallerUI.selectVersion();

            // 5. Subversion selection (if available)
            InstallerUI.selectSubVersion();

            // 6. Auto-update selection
            InstallerUI.selectAutoUpdate();
        } else {
            logger.info("Automatische Konfiguration geladen.");
        }

        // 7. Start installation
        logger.info("Starte Installationsprozess...");
        Config.selectedInstaller.init();

        // Prüfe auf bestehende Installation
        if (InstallationManager.isInstalled()) {
            InstallationManager.InstallationInfo existingInstall = InstallationManager.loadInstallationInfo();
            logger.warning("Achtung: Es ist bereits eine Installation vorhanden!");
            logger.info("Aktuelle Installation: " + 
                      existingInstall.installerName + " " + 
                      existingInstall.version + " (" + existingInstall.type + ")");
            
            // Frage den Benutzer, ob er fortfahren möchte
            logger.info("Möchten Sie die bestehende Installation überschreiben? (j/n)");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String input = reader.readLine().trim().toLowerCase();
                if (!input.equals("j") && !input.equals("ja")) {
                    logger.info("Installation abgebrochen.");
                    System.exit(0);
                    return;
                }
            } catch (IOException e) {
                logger.severe("Fehler beim Lesen der Benutzereingabe: " + e.getMessage());
                System.exit(1);
                return;
            }
        }

        // Log auto-update option
        if (Config.selectedAutoUpdate) {
            logger.info("Auto-Update ist aktiviert. (Zukünftige Funktion)");
        } else {
            logger.info("Auto-Update ist deaktiviert.");
        }

        // Installiere und starte
        try {
            logger.info("Installiere Server...");
            Config.selectedInstaller.install();
            
            // Speichere Installationsinformationen
            logger.info("Speichere Installationsinformationen...");
            InstallationManager.saveInstallationInfo(
                Config.selectedInstaller.getInstallerName(),
                Config.selectedType,
                Config.selectedVersion,
                Config.selectedSubVersion,
                Config.rootFolder.toString(),
                Config.selectedAutoUpdate
            );
            
            logger.info("Starte Server...");
            Config.selectedInstaller.start();
            
        } catch (Exception e) {
            logger.severe("Fehler während der Installation: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
