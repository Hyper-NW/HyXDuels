package me.alphatct3209.duels.commands;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelpPaginationTest
{
    @Test
    void boundsPageSizeAndClampsRequestedPages()
    {
        List<String> entries = IntStream.rangeClosed(1, 17).mapToObj(Integer::toString).toList();
        HelpPagination.Page first = HelpPagination.page(entries, -4, 6);
        assertEquals(1, first.number());
        assertEquals(3, first.count());
        assertEquals(List.of("1", "2", "3", "4", "5", "6"), first.entries());

        HelpPagination.Page last = HelpPagination.page(entries, 99, 6);
        assertEquals(3, last.number());
        assertEquals(List.of("13", "14", "15", "16", "17"), last.entries());
    }

    @Test
    void alwaysProducesOnePageForAnEmptyHelpList()
    {
        HelpPagination.Page page = HelpPagination.page(List.of(), 1, 100);
        assertEquals(1, page.number());
        assertEquals(1, page.count());
        assertEquals(List.of(), page.entries());
    }
}
