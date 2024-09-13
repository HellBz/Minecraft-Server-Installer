package de.hellbz.MinecraftServerInstaller.Utils;

import de.hellbz.MinecraftServerInstaller.MainInstaller;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ProcessHandler {

    private Process process;
    private BufferedReader reader;
    private OutputStream writer;

    // Startet den Prozess mit den angegebenen Befehlen und Umgebungsvariablen
    public void startProcess(String command, Map<String, String> envVars) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        if (envVars != null) {
            Map<String, String> environment = builder.environment();
            environment.putAll(envVars);
        }
        process = builder.start();
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        writer = process.getOutputStream();
    }

    // Methode in ProcessHandler
    public String readProcessLine() throws IOException {
        return reader.readLine();  // Liest eine Zeile aus dem Prozess
    }

    // Überprüft, ob der Prozess läuft
    public boolean isRunning() {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    // Schließt den Prozess
    public void stopProcess() throws IOException {
        if (process != null) {
            process.destroy();
            writer.close();
            reader.close();
        }
    }

    // Sendet einen Befehl an den laufenden Prozess
    public void sendCommand(String command) throws IOException {
        if (process != null && writer != null) {
            writer.write((command + "\n").getBytes());
            writer.flush();
        }
    }

    // Gibt den Output des Prozesses zurück
    public String readProcessOutput() throws IOException {
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }

    // Liest den InputStream des Prozesses
    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }

    // Gibt den Fehler-Output des Prozesses zurück
    public String readErrorStream() throws IOException {
        return readStream(process.getErrorStream());
    }

    // Gibt den Exit-Code des Prozesses zurück
    public int getExitCode() throws InterruptedException {
        return process.waitFor();
    }

    public static void main(String[] args) {
        // Erstelle einen neuen ProcessHandler
        ProcessHandler handler = new ProcessHandler();

        // Initialize LoggerUtility after the config is loaded
        Logger logger = LoggerUtility.getLogger(ProcessHandler.class);

        try {



            // Starte die JAR-Datei (Pfad zur JAR-Datei angeben)
            System.out.println("Starting JAR file...");
            handler.startProcess("java -jar forge-1.20.6-50.1.19-installer.jar nogui --installServer", null);

            // Starte einen Thread, um den Prozess-Output kontinuierlich zu lesen
            Thread outputThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = handler.readProcessLine()) != null) {
                        LoggerUtility.install( line.toString() ); // Gib die Zeile des Prozesses aus
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            outputThread.start();

            // Lies die Ausgabe während der Installation
            String processOutput = handler.readProcessOutput();
            System.out.println("Process Output: " + processOutput);

            // Warten auf das Ende des Prozesses und den Exit-Code abfragen
            int exitCode = handler.getExitCode();
            if (exitCode == 0) {
                System.out.println("Installation completed successfully with exit code: " + exitCode);
            } else {
                System.out.println("Installation failed with exit code: " + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Das aktuelle Verzeichnis
            Path dir = Paths.get("./");
            // Example: Recursive listing with backup and deletion in dry run mode
            DirectoryLister lister = new DirectoryLister(dir);
            // Recursive listing
            lister.excludeDirectory("libraries");
            lister.includeFile("README.txt");
            lister.includeFile("run.([bat|sh]+)");
            lister.includeFile("forge-([.0-9]+)-([.0-9]+)-(shim|universal|installer).([jar|zip|jar.log]+)");
            lister.list();
            //lister.enableDryRun();
            lister.delete();
            try {
                handler.stopProcess();
            } catch (Exception e) {
                System.err.println("Error stopping the process: " + e.getMessage());
            }
        }
    }
}
