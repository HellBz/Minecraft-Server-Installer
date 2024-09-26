package de.hellbz.MinecraftServerInstaller.Utils;

import de.hellbz.MinecraftServerInstaller.Utils.LoggerUtility;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FileOperation {

    private String source;
    private Map<String, String> headers = new HashMap<>();
    private String content;
    private int responseCode;
    private static final Logger logger = LoggerUtility.getLogger(FileOperation.class);

    // Constructor
    public FileOperation(String source) {
        this.source = source;
    }

    // Method to add headers
    public FileOperation header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    // Method to fetch the file from a remote URL, local file, or resource folder
    public FileOperation fetch() {
        try {
            if (source.toLowerCase().startsWith("http://") || source.toLowerCase().startsWith("https://")) {
                fetchFromUrl();
            } else if (Files.exists(Paths.get(source))) {
                fetchFromLocalFile();
            } else {
                fetchFromResource();
            }
        } catch (IOException e) {
            logger.severe("Fetching file failed: " + e.getMessage());
            responseCode = 500;
        }
        return this;
    }

    // Method to fetch from a URL
    private void fetchFromUrl() throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(source);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Set headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            connection.setConnectTimeout(5000); // 5 seconds timeout
            connection.connect();

            responseCode = connection.getResponseCode();
            InputStream in = connection.getInputStream();

            content = readContentFromStream(in);

            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Successfully fetched URL: " + source);
            } else {
                logger.severe("Failed to fetch URL: " + source + " - Server returned an error.");
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Method to fetch from a local file
    private void fetchFromLocalFile() throws IOException {
        content = new String(Files.readAllBytes(Paths.get(source)), StandardCharsets.UTF_8);
        responseCode = 200;
        logger.info("Successfully read local file: " + source);
    }

    // Method to fetch from resources
    private void fetchFromResource() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(source)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + source);
            }
            content = readContentFromStream(in);
            responseCode = 200;
            logger.info("Successfully read resource: " + source);
        }
    }

    // Method to replace content within the file
    public FileOperation replace(String target, String replacement) {
        if (content == null) {
            logger.severe("No content loaded to perform replacement.");
            return this;
        }

        content = content.replace(target, replacement);
        logger.info("Replaced \"" + target + "\" with \"" + replacement + "\" in the content.");
        return this;
    }

    // Method to save the content to a file
    public FileOperation saveTo(String destinationPath) {
        if (content == null || responseCode != 200) {
            logger.severe("No content available to save or fetch failed.");
            return this;
        }

        try {
            Files.write(Paths.get(destinationPath), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("File saved to: " + destinationPath);
        } catch (IOException e) {
            logger.severe("Failed to save file: " + e.getMessage());
        }
        return this;
    }

    // Method to cache the content
    public FileOperation cache(long maxAgeMillis, String cachePath) {
        File cacheFile = new File(cachePath);
        if (cacheFile.exists() && (System.currentTimeMillis() - cacheFile.lastModified() < maxAgeMillis)) {
            logger.info("Using cached file: " + cachePath);
            try {
                content = new String(Files.readAllBytes(Paths.get(cachePath)), StandardCharsets.UTF_8);
                responseCode = 304;  // Set response code to indicate the file was loaded from cache
            } catch (IOException e) {
                logger.severe("Failed to read cached file: " + e.getMessage());
                responseCode = 500;
            }
        } else {
            if (content != null && responseCode == 200) {
                saveTo(cachePath);
            }
        }
        return this;
    }

    // Getter for the content
    public String getContent() {
        return content;
    }

    // Getter for the response code
    public int getResponseCode() {
        return responseCode;
    }

    // Static method to create a FileOperation instance
    public static FileOperation getFile(String source) {
        return new FileOperation(source);
    }

    // Helper method to read content from an InputStream
    private String readContentFromStream(InputStream in) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
        }
        return contentBuilder.toString();
    }

    // Main method for testing
    public static void main(String[] args) {
        logger.info("Starting FileOperation tests...");

        // Test replacing content in a file from the resource folder and saving it locally
        FileOperation resourceFile = FileOperation.getFile("/config/resource_file.txt")
                .fetch()
                .replace("oldText", "newText")
                .replace("anotherOldText", "anotherNewText")
                .saveTo("resource_copy.txt");

        if (resourceFile.getResponseCode() == 200) {
            System.out.println("Resource file content after replacements: " + resourceFile.getContent());
        } else {
            logger.severe("Failed to fetch or process the resource file.");
        }

        // Test downloading a remote file and saving it locally
        FileOperation downloadResult = FileOperation.getFile("https://example.com/remote_file.txt")
                .header("Authorization", "Bearer your_token_here")
                .fetch()
                .saveTo("remote_file.txt");

        if (downloadResult.getResponseCode() == 200) {
            logger.info("Downloaded file content: " + downloadResult.getContent());
        } else {
            logger.severe("Failed to download file. Response code: " + downloadResult.getResponseCode());
        }

        // Test reading a remote file with cache support
        FileOperation cacheResult = FileOperation.getFile("https://example.com/remote_file.txt")
                .fetch()
                .cache(60000, "cached_file.txt");

        if (cacheResult.getResponseCode() == 200) {
            logger.info("Using cached file content: " + cacheResult.getContent());
        } else {
            logger.severe("Failed to fetch or cache the file. Response code: " + cacheResult.getResponseCode());
        }

        logger.info("FileOperation tests completed.");

        // Test fetching, caching, and saving a remote file
        FileOperation remoteSaveReadResultCache = FileOperation.getFile("https://api.curseforge.com/v1/minecraft/modloader/?version=1.20.4")
                .fetch()
                .cache(60000, "ModLoaderCache.json");

        if (remoteSaveReadResultCache.getResponseCode() == 200) {
            logger.info("File successfully fetched from URL and saved.");
        } else if (remoteSaveReadResultCache.getResponseCode() == 304) {
            logger.info("File loaded from cache.");
        } else {
            logger.severe("Failed to fetch the file. Response code: " + remoteSaveReadResultCache.getResponseCode());
        }

    }

}
