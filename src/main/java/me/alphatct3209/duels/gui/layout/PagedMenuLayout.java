package me.alphatct3209.duels.gui.layout;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PagedMenuLayout
{
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int BACK_SLOT = 49;
    public static final int NEXT_SLOT = 53;
    public static final int INVENTORY_SIZE = 54;

    private PagedMenuLayout() {}

    public static <T> Page<T> page(List<T> entries, int requestedPage)
    {
        int pageCount = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int start = page * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        Map<Integer, T> slots = new LinkedHashMap<>();
        for (int index = start; index < end; index++)
        {
            slots.put(index - start, entries.get(index));
        }
        return new Page<>(page, pageCount, Map.copyOf(slots));
    }

    public record Page<T>(int index, int count, Map<Integer, T> slots)
    {
        public boolean hasPrevious() { return index > 0; }
        public boolean hasNext() { return index + 1 < count; }
    }
}
