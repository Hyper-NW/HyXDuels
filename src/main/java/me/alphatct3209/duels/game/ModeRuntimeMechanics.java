package me.alphatct3209.duels.game;

import me.alphatct3209.duels.game.arenas.Arena;
import me.alphatct3209.duels.game.modes.ModeHandlerType;
import me.alphatct3209.duels.game.modes.runtime.BedWarsRuntime;
import me.alphatct3209.duels.game.modes.runtime.ModeRuntimeService;
import me.alphatct3209.duels.game.modes.runtime.SkyWarsRuntime;

import java.util.function.BooleanSupplier;

/** Match-level coordinator; each terrain/economy mode owns its own runtime service. */
final class ModeRuntimeMechanics
{
    private final Arena arena;
    private final BooleanSupplier active;
    private ModeRuntimeService service = ModeRuntimeService.NONE;

    ModeRuntimeMechanics(Arena arena, BooleanSupplier active)
    {
        this.arena = arena;
        this.active = active;
    }

    void start(ModeHandlerType type)
    {
        cancel();
        service = switch (type)
        {
            case BED_WARS -> new BedWarsRuntime(arena, active);
            case SKY_WARS -> new SkyWarsRuntime(arena, active);
            default -> ModeRuntimeService.NONE;
        };
        service.start();
    }

    void cancel()
    {
        service.stop();
        service = ModeRuntimeService.NONE;
    }
}
