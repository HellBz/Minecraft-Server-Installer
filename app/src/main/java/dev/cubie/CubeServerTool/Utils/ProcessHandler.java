package dev.cubie.CubeServerTool.Utils;

import dev.cubie.CubeServerTool.Data.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class ProcessHandler {

    private String jarFile;
    private List<String> parameters = new ArrayList<>();  // Server-specific arguments
    private List<String> jvmArguments = new ArrayList<>();  // JVM-specific arguments
    private boolean useConsole = false;  // Controls whether to use the console output
    private String workDir;  // Holds the working directory, defaults to Config.rootFolder
    private String loggerType = "default";  // Controls logging, independent of console output
    private boolean disableInput = false;  // Option to disable input
    private boolean checkJavaVersion = false;  // Whether to check Java version
    private Process process;  // Reference to the running process

    // Initialize logger
    private static final Logger logger = LoggerUtility.getLogger(ProcessHandler.class);

    // Static method to initiate builder pattern
    public static ProcessHandler create(String jarFile) {
        ProcessHandler handler = new ProcessHandler(jarFile);
        handler.populateJvmArguments();  // Automatically populate JVM arguments
        return handler;
    }

    // Constructor
    public ProcessHandler(String jarFile) {
        this.jarFile = jarFile;
        // By default, the workDir is set to Config.rootFolder (if not explicitly set)
        this.workDir = Config.rootFolder.toString();  // Uses Config.rootFolder as default
    }

    // Automatically populate JVM arguments from the current process
    private void populateJvmArguments() {
        // Get the JVM arguments passed to the current Java process
        List<String> jvmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

        // Create a map to store JVM arguments (to allow overriding)
        Map<String, String> jvmArgsMap = new HashMap<>();

        // Populate the map with default JVM arguments from the current process
        for (String arg : jvmArguments) {
            if (arg.startsWith("-")) {
                String[] splitArg = arg.split("=", 2);
                jvmArgsMap.put(splitArg[0], splitArg.length > 1 ? splitArg[1] : "");
            }
        }

        // Add the JVM arguments from the map to the ProcessHandler
        for (Map.Entry<String, String> entry : jvmArgsMap.entrySet()) {
            this.addJvmArgument(entry.getKey() + (entry.getValue().isEmpty() ? "" : "=" + entry.getValue()));
        }
    }

    // Add JVM-specific arguments (e.g., "-Xmx1024M")
    public ProcessHandler addJvmArgument(String jvmArg) {
        this.jvmArguments.add(jvmArg);
        return this;
    }

    // Add server-specific parameters (e.g., "nogui")
    public ProcessHandler addParameter(String param) {
        this.parameters.add(param);
        return this;
    }

    // Use console output (true or false)
    public ProcessHandler useConsole(boolean useConsole) {
        this.useConsole = useConsole;
        return this;
    }

    // Set the working directory using a Path object
    public ProcessHandler workDir(Path workDir) {
        // If a specific workDir is provided, use it; otherwise, default to Config.rootFolder
        this.workDir = (workDir != null) ? workDir.toAbsolutePath().normalize().toString() : Config.rootFolder.toString();
        return this;
    }

    // Method to disable console input
    public ProcessHandler disableInput() {
        this.disableInput = true;
        return this;  // Return this to allow method chaining
    }

    // Start the process
    public Process start() throws IOException {
        // Check Java version if enabled
        if (checkJavaVersion) {
            Path jarPath = new File(workDir, jarFile).toPath();
            int requiredVersion = getRequiredJavaVersion(jarPath);

            if (requiredVersion > 0) {
                logMessage("Required Java version: " + requiredVersion);

                if (!isJavaVersionSufficient(requiredVersion)) {
                    throw new IOException("This application requires Java " + requiredVersion +
                            " or higher, but you have Java " + System.getProperty("java.version"));
                }
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand());

        // Set the working directory (use Config.rootFolder if not set)
        processBuilder.directory(new File(workDir));

        // Konsoleingabe steuern (aktiviert oder deaktiviert je nach Anforderung)
        if (disableInput) {
            processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);  // Eingabe deaktivieren
        } else {
            processBuilder.inheritIO();  // Eingabe und Ausgabe in die Konsole umleiten
        }

        process = processBuilder.start();  // Start the process and save the reference

        // Ausgabe manuell verarbeiten, wenn Konsole nicht verwendet wird
        if (!useConsole) {
            handleProcessOutput(process);
        }

        return process;
    }


    // Method to retrieve the running process
    public Process getProcess() {
        return this.process;
    }

    // Method to stop the process
    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();  // Stop the process
            logger.info("Process stopped.");
        }
    }

    // Method to send a command to the running process
    public void sendCommand(String command) throws IOException {
        if (process != null && process.isAlive()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write((command + "\n").getBytes());
                os.flush();
            }
        }
    }

    // Handle the output of the process if useConsole is set to false
    private void handleProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                logMessage(line);  // Log the standard output
            }

            while ((line = errorReader.readLine()) != null) {
                logMessage("[ERROR] " + line);  // Log the error output
            }
        }
    }

    // Method to set the logger type
    public ProcessHandler useLogger(String uselogger) {
        // Set the loggerType based on the parameter passed to this method
        this.loggerType = uselogger;
        return this;  // Return this to allow method chaining
    }
    
    // Enable or disable Java version checking
    public ProcessHandler checkJavaVersion(boolean check) {
        this.checkJavaVersion = check;
        return this;
    }
    
    /**
     * Gets the minimum Java version required for a JAR file
     * @param jarPath Path to the JAR file
     * @return Required Java version or -1 if not determinable
     */
    private int getRequiredJavaVersion(Path jarPath) {
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath.toFile())) {
            // Check the main class of the JAR
            String mainClass = jarFile.getManifest()
                .getMainAttributes()
                .getValue("Main-Class");
                
            if (mainClass != null) {
                // Convert the class name to a path
                String classPath = mainClass.replace('.', '/') + ".class";
                java.util.jar.JarEntry entry = jarFile.getJarEntry(classPath);
                
                if (entry != null) {
                    try (java.io.InputStream is = jarFile.getInputStream(entry)) {
                        // The first 8 bytes of the class file contain version information
                        byte[] header = new byte[8];
                        if (is.read(header) == header.length) {
                            // Java version is stored in bytes 7-8 (as hex)
                            int version = (header[7] & 0xFF) | ((header[6] & 0xFF) << 8);
                            return version - 44; // Java 1.1 = 45, Java 1.2 = 46, etc.
                        }
                    }
                }
            }
        } catch (Exception e) {
            logMessage("Could not determine Java version for " + jarPath + ": " + e.getMessage());
        }
        return -1;
    }
    
    /**
     * Checks if the current Java version is sufficient
     * @param requiredVersion Required Java version (e.g., 8 for Java 8, 17 for Java 17)
     * @return true if the version is sufficient, false otherwise
     */
    private boolean isJavaVersionSufficient(int requiredVersion) {
        if (requiredVersion <= 0) return true; // If version cannot be determined, assume it's okay
        
        String javaVersion = System.getProperty("java.version");
        int currentVersion;
        
        try {
            // Extract the main version number
            if (javaVersion.startsWith("1.")) {
                // Older versions: 1.8.0_xyz
                currentVersion = Integer.parseInt(javaVersion.split("\\D+")[1]);
            } else {
                // Newer versions: 11, 17, etc.
                currentVersion = Integer.parseInt(javaVersion.split("\\D+")[0]);
            }
            
            return currentVersion >= requiredVersion;
        } catch (Exception e) {
            logMessage("Could not determine Java version: " + javaVersion);
            return true; // If in doubt, try to proceed
        }
    }

    // Method to log messages, independent of the console output
    private void logMessage(String message) {
        switch (loggerType.toLowerCase()) {
            case "install":
                LoggerUtility.install(message);  // Log install-related messages
                break;
            case "info":
                logger.info(message);  // Log server-related messages
                break;
            case "void":  // Do nothing (no log output)
                // No logging happens, as this is the "void" mode
                break;
            case "default":
            default:
                System.out.println(message);  // Default to console output
                break;
        }
    }

    // Build the command to start the JAR file with proper separation of JVM and server arguments
    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();

        // Add Java and JVM arguments first
        command.add("java");
        command.addAll(jvmArguments);  // Add JVM arguments (e.g., -Xms, -Xmx)

        // Add the jar and any server-specific arguments
        command.add("-jar");
        command.add(jarFile);

        // Add additional parameters for the server (if any)
        command.addAll(parameters);

        return command;
    }

    // Main method to test the ProcessHandler class
    public static void main(String[] args) {
        try {
            // Test configuration for the ProcessHandler
            Process process = ProcessHandler.create("server.jar")
                    .workDir(Config.rootFolder.resolve("RUN-TEST"))
                    .addJvmArgument("-Xms512M")
                    .addParameter("nogui")
                    .useLogger("info")  // Set logger to info level
                    .useConsole(false)   // Manual output handling, input is disabled
                    //.disableInput()      // Disable user input
                    .start();

            // Wait for the process to finish and get the exit code
            int exitCode = process.waitFor();

            // Check the exit code
            if (exitCode == 0) {
                logger.info("Process completed successfully with exit code 0.");
            } else {
                logger.severe("Process completed with exit code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            logger.severe("Error starting the process: " + e.getMessage());
        }
    }
}
