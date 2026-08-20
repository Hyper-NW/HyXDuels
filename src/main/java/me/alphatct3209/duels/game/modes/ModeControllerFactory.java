package me.alphatct3209.duels.game.modes;

import me.alphatct3209.duels.game.modes.controller.BedWarsController;
import me.alphatct3209.duels.game.modes.controller.BoxingController;
import me.alphatct3209.duels.game.modes.controller.BridgeController;
import me.alphatct3209.duels.game.modes.controller.LastStandingController;
import me.alphatct3209.duels.game.modes.controller.ParkourController;
import me.alphatct3209.duels.game.modes.controller.QuakecraftController;
import me.alphatct3209.duels.game.modes.controller.SkyWarsController;
import me.alphatct3209.duels.game.modes.controller.SpleefController;
import me.alphatct3209.duels.game.modes.controller.SumoController;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.IntFunction;

/** The only handler dispatch table. Runtime/listeners never branch on mode names. */
public final class ModeControllerFactory
{
    private static final Map<ModeHandlerType, IntFunction<ModeController>> FACTORIES = new EnumMap<>(ModeHandlerType.class);
    static
    {
        FACTORIES.put(ModeHandlerType.LAST_STANDING, ignored -> new LastStandingController());
        FACTORIES.put(ModeHandlerType.BED_WARS, ignored -> new BedWarsController());
        FACTORIES.put(ModeHandlerType.SKY_WARS, ignored -> new SkyWarsController());
        FACTORIES.put(ModeHandlerType.BOXING, BoxingController::new);
        FACTORIES.put(ModeHandlerType.BRIDGE, ignored -> new BridgeController());
        FACTORIES.put(ModeHandlerType.PARKOUR, ignored -> new ParkourController());
        FACTORIES.put(ModeHandlerType.QUAKECRAFT, ignored -> new QuakecraftController());
        FACTORIES.put(ModeHandlerType.SPLEEF, ignored -> new SpleefController());
        FACTORIES.put(ModeHandlerType.SUMO, ignored -> new SumoController());
        if (!FACTORIES.keySet().equals(EnumSet.allOf(ModeHandlerType.class)))
            throw new IllegalStateException("Every mode handler must have exactly one controller factory");
    }

    private ModeControllerFactory() {}

    public static ModeController create(ModeHandlerType type, int boxingMercyLead)
    {
        IntFunction<ModeController> factory = FACTORIES.get(type);
        if (factory == null) throw new IllegalArgumentException("No controller for " + type);
        return factory.apply(Math.max(1, boxingMercyLead));
    }

}
