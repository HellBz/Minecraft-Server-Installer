package de.hellbz.MinecraftServerInstaller;

import de.hellbz.MinecraftServerInstaller.Utils.ConfigHandler;
import de.hellbz.MinecraftServerInstaller.Utils.LoggerUtility;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

// Schnittstelle für MinecraftServerInstaller
public interface MinecraftServerInstaller {

    void init();
    String getInstallerName();
    String[] getAvailableVersions();
    String[] getAvailableSubVersions(String mainVersion);
    void install(String version, String subVersion);

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
                                        if (MinecraftServerInstaller.class.isAssignableFrom(clazz)) {
                                            MinecraftServerInstaller installer = (MinecraftServerInstaller) clazz.getDeclaredConstructor().newInstance();
                                            installers.add(installer);
                                            logger.info("Loaded internal installer: " + className);
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

    static void loadExternalJars(String directoryPath, List<MinecraftServerInstaller> installers) {
        System.out.println("Scan Dir \"" + directoryPath + "\" ...");

        File externalPluginDir = new File(directoryPath);
        if (externalPluginDir.exists() && externalPluginDir.isDirectory()) {
            File[] externalJars = externalPluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (externalJars != null && externalJars.length > 0) {
                for (File jar : externalJars) {
                    String status = "loaded"; // Standardstatus

                    MinecraftServerInstaller externalInstaller = null;
                    try {
                        externalInstaller = loadInstallerFromJar(jar);

                        if (externalInstaller != null) {
                            String externalClassName = externalInstaller.getClass().getName();

                            if (externalClassName.startsWith("de.hellbz.MinecraftServerInstallerModules")) {
                                boolean replaced = false;
                                for (int i = 0; i < installers.size(); i++) {
                                    MinecraftServerInstaller internalInstaller = installers.get(i);
                                    String internalClassName = internalInstaller.getClass().getName();

                                    if (internalClassName.equals(externalClassName)) {
                                        installers.set(i, externalInstaller);
                                        replaced = true;
                                        status = "overwrite (" + internalInstaller.getClass().getSimpleName() + ")";
                                        break;
                                    }
                                }
                                if (!replaced) {
                                    installers.add(externalInstaller);
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

                    // Ausgabe mit finalem Status
                    logger.info("Found external JAR: " + jar.getName() + " - " + status);
                }
            } else {
                logger.warning("No external JAR files found in the \"" + directoryPath + "\" directory.");
            }
        } else {
            logger.warning("The external \"" + directoryPath + "\" directory does not exist or is not a directory.");
        }
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


