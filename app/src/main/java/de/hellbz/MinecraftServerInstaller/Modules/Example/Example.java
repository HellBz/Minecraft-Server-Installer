package de.hellbz.MinecraftServerInstaller.Modules.Example;

import de.hellbz.MinecraftServerInstaller.Data.Config;
import de.hellbz.MinecraftServerInstaller.MinecraftServerInstaller;

import java.util.regex.Pattern;

public class Example implements MinecraftServerInstaller {

    @Override
    public String getInstallerName() {
        return "Example-Installer";
    }

    @Override
    public String[] getAvailableTypes() {

        // return new String[0];
        return new String[] {"Release", "Snapshot", "All"};
    }

    @Override
    public String[] getAvailableVersions() {
        return new String[] {"1.20.2", "1.19.4"};  // Beispielhafte Forge-Versionen
    }

    @Override
    public String[] getAvailableSubVersions() {
        if ("1.20.2".equals( Config.selectedVersion)) {
            return new String[] {"36.1.0", "36.1.1"};  // Beispielhafte Sub-Versionen
        }
        return new String[0];  // No Sub-Versionen for TestInstaller
    }

    @Override
    public Pattern getStartFile() {
        return null;
    }

    @Override
    public void init() {
        System.out.println("Example Minecraft Installer initialized.");
    }

    @Override
    public void install() {
        System.out.println("Installing Test Minecraft Version: " + Config.selectedVersion + ", Sub-Version: " + Config.selectedSubVersion);
    }

    @Override
    public void start() {

    }
}
