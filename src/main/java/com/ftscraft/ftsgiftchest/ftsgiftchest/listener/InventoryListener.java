package com.ftscraft.ftsgiftchest.ftsgiftchest.listener;

import com.ftscraft.ftsgiftchest.ftsgiftchest.storage.GiftChestStorage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class InventoryListener implements Listener {

    private final GiftChestStorage storage;

    public InventoryListener(GiftChestStorage storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        InventoryHolder holder = event.getInventory().getHolder();

        if (!storage.isGiftChest(holder)) {
            return;
        }

        event.setCancelled(true);

        Inventory personalInventory = storage.createPersonalInventory(player, holder);
        if (personalInventory == null) {
            player.sendMessage(ChatColor.YELLOW + "Du hast diese Giftchest bereits geleert!");
            return;
        }
        player.openInventory(personalInventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        if (!storage.isPersonalGiftChestInventory(topInventory)) {
            return;
        }

        if (event.isShiftClick()) {
            if (clickedInventory != null && clickedInventory.equals(event.getWhoClicked().getInventory())) {
                event.setCancelled(true);
            }
        }

        if (event.getAction() == InventoryAction.PLACE_ALL
                || event.getAction() == InventoryAction.PLACE_SOME
                || event.getAction() == InventoryAction.PLACE_ONE
                || event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof GiftChestStorage.GiftChestEditHolder) {
            GiftChestStorage.GiftChestEditHolder editHolder =
                    (GiftChestStorage.GiftChestEditHolder) inventory.getHolder();

            storage.updateGiftChestTemplate(editHolder.getChestLocation(), inventory.getContents());

            storage.resetAllPersonalInventories(editHolder.getChestLocation());

            if (event.getPlayer() instanceof Player) {
                Player p = (Player) event.getPlayer();
                p.sendMessage(ChatColor.GREEN + "GiftChest-Inhalt wurde aktualisiert!");
            }
            return;
        }

        if (storage.isPersonalGiftChestInventory(inventory)) {
            storage.savePersonalInventory((Player) event.getPlayer(), inventory);
        }
    }
}
