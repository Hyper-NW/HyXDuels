package me.alphatct3209.duels.divisions;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DivisionManagerTest
{
    private static final long[] DEFAULT_THRESHOLDS = {
            50, 100, 150,
            250, 350, 450,
            700, 950, 1200,
            1700, 2200, 2700,
            3700, 4700, 5700,
            7700, 9700, 11700,
            16700, 21700, 26700,
            36700, 46700, 56700,
            81700, 106700, 131700,
            181700, 231700, 281700,
            381700, 481700, 581700
    };

    @Test
    void defaultConfigurationHasExactCumulativeThresholds()
    {
        DivisionManager manager = defaultManager();
        assertEquals(33, manager.getDivisions().size());
        assertEquals(List.of(1, 2, 3), manager.getDivisions().subList(0, 3).stream()
                .map(Division::level).toList());
        assertEquals(List.of("Rookie", "Iron", "Gold", "Diamond", "Master", "Legend",
                        "Grandmaster", "Godlike", "Celestial", "Divine", "Ascended"),
                manager.getDivisions().stream().map(Division::name).distinct().toList());
        assertEquals(toList(DEFAULT_THRESHOLDS), manager.getDivisions().stream()
                .map(Division::requiredWins).toList());
    }

    @Test
    void exactBoundaryAttainsDestinationDivision()
    {
        DivisionManager manager = defaultManager();
        assertTrue(manager.getCurrentDivision(49).isEmpty());
        assertEquals("Rookie 1", manager.getCurrentDivision(50).orElseThrow().displayName());
        assertEquals("Rookie 2", manager.getNextDivision(50).orElseThrow().displayName());
        assertEquals("Rookie 3", manager.getCurrentDivision(150).orElseThrow().displayName());
        assertEquals("Iron 1", manager.getCurrentDivision(250).orElseThrow().displayName());

        DivisionProgress beforeFirst = manager.getProgress(49);
        assertEquals(49, beforeFirst.winsIntoStep());
        assertEquals(50, beforeFirst.winsForStep());
        assertEquals(49.0D / 50.0D, beforeFirst.fraction());

        DivisionProgress atBoundary = manager.getProgress(50);
        assertEquals(0, atBoundary.winsIntoStep());
        assertEquals(50, atBoundary.winsForStep());
    }

    @Test
    void maximumRankHasNoNextDivision()
    {
        DivisionManager manager = defaultManager();
        long maximumThreshold = DEFAULT_THRESHOLDS[DEFAULT_THRESHOLDS.length - 1];
        Division maximum = manager.getCurrentDivision(maximumThreshold).orElseThrow();
        assertEquals("Ascended 3", maximum.displayName());
        assertTrue(manager.getNextDivision(maximumThreshold).isEmpty());
        assertTrue(manager.getProgress(maximumThreshold + 10_000).isMaximum());
        assertEquals(1.0D, manager.getProgress(maximumThreshold).fraction());
    }

    @Test
    void returnsEveryCrossedThresholdInOrder()
    {
        DivisionManager manager = defaultManager();
        assertEquals(List.of("Rookie 1", "Rookie 2", "Rookie 3", "Iron 1"),
                manager.getCrossedDivisions(49, 250).stream().map(Division::displayName).toList());
        assertEquals(List.of("Rookie 2", "Rookie 3"),
                manager.getCrossedDivisions(50, 150).stream().map(Division::displayName).toList());
        assertTrue(manager.getCrossedDivisions(250, 250).isEmpty());
        assertTrue(manager.getCrossedDivisions(300, 250).isEmpty());
    }

    @Test
    void rejectsMalformedConfiguration() throws Exception
    {
        assertMalformed("tiers:\n  Rookie:\n    wins-per-step: 0\n    levels:\n      1: {}\n");
        assertMalformed("tiers:\n  Rookie:\n    wins-per-step: 50\n    levels:\n      bronze: {}\n");
        assertMalformed("tiers:\n  Rookie:\n    wins-per-step: 50\n    surprise: true\n    levels:\n      1: {}\n");
        assertMalformed("tiers:\n  Rookie:\n    wins-per-step: 50\n    levels:\n      2: {}\n      1: {}\n");
    }

    @Test
    void exposedCollectionsAreImmutable()
    {
        DivisionManager manager = defaultManager();
        assertThrows(UnsupportedOperationException.class,
                () -> manager.getDivisions().add(manager.getDivisions().getFirst()));
        assertThrows(UnsupportedOperationException.class,
                () -> manager.getDivisions().getFirst().rewards().add("say changed"));
        assertFalse(manager.getDivisions().isEmpty());
    }

    @Test
    void expandsRewardPlaceholdersAndDispatchesAsConsole() throws Exception
    {
        UUID uuid = UUID.fromString("12345678-1234-5678-1234-567812345678");
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[] {Player.class}, (proxy, method, arguments) -> switch (method.getName())
                {
                    case "getName" -> "Alice";
                    case "getUniqueId" -> uuid;
                    default -> null;
                });
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("tiers:\n  Rookie:\n    wins-per-step: 50\n    levels:\n"
                + "      1:\n        rewards:\n"
                + "          - '/announce {player} {uuid} {gamemode} {division} {level} {wins}'\n");
        List<String> commands = new ArrayList<>();
        DivisionManager manager = new DivisionManager(configuration, commands::add);

        manager.executeRewards(player, "uhc_classic", manager.getDivisions().getFirst(), 50);

        assertEquals(List.of("announce Alice " + uuid + " uhc_classic Rookie 1 50"), commands);
    }

    private static DivisionManager defaultManager()
    {
        Reader reader = new InputStreamReader(
                DivisionManagerTest.class.getResourceAsStream("/advanced/divisions.yml"), StandardCharsets.UTF_8);
        return new DivisionManager(YamlConfiguration.loadConfiguration(reader));
    }

    private static void assertMalformed(String yaml) throws Exception
    {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(yaml);
        assertThrows(DivisionConfigurationException.class, () -> new DivisionManager(configuration));
    }

    private static List<Long> toList(long[] values)
    {
        return java.util.Arrays.stream(values).boxed().toList();
    }
}
