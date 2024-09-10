package de.hellbz.MinecraftServerInstaller.Utils;

import java.io.IOException;
import java.util.logging.Logger;

public class Config {

    // Get Logger instance from LoggerUtility
    private static final Logger logger = LoggerUtility.getLogger(Config.class);
    private static boolean isConfigLoaded = false;  // Flag to check if config is loaded


    // Define the base data folder
    public static String dataFolder = "msi_data";  // This can be adjusted as needed

    // Define subfolders for better structure
    public static String configFolder = dataFolder + "/config";
    public static String logFolder = dataFolder + "/logs";
    public static String modulesFolder = dataFolder + "/modules";
    public static String tempFolder = dataFolder + "/temp";

    // Define paths based on the subfolders
    public static String configFilePath = configFolder + "/msi.conf";
    public static String logFilePath = logFolder + "/latest-log.csv";

    // Configurable settings loaded from config
    public static boolean logToFile = false;
    public static String logLevelConsole = "DEBUG";
    public static String logLevelCSV = "DEBUG";
    public static boolean detailedLog = false;

    // Method to ensure the config is loaded
    private static void ensureConfigLoaded() {
        if (!isConfigLoaded) {
            loadConfig();
        }
    }

    // Static method to manually load the config
    public static void loadConfig() {
        try {
            logger.info("Loading Configuration.");
            ConfigHandler.loadConfig(configFilePath);  // Load the configuration
        } catch (IOException e) {
            logger.warning("Error loading configuration. Using default settings.");
        }
    }
}
