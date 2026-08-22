package me.alphatct3209.duels.game.modes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModeArchitectureTest
{
    @Test
    void bundledConfigurationStrictlyParsesAllSeventeenAndSynthesizesDefaultAlias()
    {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/advanced/modes.yml"));
        Map<ModeKey, DuelMode> modes = ModeConfigParser.parse(
                map(yaml.getConfigurationSection("Modes")), ModeConfigParser.ROSTER);
        assertEquals(17, modes.size());
        assertEquals(ModeConfigParser.ROSTER,
                modes.keySet().stream().map(ModeKey::value).collect(java.util.stream.Collectors.toSet()));
        assertTrue(modes.get(ModeKey.parse("classic")).aliases().contains("default"));
        assertEquals(ModeHandlerType.BED_WARS, modes.get(ModeKey.parse("bed_wars")).handlerType());
        assertEquals(100, modes.get(ModeKey.parse("boxing")).targetScore());
        assertEquals("classic", modes.get(ModeKey.parse("classic")).defaultKitKey());
        assertEquals(Set.of("classic", "bow", "nodebuff", "op", "uhc"),
                modes.get(ModeKey.parse("duel_arena")).allowedKitKeys());
        modes.forEach((key, mode) -> {
            if (!key.value().equals("duel_arena")) assertEquals(key.value(), mode.defaultKitKey());
            assertFalse(mode.combat().naturalRegeneration());
        });
    }

    @Test
    void modeKeysAreStrictAndNeverDisplayNameNormalized()
    {
        assertEquals("duel_arena", ModeKey.parse("duel_arena").value());
        for (String invalid : new String[]{"Duel_Arena", "duel-arena", " duel_arena", "duel__arena", "_duel"})
            assertThrows(IllegalArgumentException.class, () -> ModeKey.parse(invalid), invalid);
    }

    @Test
    void reusableKitCanBeSelectedIndependentlyAcrossModes()
    {
        DuelMode classic = mode("classic", Set.of("shared", "alternate"), "shared");
        DuelMode bow = mode("bow", Set.of("shared"), "shared");
        assertEquals(new DuelSelection(ModeKey.parse("classic"), "shared"),
                SelectionRules.select(classic, null, Set.of("shared", "alternate")));
        assertEquals(new DuelSelection(ModeKey.parse("bow"), "shared"),
                SelectionRules.select(bow, "shared", Set.of("shared", "alternate")));
        assertThrows(IllegalArgumentException.class,
                () -> SelectionRules.select(bow, "alternate", Set.of("shared", "alternate")));
    }

    @Test
    void objectiveHandlersTrackScoresBedsAndTimeouts()
    {
        DuelMode boxing = new DuelMode(ModeKey.parse("boxing"), "Boxing", "CHEST",
                ModeHandlerType.BOXING, ResetPolicy.CELL, CombatFlags.standard(), 2,
                new ModeDurationPolicy(Duration.ofMinutes(5), ModeDurationPolicy.TimeoutPolicy.HIGHEST_SCORE),
                "shared", Set.of("shared"), Set.of(), true, true, false);
        UUID first = UUID.randomUUID(); UUID second = UUID.randomUUID();
        ModeObjective objective = new ModeObjective(boxing, first, second);
        assertTrue(objective.score(first));
        assertEquals(first, objective.timeoutWinner().orElseThrow());
        assertTrue(objective.score(first));
        assertEquals(first, objective.winner().orElseThrow());

        DuelMode beds = new DuelMode(ModeKey.parse("bed_wars"), "Bed Wars", "RED_BED",
                ModeHandlerType.BED_WARS, ResetPolicy.WORLD, CombatFlags.standard(), 1,
                new ModeDurationPolicy(Duration.ofMinutes(10), ModeDurationPolicy.TimeoutPolicy.DRAW),
                "shared", Set.of("shared"), Set.of(), true, true, false);
        ModeObjective bedObjective = new ModeObjective(beds, first, second);
        assertTrue(bedObjective.eliminate(first).isEmpty());
        assertTrue(bedObjective.destroyBed(first));
        assertEquals(second, bedObjective.eliminate(first).orElseThrow());
    }

    private static DuelMode mode(String key, Set<String> allowed, String defaultKit)
    {
        return new DuelMode(ModeKey.parse(key), key, "CHEST", ModeHandlerType.LAST_STANDING,
                ResetPolicy.CELL, CombatFlags.standard(), 1,
                new ModeDurationPolicy(Duration.ZERO, ModeDurationPolicy.TimeoutPolicy.DRAW),
                defaultKit, allowed, Set.of(), true, true, false);
    }

    private static Map<String, Object> map(ConfigurationSection section)
    {
        assertNotNull(section);
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false))
        {
            Object value = section.get(key);
            result.put(key, value instanceof ConfigurationSection nested ? map(nested) : value);
        }
        return result;
    }
}
