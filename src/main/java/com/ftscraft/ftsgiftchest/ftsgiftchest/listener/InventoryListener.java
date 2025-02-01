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

    /**
     * Wenn ein Spieler eine Kiste öffnet, wird geprüft, ob es eine GiftChest ist.
     * Falls ja, wird das echte Öffnen abgebrochen und stattdessen ein personalisiertes Inventar angezeigt.
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        InventoryHolder holder = event.getInventory().getHolder();

        // Prüfen, ob dies eine GiftChest ist
        if (!storage.isGiftChest(holder)) {
            return;
        }

        // Verhindere das Öffnen des echten Inventars
        event.setCancelled(true);

        // Öffne stattdessen das personalisierte Inventar
        Inventory personalInventory = storage.createPersonalInventory(player, holder);
        if (personalInventory == null) {
            // Falls kein Inventar vorhanden (z.B. schon leer)
            player.sendMessage(ChatColor.YELLOW + "Du hast diese Giftchest bereits geleert!");
            return;
        }
        player.openInventory(personalInventory);
    }

    /**
     * Verhindert, dass Items in die GiftChest gelegt werden, erlaubt aber das Herausnehmen.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        // Prüfen, ob das "Top-Inventory" ein personalisiertes GiftChest-Inventar ist
        if (!storage.isPersonalGiftChestInventory(topInventory)) {
            return;
        }

        // Blockiere jeden Versuch, Items ins Top-Inventar zu legen
        // SHIFT-Klick nach oben
        if (event.isShiftClick()) {
            if (clickedInventory != null && clickedInventory.equals(event.getWhoClicked().getInventory())) {
                // Spieler-Inventar -> GiftChest
                event.setCancelled(true);
            }
        }

        // Normaler Klick mit Item am Cursor
        if (event.getAction() == InventoryAction.PLACE_ALL
                || event.getAction() == InventoryAction.PLACE_SOME
                || event.getAction() == InventoryAction.PLACE_ONE
                || event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
            // Spieler versucht, Item in den Top-Slot zu legen
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Speichert den neuen Stand, wenn ein Spieler das personalisierte Inventar schließt.
     * Oder aktualisiert das Template, wenn es sich um ein Edit-Inventar handelt.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        // 1) Edit-Inventar geschlossen?
        if (inventory.getHolder() instanceof GiftChestStorage.GiftChestEditHolder) {
            GiftChestStorage.GiftChestEditHolder editHolder =
                    (GiftChestStorage.GiftChestEditHolder) inventory.getHolder();

            // Template aktualisieren (und dabei automatisch abgespeichert)
            storage.updateGiftChestTemplate(editHolder.getChestLocation(), inventory.getContents());

            // Alle bisherigen Personalisierungen löschen, damit jeder das neue Template wieder voll hat
            storage.resetAllPersonalInventories(editHolder.getChestLocation());

            if (event.getPlayer() instanceof Player) {
                Player p = (Player) event.getPlayer();
                p.sendMessage(ChatColor.GREEN + "GiftChest-Inhalt wurde aktualisiert!");
            }
            return;
        }

        // 2) Personalisiertes GiftChest-Inventar?
        if (storage.isPersonalGiftChestInventory(inventory)) {
            storage.savePersonalInventory((Player) event.getPlayer(), inventory);
        }
    }
}
