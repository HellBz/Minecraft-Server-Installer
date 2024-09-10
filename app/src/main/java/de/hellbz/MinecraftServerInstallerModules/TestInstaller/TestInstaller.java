package de.hellbz.MinecraftServerInstallerModules.TestInstaller;

import de.hellbz.MinecraftServerInstaller.MinecraftServerInstaller;

public class TestInstaller implements MinecraftServerInstaller {

    @Override
    public String getInstallerName() {
        return "TestInstaller";
    }

    @Override
    public String[] getAvailableVersions() {
        return new String[] {"1.20.2", "1.19.4"};  // Beispielhafte Forge-Versionen
    }

    @Override
    public String[] getAvailableSubVersions(String mainVersion) {
        return new String[0];  // No Sub-Versionen for TestInstaller
    }

    @Override
    public void init() {
        System.out.println("Test Minecraft Installer initialized.");
    }

    @Override
    public void install(String version, String subVersion) {
        System.out.println("Installing Test Minecraft Version: " + version + ", Sub-Version: " + subVersion);
    }
}
