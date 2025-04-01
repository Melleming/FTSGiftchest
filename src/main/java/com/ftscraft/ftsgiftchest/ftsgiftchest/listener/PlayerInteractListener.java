package com.ftscraft.ftsgiftchest.ftsgiftchest.listener;

import com.ftscraft.ftsgiftchest.ftsgiftchest.storage.GiftChestStorage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    private final GiftChestStorage storage;

    public PlayerInteractListener(GiftChestStorage storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Material type = block.getType();

        boolean isValidContainer = (type == Material.CHEST
                || type == Material.BARREL
                || type == Material.TRAPPED_CHEST);

        if ((storage.isInSetMode(player.getUniqueId()) || storage.isInEditMode(player.getUniqueId()))
                && !isValidContainer) {
            storage.disableSetMode(player.getUniqueId());
            storage.disableEditMode(player.getUniqueId());

            player.sendMessage(ChatColor.RED + "Du hast keinen gültigen Container angeklickt. Modus beendet!");
            return;
        }

        BlockState state = block.getState();
        if (!(state instanceof Chest || state instanceof Barrel || state instanceof DoubleChest)) {
            if (storage.isInSetMode(player.getUniqueId()) || storage.isInEditMode(player.getUniqueId())) {
                storage.disableSetMode(player.getUniqueId());
                storage.disableEditMode(player.getUniqueId());

                player.sendMessage(ChatColor.RED + "Kein gültiges Inventar. Modus beendet!");
            }
            return;
        }

        if (storage.isInSetMode(player.getUniqueId())) {
            storage.saveNewGiftChest(block);
            player.sendMessage(ChatColor.GREEN + "GiftChest wurde festgelegt. Inhalt ist nun gespeichert!");

            storage.disableSetMode(player.getUniqueId());

            event.setCancelled(true);
            return;
        }

        if (storage.isInEditMode(player.getUniqueId())) {
            Location loc = block.getLocation();

            if (!storage.isGiftChest(loc)) {
                player.sendMessage(ChatColor.RED + "Diese Kiste ist keine bestehende GiftChest!");
                storage.disableEditMode(player.getUniqueId());
                event.setCancelled(true);
                return;
            }


            storage.openEditInventory(player, loc);
            storage.disableEditMode(player.getUniqueId());

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if(!storage.isGiftChest(block.getLocation())) {
            return;
        }

        if (!player.hasPermission("giftchest.use")) {
            player.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung!");
            event.setCancelled(true);
            return;
        }

        if (!event.isCancelled()) {
            if (storage.isGiftChest(block.getLocation())) {
                storage.destroyGiftChest(block);

                Player p = event.getPlayer();
                p.sendMessage(ChatColor.YELLOW + "GiftChest wurde zerstört!");
            }
        }
    }
}
