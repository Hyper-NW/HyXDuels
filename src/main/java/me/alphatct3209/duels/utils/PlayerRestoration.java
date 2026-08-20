package me.alphatct3209.duels.utils;

import me.alphatct3209.duels.Duels;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class PlayerRestoration
{

    private static final HashMap<UUID, SavedPlayerInfo> savedInfo = new HashMap<>();

    public static void savePlayer(Player player)
    {
        new SavedPlayerInfo(player);
    }

    public static void discardSavedPlayer(Player player)
    {
        if (player == null) return;
        savedInfo.remove(player.getUniqueId());
        Duels plugin = JavaPlugin.getPlugin(Duels.class);
        player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "world"));
        player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "x"));
        player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "y"));
        player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "z"));
    }

    public static void restorePlayer(Player player, boolean quitting)
    {
        if(savedInfo.containsKey(player.getUniqueId()))
        {
            savedInfo.get(player.getUniqueId()).restore(quitting);
        }
        savedInfo.remove(player.getUniqueId());
        if (!quitting)
        {
            Duels plugin = JavaPlugin.getPlugin(Duels.class);
            if (plugin.isEnabled() && plugin.getDuelMenuManager() != null)
            {
                plugin.getDuelMenuManager().scheduleLobbyHotbar(player);
            }
        }
    }

    private static class SavedPlayerInfo
    {
        private UUID uuid;
        private Location location;
        private GameMode gameMode;
        private ItemStack[] inventoryContents;
        private ItemStack[] armorContents;
        private int xpLevel;
        private int foodLevel;
        private float saturation;
        private float exhaustion;
        private double health;
        private double maxHealth;
        private Double attackSpeed;
        private int maximumNoDamageTicks;
        private Collection<PotionEffect> potionEffects;

        protected SavedPlayerInfo(Player player)
        {
            if(player == null)
            {
                return;
            }

            Duels plugin = JavaPlugin.getPlugin(Duels.class);

            this.uuid = player.getUniqueId();
            this.location = player.getLocation();

            // for if player disconnects (and thus, does not teleport)
            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "world"),
                    PersistentDataType.STRING, Objects.requireNonNull(location.getWorld()).getName());
            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "x"),
                    PersistentDataType.INTEGER, location.getBlockX());
            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "y"),
                    PersistentDataType.INTEGER, location.getBlockY());
            player.getPersistentDataContainer().set(new NamespacedKey(plugin, "z"),
                    PersistentDataType.INTEGER, location.getBlockZ());

            this.gameMode = player.getGameMode();
            this.inventoryContents = player.getInventory().getContents();
            this.armorContents = player.getInventory().getArmorContents();
            this.xpLevel = player.getLevel();
            this.foodLevel = player.getFoodLevel();
            this.saturation = player.getSaturation();
            this.exhaustion = player.getExhaustion();
            this.health = player.getHealth();
            this.maxHealth = player.getMaxHealth();
            org.bukkit.attribute.AttributeInstance attackSpeedAttribute =
                    player.getAttribute(Attribute.ATTACK_SPEED);
            this.attackSpeed = attackSpeedAttribute == null ? null : attackSpeedAttribute.getBaseValue();
            this.maximumNoDamageTicks = player.getMaximumNoDamageTicks();
            this.potionEffects = player.getActivePotionEffects();

            savedInfo.put(uuid, this);
        }

        protected Player getPlayer()
        {
            return Bukkit.getPlayer(uuid);
        }

        protected void restore(boolean quitting)
        {
            getPlayer().teleport(location);
            getPlayer().setGameMode(gameMode);
            getPlayer().getInventory().setContents(inventoryContents);
            getPlayer().getInventory().setArmorContents(armorContents);
            getPlayer().setLevel(xpLevel);
            getPlayer().setFoodLevel(foodLevel);
            getPlayer().setSaturation(saturation);
            getPlayer().setExhaustion(exhaustion);
            getPlayer().setMaxHealth(maxHealth);
            getPlayer().setHealth(health);
            getPlayer().setMaximumNoDamageTicks(maximumNoDamageTicks);
            if (attackSpeed != null && getPlayer().getAttribute(Attribute.ATTACK_SPEED) != null)
            {
                getPlayer().getAttribute(Attribute.ATTACK_SPEED).setBaseValue(attackSpeed);
            }
            for(PotionEffect current : getPlayer().getActivePotionEffects())
            {
                getPlayer().removePotionEffect(current.getType());
            }
            for(PotionEffect effect : potionEffects)
            {
                getPlayer().addPotionEffect(effect);
            }

            if(!quitting)
            {
                Duels plugin = JavaPlugin.getPlugin(Duels.class);
                getPlayer().getPersistentDataContainer().remove(new NamespacedKey(plugin, "world"));
                getPlayer().getPersistentDataContainer().remove(new NamespacedKey(plugin, "x"));
                getPlayer().getPersistentDataContainer().remove(new NamespacedKey(plugin, "y"));
                getPlayer().getPersistentDataContainer().remove(new NamespacedKey(plugin, "z"));
            }

            savedInfo.remove(uuid);
        }
    }

}
