package de.hellbz.MinecraftServerInstaller.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigHandler {

    // Get Logger instance from LoggerUtility
    private static final Logger logger = LoggerUtility.getLogger(ConfigHandler.class);

    private static Properties properties = new Properties();  // Store loaded properties

    // Method to load properties from the config file
    public static void loadConfig(String filePath) throws IOException {

        logger.info("Configuration loaded.");
        Path configFilePath = Paths.get(filePath);

        if (Files.exists(configFilePath)) {
            properties.load(Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8));

            // Set static variables in Config from properties
            Config.logToFile = Boolean.parseBoolean(properties.getProperty("logToFile", "true"));
            Config.logLevelConsole = properties.getProperty("logLevelConsole", "INFO");
            Config.logLevelCSV = properties.getProperty("logLevelCSV", "DEBUG");
            Config.detailedLog = Boolean.parseBoolean(properties.getProperty("detailedLog", "false"));


            logger.config("Configuration loaded from: " + configFilePath.toAbsolutePath());
            LoggerUtility.updateLoggerConfig(Config.detailedLog, Config.logLevelConsole, Config.logLevelCSV, Config.logToFile);


        } else {
            logger.warning("Configuration file not found. Creating a default one.");
            createDefaultConfig(filePath);
            loadConfig(filePath);  // Reload the config after creating default
        }
    }

    // Method to copy the default config from resources to msi_data/config
    public static void createDefaultConfig(String destinationPath) throws IOException {
        Path configDirPath = Paths.get(Config.configFolder);

        // Create the directory if it doesn't exist
        if (!Files.exists(configDirPath)) {
            Files.createDirectories(configDirPath);
        }

        Path configFilePath = configDirPath.resolve("msi.conf");

        // Check if the config file already exists
        if (!Files.exists(configFilePath)) {
            // Load the default config from resources
            InputStream defaultConfigStream = ConfigHandler.class.getClassLoader().getResourceAsStream("default-msi.conf");

            if (defaultConfigStream == null) {
                throw new FileNotFoundException("Default configuration file not found in resources.");
            }

            // Copy the default config to the destination directory
            Files.copy(defaultConfigStream, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            logger.config("Default configuration file copied to: " + configFilePath.toAbsolutePath());
        } else {
            logger.config("Configuration file already exists at: " + configFilePath.toAbsolutePath());
        }
    }

    // Update or add a property in the config file
    public static void updateProperty(String propertiesFilePath, String key, String newValue) throws IOException {
        Properties properties = new Properties();
        Path filePath = Paths.get(propertiesFilePath);

        if (Files.exists(filePath)) {
            properties.load(Files.newBufferedReader(filePath, StandardCharsets.UTF_8));
        }

        properties.setProperty(key, newValue);

        // Save updated properties back to file
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            properties.store(writer, "Updated " + key);
        }

        logger.config("Property updated: " + key + " = " + newValue);
    }
}
