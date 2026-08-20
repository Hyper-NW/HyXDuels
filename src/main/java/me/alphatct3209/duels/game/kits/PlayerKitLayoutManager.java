package me.alphatct3209.duels.game.kits;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.configuration.PluginFiles;
import me.alphatct3209.duels.utils.ItemSerializationUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Persists player-owned permutations of shared kits without changing their items. */
public final class PlayerKitLayoutManager
{
    private static final int CONFIG_VERSION = 1;
    private final Duels plugin;
    private final File file;
    private final Map<UUID, Map<String, Layout>> layouts = new HashMap<>();

    public PlayerKitLayoutManager(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin);
        file = PluginFiles.data(plugin, "kit-layouts.yml", false);
        load();
    }

    public Layout layout(UUID playerId, Kit kit)
    {
        Layout saved = layouts.getOrDefault(playerId, Map.of()).get(kit.getKey());
        if (saved != null && saved.signature().equals(signature(kit))) return saved.copy();
        return fromKit(kit);
    }

    public boolean apply(Player player, Kit kit)
    {
        Layout saved = layouts.getOrDefault(player.getUniqueId(), Map.of()).get(kit.getKey());
        if (saved == null || !saved.signature().equals(signature(kit)))
        {
            kit.apply(player);
            return false;
        }
        player.getInventory().clear();
        player.getInventory().setStorageContents(saved.storage());
        player.getInventory().setArmorContents(saved.armor());
        player.getInventory().setItemInOffHand(saved.offhand());
        return true;
    }

    public synchronized void save(UUID playerId, Kit kit, ItemStack[] storage,
                                  ItemStack[] armor, ItemStack offhand)
    {
        Layout proposed = new Layout(signature(kit), storage, armor, offhand);
        if (!sameItems(fromKit(kit), proposed))
            throw new IllegalArgumentException("A personal layout may only rearrange the selected kit's items");
        Map<String, Layout> playerLayouts = layouts.computeIfAbsent(playerId, ignored -> new HashMap<>());
        Layout previous = playerLayouts.put(kit.getKey(), proposed);
        try { saveFile(); }
        catch (RuntimeException exception)
        {
            if (previous == null) playerLayouts.remove(kit.getKey());
            else playerLayouts.put(kit.getKey(), previous);
            if (playerLayouts.isEmpty()) layouts.remove(playerId);
            throw exception;
        }
    }

    public synchronized void invalidateKit(String kitKey)
    {
        boolean changed = false;
        for (Map<String, Layout> playerLayouts : layouts.values())
            changed |= playerLayouts.remove(kitKey) != null;
        layouts.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (changed) saveFile();
    }

    static boolean sameItems(Layout first, Layout second)
    {
        return sameSerializedItems(serializedItems(first), serializedItems(second));
    }

    static boolean sameSerializedItems(List<String> first, List<String> second)
    {
        List<String> left = new ArrayList<>(first);
        List<String> right = new ArrayList<>(second);
        left.sort(String::compareTo);
        right.sort(String::compareTo);
        return left.equals(right);
    }

    static String signature(Kit kit)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : serializedSlots(fromKit(kit)))
            {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static Layout fromKit(Kit kit)
    {
        return new Layout(signaturePlaceholder(), normalizeStorage(kit.getInventoryContents()),
                kit.getArmorContents(), kit.getOffhand());
    }

    private void load()
    {
        if (!file.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("Layouts");
        if (root == null) return;
        boolean discarded = false;
        for (String rawUuid : root.getKeys(false))
        {
            final UUID playerId;
            try { playerId = UUID.fromString(rawUuid); }
            catch (IllegalArgumentException exception)
            {
                plugin.getLogger().warning("Ignoring invalid kit-layout player UUID '" + rawUuid + "'.");
                discarded = true;
                continue;
            }
            ConfigurationSection player = root.getConfigurationSection(rawUuid);
            if (player == null) continue;
            for (String kitKey : player.getKeys(false))
            {
                try
                {
                    Layout layout = read(player, kitKey);
                    Kit kit = plugin.getKitManager().getKitByCanonicalKey(kitKey);
                    if (kit != null && layout.signature().equals(signature(kit))
                            && sameItems(fromKit(kit), layout))
                        layouts.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(kitKey, layout);
                    else discarded = true;
                }
                catch (RuntimeException exception)
                {
                    plugin.getLogger().warning("Ignoring invalid personal kit layout " + rawUuid + "/"
                            + kitKey + ": " + exception.getMessage());
                    discarded = true;
                }
            }
        }
        if (discarded) saveFile();
    }

    private Layout read(ConfigurationSection player, String kitKey)
    {
        String path = kitKey + ".";
        List<String> encoded = player.getStringList(path + "Storage");
        KitSchema.requireV2StorageSize(encoded.size(), "Layouts." + kitKey);
        ItemStack[] storage = new ItemStack[KitSchema.STORAGE_SIZE];
        for (int index = 0; index < storage.length; index++)
            storage[index] = ItemSerializationUtils.deserialize(encoded.get(index));
        ItemStack[] armor = new ItemStack[]{
                ItemSerializationUtils.deserialize(player.getString(path + "Armor.Boots")),
                ItemSerializationUtils.deserialize(player.getString(path + "Armor.Leggings")),
                ItemSerializationUtils.deserialize(player.getString(path + "Armor.Chestplate")),
                ItemSerializationUtils.deserialize(player.getString(path + "Armor.Helmet"))};
        return new Layout(Objects.requireNonNull(player.getString(path + "Signature"), "missing signature"),
                storage, armor, ItemSerializationUtils.deserialize(player.getString(path + "Offhand")));
    }

    private void saveFile()
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Config-Version", CONFIG_VERSION);
        layouts.forEach((playerId, playerLayouts) -> playerLayouts.forEach((kitKey, layout) -> {
            String path = "Layouts." + playerId + "." + kitKey + ".";
            yaml.set(path + "Signature", layout.signature());
            List<String> storage = new ArrayList<>(KitSchema.STORAGE_SIZE);
            for (ItemStack item : layout.storage()) storage.add(ItemSerializationUtils.serialize(item));
            yaml.set(path + "Storage", storage);
            ItemStack[] armor = layout.armor();
            yaml.set(path + "Armor.Boots", ItemSerializationUtils.serialize(armor[0]));
            yaml.set(path + "Armor.Leggings", ItemSerializationUtils.serialize(armor[1]));
            yaml.set(path + "Armor.Chestplate", ItemSerializationUtils.serialize(armor[2]));
            yaml.set(path + "Armor.Helmet", ItemSerializationUtils.serialize(armor[3]));
            yaml.set(path + "Offhand", ItemSerializationUtils.serialize(layout.offhand()));
        }));
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try
        {
            yaml.save(temporary);
            try
            {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException exception)
        {
            temporary.delete();
            throw new IllegalStateException("Could not save data/kit-layouts.yml", exception);
        }
    }

    private static List<String> serializedItems(Layout layout)
    {
        return serializedSlots(layout).stream().filter(value -> !value.isEmpty()).collect(
                java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static List<String> serializedSlots(Layout layout)
    {
        List<String> values = new ArrayList<>(41);
        for (ItemStack item : layout.storage()) values.add(ItemSerializationUtils.serialize(item));
        for (ItemStack item : layout.armor()) values.add(ItemSerializationUtils.serialize(item));
        values.add(ItemSerializationUtils.serialize(layout.offhand()));
        return values;
    }

    private static ItemStack[] normalizeStorage(ItemStack[] source)
    {
        ItemStack[] result = new ItemStack[KitSchema.STORAGE_SIZE];
        if (source != null)
            for (int index = 0; index < Math.min(source.length, result.length); index++)
                result[index] = cloneItem(source[index]);
        return result;
    }

    private static String signaturePlaceholder() { return "base"; }
    private static ItemStack cloneItem(ItemStack item) { return item == null ? null : item.clone(); }

    public record Layout(String signature, ItemStack[] storage, ItemStack[] armor, ItemStack offhand)
    {
        public Layout
        {
            Objects.requireNonNull(signature, "signature");
            KitSchema.requireV2StorageSize(storage == null ? 0 : storage.length, "personal layout");
            if (armor == null || armor.length != 4)
                throw new IllegalArgumentException("Personal layout armor must have four slots");
            storage = cloneItems(storage);
            armor = cloneItems(armor);
            offhand = cloneItem(offhand);
        }

        @Override public ItemStack[] storage() { return cloneItems(storage); }
        @Override public ItemStack[] armor() { return cloneItems(armor); }
        @Override public ItemStack offhand() { return cloneItem(offhand); }
        Layout copy() { return new Layout(signature, storage, armor, offhand); }

        private static ItemStack[] cloneItems(ItemStack[] source)
        {
            return Arrays.stream(source).map(PlayerKitLayoutManager::cloneItem).toArray(ItemStack[]::new);
        }
    }
}
