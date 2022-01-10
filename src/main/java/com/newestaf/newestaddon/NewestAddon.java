package com.newestaf.newestaddon;


import com.newestaf.newestaddon.ability.AddonAbilityFactory;
import com.newestaf.newestaddon.ability.NewestAbility;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.game.Category;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.utils.base.Messager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;


public final class NewestAddon extends Addon implements Listener {

    private static NewestAddon addon;
    private final ConfigLoader configLoader = new ConfigLoader();



    public static NewestAddon getAddon() {
        return addon;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        addon = this;

        AddonAbilityFactory.nameValues();

        configLoader.run();

        //Register Event
        Bukkit.getPluginManager().registerEvents(this, getPlugin());

        //Repeat Load Ability
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), configLoader);

        //Command

        //Load Complete
        Messager.sendConsoleMessage("[§bNewestAddon§f] " + getDisplayName() + "이 활성화되없습니다.");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler()
    public void onGameCredit(GameCreditEvent e) {
        if (e.getGame().getRegistration().getCategory().equals(Category.GameCategory.GAME)) {
            e.addCredit("§c네스트 애드온 §f적용중. 총 " + AddonAbilityFactory.nameValues().size() + "개의 능력이 추가되었습니다.");
            e.addCredit("§c네스트 애드온 §f제작자 : NewestAF  [§7디스코드 §f: NewestAF#9500]");
        }
    }

    public static File getAddonFile(String name) {
        return new File("plugins/AbilityWar/Addon/NewestAddon/" + name);
    }


    static class ConfigLoader implements Runnable {
        @Override
        public void run() {
            NewestAbility.config.update();
        }
    }
}
