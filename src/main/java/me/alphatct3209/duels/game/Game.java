package me.alphatct3209.duels.game;

import me.alphatct3209.duels.divisions.Division;
import me.alphatct3209.duels.divisions.DivisionManager;
import me.alphatct3209.duels.divisions.DivisionProgress;
import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.arenas.ArenaSettings;
import me.alphatct3209.duels.game.combat.CombatProfile;
import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.modes.*;
import me.alphatct3209.duels.game.modes.bedwars.BedWarsLoadout;
import me.alphatct3209.duels.game.modes.bedwars.BedWarsUpgrade;
import me.alphatct3209.duels.stats.db.StatisticsDatabase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Game
{
    public static final String OBJECTIVE_KEY = "objective_item";
    private final Arena arena;
    private Kit playerOneKit;
    private Kit playerTwoKit;
    private DuelMode mode;
    private ModeRuntimeState state;
    private ModeObjective objective;
    private ModeController controller;
    private org.bukkit.scheduler.BukkitTask timeoutTask;
    private final ModeRuntimeMechanics mechanics;
    private boolean terminal;
    private long startedAtMillis;
    private int roundNumber;
    private final Map<UUID, BedWarsLoadout> bedWarsLoadouts = new HashMap<>();

    public Game(Arena arena)
    {
        this.arena = arena;
        this.mechanics = new ModeRuntimeMechanics(arena,
                () -> !terminal && arena.getGameState() == GameState.PLAYING);
    }

    public void start()
    {
        if (arena.getGameState() != GameState.COUNTDOWN) return;
        Player one = requireOnline(arena.getPlayerOne(), "first");
        Player two = requireOnline(arena.getPlayerTwo(), "second");
        DuelSelection first = arena.getAdmittedSelection(one.getUniqueId());
        DuelSelection second = arena.getAdmittedSelection(two.getUniqueId());
        if (!first.modeKey().equals(second.modeKey())) throw new IllegalStateException("Arena participants have mismatched captured modes");
        mode = arena.getModeManager().require(first.modeKey());
        if (!arena.readyFor(mode)) throw new IllegalStateException("Arena is missing required markers for " + mode.key());
        playerOneKit = arena.getAdmittedKit(one.getUniqueId());
        playerTwoKit = arena.getAdmittedKit(two.getUniqueId());
        state = new ModeRuntimeState(one.getUniqueId(), two.getUniqueId(), mode.targetScore());
        objective = new ModeObjective(mode, state);
        controller = ModeControllerFactory.create(mode.handlerType(), arena.getObjectiveSettings().boxingMercyLead());
        if (mode.handlerType() == ModeHandlerType.BED_WARS)
        {
            bedWarsLoadouts.put(one.getUniqueId(), new BedWarsLoadout());
            bedWarsLoadouts.put(two.getUniqueId(), new BedWarsLoadout());
        }
        arena.setGameState(GameState.PLAYING);
        startedAtMillis = System.currentTimeMillis();
        resetPlayer(one, arena.getSpawnOne());
        resetPlayer(two, arena.getSpawnTwo());
        mechanics.start(mode.handlerType());
        roundNumber = 1;
        sendRoundMessages();
        Duration duration = mode.durationPolicy().maximumDuration();
        if (!duration.isZero())
        {
            long seconds = duration.getSeconds();
            timeoutTask = arena.schedule(this::timeout, Math.max(1L, seconds > Long.MAX_VALUE / 20L ? Long.MAX_VALUE : seconds * 20L));
        }
    }

    public ModeAction meleeHit(Player attacker, Player victim)
    {
        if (!activeOpponents(attacker, victim) || !mode.combat().pvp() || !mode.combat().melee()) return ModeAction.none();
        return apply(controller.meleeHit(state, attacker.getUniqueId(), victim.getUniqueId()));
    }

    public ModeAction rangedHit(Player attacker, Player victim)
    {
        if (!activeOpponents(attacker, victim) || !mode.combat().pvp() || !mode.combat().projectiles()) return ModeAction.none();
        return apply(controller.rangedHit(state, attacker.getUniqueId(), victim.getUniqueId()));
    }

    public ModeAction death(Player victim)
    {
        if (!active(victim) || terminal) return ModeAction.none();
        state.recordDeath(victim.getUniqueId());
        BedWarsLoadout bedWarsLoadout = bedWarsLoadouts.get(victim.getUniqueId());
        if (bedWarsLoadout != null) bedWarsLoadout.afterDeath();
        return apply(controller.death(state, victim.getUniqueId()));
    }

    public boolean breakBed(Player breaker, Location block)
    {
        if (!active(breaker) || terminal || block == null
                || !org.bukkit.Tag.BEDS.isTagged(block.getBlock().getType())) return false;
        UUID owner = markerOwner(block, "bed_1", "bed_2");
        if (owner == null || owner.equals(breaker.getUniqueId())) return false;
        boolean alive = state.bedAlive(owner);
        apply(controller.bedBreak(state, owner, breaker.getUniqueId()));
        boolean destroyed = alive && !state.bedAlive(owner);
        if (destroyed)
        {
            Player victim = Bukkit.getPlayer(owner);
            arena.sendConfiguredToPlayers("Messages.Bed-Destroyed", Map.of(
                    "<player>", victim == null ? "A team" : victim.getName(),
                    "<breaker>", breaker.getName()),
                    "&cBED DESTROYED! &f<player> can no longer respawn.");
        }
        return destroyed;
    }

    public void movement(Player player, Location to)
    {
        if (!active(player) || terminal || to == null || player.getGameMode() == GameMode.SPECTATOR) return;
        if (to.getY() <= arena.deathY() && (controller.ringout() || controller.finishBased()))
        {
            death(player); return;
        }
        if (controller.goalBased())
        {
            String enemyGoal = player.getUniqueId().equals(arena.getPlayerOne()) ? "goal_2" : "goal_1";
            if (near(to, arena.getPoint(enemyGoal).orElse(null), arena.getObjectiveSettings().radius("goal")))
                apply(controller.goal(state, player.getUniqueId()));
        }
        if (controller.finishBased())
        {
            List<Map.Entry<String, Location>> checkpoints = arena.getPoints().entrySet().stream()
                    .filter(entry -> entry.getKey().matches("checkpoint_[1-9][0-9]*"))
                    .sorted(Comparator.comparingInt(entry -> Integer.parseInt(entry.getKey().substring(11))))
                    .toList();
            int next = state.checkpoint(player.getUniqueId()) + 1;
            for (Map.Entry<String, Location> checkpoint : checkpoints)
            {
                int index = Integer.parseInt(checkpoint.getKey().substring(11));
                if (index == next && near(to, checkpoint.getValue(), arena.getObjectiveSettings().radius("checkpoint")))
                { state.checkpoint(player.getUniqueId(), index); break; }
            }
            if (near(to, arena.getPoint("finish").orElse(null), arena.getObjectiveSettings().radius("finish"))
                    && state.checkpoint(player.getUniqueId()) == checkpoints.size())
                apply(controller.finish(state, player.getUniqueId()));
        }
    }

    private UUID markerOwner(Location block, String first, String second)
    {
        if (nearBlock(block, arena.getPoint(first).orElse(null), 1, 2)) return arena.getPlayerOne();
        if (nearBlock(block, arena.getPoint(second).orElse(null), 1, 2)) return arena.getPlayerTwo();
        return null;
    }

    private boolean nearBlock(Location block, Location marker, int horizontal, int vertical)
    {
        return block != null && marker != null && block.getWorld() == marker.getWorld()
                && Math.abs(block.getBlockX() - marker.getBlockX()) <= horizontal
                && Math.abs(block.getBlockY() - marker.getBlockY()) <= vertical
                && Math.abs(block.getBlockZ() - marker.getBlockZ()) <= horizontal;
    }

    private boolean sameBlock(Location a, Location b)
    {
        return a != null && b != null && a.getWorld() == b.getWorld()
                && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }
    private boolean near(Location a, Location b, double radius)
    {
        return a != null && b != null && a.getWorld() == b.getWorld() && a.distanceSquared(b) <= radius * radius;
    }

    private ModeAction apply(ModeAction action)
    {
        if (action == null || terminal) return ModeAction.none();
        switch (action.type())
        {
            case NONE -> { }
            case RESPAWN -> respawn(action.target());
            case ROUND_RESET -> roundReset();
            case WIN -> completeWin(action.winner());
            case DRAW -> draw();
        }
        return action;
    }

    private void respawn(UUID playerId)
    {
        Player player = requireOnline(playerId, "respawning");
        Location destination = playerId.equals(arena.getPlayerOne()) ? arena.getSpawnOne() : arena.getSpawnTwo();
        if (controller.finishBased() && state.checkpoint(playerId) > 0)
            destination = arena.getPoint("checkpoint_" + state.checkpoint(playerId)).orElse(destination);
        if (mode.handlerType() != ModeHandlerType.BED_WARS)
        {
            resetPlayer(player, destination);
            return;
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.SPECTATOR);
        if (!arena.teleportSafely(player, destination)) return;
        int seconds = arena.getObjectiveSettings().bedWarsRespawnSeconds();
        arena.sendConfigured(player, "Messages.Respawn-Wait", Map.of(
                "<seconds>", seconds, "<player>", player.getName()),
                "&eYou will respawn in <seconds> seconds...");
        Location finalDestination = destination.clone();
        arena.schedule(() -> {
            Player current = Bukkit.getPlayer(playerId);
            if (current != null && active(current) && !terminal)
            {
                resetPlayer(current, finalDestination);
                arena.sendConfigured(current, "Messages.Respawned", Map.of(
                        "<player>", current.getName()), "&aRespawned!");
            }
        }, Math.multiplyExact((long) seconds, 20L));
    }

    private void roundReset()
    {
        resetPlayer(requireOnline(arena.getPlayerOne(), "first"), arena.getSpawnOne());
        resetPlayer(requireOnline(arena.getPlayerTwo(), "second"), arena.getSpawnTwo());
        roundNumber++;
        sendRoundMessages();
    }

    private void sendRoundMessages()
    {
        for (UUID playerId : List.of(arena.getPlayerOne(), arena.getPlayerTwo()))
        {
            Player player = requireOnline(playerId, "round");
            UUID opponentId = state.opponent(playerId);
            Player opponent = requireOnline(opponentId, "round opponent");
            Kit kit = getKit(playerId);
            arena.sendConfigured(player, "Messages.Round",
                    Map.of("<round>", roundNumber, "<player>", player.getName(),
                            "<opponent>", opponent.getName(), "<mode>", mode.displayName(),
                            "<mode_key>", mode.key().value(), "<kit>", kit.getName(),
                            "<score>", state.score(playerId),
                            "<opponent_score>", state.score(opponentId),
                            "<legacy_pvp>", arena.usesLegacyPvp()),
                    "&eRound <round> &7- &f<player> &7vs &f<opponent>");
        }
    }

    private void resetPlayer(Player player, Location destination)
    {
        if (!arena.teleportSafely(player, destination))
            throw new IllegalStateException("Arena destination became unavailable before teleport");
        player.setFireTicks(0);
        player.setFallDistance(0F);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        arena.getPlayerKitLayoutManager().apply(player, getKit(player.getUniqueId()));
        if (mode.handlerType() == ModeHandlerType.BED_WARS)
            bedWarsLoadouts.get(player.getUniqueId()).apply(player, teamColor(player.getUniqueId()));
        player.setGameMode(GameMode.SURVIVAL);
        CombatProfile combat = CombatProfile.resolve(
                arena.usesLegacyPvp(), mode.combat().noHitDelay());
        player.setMaximumNoDamageTicks(combat.maximumNoDamageTicks());
        if (player.getAttribute(Attribute.ATTACK_SPEED) != null)
            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(combat.attackSpeed());
        if (!controller.inventoryAllowed())
        {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
        }
        injectEssentials(player);
        injectModeConsumables(player);
    }

    private void injectModeConsumables(Player player)
    {
        me.alphatct3209.duels.game.items.GoldenHead goldenHead = arena.getGoldenHead();
        if (!mode.key().value().equals("uhc") || !goldenHead.enabled()
                || !goldenHead.giveOnUhcStart()) return;
        boolean present = java.util.Arrays.stream(player.getInventory().getStorageContents())
                .anyMatch(me.alphatct3209.duels.game.items.GoldenHead::isGoldenHead);
        if (!present)
            player.getInventory().addItem(goldenHead.create(player, goldenHead.uhcAmount()));
    }

    private void injectEssentials(Player player)
    {
        for (ModeController.EssentialItem item : controller.essentialItems())
        {
            Material material = switch (item)
            {
                case RAILGUN -> Material.BLAZE_ROD;
                case DASH -> Material.FEATHER;
                case COMPASS -> Material.COMPASS;
                case SPLEEF_SHOVEL -> Material.DIAMOND_SHOVEL;
            };
            if (player.getInventory().contains(material)) continue;
            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            meta.getPersistentDataContainer().set(new NamespacedKey("hyxduels", OBJECTIVE_KEY),
                    PersistentDataType.STRING, item.name());
            meta.setDisplayName(ChatColor.AQUA + item.name().replace('_', ' '));
            stack.setItemMeta(meta);
            player.getInventory().addItem(stack);
        }
    }

    public boolean canPurchaseBedWarsUpgrade(Player player, BedWarsUpgrade upgrade)
    {
        BedWarsLoadout loadout = bedWarsLoadouts.get(player.getUniqueId());
        return active(player) && isMode(ModeHandlerType.BED_WARS)
                && loadout != null && loadout.canPurchase(upgrade);
    }

    public boolean purchaseBedWarsUpgrade(Player player, BedWarsUpgrade upgrade)
    {
        if (!canPurchaseBedWarsUpgrade(player, upgrade)) return false;
        BedWarsLoadout loadout = bedWarsLoadouts.get(player.getUniqueId());
        if (!loadout.purchase(upgrade)) return false;
        loadout.apply(player, teamColor(player.getUniqueId()));
        return true;
    }

    private org.bukkit.Color teamColor(UUID player)
    {
        return player.equals(arena.getPlayerOne())
                ? org.bukkit.Color.fromRGB(0xB02E26) : org.bukkit.Color.fromRGB(0x3C44AA);
    }

    public boolean isMode(ModeHandlerType type)
    {
        return mode != null && mode.handlerType() == type && arena.getGameState() == GameState.PLAYING && !terminal;
    }

    private Kit getKit(UUID player) { return player.equals(arena.getPlayerOne()) ? playerOneKit : playerTwoKit; }
    private boolean active(Player player) { return arena.getGameState() == GameState.PLAYING && arena.isParticipant(player.getUniqueId()); }
    private boolean activeOpponents(Player attacker, Player victim)
    { return active(attacker) && active(victim) && arena.areOpponents(victim.getUniqueId(), attacker.getUniqueId()); }

    private void completeWin(UUID winner)
    {
        if (winner == null) { draw(); return; }
        kill(requireOnline(state.opponent(winner), "losing"));
    }

    public void announceKill(Player killer, Player victim, double victimHealth)
    {
        if (killer == null || victim == null || !activeOpponents(killer, victim)) return;
        double killerHealth = Math.max(0D, killer.getHealth());
        Map<String, Object> values = Map.ofEntries(
                Map.entry("<killer>", killer.getName()),
                Map.entry("<victim>", victim.getName()),
                Map.entry("<killer_health>", health(killerHealth)),
                Map.entry("<killer_max_health>", health(killer.getMaxHealth())),
                Map.entry("<victim_health>", health(Math.max(0D, victimHealth))),
                Map.entry("<victim_max_health>", health(victim.getMaxHealth())),
                Map.entry("<health>", health(killerHealth)),
                Map.entry("<round>", roundNumber),
                Map.entry("<mode>", mode.displayName()),
                Map.entry("<mode_key>", mode.key().value()),
                Map.entry("<kit>", getKit(killer.getUniqueId()).getName()),
                Map.entry("<killer_score>", state.score(killer.getUniqueId())),
                Map.entry("<victim_score>", state.score(victim.getUniqueId())));
        arena.sendConfiguredToPlayers("Messages.Kill", values,
                "&c<victim> &7was killed by &a<killer> &7(&c<killer_health>❤&7).");
        Location blast = victim.getLocation().clone().add(0D, 1D, 0D);
        for (UUID viewerId : arena.getPlayers())
        {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && arena.getSocialManager().preferences(viewerId).blastParticles())
                viewer.spawnParticle(org.bukkit.Particle.EXPLOSION, blast, 4,
                        0.35D, 0.45D, 0.35D, 0D);
        }
    }

    private String health(double value)
    {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    public boolean kill(Player defeatedPlayer)
    {
        if (terminal || !arena.beginDuelCompletion()) return false;
        terminal = true; cancelTimeout();
        try
        {
            UUID loserId = defeatedPlayer.getUniqueId();
            UUID winnerId = state.opponent(loserId);
            Player winner = requireOnline(winnerId, "winning");
            Kit winnerKit = getKit(winnerId);
            StatisticsDatabase database = arena.getStatisticsDatabase();
            StatisticsDatabase.DuelWinUpdate update =
                    database.recordDuelWin(winnerId, loserId, mode.key());
            arena.sendConfiguredToPlayers("Messages.Win", Map.of(
                    "<winner>", winner.getName(), "<loser>", defeatedPlayer.getName(),
                    "<mode>", mode.displayName(), "<mode_key>", mode.key().value(),
                    "<kit>", winnerKit.getName(), "<health>", health(winner.getHealth())),
                    "&a<winner> wins!");
            sendDivisionMessages(winner, winnerKit, mode,
                    update.previousModeWins(), update.currentModeWins());
            return true;
        }
        finally { arena.requestLeaderboardRefresh(); arena.finishDuelCompletion(); }
    }

    private void timeout()
    {
        timeoutTask = null;
        if (!terminal && arena.getGameState() == GameState.PLAYING) apply(controller.timeout(state, mode.durationPolicy().timeoutPolicy()));
    }
    private void draw()
    {
        if (terminal || !arena.beginDuelCompletion()) return;
        terminal = true; cancelTimeout();
        try { arena.sendConfiguredToPlayers("Messages.Draw", Map.of(
                "<mode>", mode.displayName(), "<mode_key>", mode.key().value()),
                "&eThe duel ended in a draw."); }
        finally { arena.finishDuelCompletion(); }
    }
    private void cancelTimeout()
    {
        if (timeoutTask != null) { timeoutTask.cancel(); timeoutTask = null; }
        mechanics.cancel();
    }

    public void shutdown() { cancelTimeout(); }

    public long remainingSeconds()
    {
        if (mode == null || mode.durationPolicy().maximumDuration().isZero()) return 0L;
        long elapsed = Math.max(0L, (System.currentTimeMillis() - startedAtMillis) / 1000L);
        return Math.max(0L, mode.durationPolicy().maximumDuration().getSeconds() - elapsed);
    }
    public int score(UUID player) { return state == null || !state.participant(player) ? 0 : state.score(player); }
    public int checkpoint(UUID player) { return state == null || !state.participant(player) ? 0 : state.checkpoint(player); }
    public boolean bedAlive(UUID player) { return state != null && state.participant(player) && state.bedAlive(player); }
    public Optional<ModeRuntimeState> getRuntimeState() { return Optional.ofNullable(state); }
    public Optional<ModeController> getController() { return Optional.ofNullable(controller); }
    public Optional<ModeObjective> getObjective() { return Optional.ofNullable(objective); }
    public Optional<DuelMode> getMode() { return Optional.ofNullable(mode); }

    /** Compatibility hooks now pass through controller behavior. */
    public boolean recordObjectivePoint(Player player)
    {
        if (state == null || !state.participant(player.getUniqueId())) return false;
        ModeAction action = controller.goalBased() ? controller.goal(state, player.getUniqueId()) : ModeAction.none();
        apply(action); return action.type() != ModeAction.Type.NONE;
    }
    public boolean eliminate(Player player) { return death(player).terminal(); }
    public boolean destroyBed(Player owner)
    {
        if (state == null || !state.participant(owner.getUniqueId())) return false;
        return state.breakBed(owner.getUniqueId());
    }

    private void sendDivisionMessages(Player winner, Kit kit, DuelMode selectedMode, int oldWins, int newWins)
    {
        DivisionManager divisions = arena.getDivisionManager();
        Optional<Division> oldDivision = divisions.getCurrentDivision(oldWins);
        Optional<Division> newDivision = divisions.getCurrentDivision(newWins);
        for (Division promoted : divisions.getCrossedDivisions(oldWins, newWins))
        {
            sendConfiguredMessage(winner, "Messages.Division-Promotion", kit, selectedMode, newWins,
                    oldDivision, Optional.of(promoted), divisions.getProgress(newWins));
            divisions.executeRewards(winner, selectedMode.key().value(), promoted, newWins);
        }
        sendConfiguredMessage(winner, "Messages.Division-Progress", kit, selectedMode, newWins,
                oldDivision, newDivision, divisions.getProgress(newWins));
    }
    private void sendConfiguredMessage(Player player, String path, Kit kit, DuelMode selectedMode, int wins,
                                       Optional<Division> oldDivision, Optional<Division> currentDivision,
                                       DivisionProgress progress)
    {
        Map<String, Object> values = Map.ofEntries(
                Map.entry("<player>", player.getName()),
                Map.entry("<gamemode>", selectedMode.displayName()),
                Map.entry("<gamemode_key>", selectedMode.key().value()),
                Map.entry("<mode>", selectedMode.displayName()),
                Map.entry("<mode_key>", selectedMode.key().value()),
                Map.entry("<kit>", kit.getName()),
                Map.entry("<wins>", wins),
                Map.entry("<old_division>", oldDivision.map(Division::displayName).orElse("Unranked")),
                Map.entry("<division>", currentDivision.map(Division::displayName).orElse("Unranked")),
                Map.entry("<next_division>", progress.next().map(Division::displayName).orElse("Maximum")),
                Map.entry("<progress>", progress.winsIntoStep()),
                Map.entry("<required>", progress.winsForStep()),
                Map.entry("<wins_to_next>", progress.next().map(d -> d.requiredWins() - wins).orElse(0L)),
                Map.entry("<progress_percent>", Math.round(progress.fraction() * 100D)));
        arena.sendConfigured(player, path, values);
    }
    private Player requireOnline(UUID uuid, String description)
    {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) throw new IllegalStateException("The " + description + " duel player must be online");
        return player;
    }
}
