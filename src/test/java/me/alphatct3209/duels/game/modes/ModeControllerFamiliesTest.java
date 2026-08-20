package me.alphatct3209.duels.game.modes;

import me.alphatct3209.duels.game.arenas.ArenaCombatAuthorization;
import me.alphatct3209.duels.game.arenas.ArenaModeReadiness;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModeControllerFamiliesTest
{
    private final UUID one = UUID.randomUUID();
    private final UUID two = UUID.randomUUID();

    @Test void lastStandingAndRingoutFamiliesAreTerminal()
    {
        for (ModeHandlerType type : Set.of(ModeHandlerType.LAST_STANDING, ModeHandlerType.SKY_WARS,
                ModeHandlerType.SPLEEF, ModeHandlerType.SUMO))
        {
            ModeRuntimeState state = state(1);
            assertEquals(two, ModeControllerFactory.create(type, 50).death(state, one).winner());
            assertTrue(state.terminal().isPresent());
        }
    }

    @Test void bedWarsRespawnsUntilOwnedBedIsBroken()
    {
        ModeController controller = ModeControllerFactory.create(ModeHandlerType.BED_WARS, 50);
        ModeRuntimeState state = state(1);
        assertEquals(ModeAction.Type.RESPAWN, controller.death(state, one).type());
        controller.bedBreak(state, one, two);
        assertFalse(state.bedAlive(one));
        assertEquals(two, controller.death(state, one).winner());
    }

    @Test void boxingCountsAcceptedMeleeHitsAndMercyLead()
    {
        ModeController controller = ModeControllerFactory.create(ModeHandlerType.BOXING, 2);
        ModeRuntimeState state = state(100);
        assertEquals(ModeAction.Type.NONE, controller.meleeHit(state, one, two).type());
        assertEquals(one, controller.meleeHit(state, one, two).winner());
        assertEquals(2, state.score(one));
        assertFalse(controller.healthDamage());
    }

    @Test void bridgeResetsRoundsUntilTargetAndDeathsRespawn()
    {
        ModeController controller = ModeControllerFactory.create(ModeHandlerType.BRIDGE, 50);
        ModeRuntimeState state = state(2);
        assertEquals(ModeAction.Type.RESPAWN, controller.death(state, one).type());
        assertEquals(ModeAction.Type.ROUND_RESET, controller.goal(state, one).type());
        assertEquals(one, controller.goal(state, one).winner());
    }

    @Test void parkourFinishWinsAndCheckpointTimeoutIsDeterministic()
    {
        ModeController controller = ModeControllerFactory.create(ModeHandlerType.PARKOUR, 50);
        ModeRuntimeState state = state(1);
        assertTrue(state.checkpoint(one, 1));
        assertEquals(one, controller.timeout(state, ModeDurationPolicy.TimeoutPolicy.HIGHEST_SCORE).winner());
        ModeRuntimeState finish = state(1);
        assertEquals(one, controller.finish(finish, one).winner());
    }

    @Test void quakeScoresSixAndRespawnsVictimBeforeTarget()
    {
        ModeController controller = ModeControllerFactory.create(ModeHandlerType.QUAKECRAFT, 50);
        ModeRuntimeState state = state(6);
        for (int i = 0; i < 5; i++) assertEquals(ModeAction.Type.RESPAWN, controller.rangedHit(state, one, two).type());
        assertEquals(one, controller.rangedHit(state, one, two).winner());
    }

    @Test void highestScoreTimeoutSelectsLeaderOrDraw()
    {
        ModeController controller = ModeControllerFactory.create(ModeHandlerType.BOXING, 50);
        assertEquals(ModeAction.Type.DRAW, controller.timeout(state(100), ModeDurationPolicy.TimeoutPolicy.HIGHEST_SCORE).type());
        ModeRuntimeState state = state(100); controller.meleeHit(state, two, one);
        assertEquals(two, controller.timeout(state, ModeDurationPolicy.TimeoutPolicy.HIGHEST_SCORE).winner());
    }

    @Test void markerReadinessIsStrictByHandlerFamily()
    {
        assertEquals(Set.of("bed_2", "generator_1", "generator_2", "shop_1", "shop_2"),
                ArenaModeReadiness.missing(ModeHandlerType.BED_WARS, Set.of("bed_1")));
        assertEquals(Set.of("chest_2", "mid_chest"),
                ArenaModeReadiness.missing(ModeHandlerType.SKY_WARS, Set.of("chest_1")));
        assertTrue(ArenaModeReadiness.ready(ModeHandlerType.SKY_WARS,
                Set.of("chest_1", "chest_2", "mid_chest")));
        assertFalse(ArenaModeReadiness.ready(ModeHandlerType.BRIDGE, Set.of("goal_1")));
        assertTrue(ArenaModeReadiness.ready(ModeHandlerType.PARKOUR, Set.of("finish", "checkpoint_1")));
        assertTrue(ArenaModeReadiness.ready(ModeHandlerType.SUMO, Set.of()));
    }

    @Test void resetPolicyIsWorldExceptDuelArenaCell()
    {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/advanced/modes.yml"));
        Map<ModeKey, DuelMode> modes = ModeConfigParser.parse(map(yaml.getConfigurationSection("Modes")), ModeConfigParser.ROSTER);
        modes.forEach((key, mode) -> assertEquals(key.value().equals("duel_arena") ? ResetPolicy.CELL : ResetPolicy.WORLD,
                mode.resetPolicy(), key.value()));
        assertEquals(6, modes.get(ModeKey.parse("quakecraft")).targetScore());
        assertEquals(ModeHandlerType.SKY_WARS, modes.get(ModeKey.parse("skywars")).handlerType());
        assertFalse(modes.get(ModeKey.parse("bow")).combat().melee());
        assertTrue(modes.get(ModeKey.parse("boxing")).combat().noHitDelay());
        assertTrue(modes.get(ModeKey.parse("combo")).combat().noHitDelay());
        assertEquals(60, modes.get(ModeKey.parse("duel_arena")).durationPolicy()
                .maximumDuration().toSeconds());
    }

    @Test void authorizationRequiresTheExactOpponentPair()
    {
        UUID outsider = UUID.randomUUID();
        assertTrue(ArenaCombatAuthorization.opponents(one, two, one, two));
        assertFalse(ArenaCombatAuthorization.opponents(one, outsider, one, two));
        assertFalse(ArenaCombatAuthorization.opponents(one, one, one, two));
    }

    private ModeRuntimeState state(int target) { return new ModeRuntimeState(one, two, target); }
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
