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
                            
                            // Automatisches Update durchführen
                            logger.info("Führe Update auf Version " + latestVersion + " durch...");
                            String newSubVersion = existingInstall.subVersion; // Standardmäßig alte Subversion beibehalten
                            
                            // Wenn eine neue Subversion verfügbar ist, diese verwenden
                            if (availableSubVersions != null && availableSubVersions.length > 0) {
                                newSubVersion = availableSubVersions[0];
                                logger.info("Neue Subversion gefunden: " + newSubVersion);
                            }
                            
                            try {
                                // 1. Backup erstellen
                                if (Config.selectedInstaller.shouldCreateBackup()) {
                                    logger.info("Erstelle Backup der aktuellen Installation...");
                                    String backupVersion = existingInstall.versionCurrent != null ? 
                                            existingInstall.versionCurrent : existingInstall.version;
                                    String backupSubVersion = existingInstall.subVersionCurrent != null ? 
                                            existingInstall.subVersionCurrent : existingInstall.subVersion;
                                            
                                    boolean backupSuccess = Config.selectedInstaller.createBackup(
                                        backupVersion,
                                        backupSubVersion
                                    );
                                    
                                    if (!backupSuccess) {
                                        logger.warning("Backup konnte nicht erstellt werden, fahre trotzdem fort...");
                                    }
                                } else {
                                    logger.info("Backup wurde übersprungen (in den Einstellungen deaktiviert)");
                                }
                                
                                // 2. Neue Version installieren (wie eine Neuinstallation)
                                logger.info("Installiere neue Version " + latestVersion + 
                                    (newSubVersion != null ? " (Build: " + newSubVersion + ")" : "") + "...");
                                
                                // Hier würde die eigentliche Installationslogik stehen
                                // Z.B.: Config.selectedInstaller.install(latestVersion, newSubVersion);
                                
                                // 3. Installationsinformationen aktualisieren
                                existingInstall.versionCurrent = latestVersion;
                                existingInstall.subVersionCurrent = newSubVersion;
                                try {
                                    existingInstall.saveToFile(Config.installationFilePath);
                                    logger.info("Installationsinformationen wurden aktualisiert.");
                                } catch (IOException e) {
                                    logger.warning("Konnte Installationsinformationen nicht speichern: " + e.getMessage());
                                }
                                
                                logger.info("Update erfolgreich durchgeführt!");
                                
                            } catch (Exception e) {
                                logger.severe("Fehler während des Updates: " + e.getMessage());
                                logger.severe("Bitte starte den Server manuell neu oder stelle das Backup wieder her.");
                            }
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
                    logger.info("Initialisiere Server...");
                    Config.selectedInstaller.init();
                    logger.info("Starte Server...");
                    Config.selectedInstaller.start();
                    logger.info("Server erfolgreich gestartet.");
                    return; // Erfolgreich gestartet
                } catch (Exception e) {
                    logger.log(java.util.logging.Level.SEVERE, "Fehler beim Starten des Servers", e);
                    logger.warning("Falls das Problem weiterhin besteht, versuchen Sie bitte eine Neuinstallation.");
                }
            } else {
                logger.warning("Kein passender Installer für die vorhandene Installation gefunden.");
                logger.info("Verfügbare Module: " + CubeServerModule.loadInternalInstallers().stream()
                    .map(CubeServerModule::getInstallerName)
                    .collect(java.util.stream.Collectors.joining(", ")));
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Schwerwiegender Fehler beim Verarbeiten der Installation", e);
        }
        
        // Falls wir hier ankommen, ist etwas schiefgelaufen
        logger.info("Starte interaktiven Modus zur Fehlerbehebung...");
        try {
            startInteractiveMode();
        } catch (Exception e) {
            logger.severe("Kritischer Fehler im interaktiven Modus: " + e.getMessage());
            System.exit(1);
        }
    }

    private static CubeServerModule findMatchingInstaller(InstallationManager.InstallationInfo info) {
        if (info == null || info.installerName == null) {
            return null;
        }
        
        // Lade verfügbare Installer
        List<CubeServerModule> installers = new ArrayList<>(CubeServerModule.loadInternalInstallers());
        CubeServerModule.loadExternalJars(Config.modulesFolder, installers);
        
        // Durchsuche alle verfügbaren Module nach einem passenden Installer
        for (CubeServerModule module : installers) {
            if (info.installerName.equalsIgnoreCase(module.getInstallerName())) {
                return module;
            }
        }
        
        return null;
    }

    public static void startInteractiveMode() {
        Logger logger = LoggerUtility.getLogger(CubeServerTool.class);
        logger.info("Starte interaktiven Modus...");
        
        // Lade verfügbare Installer
        List<CubeServerModule> installers = new ArrayList<>(CubeServerModule.loadInternalInstallers());
        CubeServerModule.loadExternalJars(Config.modulesFolder, installers);

        // Prüfe ob Installer geladen wurden
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
