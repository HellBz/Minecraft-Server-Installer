package de.hellbz.MinecraftServerInstallerModules.MinecraftVanillaInstaller;

import de.hellbz.MinecraftServerInstaller.MinecraftServerInstaller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import de.hellbz.MinecraftServerInstaller.Utils.Config;
import de.hellbz.MinecraftServerInstaller.Utils.FileOperation;
import org.json.JSONArray;
import org.json.JSONObject;

// Implementierung des Vanilla-Installers
public class MinecraftVanillaInstaller implements MinecraftServerInstaller {

    @Override
    public void init() {
        System.out.println("Vanilla Minecraft Installer initialized.");
    }

    @Override
    public String getInstallerName() {
        return "Minecraft-Vanilla";
    }

    @Override
    public String[] getAvailableVersions() {

        // Definiere den Pfad zur Cache-Datei
        String cacheFile = Config.tempFolder.resolve("minecraft_versions.json").toAbsolutePath().toString();

        // Verwende FileOperation, um die Datei herunterzuladen und zu cachen
        FileOperation downloadResult = FileOperation.getFile("https://launchermeta.mojang.com/mc/game/version_manifest.json")
                .fetch()
                .cache(60000, cacheFile);  // Prüft den Cache und lädt die Datei nur neu, wenn der Cache abgelaufen ist

        // Überprüfe den ResponseCode, um zu sehen, ob die Datei aus dem Cache oder von der URL geladen wurde
        if (downloadResult.getResponseCode() == 200) {
            System.out.println("File successfully fetched from URL and saved.");
        } else if (downloadResult.getResponseCode() == 304) {
            System.out.println("File loaded from cache.");
        } else {
            System.out.println("Failed to fetch the file. Response code: " + downloadResult.getResponseCode());
        }

        // JSON parsen und "versions" Array extrahieren
        JSONObject jsonObj = new JSONObject(downloadResult.getContent());
        JSONArray versionsArray = jsonObj.getJSONArray("versions");

        // String-Array für die "id"-Werte
        String[] versionIds = new String[versionsArray.length()];

        // Durch das "versions" Array iterieren und die "id" Werte extrahieren
        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionObj = versionsArray.getJSONObject(i);
            versionIds[i] = versionObj.getString("id");
        }
        return versionIds;
    }

    @Override
    public String[] getAvailableSubVersions(String mainVersion) {
        return new String[0];  // Keine Sub-Versionen für Vanilla
    }

    // Funktion zum Abrufen des JSON-Inhalts von einer URL
    private static String fetchJsonFromUrl(String urlString) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    @Override
    public void install(String version, String subVersion) {
        System.out.println("Installing Vanilla Minecraft version: " + version);
    }
}
