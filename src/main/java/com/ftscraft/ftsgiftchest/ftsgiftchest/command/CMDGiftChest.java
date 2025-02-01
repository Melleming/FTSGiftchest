package com.ftscraft.ftsgiftchest.ftsgiftchest.command;

import com.ftscraft.ftsgiftchest.ftsgiftchest.FTSGiftchest;
import com.ftscraft.ftsgiftchest.ftsgiftchest.storage.GiftChestStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CMDGiftChest implements CommandExecutor, TabCompleter {

    private final GiftChestStorage storage;

    public CMDGiftChest(GiftChestStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Not a player");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("giftchest.use")) {
            player.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung!");
            return true;
        }

        // /giftchest ohne Argumente -> Set-Modus
        if (args.length == 0) {
            storage.enableSetMode(player.getUniqueId());
            storage.disableEditMode(player.getUniqueId());

            player.sendMessage(ChatColor.GREEN + "Bitte klicke jetzt eine Kiste an!");
            return true;
        }

        // /giftchest edit
        if (args.length == 1 && args[0].equalsIgnoreCase("edit")) {
            storage.enableEditMode(player.getUniqueId());
            storage.disableSetMode(player.getUniqueId());

            player.sendMessage(ChatColor.GREEN + "Bitte klicke jetzt eine bestehende GiftChest an, "
                    + "um deren Inhalt neu zu definieren!");
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("edit".startsWith(args[0].toLowerCase())) {
                completions.add("edit");
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
