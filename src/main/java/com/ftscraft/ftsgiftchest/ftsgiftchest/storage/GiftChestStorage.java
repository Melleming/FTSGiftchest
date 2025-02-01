package com.ftscraft.ftsgiftchest.ftsgiftchest.storage;

import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class GiftChestStorage {
    public static class GiftChestData {
        private final UUID id;
        private Location location;
        private ItemStack[] template;
        private final Set<UUID> emptiedPlayers = new HashSet<>();
        private final Map<UUID, ItemStack[]> partialInventories = new HashMap<>();

        public GiftChestData(UUID id) {
            this.id = id;
        }

        public UUID getId() {
            return id;
        }
        public Location getLocation() {
            return location;
        }
        public void setLocation(Location location) {
            this.location = location;
        }
        public ItemStack[] getTemplate() {
            return template;
        }
        public void setTemplate(ItemStack[] template) {
            this.template = template;
        }
        public Set<UUID> getEmptiedPlayers() {
            return emptiedPlayers;
        }
        public Map<UUID, ItemStack[]> getPartialInventories() {
            return partialInventories;
        }
    }

    public static class GiftChestEditHolder implements InventoryHolder {
        private final Location chestLocation;

        public GiftChestEditHolder(Location chestLocation) {
            this.chestLocation = chestLocation;
        }

        public Location getChestLocation() {
            return chestLocation;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class GiftChestPersonalHolder implements InventoryHolder {
        private final Location chestLocation;

        public GiftChestPersonalHolder(Location chestLocation) {
            this.chestLocation = chestLocation;
        }

        public Location getLocation() {
            return chestLocation;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final Plugin plugin;

    private final Map<UUID, GiftChestData> giftChestsById = new HashMap<>();

    private final Map<Location, UUID> chestIdByLocation = new HashMap<>();

    private final Set<UUID> setGiftChestMode = new HashSet<>();
    private final Set<UUID> editGiftChestMode = new HashSet<>();

    private File mainFolder;       // plugins/GiftChestPlugin
    private File giftChestsFile;   // plugins/GiftChestPlugin/GiftChests.yml

    public GiftChestStorage(Plugin plugin) {
        this.plugin = plugin;
        setupFolders();
        loadAllData();
    }

    private void setupFolders() {
        mainFolder = plugin.getDataFolder();
        if (!mainFolder.exists()) {
            mainFolder.mkdirs();
        }

        giftChestsFile = new File(mainFolder, "GiftChests.yml");
        if (!giftChestsFile.exists()) {
            try {
                giftChestsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte GiftChests.yml nicht erstellen!");
                e.printStackTrace();
            }
        }
    }

    public void loadAllData() {
        giftChestsById.clear();
        chestIdByLocation.clear();

        FileConfiguration config = YamlConfiguration.loadConfiguration(giftChestsFile);

        if (!config.contains("GiftChests")) {
            return;
        }

        ConfigurationSection gcSection = config.getConfigurationSection("GiftChests");
        if (gcSection == null) return;

        for (String key : gcSection.getKeys(false)) {
            try {
                UUID chestId = UUID.fromString(key);
                String worldName = gcSection.getString(key + ".world");
                int x = gcSection.getInt(key + ".x");
                int y = gcSection.getInt(key + ".y");
                int z = gcSection.getInt(key + ".z");

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    continue;
                }

                GiftChestData gcd = new GiftChestData(chestId);
                gcd.setLocation(new Location(world, x, y, z));
                giftChestsById.put(chestId, gcd);
                chestIdByLocation.put(gcd.getLocation(), chestId);

                loadGiftChestFolder(gcd);
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Laden einer GiftChest: " + key);
                e.printStackTrace();
            }
        }
    }

    private void loadGiftChestFolder(GiftChestData gcd) {
        File chestFolder = new File(mainFolder, gcd.getId().toString());
        if (!chestFolder.exists()) {
            return;
        }

        // template.yml
        File templateFile = new File(chestFolder, "template.yml");
        if (templateFile.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(templateFile);
            List<ItemStack> list = (List<ItemStack>) cfg.getList("template", new ArrayList<>());
            gcd.setTemplate(list.toArray(new ItemStack[0]));
        } else {
            gcd.setTemplate(new ItemStack[0]);
        }

        File emptiedPlayersFile = new File(chestFolder, "emptiedPlayers.yml");
        if (emptiedPlayersFile.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(emptiedPlayersFile);
            List<String> uuids = cfg.getStringList("emptiedPlayers");
            for (String s : uuids) {
                gcd.getEmptiedPlayers().add(UUID.fromString(s));
            }
        }

        File openInvFolder = new File(chestFolder, "OpenInventorys");
        if (openInvFolder.exists() && openInvFolder.isDirectory()) {
            File[] playerFiles = openInvFolder.listFiles();
            if (playerFiles != null) {
                for (File f : playerFiles) {
                    if (f.getName().toLowerCase().endsWith(".yml")) {
                        String baseName = f.getName().substring(0, f.getName().length() - 4);
                        try {
                            UUID pid = UUID.fromString(baseName);
                            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                            List<ItemStack> stacks = (List<ItemStack>) cfg.getList("inventory", new ArrayList<>());
                            gcd.getPartialInventories().put(pid, stacks.toArray(new ItemStack[0]));
                        } catch (Exception e) {
                            plugin.getLogger().warning("Fehler beim Laden von " + f.getName());
                        }
                    }
                }
            }
        }
    }

    public void saveAllData() {
        FileConfiguration config = new YamlConfiguration();
        config.createSection("GiftChests");

        for (Map.Entry<UUID, GiftChestData> entry : giftChestsById.entrySet()) {
            UUID id = entry.getKey();
            GiftChestData gcd = entry.getValue();
            Location loc = gcd.getLocation();
            if (loc == null || loc.getWorld() == null) {
                continue;
            }
            String path = "GiftChests." + id.toString();
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
        }

        try {
            config.save(giftChestsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der GiftChests.yml!");
            e.printStackTrace();
        }

        for (GiftChestData gcd : giftChestsById.values()) {
            saveGiftChestFolder(gcd);
        }
    }

    private void saveGiftChestFolder(GiftChestData gcd) {
        File chestFolder = new File(mainFolder, gcd.getId().toString());
        if (!chestFolder.exists()) {
            chestFolder.mkdirs();
        }

        File templateFile = new File(chestFolder, "template.yml");
        YamlConfiguration tCfg = new YamlConfiguration();
        tCfg.set("template", Arrays.asList(gcd.getTemplate() != null ? gcd.getTemplate() : new ItemStack[0]));
        try {
            tCfg.save(templateFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern von template.yml für " + gcd.getId());
        }

        File emptiedPlayersFile = new File(chestFolder, "emptiedPlayers.yml");
        YamlConfiguration epCfg = new YamlConfiguration();
        List<String> epList = new ArrayList<>();
        for (UUID p : gcd.getEmptiedPlayers()) {
            epList.add(p.toString());
        }
        epCfg.set("emptiedPlayers", epList);
        try {
            epCfg.save(emptiedPlayersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern von emptiedPlayers.yml für " + gcd.getId());
        }

        File openInvFolder = new File(chestFolder, "OpenInventorys");
        if (!openInvFolder.exists()) {
            openInvFolder.mkdirs();
        }

        File[] existing = openInvFolder.listFiles();
        if (existing != null) {
            for (File f : existing) {
                if (f.getName().toLowerCase().endsWith(".yml")) {
                    String baseName = f.getName().substring(0, f.getName().length() - 4);
                    UUID pid;
                    try {
                        pid = UUID.fromString(baseName);
                    } catch (Exception ex) {
                        continue;
                    }
                    if (!gcd.getPartialInventories().containsKey(pid)) {
                        f.delete();
                    }
                }
            }
        }

        for (Map.Entry<UUID, ItemStack[]> e : gcd.getPartialInventories().entrySet()) {
            UUID pid = e.getKey();
            ItemStack[] items = e.getValue();

            File pFile = new File(openInvFolder, pid.toString() + ".yml");
            YamlConfiguration pcfg = new YamlConfiguration();
            pcfg.set("inventory", Arrays.asList(items != null ? items : new ItemStack[0]));
            try {
                pcfg.save(pFile);
            } catch (IOException ioException) {
                plugin.getLogger().severe("Fehler beim Speichern eines Partial-Inventars von Spieler " + pid);
            }
        }
    }

    public void saveNewGiftChest(Block block) {
        GiftChestData gcd = new GiftChestData(UUID.randomUUID());
        gcd.setLocation(block.getLocation());

        Inventory blockInv = getBlockInventory(block);
        if (blockInv != null) {
            gcd.setTemplate(blockInv.getContents());
        } else {
            gcd.setTemplate(new ItemStack[0]);
        }

        giftChestsById.put(gcd.getId(), gcd);
        chestIdByLocation.put(gcd.getLocation(), gcd.getId());

        saveAllData();
    }

    public void destroyGiftChest(Block block) {
        Location loc = block.getLocation();
        UUID gcId = chestIdByLocation.remove(loc);
        if (gcId == null) {
            return;
        }
        GiftChestData gcd = giftChestsById.remove(gcId);
        if (gcd == null) {
            return;
        }

        File chestFolder = new File(mainFolder, gcId.toString());
        deleteDirectory(chestFolder);

        saveAllData();
    }

    private void deleteDirectory(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) {
                for (File f : kids) {
                    deleteDirectory(f);
                }
            }
        }
        file.delete();
    }

    public boolean isGiftChest(Location loc) {
        return chestIdByLocation.containsKey(loc);
    }

    public boolean isGiftChest(InventoryHolder holder) {
        Location loc = getHolderLocation(holder);
        if (loc == null) return false;
        return chestIdByLocation.containsKey(loc);
    }

    public GiftChestData getGiftChestData(Location loc) {
        UUID id = chestIdByLocation.get(loc);
        if (id == null) return null;
        return giftChestsById.get(id);
    }

    public GiftChestData getGiftChestData(InventoryHolder holder) {
        Location loc = getHolderLocation(holder);
        if (loc == null) return null;
        return getGiftChestData(loc);
    }

    public void enableSetMode(UUID uuid) {
        setGiftChestMode.add(uuid);
    }
    public void disableSetMode(UUID uuid) {
        setGiftChestMode.remove(uuid);
    }
    public boolean isInSetMode(UUID uuid) {
        return setGiftChestMode.contains(uuid);
    }

    public void enableEditMode(UUID uuid) {
        editGiftChestMode.add(uuid);
    }
    public void disableEditMode(UUID uuid) {
        editGiftChestMode.remove(uuid);
    }
    public boolean isInEditMode(UUID uuid) {
        return editGiftChestMode.contains(uuid);
    }

    public void openEditInventory(Player player, Location loc) {
        GiftChestData gcd = getGiftChestData(loc);
        if (gcd == null) {
            player.sendMessage(ChatColor.RED + "Diese Kiste ist keine GiftChest!");
            return;
        }

        ItemStack[] template = gcd.getTemplate() != null ? gcd.getTemplate() : new ItemStack[0];
        int size = template.length;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        Inventory editInv = Bukkit.createInventory(new GiftChestEditHolder(loc),
                size,
                ChatColor.BLUE + "GiftChest bearbeiten");
        editInv.setContents(cloneItemStackArray(template));
        player.openInventory(editInv);
    }

    public void updateGiftChestTemplate(Location loc, ItemStack[] newTemplate) {
        GiftChestData gcd = getGiftChestData(loc);
        if (gcd == null) return;

        gcd.setTemplate(cloneItemStackArray(newTemplate));
        gcd.getPartialInventories().clear();
        gcd.getEmptiedPlayers().clear();

        saveGiftChestFolder(gcd);
    }

    public Inventory createPersonalInventory(Player player, InventoryHolder holder) {
        GiftChestData gcd = getGiftChestData(holder);
        if (gcd == null) return null;

        if (gcd.getEmptiedPlayers().contains(player.getUniqueId())) {
            return null;
        }

        ItemStack[] personal = gcd.getPartialInventories().get(player.getUniqueId());
        if (personal == null) {
            personal = cloneItemStackArray(gcd.getTemplate());
            gcd.getPartialInventories().put(player.getUniqueId(), personal);
        }

        if (isArrayEmpty(personal)) {
            gcd.getEmptiedPlayers().add(player.getUniqueId());
            gcd.getPartialInventories().remove(player.getUniqueId());
            saveGiftChestFolder(gcd);
            return null;
        }

        int size = personal.length;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        String displayTitle = ChatColor.GOLD + "GiftChest";

        Inventory inv = Bukkit.createInventory(
                new GiftChestPersonalHolder(gcd.getLocation()),
                size,
                displayTitle
        );
        inv.setContents(cloneItemStackArray(personal));
        return inv;
    }

    public void savePersonalInventory(Player player, Inventory inv) {
        if (!(inv.getHolder() instanceof GiftChestPersonalHolder)) {
            return;
        }
        GiftChestPersonalHolder holder = (GiftChestPersonalHolder) inv.getHolder();
        Location loc = holder.getLocation();

        GiftChestData gcd = getGiftChestData(loc);
        if (gcd == null) return;

        UUID pid = player.getUniqueId();
        ItemStack[] newContents = inv.getContents();

        if (isArrayEmpty(newContents)) {
            gcd.getEmptiedPlayers().add(pid);
            gcd.getPartialInventories().remove(pid);
        } else {
            gcd.getPartialInventories().put(pid, cloneItemStackArray(newContents));
        }

        saveGiftChestFolder(gcd);
    }

    public boolean isPersonalGiftChestInventory(Inventory inv) {
        if (inv == null) return false;
        return inv.getHolder() instanceof GiftChestPersonalHolder;
    }

    public void resetAllPersonalInventories(Location loc) {
        GiftChestData gcd = getGiftChestData(loc);
        if (gcd == null) return;

        gcd.getPartialInventories().clear();
        gcd.getEmptiedPlayers().clear();

        saveGiftChestFolder(gcd);
    }

    private Inventory getBlockInventory(Block block) {
        BlockState state = block.getState();
        if (state instanceof Chest) {
            return ((Chest) state).getInventory();
        } else if (state instanceof Barrel) {
            return ((Barrel) state).getInventory();
        } else if (state instanceof DoubleChest) {
            return ((DoubleChest) state).getInventory();
        }
        return null;
    }

    private Location getHolderLocation(InventoryHolder holder) {
        if (holder instanceof Chest) {
            return ((Chest) holder).getLocation();
        }
        if (holder instanceof Barrel) {
            return ((Barrel) holder).getLocation();
        }
        if (holder instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) holder;
            InventoryHolder left = dc.getLeftSide();
            if (left instanceof Chest) {
                return ((Chest) left).getLocation();
            }
            return dc.getLocation();
        }
        return null;
    }

    private boolean isArrayEmpty(ItemStack[] items) {
        if (items == null) return true;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private ItemStack[] cloneItemStackArray(ItemStack[] original) {
        if (original == null) return new ItemStack[0];
        ItemStack[] clone = new ItemStack[original.length];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                clone[i] = original[i].clone();
            }
        }
        return clone;
    }
}
