package me.alphatct3209.duels.gui;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.arenas.ArenaSettings;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.modes.DuelMode;
import me.alphatct3209.duels.game.modes.DuelSelection;
import me.alphatct3209.duels.gui.config.MenuAction;
import me.alphatct3209.duels.gui.config.MenuConfiguration;
import me.alphatct3209.duels.gui.config.MenuOpener;
import me.alphatct3209.duels.gui.item.MenuItemFactory;
import me.alphatct3209.duels.gui.layout.PagedMenuLayout;
import me.alphatct3209.duels.gui.view.MenuInventoryHolder;
import me.alphatct3209.duels.gui.view.MenuView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Owns configurable opener items and the main, mode, map, and opponent menu flow. */
public final class DuelMenuManager implements Listener
{
    private static final String AUTOMATIC = "__automatic";
    private final Duels plugin;
    private final MenuConfiguration configuration;
    private final MenuItemFactory items;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, UUID> activeInventories = new HashMap<>();
    /** Original hotbar contents displaced by Force-Slot lobby items. */
    private final Map<UUID, Map<Integer, ItemStack>> displacedHotbar = new HashMap<>();

    public DuelMenuManager(Duels plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        configuration = new MenuConfiguration(plugin);
        items = new MenuItemFactory(plugin, configuration);
    }

    public void open(Player player)
    {
        if (!available(player)) return;
        Session session = session(player);
        session.context = MenuContext.QUEUE;
        session.target = null;
        openModes(player, 0);
    }

    public void openChallenge(Player player, Player target)
    {
        if (!available(player) || target == null || !target.isOnline())
        {
            message(player, "Messages.Player-Unavailable", "&cThat player is no longer available.");
            return;
        }
        Session session = session(player);
        session.context = MenuContext.CHALLENGE;
        session.target = target.getUniqueId();
        openModes(player, 0);
    }

    public void open(Player player, MenuAction action)
    {
        if (!available(player)) return;
        switch (action)
        {
            case DUEL_MENU -> open(player);
            case MODE_SELECTOR -> {
                Session session = session(player);
                session.context = MenuContext.QUEUE;
                session.target = null;
                openModes(player, 0);
            }
            case MAP_SELECTOR -> openMaps(player, 0);
            case SETTINGS -> plugin.getSettingsGui().open(player);
            case KIT_EDITOR -> openKitEditorSelection(player, 0);
            case PARTY -> {
                if (!player.hasPermission("duels.party"))
                {
                    message(player, "Messages.No-Permission",
                            "&cYou do not have permission to manage parties.");
                    return;
                }
                plugin.getPartyManager().getOrCreate(player);
                plugin.getPartyGui().open(player);
            }
        }
    }

    public void giveOpeners(Player player)
    {
        if (!configuration.enabled() || !configuration.giveOnJoin()
                || !player.hasPermission("duels.gui")
                || plugin.getArenaManager().getArena(player) != null)
        {
            return;
        }
        // Normalize an existing managed hotbar first so changed configuration never leaves duplicates.
        suspendOpeners(player);
        for (MenuOpener configured : configuration.openers())
        {
            if (!configured.enabled())
            {
                continue;
            }
            ItemStack opener = items.opener(configured);
            ItemStack preferred = player.getInventory().getItem(configured.slot());
            if (configured.forceSlot())
            {
                displacedHotbar.computeIfAbsent(player.getUniqueId(), ignored -> new LinkedHashMap<>())
                        .put(configured.slot(), preferred == null
                                ? new ItemStack(Material.AIR) : preferred.clone());
                player.getInventory().setItem(configured.slot(), opener);
            }
            else if (preferred == null || preferred.getType().isAir())
            {
                player.getInventory().setItem(configured.slot(), opener);
            }
            else
            {
                int empty = player.getInventory().firstEmpty();
                if (empty >= 0)
                {
                    player.getInventory().setItem(empty, opener);
                }
            }
        }
    }

