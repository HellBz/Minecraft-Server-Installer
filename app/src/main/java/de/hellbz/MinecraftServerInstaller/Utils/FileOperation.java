package de.hellbz.MinecraftServerInstaller.Utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.logging.Logger;

public class FileOperation {

    private final int responseCode;
    private final Object content;
    private final Object additionalData;

    // Constructor
    public FileOperation(int responseCode, Object content, Object additionalData) {
        this.responseCode = responseCode;
        this.content = content;
        this.additionalData = additionalData;
    }

    // Getter methods
    public int getResponseCode() {
        return responseCode;
    }

    public Object getContent() {
        return content;
    }

    public Object getAdditionalData() {
        return additionalData;
    }

    // Logger initialization
    private static final Logger logger = LoggerUtility.getLogger(FileOperation.class);

    /**
     * Calls a URL without waiting for a response.
     *
     * @param urlString The URL as a String.
     * @return A FileOperation object with the status of the operation.
     */
    public static FileOperation callUrlWithoutResponse(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds timeout
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Successfully called URL: " + urlString);
                return new FileOperation(responseCode, "URL successfully called, response ignored.", null);
            } else {
                logger.severe("Failed to call URL: " + urlString + " - Server returned an error.");
                return new FileOperation(responseCode, null, "Server returned an error.");
            }
        } catch (IOException e) {
            logger.severe("URL call failed: " + e.getMessage());
            return new FileOperation(500, null, "URL call failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Reads content from a local file or a URL and optionally downloads the file.
     *
     * @param source          File or URL path.
     * @param destinationPath Optional path where the content should be saved.
     * @return A FileOperation object with the file content or download status.
     */
    public static FileOperation readOrDownloadFile(String source, String destinationPath) {
        boolean isUrl = source.toLowerCase().startsWith("http://") || source.toLowerCase().startsWith("https://");

        try (InputStream in = isUrl ? new URL(source).openStream() : new FileInputStream(source)) {
            logger.info("Reading file: " + source);
            return handleFileContent(in, destinationPath);
        } catch (IOException e) {
            logger.severe("File operation failed: " + e.getMessage());
            return new FileOperation(500, null, "File operation failed: " + e.getMessage());
        }
    }

    /**
     * Reads content from a file with cache support.
     *
     * @param source          URL or file path.
     * @param destinationPath Local path to save or check the file.
     * @param maxAge          Maximum age of the file cache in milliseconds.
     * @return A FileOperation object with the status and content or error info.
     */
    public static FileOperation readFileWithCache(String source, String destinationPath, long maxAge) {
        File file = new File(destinationPath);
        if (file.exists() && (System.currentTimeMillis() - file.lastModified() < maxAge)) {
            logger.info("Using cached file: " + destinationPath);
            return readOrDownloadFile(file.getAbsolutePath(), null);
        }
        logger.info("Downloading new file: " + source);
        return readOrDownloadFile(source, destinationPath);
    }

    /**
     * Replaces occurrences of a target string with a replacement string in a file.
     *
     * @param filePath    The path of the file.
     * @param target      The string to replace.
     * @param replacement The string to replace the target with.
     * @return FileOperation The result of the operation with the modified content and status.
     */
    public static FileOperation replaceInFile(String filePath, String target, String replacement) {
        try {
            // Read the content
            String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);

            // Replace target
            String modifiedContent = content.replace(target, replacement);

            // Write the modified content back to the file
            Files.write(Paths.get(filePath), modifiedContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

            logger.info("Content replaced in file: " + filePath);
            return new FileOperation(200, modifiedContent, "Content replaced and file saved successfully.");
        } catch (IOException e) {
            logger.severe("Error replacing content: " + e.getMessage());
            return new FileOperation(500, null, "Error replacing content: " + e.getMessage());
        }
    }

    // Private helper methods

    /**
     * Handles reading content from an InputStream and optionally saves the content to a file.
     *
     * @param in             InputStream to read from.
     * @param destinationPath Optional path to save the content.
     * @return A FileOperation object with the file content or save status.
     * @throws IOException In case of reading or writing errors.
     */
    private static FileOperation handleFileContent(InputStream in, String destinationPath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }

        if (destinationPath != null && !destinationPath.isEmpty()) {
            Files.write(Paths.get(destinationPath), content.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("File saved to: " + destinationPath);
            return new FileOperation(200, content.toString(), "File downloaded and saved.");
        } else {
            return new FileOperation(200, content.toString(), "Read content from input stream.");
        }
    }

    // Main method for testing
    public static void main(String[] args) {
        logger.info("Starting FileOperation tests...");

        // Test replacing content in a file
        FileOperation replaceResult = replaceInFile("example.txt", "oldText", "newText");
        System.out.println(replaceResult.getAdditionalData());

        // Test downloading a remote file
        FileOperation downloadResult = readOrDownloadFile("https://example.com/remote_file.txt", "remote_file.txt");
        if (downloadResult.getResponseCode() == 200) {
            logger.info("Downloaded file content: " + downloadResult.getContent());
        } else {
            logger.severe("Failed to download file. Response code: " + downloadResult.getResponseCode());
        }

        // Test reading from a file with cache support
        FileOperation cacheResult = readFileWithCache("https://example.com/remote_file.txt", "cached_file.txt", 60000);
        System.out.println("Cache result: " + cacheResult.getAdditionalData());

        logger.info("FileOperation tests completed.");

        // Testen des Speicherns und Lesens von Remote-Dateien mit caching file
        FileOperation remoteSaveReadResultCache = readFileWithCache("https://api.curseforge.com/v1/minecraft/modloader/?version=1.20.4", "ModLoaderCache.json", 60000 );
        if (remoteSaveReadResultCache.getResponseCode() == 200) {
            logger.info("Datei erfolgreich gespeichert: " + remoteSaveReadResultCache.getAdditionalData());
        } else {
            logger.severe("Fehler beim Lesen der Remote-Datei. Response-Code: " + remoteSaveReadResultCache.getResponseCode());
            logger.severe("Zusätzliche Informationen: " + remoteSaveReadResultCache.getAdditionalData());
        }


    }


}
