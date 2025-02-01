package com.ftscraft.ftsgiftchest.ftsgiftchest;

import com.ftscraft.ftsgiftchest.ftsgiftchest.command.CMDGiftChest;
import com.ftscraft.ftsgiftchest.ftsgiftchest.listener.InventoryListener;
import com.ftscraft.ftsgiftchest.ftsgiftchest.listener.PlayerInteractListener;
import com.ftscraft.ftsgiftchest.ftsgiftchest.storage.GiftChestStorage;
import org.bukkit.plugin.java.JavaPlugin;

public final class FTSGiftchest extends JavaPlugin {

    private GiftChestStorage storage;

    @Override
    public void onEnable() {
        storage = new GiftChestStorage(this);

        CMDGiftChest commandExecutor = new CMDGiftChest(storage);
        getCommand("giftchest").setExecutor(commandExecutor);
        getCommand("giftchest").setTabCompleter(commandExecutor);

        getServer().getPluginManager().registerEvents(new PlayerInteractListener(storage), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(storage), this);

        getLogger().info("GiftChestPlugin aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GiftChestPlugin deaktiviert!");
    }
}
