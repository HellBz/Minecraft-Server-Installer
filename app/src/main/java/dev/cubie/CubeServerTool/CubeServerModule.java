package dev.cubie.CubeServerTool;

import dev.cubie.CubeServerTool.Utils.LoggerUtility;
import dev.cubie.CubeServerTool.Data.Config;
import dev.cubie.CubeServerTool.Utils.ProcessHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import dev.cubie.CubeServerTool.Utils.DirectoryLister;

// Schnittstelle für CubeServerTool
public interface CubeServerModule {

    /**
     * Wird beim Laden des Moduls aufgerufen.
     * Kann überschrieben werden, um Initialisierungslogik hinzuzufügen.
     */
    default void init() {}
    
    /**
     * Gibt den Anzeigenamen des Installers/Moduls zurück.
     * @return Der Anzeigename als String
     */
    String getInstallerName();
    
    /**
     * Gibt die verfügbaren Server-Typen zurück (z.B. "Release", "Snapshot").
     * @return Array von verfügbaren Typen
     */
    String[] getAvailableTypes();
    
    /**
     * Gibt die aktuell installierte Version zurück.
     * Standardimplementierung sucht nach einer server.properties-Datei oder nach der Server-JAR.
     * @return Die installierte Version oder null, wenn nicht ermittelbar
     */
    default String getCurrentVersion() {
        Logger logger = LoggerUtility.getLogger(getClass());
        
        // 1. Versuche, die Version aus der server.properties zu lesen
        try {
            Path serverProps = Config.rootFolder.resolve("server.properties");
            if (Files.exists(serverProps)) {
                for (String line : Files.readAllLines(serverProps, StandardCharsets.UTF_8)) {
                    if (line.startsWith("version=")) {
                        String version = line.substring(8).trim();
                        if (!version.isEmpty()) {
                            logger.fine("Version aus server.properties gelesen: " + version);
                            return version;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Konnte Version nicht aus server.properties lesen: " + e.getMessage());
        }
        
        // 2. Versuche, die Version aus dem Dateinamen der Server-JAR zu extrahieren
        try {
            Pattern jarPattern = getStartFile();
            if (jarPattern != null) {
                try (java.util.stream.Stream<Path> files = Files.list(Config.rootFolder)) {
                    return files
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .map(jarPattern::matcher)
                        .filter(java.util.regex.Matcher::find)
                        .map(m -> m.group(1))
                        .findFirst()
                        .orElse(null);
                }
            }
        } catch (Exception e) {
            logger.warning("Konnte Version nicht aus JAR-Dateinamen extrahieren: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Gibt die verfügbaren Versionen zurück.
     * @return Array von verfügbaren Versionen
     */
    String[] getAvailableVersions();
    
    /**
     * Gibt verfügbare Unterversionen zurück (z.B. Build-Nummern).
     * @return Array von verfügbaren Unterversionen
     */
    String[] getAvailableSubVersions();

    /**
     * Gibt das Pattern zurück, mit dem nach der Startdatei gesucht wird.
     * @return Pattern für die Dateisuche
     */
    Pattern getStartFile();
    
    /**
     * Gibt eine Liste der zu sichernden Verzeichnisse zurück.
     * Standardmäßig wird das Root-Verzeichnis gesichert.
     * @return Liste der zu sichernden Verzeichnisse
     */
    default List<Path> getBackupDirectories() {
        List<Path> dirs = new ArrayList<>();
        dirs.add(Config.rootFolder);
        return dirs;
    }
    
    /**
     * Gibt eine Liste der auszuschließenden Dateimuster zurück.
     * @return Liste der auszuschließenden Dateimuster
     */
    default List<String> getBackupExcludes() {
        List<String> excludes = new ArrayList<>();
        excludes.add("session.lock");
        excludes.add("*.log");
        excludes.add("*.lck");
        excludes.add("*.tmp");
        excludes.add("logs/**");
        excludes.add("crash-reports/**");
        excludes.add("cst_data/**");
        return excludes;
    }
    
    /**
     * Bestimmt, ob für dieses Modul ein Backup erstellt werden soll.
     * @return true, wenn ein Backup erstellt werden soll, sonst false
     */
    default boolean shouldCreateBackup() {
        return true;
    }
    
    /**
     * Gibt den zu verwendenden Versions-String für das Backup zurück.
     * @param version Die Hauptversion
     * @param subVersion Die Subversion (kann null sein)
     * @return Der zu verwendende Versions-String
     */
    default String getBackupVersionString(String version, String subVersion) {
        return version + (subVersion != null && !subVersion.isEmpty() ? "-" + subVersion : "");
    }

    /**
     * Führt die Installation des Servers durch.
     * Muss von konkreten Implementierungen implementiert werden.
     */
    void install();
    
    /**
     * Erstellt ein Backup des aktuellen Serververzeichnisses.
     * @param version Die Zielversion, zu der aktualisiert wird
     * @param subVersion Die Ziel-Subversion (kann null sein)
     * @return true, wenn das Backup erfolgreich war, sonst false
     */
    default boolean createBackup(String version, String subVersion) {
        if (!shouldCreateBackup()) {
            LoggerUtility.getLogger(getClass()).info("Backup-Erstellung für dieses Modul deaktiviert");
            return true;
        }
        
        Logger logger = LoggerUtility.getLogger(getClass());
        try {
            // Bestimme den Backup-Namen
            String versionString = getBackupVersionString(version, subVersion);
            String backupName = String.format("%s-%s.zip", getClass().getSimpleName(), versionString);
            
            // Verwende den Backup-Ordner aus der Config
            Path backupDir = Config.backupFolder;
            Files.createDirectories(backupDir);
            
            Path backupPath = backupDir.resolve(backupName);
            
            // Lösche ältere Backups der gleichen Version
            cleanupOldBackups(backupDir, backupName, versionString);
            
            logger.info("Erstelle Backup: " + backupPath);
            
            // Erstelle das Backup mit DirectoryLister für jedes zu sichernde Verzeichnis
            for (Path dir : getBackupDirectories()) {
                if (!Files.exists(dir)) {
                    logger.warning("Verzeichnis existiert nicht und wird übersprungen: " + dir);
                    continue;
                }
                
                DirectoryLister lister = new DirectoryLister(dir);
                
                // Füge alle Ausschlussmuster hinzu
                for (String exclude : getBackupExcludes()) {
                    if (exclude.endsWith("**")) {
                        lister.excludeDirectory(exclude.substring(0, exclude.length() - 3));
                    } else if (exclude.endsWith("*")) {
                        lister.excludeFile(exclude);
                    } else {
                        lister.excludeFile(exclude);
                    }
                }
                
                // Erstelle einen relativen Pfad für das Backup
                String relativePath = Config.rootFolder.relativize(dir).toString();
                if (relativePath.isEmpty() || relativePath.equals(".")) {
                    relativePath = "";
                } else {
                    relativePath = relativePath + "/";
                }
                
                lister.backup(relativePath + backupPath.getFileName().toString(), backupDir);
            }
            
            logger.info("Backup erfolgreich erstellt: " + backupPath);
            return true;
        } catch (Exception e) {
            logger.severe("Fehler beim Erstellen des Backups: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Bereinigt ältere Backups der gleichen Version.
     * @param backupDir Das Backup-Verzeichnis
     * @param currentBackupName Der Name des aktuellen Backups
     * @param versionString Die Versionszeichenkette für die Suche
     */
    default void cleanupOldBackups(Path backupDir, String currentBackupName, String versionString) {
        Logger logger = LoggerUtility.getLogger(getClass());
        try {
            String prefixToDelete = getClass().getSimpleName() + "-" + versionString;
            
            Files.list(backupDir)
                .filter(path -> {
                    String filename = path.getFileName().toString();
                    return filename.startsWith(prefixToDelete) && 
                           filename.endsWith(".zip") &&
                           !filename.equals(currentBackupName);
                })
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        logger.info("Älteres Backup gelöscht: " + path);
                    } catch (IOException e) {
                        logger.warning("Konnte älteres Backup nicht löschen: " + path + ": " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            logger.warning("Fehler beim Bereinigen älterer Backups: " + e.getMessage());
        }
    }
    
    /**
     * Prüft auf verfügbare Updates für den Server.
     * @return UpdateInfo-Objekt mit Informationen zu verfügbaren Updates, oder null wenn keine Updates verfügbar sind
     */
    default dev.cubie.CubeServerTool.Model.UpdateInfo checkForUpdates() {
        Logger logger = LoggerUtility.getLogger(getClass());
        logger.info("Prüfe auf Updates... (Standardimplementierung - keine Update-Prüfung konfiguriert)");
        return null;
    }
    
    /**
     * Startet den Server.
     * Standardimplementierung sucht nach der Server-JAR-Datei und startet sie.
     */
    default void start() {
        Logger logger = LoggerUtility.getLogger(getClass());
        logger.info("Starte Server " + getInstallerName() + "...");
        
        try {
            // Suche nach der Server-JAR-Datei
            Pattern jarPattern = getStartFile();
            Optional<Path> serverJar = Files.list(Config.rootFolder)
                .filter(path -> jarPattern.matcher(path.getFileName().toString()).matches())
                .findFirst();
                
            if (!serverJar.isPresent()) {
                logger.severe("Keine passende Server-JAR-Datei gefunden in: " + Config.rootFolder);
                logger.info("Erwartetes Dateimuster: " + jarPattern.pattern());
                logger.info("Starte automatische Installation...");
                try {
                    install(); // Installation direkt starten
                    // Nach der Installation erneut versuchen zu starten
                    start();
                } catch (Exception e) {
                    logger.severe("Fehler während der automatischen Installation: " + e.getMessage());
                }
                return;
            }
            
            String jarFileName = serverJar.get().getFileName().toString();
            logger.info("Gefundene Server-JAR: " + jarFileName);
            
            // Erstelle oder aktualisiere eula.txt
            Path eulaFile = Config.rootFolder.resolve("eula.txt");
            try {
                String eulaContent;
                boolean needsUpdate = false;
                
                if (Files.exists(eulaFile)) {
                    // Lese bestehende Datei
                    eulaContent = new String(Files.readAllBytes(eulaFile), StandardCharsets.UTF_8);
                    
                    // Prüfe ob eula=false gesetzt ist und ersetze es
                    if (eulaContent.contains("eula=false")) {
                        logger.info("Aktualisiere eula.txt (eula=false -> eula=true)...");
                        eulaContent = eulaContent.replace("eula=false", "eula=true");
                        needsUpdate = true;
                    }
                } else {
                    // Erstelle neue eula.txt
                    logger.info("Erstelle eula.txt...");
                    eulaContent = "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\n" +
                               "#" + java.time.LocalDateTime.now().toString() + "\n" +
                               "eula=true";
                    needsUpdate = true;
                }
                
                // Schreibe die Datei nur wenn nötig
                if (needsUpdate) {
                    try (BufferedWriter writer = Files.newBufferedWriter(eulaFile, StandardCharsets.UTF_8)) {
                        writer.write(eulaContent);
                    }
                    logger.info("eula.txt erfolgreich aktualisiert.");
                }
            } catch (IOException e) {
                logger.warning("Fehler beim Verarbeiten der eula.txt: " + e.getMessage());
            }
            
            // Starte den Server mit ProcessHandler und Java-Versionsprüfung
            ProcessHandler processHandler = ProcessHandler.create(jarFileName)
                .addParameter("nogui")
                .useConsole(true)
                .workDir(Config.rootFolder)
                .useLogger("info")
                .checkJavaVersion(true);
                
            logger.info("Starte Serverprozess...");
            Process process = processHandler.start();
            
            // Warte auf das Ende des Prozesses
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Server wurde ordnungsgemäß beendet.");
            } else {
                logger.warning("Server wurde mit Fehlercode " + exitCode + " beendet.");
            }
            
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("This application requires Java")) {
                logger.severe("Fehler beim Starten des Servers: " + e.getMessage());
            } else {
                logger.severe("Fehler beim Starten des Servers: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            logger.severe("Fehler beim Starten des Servers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get Logger instance from LoggerUtility
    static final Logger logger = LoggerUtility.getLogger(CubeServerModule.class);

    static List<CubeServerModule> loadInternalInstallers() {
        List<CubeServerModule> installers = new ArrayList<>();

        try {
            String packageName = "dev.cubie.CubeServerTool.Modules";
            String path = packageName.replace('.', '/');
            URL resource = CubeServerTool.class.getClassLoader().getResource(path);

            if (resource != null) {
                if (resource.getProtocol().equals("file")) {
                    // Läuft in einer Entwicklungsumgebung
                    File directory = new File(resource.toURI());
                    if (directory.exists()) {
                        for (File subdir : directory.listFiles(File::isDirectory)) {
                            String subdirName = subdir.getName();
                            File classFile = new File(subdir, subdirName + ".class");
                            if (classFile.exists()) {
                                String className = packageName + '.' + subdirName + '.' + subdirName;
                                Class<?> clazz = Class.forName(className);
                                if (CubeServerModule.class.isAssignableFrom(clazz)) {
                                    CubeServerModule installer = (CubeServerModule) clazz.getDeclaredConstructor().newInstance();
                                    installers.add(installer);
                                    logger.info("Loaded internal installer: " + subdirName);
                                }
                            }
                        }
                    }
                } else if (resource.getProtocol().equals("jar")) {
                    // Läuft innerhalb einer JAR-Datei
                    String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                    try (JarFile jarFile = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String entryName = entry.getName();
                            if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                                String relativePath = entryName.substring(path.length() + 1);
                                int slashIndex = relativePath.indexOf('/');
                                if (slashIndex != -1) {
                                    String subdirName = relativePath.substring(0, slashIndex);
                                    String className = packageName + '.' + subdirName + '.' + subdirName;
                                    if (relativePath.equals(subdirName + "/" + subdirName + ".class")) {
                                        Class<?> clazz = Class.forName(className);

                                        // Prüfen, ob die Klasse ein CubeServerModule ist
                                        if (CubeServerModule.class.isAssignableFrom(clazz)) {
                                            // Prüfen, ob die Variable 'isPublic' existiert
                                            boolean loadClass = false;
                                            try {
                                                // Reflektiere die Variable 'isPublic'
                                                java.lang.reflect.Field isPublicField = clazz.getField("isPublic");

                                                // Überprüfe den Wert der statischen Variable 'isPublic'
                                                boolean isPublic = isPublicField.getBoolean(null); // Null, da es eine statische Variable ist

                                                // Lade nur, wenn 'isPublic' true ist
                                                if (isPublic) {
                                                    loadClass = true;
                                                }
                                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                                // Falls die Variable nicht existiert oder nicht zugänglich ist, Klasse nicht laden
                                                logger.warning("Field 'isPublic' not found or not accessible in class: " + className);
                                            }

                                            // Wenn die Klasse geladen werden soll
                                            if (loadClass) {
                                                CubeServerModule installer = (CubeServerModule) clazz.getDeclaredConstructor().newInstance();
                                                installers.add(installer);
                                                logger.info("Loaded internal installer: " + className);
                                            } else {
                                                logger.info("Skipped internal installer: " + className + " (isPublic is false or missing)");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return installers;
    }

    static void loadExternalJars(Path directoryPath, List<CubeServerModule> installers) {
        logger.info("Scanning directory: \"" + directoryPath.toAbsolutePath() + "\" ...");

        if (Files.exists(directoryPath) && Files.isDirectory(directoryPath)) {
            try {
                // Stream all the JAR files in the directory
                Files.list(directoryPath)
                        .filter(path -> path.toString().endsWith(".jar"))  // Filter for JAR files
                        .forEach(jar -> loadSingleJar(jar.toFile(), installers));  // Convert to File and load

            } catch (IOException e) {
                logger.severe("Error reading directory: \"" + directoryPath + "\". " + e.getMessage());
            }
        } else {
            logger.warning("Directory does not exist or is not a directory: \"" + directoryPath.toAbsolutePath() + "\".");
        }
    }

    static void loadSingleJar(File jar, List<CubeServerModule> installers) {
        String status = "loaded"; // Default status
        CubeServerModule externalInstaller = null;

        try {
            externalInstaller = loadInstallerFromJar(jar);
            if (externalInstaller != null) {
                String externalClassName = externalInstaller.getClass().getName();

                if (externalClassName.startsWith("dev.cubie.CubeServerTool.Modules")) {
                    // Prüfen, ob die Variable 'isPublic' existiert und true ist
                    boolean loadClass = false;
                    try {
                        Class<?> clazz = externalInstaller.getClass();

                        // Reflektiere die Variable 'isPublic'
                        java.lang.reflect.Field isPublicField = clazz.getField("isPublic");

                        // Überprüfe den Wert der statischen Variable 'isPublic'
                        boolean isPublic = isPublicField.getBoolean(null); // Null, da es eine statische Variable ist

                        // Lade nur, wenn 'isPublic' true ist
                        if (isPublic) {
                            loadClass = true;
                        } else {
                            status = "skipped (isPublic is false)";
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        // Falls die Variable nicht existiert oder nicht zugänglich ist, Klasse nicht laden
                        status = "skipped (isPublic not found or not accessible)";
                        logger.warning("Field 'isPublic' not found or not accessible in class: " + externalClassName);
                    }

                    // Wenn die Klasse geladen werden soll
                    if (loadClass) {
                        status = handleJarReplacement(externalInstaller, externalClassName, installers);
                    }
                } else {
                    status = "skipped (not in the allowed package)";
                }
            } else {
                status = "skipped (unable to load Main-Class correctly)";
            }
        } catch (Exception e) {
            status = "skipped (error loading class)";
            logger.severe("Error loading installer from JAR: " + jar.getName());
            e.printStackTrace();
        }

        // Log final status
        logger.info("Found external JAR: " + jar.getName() + " - " + status);
    }


    static String handleJarReplacement(CubeServerModule externalInstaller, String externalClassName, List<CubeServerModule> installers) {
        boolean replaced = false;
        for (int i = 0; i < installers.size(); i++) {
            CubeServerModule internalInstaller = installers.get(i);
            String internalClassName = internalInstaller.getClass().getName();

            if (internalClassName.equals(externalClassName)) {
                installers.set(i, externalInstaller);
                replaced = true;
                return "overwrite (" + internalInstaller.getClass().getSimpleName() + ")";
            }
        }

        if (!replaced) {
            installers.add(externalInstaller);
        }
        return "loaded";
    }


    static CubeServerModule loadInstallerFromJar(File jarFile) throws Exception {
        URL jarUrl = jarFile.toURI().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, CubeServerTool.class.getClassLoader())) {

            // Öffne das JAR und lese die Manifest-Datei
            try (JarFile jar = new JarFile(jarFile)) {
                Manifest manifest = jar.getManifest();
                Attributes attrs = manifest.getMainAttributes();
                String className = attrs.getValue("Main-Class");

                if (className != null) {
                    //System.out.println("Loading class: " + className + " from " + jarFile.getName());
                    //System.out.print("loading, ");
                    Class<?> clazz = Class.forName(className, true, classLoader);
                    return (CubeServerModule) clazz.getDeclaredConstructor().newInstance();
                } else {
                    //System.out.println("No Main-Class attribute found in " + jarFile.getName());
                    return null; // Keine Main-Class angegeben
                }
            }
        } catch (ClassNotFoundException | ClassCastException e) {
            // Ungültige JAR-Datei oder Klasse kann nicht geladen werden
            return null;
        } catch (Exception e) {
            // Allgemeine Fehlerbehandlung für ungültige JAR-Dateien
            e.printStackTrace();
            return null;
        }
    }


}


