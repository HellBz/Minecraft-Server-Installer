package dev.cubie.CubeServerTool.Utils;

import dev.cubie.CubeServerTool.CubeServerModule;
import dev.cubie.CubeServerTool.Data.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class AutoInstallFileHandler {
    private static final Logger logger = LoggerUtility.getLogger(AutoInstallFileHandler.class);

    public static boolean checkAutoInstallFile() {
        Path autoInstallFile = Paths.get("auto-install.conf");
        
        if (!Files.exists(autoInstallFile)) {
            logger.warning("Auto-install file does not exist.");
            return false;
        }
        
        try {
            Properties properties = new Properties();
            properties.load(Files.newBufferedReader(autoInstallFile));
            
            String installerType = properties.getProperty("Installer");
            String type = properties.getProperty("Type");
            
            boolean hasInstaller = installerType != null && !installerType.isEmpty();
            boolean hasType = type != null && (type.equals("Release") || type.equals("Snapshot") || type.equals("All"));
            
            if (hasInstaller && hasType) {
                logger.info("Auto-install file found with valid content.");
                return true;
            } else {
                logger.warning("Auto-install file is missing required configuration.");
                if (!hasInstaller) logger.warning("Missing or invalid Installer");
                if (!hasType) logger.warning("Missing or invalid Type");
                return false;
            }
        } catch (IOException e) {
            logger.severe("Error reading auto-install file: " + e.getMessage());
            return false;
        }
    }

    public static void processAutoInstall(List<CubeServerModule> installers, Logger logger) throws Exception {
        // Pfad zur Auto-Install-Konfigurationsdatei
        java.nio.file.Path autoInstallPath = Paths.get("auto-install.conf");
        
        if (Files.exists(autoInstallPath)) {
            Properties props = new Properties();
            props.load(Files.newInputStream(autoInstallPath));

            if (checkAutoInstallFile()) {
                // Installer auswählen
                String installerProp = props.getProperty("Installer");
                CubeServerModule selectedInstaller = installers.stream()
                    .filter(installer -> 
                        installer.getInstallerName().equals(installerProp) || 
                        installer.getClass().getSimpleName().equals(installerProp) ||
                        installer.getClass().getName().equals(installerProp))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Installer not found: " + installerProp));

                Config.selectedInstaller = selectedInstaller;
                Config.selectedType = props.getProperty("Type");
                Config.selectedVersion = props.getProperty("Version").equals("latest") 
                    ? selectedInstaller.getAvailableVersions()[0] 
                    : props.getProperty("Version");
                
                // SubVersion-Behandlung
                String subVersion = props.getProperty("SubVersion");
                if (subVersion.equals("latest")) {
                    String[] subVersions = selectedInstaller.getAvailableSubVersions();
                    Config.selectedSubVersion = subVersions.length > 0 ? subVersions[0] : "";
                } else {
                    Config.selectedSubVersion = subVersion;
                }

                // Auto-Update-Einstellung
                Config.selectedAutoUpdate = Boolean.parseBoolean(props.getProperty("AutoUpdate"));

                // Verschiebe Auto-Install-Datei in cst_data Ordner
                Path cstDataFolder = Config.configFolder;
                Path destinationFile = cstDataFolder.resolve("cst-auto-install.txt");
                
                try {
                    // Stelle sicher, dass der Zielordner existiert
                    if (!Files.exists(cstDataFolder)) {
                        Files.createDirectories(cstDataFolder);
                    }
                    
                    // Verschiebe Datei
                    Files.move(autoInstallPath, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Auto-install file moved to: " + destinationFile);
                } catch (IOException e) {
                    logger.warning("Could not move auto-install file: " + e.getMessage());
                }

                logger.info("Auto-install configuration processed successfully.");
                return;
            }
        }

        // Fallback zur manuellen Installer-Auswahl
        logger.warning("Auto-install configuration not found or invalid. Falling back to manual installer selection.");
    }

}
