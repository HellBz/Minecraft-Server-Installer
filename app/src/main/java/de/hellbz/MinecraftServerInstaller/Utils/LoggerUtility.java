package de.hellbz.MinecraftServerInstaller.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerUtility {

    private static Logger logger;
    private static boolean detailedLog = false; // Default: false

    // Standardwerte für LogLevel
    private static Level logLevelConsole = Level.INFO; // Default log level
    private static Level logLevelCSV = Level.FINE;     // Default CSV log level
    private static boolean logToFile = false;           // Default log to file

    // Get the logger instance
    public static Logger getLogger(Class<?> clazz) {
        if (logger == null) {
            logger = Logger.getLogger(clazz.getName());
            configureLogger();  // Configure with default values
        }
        return logger;
    }

    // Configure logger with current settings
    public static void configureLogger() {
        logger.setLevel(Level.ALL);  // Capture all logs
        logger.setUseParentHandlers(false);  // Disable default handlers

        // Console handler with default or configured log level
        ConsoleHandler consoleHandler = new ConsoleHandler() {
            @Override
            public void publish(LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }

                String logLevelName = record.getLevel().getLocalizedName();

                // Colorize log levels for console
                if (record.getLevel() == Level.FINE) {
                    logLevelName = ConsoleColors.CYAN + logLevelName + ConsoleColors.RESET;
                } else if (record.getLevel() == Level.SEVERE) {
                    logLevelName = ConsoleColors.RED + logLevelName + ConsoleColors.RESET;
                } else if (record.getLevel() == Level.WARNING) {
                    logLevelName = ConsoleColors.YELLOW + logLevelName + ConsoleColors.RESET;
                } else if (record.getLevel() == Level.INFO) {
                    logLevelName = ConsoleColors.GREEN + logLevelName + ConsoleColors.RESET;
                } else if (record.getLevel() == Level.CONFIG) {
                    logLevelName = ConsoleColors.CYAN + logLevelName + ConsoleColors.RESET;
                }

                // Format the message for console output
                String formattedMessage = String.format(
                        ConsoleColors.RESET + "[%1$tF %1$tT] " + logLevelName + ": %2$s" + ConsoleColors.RESET,
                        record.getMillis(),
                        record.getMessage()
                );
                System.out.println(formattedMessage);

                // Display detailed log info if enabled
                if (detailedLog) {
                    String debugInfo = String.format(
                            ConsoleColors.RESET + "[%1$tF %1$tT] " + ConsoleColors.CYAN + "DETAIL" + ConsoleColors.RESET + ": ╙► " + ConsoleColors.CYAN + "%2$s" + ConsoleColors.RESET + " %3$s",
                            record.getMillis(),
                            record.getSourceClassName(),
                            record.getSourceMethodName()
                    );
                    System.out.println(debugInfo);
                }
            }
        };
        consoleHandler.setLevel(logLevelConsole);  // Set console log level
        logger.addHandler(consoleHandler);

        // FileHandler for CSV logs if enabled
        if (logToFile) {
            try {
                CSVFileHandler csvFileHandler = new CSVFileHandler(Config.logFilePath);
                csvFileHandler.setLevel(logLevelCSV);  // Set CSV log level
                logger.addHandler(csvFileHandler);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create CSV log handler", e);
            }
        }
    }

    // Method to update logger configuration dynamically
    public static void updateLoggerConfig(boolean newDetailedLog, String newLogLevelConsole, String newLogLevelCSV, boolean newLogToFile) {

        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }

        detailedLog = newDetailedLog;
        logLevelConsole = convertLogLevel(newLogLevelConsole);
        logLevelCSV = convertLogLevel(newLogLevelCSV);
        logToFile = newLogToFile;

        // Reconfigure the logger with new settings
        configureLogger();
    }

    // Helper method to convert string log levels to Level
    private static Level convertLogLevel(String logLevel) {
        switch (logLevel.toUpperCase()) {
            case "DEBUG":
                return Level.FINE;
            case "INFO":
                return Level.INFO;
            case "WARNING":
                return Level.WARNING;
            case "SEVERE":
                return Level.SEVERE;
            case "CONFIG":
                return Level.CONFIG;
            default:
                System.err.println("Unknown log level: " + logLevel + ". Defaulting to INFO.");
                return Level.INFO;
        }
    }

    // CSVFileHandler for CSV formatting
    public static class CSVFileHandler extends Handler {
        private final FileWriter writer;

        public CSVFileHandler(String fileName) throws IOException {
            Path path = Paths.get(fileName);
            this.writer = new FileWriter(path.toFile(), true);  // Append to the file

            // Write CSV header if the file is new
            if (path.toFile().length() == 0) {
                writer.append("Timestamp,Log Level,Message,Class,Method\n");
            }
        }

        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }

            try {
                String logEntry = String.format(
                        "%1$tF %1$tT,%2$s,%3$s,%4$s,%5$s%n",
                        record.getMillis(),
                        record.getLevel(),
                        record.getMessage().replaceAll("\\x1b\\[[\\d;]*m", "").replaceAll(",", ""),  // Remove colors and commas
                        record.getSourceClassName(),
                        record.getSourceMethodName()
                );
                writer.append(logEntry);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void flush() {
            try {
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void close() throws SecurityException {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to enable or disable detailed logging
    public static void setDetailedLog(boolean enabled) {
        detailedLog = enabled;
    }

    public static void main(String[] args) {
        // Testing LoggerUtility
        Logger logger = LoggerUtility.getLogger(LoggerUtility.class);

        // Test logging with various levels
        logger.info("This is an info message.");
        logger.warning("This is a warning message.");
        logger.severe("This is a severe error message.");

        // Enable detailed logging
        LoggerUtility.setDetailedLog(true);
        logger.info("Detailed logging enabled.");
        logger.fine("This is a fine (debug) message.");

        // Example of formatting with colors
        logger.info("Dies ist eine toll " +
                ConsoleColors.RED + "f" +
                ConsoleColors.ORANGE + "o" +
                ConsoleColors.YELLOW + "r" +
                ConsoleColors.GREEN + "m" +
                ConsoleColors.BLUE + "a" +
                ConsoleColors.INDIGO + "t" +
                ConsoleColors.VIOLET + "i" +
                ConsoleColors.RED + "e" +
                ConsoleColors.ORANGE + "r" +
                ConsoleColors.YELLOW + "t" +
                ConsoleColors.GREEN + "e" +
                ConsoleColors.RESET + " Info.");

        // Simulate an exception log
        try {
            throw new Exception("This is a test exception");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An exception occurred", e);
        }
    }
}
