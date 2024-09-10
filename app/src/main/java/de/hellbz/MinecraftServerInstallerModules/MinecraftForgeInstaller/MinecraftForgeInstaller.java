package de.hellbz.MinecraftServerInstallerModules.MinecraftForgeInstaller;

import de.hellbz.MinecraftServerInstaller.MinecraftServerInstaller;

// Implementierung des Forge-Installers
public class MinecraftForgeInstaller implements MinecraftServerInstaller {

    @Override
    public void init() {
        System.out.println("Forge Minecraft Installer initialized.");
    }

    @Override
    public void install(String version, String subVersion) {
        System.out.println("Installing Forge Minecraft version: " + version + ", Forge version: " + subVersion);
    }

    @Override
    public String getInstallerName() {
        return "Minecraft-FORGE";
    }

    @Override
    public String[] getAvailableVersions() {
        return new String[] {"1.20.2", "1.19.4"};  // Beispielhafte Forge-Versionen
    }

    @Override
    public String[] getAvailableSubVersions(String mainVersion) {
        if ("1.20.2".equals(mainVersion)) {
            return new String[] {"36.1.0", "36.1.1"};  // Beispielhafte Sub-Versionen
        }
        return new String[0];
    }
}
