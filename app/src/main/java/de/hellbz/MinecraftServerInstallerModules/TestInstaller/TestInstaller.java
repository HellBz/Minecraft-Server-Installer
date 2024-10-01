package de.hellbz.MinecraftServerInstallerModules.TestInstaller;

import de.hellbz.MinecraftServerInstaller.Data.Config;
import de.hellbz.MinecraftServerInstaller.MinecraftServerInstaller;

import java.util.regex.Pattern;

public class TestInstaller implements MinecraftServerInstaller {

    @Override
    public String getInstallerName() {
        return "TestInstaller";
    }

    @Override
    public String[] getAvailableTypes() {
        return new String[0];
    }

    @Override
    public String[] getAvailableVersions() {
        return new String[] {"1.20.2", "1.19.4"};  // Beispielhafte Forge-Versionen
    }

    @Override
    public String[] getAvailableSubVersions() {
        return new String[0];  // No Sub-Versionen for TestInstaller
    }

    @Override
    public Pattern getStartFile() {
        return null;
    }

    @Override
    public void init() {
        System.out.println("Test Minecraft Installer initialized.");
    }

    @Override
    public void install() {
        System.out.println("Installing Test Minecraft Version: " + Config.selectedVersion + ", Sub-Version: " + Config.selectedSubVersion);
    }

    @Override
    public void start() {

    }
}
