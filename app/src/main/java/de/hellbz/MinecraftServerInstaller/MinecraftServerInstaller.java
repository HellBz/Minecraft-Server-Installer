package de.hellbz.MinecraftServerInstaller;

import de.hellbz.MinecraftServerInstaller.Utils.LoggerUtility;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.regex.Pattern;

// Schnittstelle für MinecraftServerInstaller
public interface MinecraftServerInstaller {

    void init();
    String getInstallerName();
    String[] getAvailableTypes();
    String[] getAvailableVersions();
    String[] getAvailableSubVersions();

    Pattern getStartFile();

    void install();
    void start();

    // Get Logger instance from LoggerUtility
    static final Logger logger = LoggerUtility.getLogger(MinecraftServerInstaller.class);

    static List<MinecraftServerInstaller> loadInternalInstallers() {
        List<MinecraftServerInstaller> installers = new ArrayList<>();

        try {
            String packageName = "de.hellbz.MinecraftServerInstallerModules";
            String path = packageName.replace('.', '/');
            URL resource = MainInstaller.class.getClassLoader().getResource(path);

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
                                if (MinecraftServerInstaller.class.isAssignableFrom(clazz)) {
                                    MinecraftServerInstaller installer = (MinecraftServerInstaller) clazz.getDeclaredConstructor().newInstance();
                                    installers.add(installer);
                                    logger.info("Loaded internal installer: " + subdirName );
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

    static void loadExternalJars(Path directoryPath, List<MinecraftServerInstaller> installers) {
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

    static void loadSingleJar(File jar, List<MinecraftServerInstaller> installers) {
        String status = "loaded"; // Default status
        MinecraftServerInstaller externalInstaller = null;

        try {
            externalInstaller = loadInstallerFromJar(jar);
            if (externalInstaller != null) {
                String externalClassName = externalInstaller.getClass().getName();
                if (externalClassName.startsWith("de.hellbz.MinecraftServerInstallerModules")) {
                    status = handleJarReplacement(externalInstaller, externalClassName, installers);
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

    static String handleJarReplacement(MinecraftServerInstaller externalInstaller, String externalClassName, List<MinecraftServerInstaller> installers) {
        boolean replaced = false;
        for (int i = 0; i < installers.size(); i++) {
            MinecraftServerInstaller internalInstaller = installers.get(i);
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


    static MinecraftServerInstaller loadInstallerFromJar(File jarFile) throws Exception {
        URL jarUrl = jarFile.toURI().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, MainInstaller.class.getClassLoader())) {

            // Öffne das JAR und lese die Manifest-Datei
            try (JarFile jar = new JarFile(jarFile)) {
                Manifest manifest = jar.getManifest();
                Attributes attrs = manifest.getMainAttributes();
                String className = attrs.getValue("Main-Class");

                if (className != null) {
                    //System.out.println("Loading class: " + className + " from " + jarFile.getName());
                    //System.out.print("loading, ");
                    Class<?> clazz = Class.forName(className, true, classLoader);
                    return (MinecraftServerInstaller) clazz.getDeclaredConstructor().newInstance();
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


