package com.newestaf.newestaddon;


import daybreak.abilitywar.addon.Addon;

import java.io.File;


public final class NewestAddon extends Addon {

    private static NewestAddon addon;


    public static NewestAddon getAddon() {
        return addon;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        addon = this;

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static File getAddonFile(String name) {
        return new File("plugins/AbilityWar/Addon/NewestAddon/" + name);
    }

}
