package me.alphatct3209.duels.challenge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Pure pending-challenge state. Bukkit scheduling and messaging live in ChallengeManager. */
public final class ChallengeState
{
    private final Map<UUID, Pending> byToken = new LinkedHashMap<>();
    private final Map<UUID, Pending> byParticipant = new LinkedHashMap<>();

    public Pending create(UUID challenger, UUID target, String kitKey, long expiresAt)
    {
        return create(challenger, target, "classic", kitKey, null, false, expiresAt);
    }

    public Pending create(UUID challenger, UUID target, String modeKey, String kitKey, long expiresAt)
    {
        return create(challenger, target, modeKey, kitKey, null, false, expiresAt);
    }

    public Pending create(UUID challenger, UUID target, String modeKey, String kitKey,
                          Integer arenaId, long expiresAt)
    {
        return create(challenger, target, modeKey, kitKey, arenaId, false, expiresAt);
    }

    public Pending create(UUID challenger, UUID target, String modeKey, String kitKey,
                          Integer arenaId, boolean legacyPvp, long expiresAt)
    {
        Objects.requireNonNull(challenger, "challenger");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(modeKey, "modeKey");
        Objects.requireNonNull(kitKey, "kitKey");
        if (arenaId != null && arenaId < 0) throw new IllegalArgumentException("arenaId cannot be negative");
        if (challenger.equals(target)) throw new IllegalArgumentException("A player cannot challenge themselves");
        if (byParticipant.containsKey(challenger) || byParticipant.containsKey(target))
            throw new IllegalStateException("A participant already has a pending challenge");

        Pending pending = new Pending(UUID.randomUUID(), challenger, target, modeKey, kitKey,
                arenaId, legacyPvp, expiresAt);
        byToken.put(pending.token(), pending);
        byParticipant.put(challenger, pending);
        byParticipant.put(target, pending);
        return pending;
    }

    public Pending forParticipant(UUID participant) { return byParticipant.get(participant); }
    public Pending forToken(UUID token) { return byToken.get(token); }

    public Pending forTarget(UUID target)
    {
        Pending pending = byParticipant.get(target);
        return pending != null && pending.target().equals(target) ? pending : null;
    }

    public Pending removeForTarget(UUID target, UUID token)
    {
        Pending pending = forTarget(target);
        return pending != null && pending.token().equals(token) ? remove(token) : null;
    }

    public Pending removeParticipant(UUID participant)
    {
        Pending pending = byParticipant.get(participant);
        return pending == null ? null : remove(pending.token());
    }

    public Pending expire(UUID token, long now)
    {
        Pending pending = byToken.get(token);
        return pending != null && pending.expiresAt() <= now ? remove(token) : null;
    }

    public List<Pending> expireDue(long now)
    {
        List<Pending> expired = new ArrayList<>();
        for (Pending pending : List.copyOf(byToken.values()))
            if (pending.expiresAt() <= now) expired.add(remove(pending.token()));
        return List.copyOf(expired);
    }

    public void clear()
    {
        byToken.clear();
        byParticipant.clear();
    }

    private Pending remove(UUID token)
    {
        Pending pending = byToken.remove(token);
        if (pending != null)
        {
            byParticipant.remove(pending.challenger(), pending);
            byParticipant.remove(pending.target(), pending);
        }
        return pending;
    }

    public record Pending(UUID token, UUID challenger, UUID target, String modeKey,
                          String kitKey, Integer arenaId, boolean legacyPvp, long expiresAt)
    {
        public Pending
        {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(challenger, "challenger");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(modeKey, "modeKey");
            Objects.requireNonNull(kitKey, "kitKey");
        }
    }
}
