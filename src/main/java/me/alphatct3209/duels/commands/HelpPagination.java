package me.alphatct3209.duels.commands;

import java.util.List;

/** Pure pagination for command help, kept separate from Bukkit rendering for deterministic tests. */
public final class HelpPagination
{
    private HelpPagination() { }

    public static Page page(List<String> entries, int requestedPage, int requestedPageSize)
    {
        int pageSize = Math.max(3, Math.min(10, requestedPageSize));
        int pageCount = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        int page = Math.max(1, Math.min(requestedPage, pageCount));
        int from = Math.min(entries.size(), (page - 1) * pageSize);
        int to = Math.min(entries.size(), from + pageSize);
        return new Page(page, pageCount, List.copyOf(entries.subList(from, to)));
    }

    public record Page(int number, int count, List<String> entries) { }
}
