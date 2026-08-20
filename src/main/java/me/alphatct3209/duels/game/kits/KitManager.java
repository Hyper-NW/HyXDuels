package me.alphatct3209.duels.game.kits;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.utils.ItemSerializationUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitManager
{

    private final Duels plugin;
    private final List<Kit> kitList;

    public KitManager(Duels plugin)
    {
        this.plugin = plugin;
        this.kitList = new ArrayList<>();

        loadKits();
        BuiltInModeKits.addMissingTo(kitList);
    }

    public Kit resolveKit(Player player)
    {
        Objects.requireNonNull(player, "player");
        if (plugin.getSelectionService() != null)
        {
            return plugin.getSelectionService().kit(
                    plugin.getSelectionService().resolve(player.getUniqueId()));
        }
        Kit fallback = getKit("Default");
        if (isUsable(fallback))
        {
            return fallback;
        }
        throw new IllegalStateException("No usable kit is available for " + player.getName()
                + "; configure a valid kit named 'Default'");
    }

    public Kit giveKit(Player player)
    {
        Kit kit = resolveKit(player);
        kit.apply(player);
        return kit;
    }

    private boolean isUsable(Kit kit)
    {
        if (kit == null || kit.getName() == null)
        {
            return false;
        }
        try
        {
            kit.getKey();
            return true;
        }
        catch (IllegalArgumentException exception)
        {
            return false;
        }
    }

    static String requireAvailableKitKey(String kitName, Collection<Kit> existingKits)
    {
        String candidateKey = GamemodeKey.fromKitName(kitName);
        for (Kit existingKit : existingKits)
        {
            if (existingKit.getKey().equals(candidateKey))
            {
                throw new IllegalArgumentException("Kit name '" + kitName
                        + "' conflicts with existing kit '" + existingKit.getName()
                        + "' because both use kit key '" + candidateKey + "'");
            }
        }
        return candidateKey;
    }

    /** @deprecated use requireAvailableKitKey; retained for source compatibility. */
    @Deprecated
    static String requireAvailableGamemodeKey(String kitName, Collection<Kit> existingKits)
    {
        return requireAvailableKitKey(kitName, existingKits);
    }

    private void loadKits()
    {
        FileConfiguration config = plugin.getConfig();
        if(!config.contains("Kits"))
        {
            return;
        }
        for(String kitIdStr : Objects.requireNonNull(config.getConfigurationSection("Kits")).getKeys(false))
        {
            String path = "Kits." + kitIdStr;
            int id;
            try
            {
                id = Integer.parseInt(kitIdStr);
            }
            catch (NumberFormatException e)
            {
                Bukkit.getLogger().severe("Failed to parse integer '" + kitIdStr + "' from config.yml (kit id)");
                continue;
            }

            KitFormat format = KitSchema.readFormat(config.isSet(path + ".Format-Version"),
                    config.get(path + ".Format-Version"), path);
            String name = config.getString(path + ".Name");
            String helmetBase64 = config.getString(path + ".Armor.Helmet");
            String chestplateBase64 = config.getString(path + ".Armor.Chestplate");
            String leggingsBase64 = config.getString(path + ".Armor.Leggings");
            String bootsBase64 = config.getString(path + ".Armor.Boots");
            List<String> inventoryBase64 = config.getStringList(path + ".Inventory");
            if (format == KitFormat.POSITIONAL_V2)
            {
                KitSchema.requireV2StorageSize(inventoryBase64.size(), path);
            }

            ItemStack[] armor = new ItemStack[4];
            armor[3] = ItemSerializationUtils.deserialize(helmetBase64);
            armor[2] = ItemSerializationUtils.deserialize(chestplateBase64);
            armor[1] = ItemSerializationUtils.deserialize(leggingsBase64);
            armor[0] = ItemSerializationUtils.deserialize(bootsBase64);

            ItemStack[] inventory = new ItemStack[inventoryBase64.size()];
            int i = 0;
            for(String base64 : inventoryBase64)
            {
                inventory[i++] = ItemSerializationUtils.deserialize(base64);
            }

            Kit kit;
            if (format == KitFormat.LEGACY_V1)
            {
                kit = new Kit(id, name, armor, inventory);
            }
            else
            {
                ItemStack offhand = ItemSerializationUtils.deserialize(config.getString(path + ".Offhand"));
                kit = Kit.positionalV2(id, name, armor, inventory, offhand);
            }
            try
            {
                requireAvailableKitKey(name, kitList);
            }
            catch (IllegalArgumentException | NullPointerException exception)
            {
                throw new IllegalStateException("Invalid kit gamemode at " + path + ": "
                        + exception.getMessage(), exception);
            }
            kitList.add(kit);
        }
    }

    public void deleteKit(String kitName)
    {
        Kit kit = getKit(kitName);
        if (kit != null)
        {
            if (plugin.getModeManager() != null && plugin.getModeManager().isKitReferenced(kit.getKey()))
            {
                throw new IllegalStateException("Kit '" + kit.getName()
                        + "' is a default or allowed kit for one or more duel modes");
            }
            plugin.getConfig().set("Kits." + kit.getId(), null);
            plugin.saveKitData();
            kitList.remove(kit);
            invalidatePersonalLayouts(kit.getKey());
            plugin.requestLeaderboardRefresh();
        }
    }

    public Kit createKit(String kitName, Player player)
    {
        requireAvailableKitKey(kitName, kitList);
        int id = getNextId();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] storage = player.getInventory().getStorageContents();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        KitSchema.requireV2StorageSize(storage.length, "Kits." + id);

        FileConfiguration config = plugin.getConfig();
        String path = "Kits." + id;
        config.set(path + ".Format-Version", KitSchema.CURRENT_FORMAT_VERSION);
        config.set(path + ".Name", kitName);
        config.set(path + ".Armor.Helmet", ItemSerializationUtils.serialize(armor[3]));
        config.set(path + ".Armor.Chestplate", ItemSerializationUtils.serialize(armor[2]));
        config.set(path + ".Armor.Leggings", ItemSerializationUtils.serialize(armor[1]));
        config.set(path + ".Armor.Boots", ItemSerializationUtils.serialize(armor[0]));

        List<String> inventory = new ArrayList<>(KitSchema.STORAGE_SIZE);
        for (ItemStack item : storage)
        {
            inventory.add(ItemSerializationUtils.serialize(item));
        }
        config.set(path + ".Inventory", inventory);
        config.set(path + ".Offhand", ItemSerializationUtils.serialize(offhand));
        plugin.saveKitData();

        Kit kit = Kit.positionalV2(id, kitName, armor, storage, offhand);
        kitList.add(kit);
        plugin.requestLeaderboardRefresh();
        return kit;
    }

    static int layoutPersistenceId(Kit current, int nextId)
    {
        Objects.requireNonNull(current, "current");
        if (nextId < 1) throw new IllegalArgumentException("nextId must be positive");
        return current.getId() >= 0 ? current.getId() : nextId;
    }

    public synchronized Kit saveLayout(Kit edited, ItemStack[] storage,
                                       ItemStack[] armor, ItemStack offhand)
    {
        Objects.requireNonNull(edited, "edited");
        KitSchema.requireV2StorageSize(storage == null ? 0 : storage.length, "kit editor");
        if (armor == null || armor.length != 4)
            throw new IllegalArgumentException("Kit editor armor must contain exactly 4 slots");

        Kit current = getKitByCanonicalKey(edited.getKey());
        if (current == null) throw new IllegalStateException("That kit no longer exists");
        int id = layoutPersistenceId(current, getNextId());
        FileConfiguration config = plugin.getConfig();
        String path = "Kits." + id;
        config.set(path + ".Format-Version", KitSchema.CURRENT_FORMAT_VERSION);
        config.set(path + ".Name", current.getName());
        config.set(path + ".Armor.Helmet", ItemSerializationUtils.serialize(armor[3]));
        config.set(path + ".Armor.Chestplate", ItemSerializationUtils.serialize(armor[2]));
        config.set(path + ".Armor.Leggings", ItemSerializationUtils.serialize(armor[1]));
        config.set(path + ".Armor.Boots", ItemSerializationUtils.serialize(armor[0]));
        List<String> inventory = new ArrayList<>(KitSchema.STORAGE_SIZE);
        for (ItemStack item : storage) inventory.add(ItemSerializationUtils.serialize(item));
        config.set(path + ".Inventory", inventory);
        config.set(path + ".Offhand", ItemSerializationUtils.serialize(offhand));
        plugin.saveKitData();

        Kit replacement = Kit.positionalV2(id, current.getName(), armor, storage, offhand);
        int index = kitList.indexOf(current);
        if (index < 0) throw new IllegalStateException("That kit no longer exists");
        kitList.set(index, replacement);
        invalidatePersonalLayouts(replacement.getKey());
        plugin.requestLeaderboardRefresh();
        return replacement;
    }

    public Kit getKit(String kitName)
    {
        if (kitName == null)
        {
            return null;
        }
        for (Kit kit : kitList)
        {
            if (kit.getName() != null && kit.getName().equalsIgnoreCase(kitName))
            {
                return kit;
            }
        }
        return null;
    }

    private void invalidatePersonalLayouts(String kitKey)
    {
        if (plugin.getPlayerKitLayoutManager() == null) return;
        try { plugin.getPlayerKitLayoutManager().invalidateKit(kitKey); }
        catch (RuntimeException exception)
        {
            plugin.getLogger().warning("The shared kit was saved, but its personal layout file could not "
                    + "be rewritten. Signature checks will still reject stale layouts: "
                    + exception.getMessage());
        }
    }

    public Kit getKitByCanonicalKey(String key)
    {
        if (key == null)
        {
            return null;
        }
        for (Kit kit : kitList)
        {
            try
            {
                if (kit.getKey().equals(key))
                {
                    return kit;
                }
            }
            catch (IllegalArgumentException | NullPointerException ignored)
            {
                // Invalid configured kits are never challengeable.
            }
        }
        return null;
    }

    public List<Kit> getKitList()
    {
        return kitList;
    }

    public Kit getKitByNameOrKey(String input)
    {
        Kit direct = getKit(input);
        if (direct != null)
        {
            return direct;
        }

        final String key;
        try
        {
            key = GamemodeKey.fromKitName(input);
        }
        catch (IllegalArgumentException | NullPointerException exception)
        {
            return null;
        }
        for (Kit kit : kitList)
        {
            try
            {
                if (kit.getKey().equals(key))
                {
                    return kit;
                }
            }
            catch (IllegalArgumentException | NullPointerException ignored)
            {
                // Invalid configured kits cannot be selected as gamemodes.
            }
        }
        return null;
    }

    public int getNextId()
    {
        int max = 0;
        for(Kit kit : kitList)
        {
            int id = kit.getId();
            if(id > max)
            {
                max = id;
            }
        }
        return max + 1;
    }

}
