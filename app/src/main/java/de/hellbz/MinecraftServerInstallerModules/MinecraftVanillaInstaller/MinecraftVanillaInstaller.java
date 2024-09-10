package de.hellbz.MinecraftServerInstallerModules.MinecraftVanillaInstaller;

import de.hellbz.MinecraftServerInstaller.MinecraftServerInstaller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

        // URL zur JSON-Datei
        String url = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
        // JSON-Daten von der URL abrufen
        String json = null;
        try {
            json = fetchJsonFromUrl(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // JSON parsen und "versions" Array extrahieren
        JSONObject jsonObj = new JSONObject(json);
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
