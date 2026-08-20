package me.alphatct3209.duels.game.modes;

import java.util.Optional;

/** Pure two-player queue claim: entrant one fixes the mode and entrant two must match it. */
public final class ModeQueueClaim
{
    private ModeKey claimedMode;
    private int participants;

    public boolean canAdmit(ModeKey mode)
    {
        return mode != null && participants < 2 && (claimedMode == null || claimedMode.equals(mode));
    }

    public boolean admit(ModeKey mode)
    {
        if (!canAdmit(mode)) return false;
        if (claimedMode == null) claimedMode = mode;
        participants++;
        return true;
    }

    public void leave()
    {
        if (participants > 0) participants--;
        if (participants == 0) claimedMode = null;
    }

    public void clear()
    {
        participants = 0;
        claimedMode = null;
    }

    public Optional<ModeKey> claimedMode() { return Optional.ofNullable(claimedMode); }
    public int participants() { return participants; }
}