    /** Restores every inventory slot temporarily displaced by managed lobby items. */
    public void suspendOpeners(Player player)
    {
        if (player == null) return;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++)
        {
            if (items.isOpener(player.getInventory().getItem(slot)))
            {
                player.getInventory().setItem(slot, null);
            }
        }
        Map<Integer, ItemStack> displaced = displacedHotbar.remove(player.getUniqueId());
        if (displaced == null) return;
        for (Map.Entry<Integer, ItemStack> entry : displaced.entrySet())
        {
            ItemStack original = entry.getValue();
            if (original == null || original.getType().isAir()) continue;
            ItemStack current = player.getInventory().getItem(entry.getKey());
            if (current == null || current.getType().isAir())
            {
                player.getInventory().setItem(entry.getKey(), original);
            }
            else
            {
                player.getInventory().addItem(original);
            }
        }
    }

    /** Reapplies the managed hotbar after arena membership/restoration has settled. */
    public void scheduleLobbyHotbar(Player player)
    {
        scheduleGive(player);
    }

    public void shutdown()
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuInventoryHolder)
            {
                player.closeInventory();
            }
            suspendOpeners(player);
        }
        activeInventories.clear();
        sessions.clear();
        displacedHotbar.clear();
    }

    private void openMain(Player player)
    {
        DuelSelection selection = plugin.getSelectionService().resolve(player.getUniqueId());
        DuelMode mode = plugin.getSelectionService().mode(selection);
        Arena selected = selectedArena(player, selection);
        int size = 27;
        Map<Integer, String> choices = new LinkedHashMap<>();
        UUID token = UUID.randomUUID();
        MenuInventoryHolder holder = new MenuInventoryHolder(token, player.getUniqueId(),
                MenuView.MAIN, 0, 1, choices);
        Inventory inventory = Bukkit.createInventory(holder, size,
                items.color(configuration.text("Menus.Main.Title", "&8Duels")));
        holder.attach(inventory);

        int modeSlot = configuration.slot("Menus.Main.Mode-Slot", 11, size);
        int mapSlot = configuration.slot("Menus.Main.Map-Slot", 13, size);
        int opponentSlot = configuration.slot("Menus.Main.Opponent-Slot", 15, size);
        int quickSlot = configuration.slot("Menus.Main.Quick-Join-Slot", 22, size);
        choices.put(modeSlot, "mode");
        choices.put(mapSlot, "map");
        choices.put(opponentSlot, "opponent");
        choices.put(quickSlot, "quick");

        inventory.setItem(modeSlot, icon(Material.NETHER_STAR,
                "Menus.Main.Mode-Name", "&eSelect Mode", "Menus.Main.Mode-Lore",
                Map.of("<mode>", mode.displayName()), false));
        inventory.setItem(mapSlot, icon(Material.FILLED_MAP,
                "Menus.Main.Map-Name", "&aSelect Map", "Menus.Main.Map-Lore",
                mapValues(selected), false));
        inventory.setItem(opponentSlot, icon(Material.DIAMOND_SWORD,
                "Menus.Main.Opponent-Name", "&cChallenge Player", "Menus.Main.Opponent-Lore",
                Map.of(), false));
        Map<String, String> quickValues = new HashMap<>(mapValues(selected));
        quickValues.put("<mode>", mode.displayName());
        inventory.setItem(quickSlot, icon(Material.ENDER_PEARL,
                "Menus.Main.Quick-Join-Name", "&bQuick Join", "Menus.Main.Quick-Join-Lore",
                quickValues, true));
        open(player, holder, inventory);
    }

    private void openModes(Player player, int requestedPage)
    {
        List<DuelMode> modes = plugin.getModeManager().enabledModes().stream()
                .sorted(Comparator.comparing(DuelMode::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        PagedMenuLayout.Page<DuelMode> page = PagedMenuLayout.page(modes, requestedPage);
        Map<Integer, String> choices = new LinkedHashMap<>();
        UUID token = UUID.randomUUID();
        MenuInventoryHolder holder = new MenuInventoryHolder(token, player.getUniqueId(),
                MenuView.MODE, page.index(), page.count(), choices);
        Inventory inventory = Bukkit.createInventory(holder, PagedMenuLayout.INVENTORY_SIZE,
                title("Menus.Mode.Title", "&8Select Mode &7(<page>/<pages>)", page.index(), page.count()));
        holder.attach(inventory);
        DuelSelection selected = plugin.getSelectionService().resolve(player.getUniqueId());
        page.slots().forEach((slot, mode) -> {
            choices.put(slot, mode.key().value());
            Material material = Material.matchMaterial(mode.icon());
            inventory.setItem(slot, icon(material == null ? Material.IRON_SWORD : material,
                    "Menus.Mode.Item-Name", "&e<mode>", "Menus.Mode.Item-Lore",
                    Map.of("<mode>", mode.displayName(), "<mode_key>", mode.key().value(),
                            "<kit>", mode.defaultKitKey()), mode.key().equals(selected.modeKey())));
        });
        int legacySlot = configuration.slot("Menus.Mode.Legacy-PvP-Slot", 47,
                PagedMenuLayout.INVENTORY_SIZE);
        choices.put(legacySlot, "__legacy_pvp");
        boolean legacy = plugin.getSelectionService().legacyPvp(player.getUniqueId());
        inventory.setItem(legacySlot, icon(legacy ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                "Menus.Mode.Legacy-PvP-Name", "&bLegacy 1.8 PvP: <legacy_pvp>",
                "Menus.Mode.Legacy-PvP-Lore", Map.of("<legacy_pvp>", enabled(legacy)), legacy));
        navigation(inventory, page.hasPrevious(), page.hasNext());
        open(player, holder, inventory);
    }

    private void openKits(Player player, DuelMode mode, int requestedPage)
    {
        session(player).pendingModeKey = mode.key().value();
        List<Kit> kits = mode.allowedKitKeys().stream()
                .map(plugin.getKitManager()::getKitByCanonicalKey)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Kit::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        PagedMenuLayout.Page<Kit> page = PagedMenuLayout.page(kits, requestedPage);
        Map<Integer, String> choices = new LinkedHashMap<>();
        UUID token = UUID.randomUUID();
        MenuInventoryHolder holder = new MenuInventoryHolder(token, player.getUniqueId(),
                MenuView.KIT, page.index(), page.count(), choices);
        Inventory inventory = Bukkit.createInventory(holder, PagedMenuLayout.INVENTORY_SIZE,
                title("Menus.Kit.Title", "&8Select Kit &7(<page>/<pages>)", page.index(), page.count()));
        holder.attach(inventory);
        DuelSelection selected = plugin.getSelectionService().resolve(player.getUniqueId());
        page.slots().forEach((slot, kit) -> {
            choices.put(slot, kit.getKey());
            inventory.setItem(slot, icon(Material.CHEST,
                    "Menus.Kit.Item-Name", "&a<kit>", "Menus.Kit.Item-Lore",
                    Map.of("<kit>", kit.getName(), "<kit_key>", kit.getKey(),
                            "<mode>", mode.displayName()),
                    mode.key().equals(selected.modeKey()) && kit.getKey().equals(selected.kitKey())));
        });
        navigation(inventory, page.hasPrevious(), page.hasNext());
        open(player, holder, inventory);
    }

    private void openMaps(Player player, int requestedPage)
    {
        DuelSelection selection = plugin.getSelectionService().resolve(player.getUniqueId());
        DuelMode mode = plugin.getSelectionService().mode(selection);
        List<Arena> arenas = plugin.getArenaManager().getArenaList().stream()
                .filter(arena -> arena.readyFor(mode))
                .filter(arena -> plugin.getArenaManager().isCompatible(arena, mode.key()))
                .sorted(Comparator.comparing(Arena::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        PagedMenuLayout.Page<Arena> page = PagedMenuLayout.page(arenas, requestedPage);
        Map<Integer, String> choices = new LinkedHashMap<>();
        UUID token = UUID.randomUUID();
        MenuInventoryHolder holder = new MenuInventoryHolder(token, player.getUniqueId(),
                MenuView.MAP, page.index(), page.count(), choices);
        Inventory inventory = Bukkit.createInventory(holder, PagedMenuLayout.INVENTORY_SIZE,
                title("Menus.Map.Title", "&8Select Map &7(<page>/<pages>)", page.index(), page.count()));
        holder.attach(inventory);
        Integer preferred = session(player).preferredArenaId;
        Kit selectedKit = plugin.getSelectionService().kit(selection);
        page.slots().forEach((slot, arena) -> {
            choices.put(slot, Integer.toString(arena.getId()));
            Map<String, String> values = new HashMap<>(mapValues(arena));
            values.put("<status>", status(arena, selection, selectedKit));
            inventory.setItem(slot, icon(Material.FILLED_MAP,
                    "Menus.Map.Item-Name", "&a<map>", "Menus.Map.Item-Lore", values,
                    preferred != null && preferred == arena.getId()));
        });
        choices.put(48, AUTOMATIC);
        inventory.setItem(48, icon(Material.COMPASS,
                "Menus.Map.Automatic-Name", "&bAutomatic Map", "Menus.Map.Automatic-Lore",
                Map.of(), preferred == null));
        navigation(inventory, page.hasPrevious(), page.hasNext());
        open(player, holder, inventory);
    }

    private void openOpponents(Player player, int requestedPage)
    {
        List<Player> opponents = Bukkit.getOnlinePlayers().stream()
                .map(Player.class::cast)
                .filter(other -> !other.equals(player))
                .filter(Player::isOnline)
                .filter(player::canSee)
                .filter(other -> plugin.getArenaManager().getArena(other) == null)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        PagedMenuLayout.Page<Player> page = PagedMenuLayout.page(opponents, requestedPage);
        Map<Integer, String> choices = new LinkedHashMap<>();
        UUID token = UUID.randomUUID();
        MenuInventoryHolder holder = new MenuInventoryHolder(token, player.getUniqueId(),
                MenuView.OPPONENT, page.index(), page.count(), choices);
        Inventory inventory = Bukkit.createInventory(holder, PagedMenuLayout.INVENTORY_SIZE,
                title("Menus.Opponent.Title", "&8Select Opponent &7(<page>/<pages>)",
                        page.index(), page.count()));
        holder.attach(inventory);
        DuelSelection selection = plugin.getSelectionService().resolve(player.getUniqueId());
        DuelMode mode = plugin.getSelectionService().mode(selection);
        Arena selected = selectedArena(player, selection);
        page.slots().forEach((slot, opponent) -> {
            choices.put(slot, opponent.getUniqueId().toString());
            Map<String, String> values = Map.of("<player>", opponent.getName(),
                    "<mode>", mode.displayName(), "<map>", selected == null ? "Automatic" : selected.getName());
            ItemStack item = icon(Material.PLAYER_HEAD,
                    "Menus.Opponent.Item-Name", "&c<player>", "Menus.Opponent.Item-Lore", values, false);
            if (item.getItemMeta() instanceof SkullMeta skull)
            {
                skull.setOwningPlayer(opponent);
                item.setItemMeta(skull);
            }
            inventory.setItem(slot, item);
        });
        navigation(inventory, page.hasPrevious(), page.hasNext());
        open(player, holder, inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event)
    {
        if (event.getView().getTopInventory().getHolder() instanceof MenuInventoryHolder holder)
        {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player) || !active(player, holder))
            {
                return;
            }
            int rawSlot = event.getRawSlot();
            if (rawSlot == PagedMenuLayout.PREVIOUS_SLOT && holder.page() > 0)
            {
                reopenPage(player, holder.view(), holder.page() - 1);
            }
            else if (rawSlot == PagedMenuLayout.NEXT_SLOT && holder.page() + 1 < holder.pageCount())
            {
                reopenPage(player, holder.view(), holder.page() + 1);
            }
            else if (rawSlot == PagedMenuLayout.BACK_SLOT && holder.view() != MenuView.MAIN)
            {
                if (holder.view() == MenuView.KIT)
                {
                    openModes(player, 0);
                }
                else if (holder.view() == MenuView.MODE
                        && session(player).context == MenuContext.CHALLENGE)
                {
                    activeInventories.remove(player.getUniqueId());
                    player.closeInventory();
                }
                else
                {
                    openMain(player);
                }
            }
            else
            {
                String choice = holder.choices().get(rawSlot);
                if (choice != null)
                {
                    choose(player, holder.view(), choice);
                }
            }
            return;
        }
        MenuOpener current = items.configured(event.getCurrentItem());
        MenuOpener cursor = items.configured(event.getCursor());
        MenuOpener hotbar = event.getHotbarButton() >= 0
                ? items.configured(event.getWhoClicked().getInventory().getItem(event.getHotbarButton())) : null;
        MenuOpener offhand = event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND
                ? items.configured(event.getWhoClicked().getInventory().getItemInOffHand()) : null;
        if ((current != null && current.locked()) || (cursor != null && cursor.locked())
                || (hotbar != null && hotbar.locked()) || (offhand != null && offhand.locked()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuDrag(InventoryDragEvent event)
    {
        if (event.getView().getTopInventory().getHolder() instanceof MenuInventoryHolder)
        {
            event.setCancelled(true);
            return;
        }
        if (event.getNewItems().values().stream().map(items::configured)
                .anyMatch(value -> value != null && value.locked()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event)
    {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK))
        {
            return;
        }
        MenuOpener opener = items.configured(event.getItem());
        if (opener == null || !opener.enabled())
        {
            return;
        }
        event.setCancelled(true);
        open(event.getPlayer(), opener.action());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event)
    {
        MenuOpener opener = items.configured(event.getItemDrop().getItemStack());
        if (opener != null && opener.locked())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event)
    {
        MenuOpener main = items.configured(event.getMainHandItem());
        MenuOpener off = items.configured(event.getOffHandItem());
        if ((main != null && main.locked()) || (off != null && off.locked()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event)
    {
        if (event.getInventory().getHolder() instanceof MenuInventoryHolder holder)
        {
            activeInventories.remove(event.getPlayer().getUniqueId(), holder.sessionToken());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        scheduleGive(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event)
    {
        event.getDrops().removeIf(item -> {
            MenuOpener opener = items.configured(item);
            return opener != null && opener.locked();
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event)
    {
        scheduleGive(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        suspendOpeners(event.getPlayer());
        activeInventories.remove(event.getPlayer().getUniqueId());
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void openKitEditorSelection(Player player, int requestedPage)
    {
        if (!player.hasPermission("duels.kits.edit"))
        {
            message(player, "Messages.No-Permission", "&cYou do not have permission to edit kit layouts.");
            return;
        }
        List<Kit> kits = plugin.getKitManager().getKitList().stream()
                .collect(java.util.stream.Collectors.toMap(Kit::getKey, value -> value,
                        (first, ignored) -> first, LinkedHashMap::new)).values().stream()
                .sorted(Comparator.comparing(Kit::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        PagedMenuLayout.Page<Kit> page = PagedMenuLayout.page(kits, requestedPage);
        Map<Integer, String> choices = new LinkedHashMap<>();
        UUID token = UUID.randomUUID();
        MenuInventoryHolder holder = new MenuInventoryHolder(token, player.getUniqueId(),
                MenuView.EDIT_KIT, page.index(), page.count(), choices);
        Inventory inventory = Bukkit.createInventory(holder, PagedMenuLayout.INVENTORY_SIZE,
                title("Menus.Kit-Editor-Selector.Title", "&8Kit Editor &7(<page>/<pages>)",
                        page.index(), page.count()));
        holder.attach(inventory);
        page.slots().forEach((slot, kit) -> {
            choices.put(slot, kit.getKey());
            inventory.setItem(slot, icon(Material.WRITABLE_BOOK,
                    "Menus.Kit-Editor-Selector.Item-Name", "&a<kit>",
                    "Menus.Kit-Editor-Selector.Item-Lore",
                    Map.of("<kit>", kit.getName(), "<kit_key>", kit.getKey()), false));
        });
        navigation(inventory, page.hasPrevious(), page.hasNext());
        open(player, holder, inventory);
    }

    private void choose(Player player, MenuView view, String choice)
    {
        switch (view)
        {
            case MAIN -> {
                switch (choice)
                {
                    case "mode" -> openModes(player, 0);
                    case "map" -> openMaps(player, 0);
                    case "opponent" -> openOpponents(player, 0);
                    case "quick" -> quickJoin(player);
                    default -> { }
                }
            }
            case MODE -> selectMode(player, choice);
            case KIT -> selectKit(player, choice);
            case MAP -> selectMap(player, choice);
            case OPPONENT -> challenge(player, choice);
            case EDIT_KIT -> {
                Kit kit = plugin.getKitManager().getKitByCanonicalKey(choice);
                if (kit != null) plugin.getKitLayoutEditor().open(player, kit);
            }
        }
    }

    private void selectMode(Player player, String key)
    {
        if (key.equals("__legacy_pvp"))
        {
            boolean changed = !plugin.getSelectionService().legacyPvp(player.getUniqueId());
            plugin.getSelectionService().setLegacyPvp(player.getUniqueId(), changed);
            openModes(player, 0);
            return;
        }
        DuelMode mode = plugin.getModeManager().resolve(key).orElse(null);
        if (mode == null || !mode.enabled()) return;
        List<Kit> kits = mode.allowedKitKeys().stream()
                .map(plugin.getKitManager()::getKitByCanonicalKey)
                .filter(Objects::nonNull).toList();
        if (kits.size() > 1)
        {
            openKits(player, mode, 0);
            return;
        }
        String kitKey = kits.isEmpty() ? mode.defaultKitKey() : kits.getFirst().getKey();
        completeSelection(player, mode, kitKey);
    }

    private void selectKit(Player player, String kitKey)
    {
        String modeKey = session(player).pendingModeKey;
        DuelMode mode = plugin.getModeManager().resolve(modeKey).orElse(null);
        if (mode == null || !mode.allowsKit(kitKey)
                || plugin.getKitManager().getKitByCanonicalKey(kitKey) == null)
        {
            openModes(player, 0);
            return;
        }
        completeSelection(player, mode, kitKey);
    }

    private void completeSelection(Player player, DuelMode mode, String kitKey)
    {
        DuelSelection selection;
        try
        {
            selection = plugin.getSelectionService().select(player.getUniqueId(),
                    mode.key().value(), kitKey);
        }
        catch (IllegalArgumentException exception)
        {
            message(player, "Messages.Join-Failed", "&cThat mode or kit is no longer available.");
            return;
        }
        Session current = session(player);
        boolean completed;
        if (current.context == MenuContext.CHALLENGE)
        {
            Player target = current.target == null ? null : Bukkit.getPlayer(current.target);
            Kit kit = plugin.getSelectionService().kit(selection);
            completed = target != null && plugin.getChallengeManager().send(player, target, kit);
        }
        else
        {
            completed = plugin.getQueueManager().join(player, selection,
                    current.preferredArenaId);
        }
        if (completed)
        {
            activeInventories.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    private void selectMap(Player player, String choice)
    {
        if (choice.equals(AUTOMATIC))
        {
            session(player).preferredArenaId = null;
            openMain(player);
            return;
        }
        Arena arena;
        try
        {
            arena = plugin.getArenaManager().getArena(Integer.parseInt(choice));
        }
        catch (NumberFormatException exception)
        {
            return;
        }
        DuelSelection selection = plugin.getSelectionService().resolve(player.getUniqueId());
        DuelMode mode = plugin.getSelectionService().mode(selection);
        if (arena == null || !arena.readyFor(mode)
                || !plugin.getArenaManager().isCompatible(arena, mode.key()))
        {
            message(player, "Messages.Map-Incompatible", "&cThat map does not support your selected mode.");
            return;
        }
        session(player).preferredArenaId = arena.getId();
        openMain(player);
    }

    private void challenge(Player player, String rawUuid)
    {
        Player target;
        try
        {
            target = Bukkit.getPlayer(UUID.fromString(rawUuid));
        }
        catch (IllegalArgumentException exception)
        {
            target = null;
        }
        if (target == null || !target.isOnline() || !player.canSee(target)
                || plugin.getArenaManager().getArena(target) != null)
        {
            message(player, "Messages.Player-Unavailable", "&cThat player is no longer available.");
            openOpponents(player, 0);
            return;
        }
        DuelSelection selection = plugin.getSelectionService().resolve(player.getUniqueId());
        Kit kit = plugin.getSelectionService().kit(selection);
        Arena selected = selectedArena(player, selection);
        if (plugin.getChallengeManager().send(player, target, kit,
                selected == null ? null : selected.getId()))
        {
            activeInventories.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    private void quickJoin(Player player)
    {
        DuelSelection selection = plugin.getSelectionService().resolve(player.getUniqueId());
        if (plugin.getQueueManager().join(player, selection,
                session(player).preferredArenaId))
        {
            activeInventories.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    private Arena selectedArena(Player player, DuelSelection selection)
    {
        Session session = session(player);
        if (session.preferredArenaId == null)
        {
            return null;
        }
        Arena arena = plugin.getArenaManager().getArena(session.preferredArenaId);
        DuelMode mode = plugin.getSelectionService().mode(selection);
        if (arena == null || !arena.readyFor(mode)
                || !plugin.getArenaManager().isCompatible(arena, mode.key()))
        {
            session.preferredArenaId = null;
            return null;
        }
        return arena;
    }

    private Map<String, String> mapValues(Arena arena)
    {
        Map<String, String> values = new HashMap<>();
        values.put("<map>", arena == null ? "Automatic" : arena.getName());
        values.put("<block_break>", arena == null ? "Varies" : enabled(
                arena.getSettings().get(ArenaSettings.Flag.BLOCK_BREAK)));
        values.put("<block_place>", arena == null ? "Varies" : enabled(
                arena.getSettings().get(ArenaSettings.Flag.BLOCK_PLACE)));
        return values;
    }

    private String status(Arena arena, DuelSelection selection, Kit kit)
    {
        boolean available = plugin.getArenaManager().canAdmit(arena, selection, kit);
        return items.color(configuration.text(available
                ? "Menus.Common.Available" : "Menus.Common.Unavailable",
                available ? "&aAvailable" : "&cUnavailable"));
    }

    private String enabled(boolean value)
    {
        return items.color(configuration.text(value ? "Menus.Common.Enabled" : "Menus.Common.Disabled",
                value ? "&aEnabled" : "&cDisabled"));
    }

    private ItemStack icon(Material material, String namePath, String fallbackName,
                           String lorePath, Map<String, String> replacements, boolean glow)
    {
        String name = replace(configuration.text(namePath, fallbackName), replacements);
        List<String> lore = configuration.lines(lorePath).stream()
                .map(line -> replace(line, replacements)).toList();
        return items.icon(material, name, lore, glow);
    }

    private String title(String path, String fallback, int page, int pages)
    {
        return items.color(configuration.text(path, fallback)
                .replace("<page>", Integer.toString(page + 1))
                .replace("<pages>", Integer.toString(pages)));
    }

    private String replace(String value, Map<String, String> replacements)
    {
        String result = value;
        for (Map.Entry<String, String> replacement : replacements.entrySet())
        {
            result = result.replace(replacement.getKey(), replacement.getValue());
        }
        return result;
    }

    private void navigation(Inventory inventory, boolean previous, boolean next)
    {
        if (previous)
        {
            inventory.setItem(PagedMenuLayout.PREVIOUS_SLOT, items.icon(Material.ARROW,
                    configuration.text("Menus.Common.Previous", "&ePrevious page"), List.of(), false));
        }
        inventory.setItem(PagedMenuLayout.BACK_SLOT, items.icon(Material.BARRIER,
                configuration.text("Menus.Common.Back", "&cBack"), List.of(), false));
        if (next)
        {
            inventory.setItem(PagedMenuLayout.NEXT_SLOT, items.icon(Material.ARROW,
                    configuration.text("Menus.Common.Next", "&eNext page"), List.of(), false));
        }
    }

    private void reopenPage(Player player, MenuView view, int page)
    {
        switch (view)
        {
            case MODE -> openModes(player, page);
            case KIT -> {
                DuelMode mode = plugin.getModeManager().resolve(session(player).pendingModeKey).orElse(null);
                if (mode == null) openModes(player, 0); else openKits(player, mode, page);
            }
            case MAP -> openMaps(player, page);
            case OPPONENT -> openOpponents(player, page);
            case EDIT_KIT -> openKitEditorSelection(player, page);
            case MAIN -> openMain(player);
        }
    }

    private void open(Player player, MenuInventoryHolder holder, Inventory inventory)
    {
        activeInventories.put(player.getUniqueId(), holder.sessionToken());
        player.openInventory(inventory);
    }

    private boolean active(Player player, MenuInventoryHolder holder)
    {
        return holder.viewer().equals(player.getUniqueId())
                && holder.sessionToken().equals(activeInventories.get(player.getUniqueId()));
    }

    private boolean available(Player player)
    {
        if (!configuration.enabled())
        {
            return false;
        }
        if (!player.hasPermission("duels.gui"))
        {
            message(player, "Messages.No-Permission", "&cYou do not have permission to use the duel menu.");
            return false;
        }
        if (plugin.getArenaManager().getArena(player) != null)
        {
            message(player, "Messages.Unavailable", "&cThe duel menu is unavailable in an arena.");
            return false;
        }
        return true;
    }

    private boolean containsOpener(Player player, String id)
    {
        for (ItemStack item : player.getInventory().getContents())
        {
            MenuOpener configured = items.configured(item);
            if (configured != null && configured.id().equals(id))
            {
                return true;
            }
        }
        return false;
    }

    private void scheduleGive(Player player)
    {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline())
            {
                giveOpeners(online);
            }
        }, configuration.giveDelayTicks());
    }

    private void message(Player player, String path, String fallback, String... replacements)
    {
        for (String configured : configuration.messageLines(path, fallback))
        {
            String value = configured;
            for (int index = 0; index + 1 < replacements.length; index += 2)
                value = value.replace(replacements[index], replacements[index + 1]);
            if (!value.isEmpty()) player.sendMessage(items.color(value));
        }
    }

    private Session session(Player player)
    {
        return sessions.computeIfAbsent(player.getUniqueId(), ignored -> new Session());
    }

    private enum MenuContext
    {
        QUEUE,
        CHALLENGE
    }

    private static final class Session
    {
        private Integer preferredArenaId;
        private MenuContext context = MenuContext.QUEUE;
        private UUID target;
        private String pendingModeKey;
    }
}
