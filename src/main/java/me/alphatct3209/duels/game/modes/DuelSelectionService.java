package me.alphatct3209.duels.game.modes;

import me.alphatct3209.duels.game.kits.Kit;
import me.alphatct3209.duels.game.kits.KitManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DuelSelectionService
{
    private final ModeManager modes;
    private final KitManager kits;
    private final Map<UUID, DuelSelection> selections = new HashMap<>();
    private final Map<UUID, Boolean> legacyPreferences = new HashMap<>();

    public DuelSelectionService(ModeManager modes, KitManager kits)
    {
        this.modes = Objects.requireNonNull(modes, "modes");
        this.kits = Objects.requireNonNull(kits, "kits");
    }

    public DuelSelection select(UUID player, String modeInput, String kitInput)
    {
        DuelMode mode = modes.resolve(modeInput).orElseThrow(
                () -> new IllegalArgumentException("Unknown duel mode '" + modeInput + "'"));
        if (!mode.enabled()) throw new IllegalArgumentException("Duel mode '" + mode.key() + "' is disabled");
        DuelSelection selection = create(mode, kitInput, legacyPvp(player));
        selections.put(Objects.requireNonNull(player, "player"), selection);
        return selection;
    }

    public DuelSelection setLegacyPvp(UUID player, boolean enabled)
    {
        Objects.requireNonNull(player, "player");
        legacyPreferences.put(player, enabled);
        DuelSelection current = resolve(player);
        DuelSelection changed = new DuelSelection(current.modeKey(), current.kitKey(), enabled);
        selections.put(player, changed);
        return changed;
    }

    public boolean legacyPvp(UUID player)
    {
        DuelSelection selection = selections.get(player);
        return selection == null ? legacyPreferences.getOrDefault(player, false) : selection.legacyPvp();
    }

    public DuelSelection create(DuelMode mode, String kitInput)
    {
        return create(mode, kitInput, false);
    }

    public DuelSelection create(DuelMode mode, String kitInput, boolean legacyPvp)
    {
        String requested = kitInput == null || kitInput.isBlank() ? mode.defaultKitKey() : kitInput;
        Kit kit = kits.getKitByNameOrKey(requested);
        if (kit == null) throw new IllegalArgumentException("Unknown kit '" + requested + "'");
        return SelectionRules.select(mode, kit.getKey(),
                kits.getKitList().stream().map(Kit::getKey).toList(), legacyPvp);
    }

    public DuelSelection resolve(UUID player)
    {
        DuelSelection selected = selections.get(player);
        if (selected != null && valid(selected)) return selected;
        DuelMode fallback = modes.resolve("classic").filter(DuelMode::enabled)
                .orElseGet(() -> modes.enabledModes().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("No enabled duel modes are configured")));
        DuelSelection resolved = create(fallback, null, legacyPreferences.getOrDefault(player, false));
        selections.put(player, resolved);
        return resolved;
    }

    public Kit kit(DuelSelection selection)
    {
        Kit kit = kits.getKitByCanonicalKey(selection.kitKey());
        if (kit == null) throw new IllegalStateException("Selected kit '" + selection.kitKey() + "' is unavailable");
        return kit;
    }

    public DuelMode mode(DuelSelection selection) { return modes.require(selection.modeKey()); }

    private boolean valid(DuelSelection selection)
    {
        try
        {
            DuelMode mode = modes.require(selection.modeKey());
            return mode.enabled() && mode.allowsKit(selection.kitKey())
                    && kits.getKitByCanonicalKey(selection.kitKey()) != null;
        }
        catch (IllegalArgumentException exception) { return false; }
    }
}
