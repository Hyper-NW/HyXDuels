package me.alphatct3209.duels.game.modes.runtime;

public interface ModeRuntimeService
{
    ModeRuntimeService NONE = new ModeRuntimeService()
    {
        @Override public void start() { }
        @Override public void stop() { }
    };

    void start();
    void stop();
}
