package dev.cubie.CubeServerTool.Utils;

import dev.cubie.CubeServerTool.Data.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class InstallationManager {
    private static final Logger logger = LoggerUtility.getLogger(InstallationManager.class);

    public static class InstallationInfo {
        public String installerName;
        public String type;
        public String version;
        public String subVersion;
        public String versionCurrent;
        public String subVersionCurrent;
        public long timestamp;
        public String installationPath;
        public boolean autoUpdate;

        public InstallationInfo() {
            this.timestamp = System.currentTimeMillis();
        }

        public InstallationInfo(String installerName, String type, String version, String subVersion, 
                              String installationPath, boolean autoUpdate) {
            this.installerName = installerName;
            this.type = type;
            this.version = version;
            this.subVersion = subVersion;
            this.installationPath = installationPath;
            this.autoUpdate = autoUpdate;
            this.timestamp = System.currentTimeMillis();
        }

        public void saveToFile(Path file) throws IOException {
            // Lese die bestehende Datei, falls vorhanden, sonst die Vorlage
            List<String> lines;
            if (Files.exists(file)) {
                // Lese die bestehende Datei
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } else {
                // Lese die Vorlage aus den Ressourcen
                try (InputStream templateStream = InstallationManager.class.getResourceAsStream("/installation.conf.template")) {
                    if (templateStream == null) {
                        throw new FileNotFoundException("Template file 'installation.conf.template' not found in resources");
                    }
                    lines = new BufferedReader(new InputStreamReader(templateStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.toList());
                }
            }

            // Erstelle eine Map mit den zu setzenden Werten
            Map<String, String> newValues = new HashMap<>();
            newValues.put("installerName", installerName != null ? installerName : "");
            newValues.put("type", type != null ? type : "");
            newValues.put("version", version != null ? version : "");
            newValues.put("subVersion", subVersion != null ? subVersion : "");
            newValues.put("versionCurrent", versionCurrent != null ? versionCurrent : "");
            newValues.put("subVersionCurrent", subVersionCurrent != null ? subVersionCurrent : "");
            newValues.put("timestamp", String.valueOf(timestamp));
            newValues.put("installationPath", installationPath != null ? installationPath : "./");
            newValues.put("autoUpdate", String.valueOf(autoUpdate));
            
            // Erstelle eine Map, um zu verfolgen, welche Werte bereits in der Datei vorhanden sind
            Set<String> foundKeys = new HashSet<>();

            // Erstelle die Ausgabedatei
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    
                    // Behalte Leerzeilen und Kommentare bei
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                        writer.write(line);
                        writer.newLine();
                        continue;
                    }
                    
                    // Wenn es sich um eine Eigenschaft handelt
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = line.substring(0, equalsIndex).trim();
                        
                        // Wenn der Schlüssel in unseren neuen Werten existiert
                        if (newValues.containsKey(key)) {
                            foundKeys.add(key);
                            // Behalte die ursprüngliche Formatierung bei (Einrückung, Leerzeichen)
                            String prefix = line.substring(0, line.indexOf(key) + key.length() + 1);
                            writer.write(prefix + newValues.get(key));
                            continue;
                        }
                    }
                    // Schreibe die Zeile unverändert
                    writer.write(line);
                    writer.newLine();
                }
                
                // Füge fehlende Eigenschaften am Ende hinzu
                if (!foundKeys.containsAll(newValues.keySet())) {
                    writer.newLine();
                    writer.write("# Automatisch hinzugefügte Eigenschaften");
                    writer.newLine();
                    
                    for (Map.Entry<String, String> entry : newValues.entrySet()) {
                        if (!foundKeys.contains(entry.getKey())) {
                            writer.write(entry.getKey() + "=" + entry.getValue());
                            writer.newLine();
                        }
                    }
                }
            }
        }

        public static InstallationInfo loadFromFile(Path file) throws IOException {
            if (!Files.exists(file)) {
                logger.warning("Installationsdatei nicht gefunden: " + file);
                return null;
            }
            
            InstallationInfo info = new InstallationInfo();
            Properties props = new Properties();
            
            // Lade nur Zeilen, die nicht mit # beginnen und nicht leer sind
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int separator = line.indexOf('=');
                    if (separator > 0) {
                        String key = line.substring(0, separator).trim();
                        String value = line.substring(separator + 1).trim();
                        props.setProperty(key, value);
                    }
                }
            }

            info.installerName = props.getProperty("installerName", "");
            info.type = props.getProperty("type", "");
            info.version = props.getProperty("version", "");
            info.subVersion = props.getProperty("subVersion", "");
            info.versionCurrent = props.getProperty("versionCurrent", "");
            info.subVersionCurrent = props.getProperty("subVersionCurrent", "");
            info.timestamp = Long.parseLong(props.getProperty("timestamp", "0"));
            info.installationPath = props.getProperty("installationPath", "./");
            info.autoUpdate = Boolean.parseBoolean(props.getProperty("autoUpdate", "false"));

            return info;
        }
    }

    public static void saveInstallationInfo(String installerName, String type, String version, String subVersion, String installationPath, boolean autoUpdate) {
        try {
            // Erstelle das Verzeichnis, falls es nicht existiert
            Files.createDirectories(Config.configFolder);
            
            // Lade vorhandene Installationsinformationen, falls vorhanden
            InstallationInfo existingInfo = loadInstallationInfo();
            
            // Erstelle ein neues InstallationInfo-Objekt
            InstallationInfo info = new InstallationInfo(installerName, type, version, subVersion, 
                installationPath != null ? installationPath : "./", autoUpdate);
                
            // Behalte die aktuellen Versionsinformationen bei, falls vorhanden
            if (existingInfo != null) {
                info.versionCurrent = existingInfo.versionCurrent;
                info.subVersionCurrent = existingInfo.subVersionCurrent;
            }
            
            // Wenn die Version nicht "latest" ist, setze die aktuelle Version
            if (!"latest".equalsIgnoreCase(version)) {
                info.versionCurrent = version;
                info.subVersionCurrent = subVersion;
            }
            
            // Speichere die Informationen in die Datei
            info.saveToFile(Config.installationFilePath);
            logger.info("Installationsinformationen wurden gespeichert: " + Config.installationFilePath);
        } catch (IOException e) {
            logger.severe("Fehler beim Speichern der Installationsinformationen");
            logger.severe(e.toString());
            e.printStackTrace();
        }
    }

    public static InstallationInfo loadInstallationInfo() {
        try {
            return InstallationInfo.loadFromFile(Config.installationFilePath);
        } catch (IOException e) {
            logger.warning("Fehler beim Lesen der Installationsinformationen: " + e.getMessage());
            return null;
        }
    }

    public static boolean isInstalled() {
        return Files.exists(Config.installationFilePath);
    }
}
