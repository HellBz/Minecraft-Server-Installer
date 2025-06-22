package dev.cubie.CubeServerTool.Model;

import java.util.Objects;

/**
 * Enthält Informationen über verfügbare Updates für ein Servermodul.
 */
public class UpdateInfo {
    private final String currentVersion;
    private final String newVersion;
    private final String updateUrl;
    private final String changelog;
    private final boolean isUpdateAvailable;

    /**
     * Erstellt ein neues UpdateInfo-Objekt.
     * 
     * @param currentVersion Die aktuelle Version
     * @param newVersion Die neue verfügbare Version (kann null sein, wenn kein Update verfügbar ist)
     * @param updateUrl Die URL zum Herunterladen des Updates (optional)
     * @param changelog Änderungsprotokoll des Updates (optional)
     */
    public UpdateInfo(String currentVersion, String newVersion, String updateUrl, String changelog) {
        this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion darf nicht null sein");
        this.newVersion = newVersion;
        this.updateUrl = updateUrl;
        this.changelog = changelog;
        this.isUpdateAvailable = newVersion != null && !newVersion.isEmpty();
    }

    /**
     * Gibt die aktuelle Version zurück.
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Gibt die neue verfügbare Version zurück.
     * @return Die neue Version oder null, wenn kein Update verfügbar ist
     */
    public String getNewVersion() {
        return newVersion;
    }

    /**
     * Gibt die URL zum Herunterladen des Updates zurück.
     * @return Die Download-URL oder null, wenn nicht verfügbar
     */
    public String getUpdateUrl() {
        return updateUrl;
    }

    /**
     * Gibt das Änderungsprotokoll des Updates zurück.
     * @return Das Änderungsprotokoll oder null, wenn nicht verfügbar
     */
    public String getChangelog() {
        return changelog;
    }

    /**
     * Prüft, ob ein Update verfügbar ist.
     * @return true, wenn ein Update verfügbar ist, sonst false
     */
    public boolean isUpdateAvailable() {
        return isUpdateAvailable;
    }

    @Override
    public String toString() {
        return "UpdateInfo{" +
               "currentVersion='" + currentVersion + '\'' +
               ", newVersion='" + newVersion + '\'' +
               ", updateUrl='" + updateUrl + '\'' +
               ", isUpdateAvailable=" + isUpdateAvailable +
               '}';
    }
}
