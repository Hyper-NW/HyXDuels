package me.alphatct3209.duels.challenge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChallengeGuiLayout
{
    public static final int INVENTORY_SIZE = 54;
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int INFO_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private ChallengeGuiLayout()
    {
    }

    public static <T> Page<T> page(List<T> entries, int requestedPage)
    {
        int pageCount = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int pageIndex = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int from = pageIndex * PAGE_SIZE;
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        Map<Integer, T> slots = new LinkedHashMap<>();
        for (int index = from; index < to; index++)
        {
            slots.put(index - from, entries.get(index));
        }
        return new Page<>(pageIndex, pageCount, Map.copyOf(slots), pageIndex > 0,
                pageIndex + 1 < pageCount);
    }

    public record Page<T>(int index, int count, Map<Integer, T> slots,
                          boolean hasPrevious, boolean hasNext)
    {
    }
}
