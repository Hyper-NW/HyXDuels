package me.alphatct3209.duels.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagedMenuLayoutTest
{
    @Test
    void emptyMenusStillHaveOneSafePage()
    {
        PagedMenuLayout.Page<Integer> page = PagedMenuLayout.page(List.of(), 99);
        assertEquals(0, page.index());
        assertEquals(1, page.count());
        assertTrue(page.slots().isEmpty());
        assertFalse(page.hasPrevious());
        assertFalse(page.hasNext());
    }

    @Test
    void mapsFortyFiveEntriesPerPageAndClampsPageRequests()
    {
        List<Integer> entries = IntStream.range(0, 91).boxed().toList();
        PagedMenuLayout.Page<Integer> middle = PagedMenuLayout.page(entries, 1);
        assertEquals(3, middle.count());
        assertEquals(45, middle.slots().size());
        assertEquals(45, middle.slots().get(0));
        assertEquals(89, middle.slots().get(44));
        assertTrue(middle.hasPrevious());
        assertTrue(middle.hasNext());

        PagedMenuLayout.Page<Integer> last = PagedMenuLayout.page(entries, 50);
        assertEquals(2, last.index());
        assertEquals(1, last.slots().size());
        assertEquals(90, last.slots().get(0));
        assertFalse(last.hasNext());
    }
}
