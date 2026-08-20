package me.alphatct3209.duels.challenge;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeGuiLayoutTest
{
    @Test
    void emptyAndShortListsUseOnePageAndOnlyMappedKitSlots()
    {
        ChallengeGuiLayout.Page<Integer> empty = ChallengeGuiLayout.page(List.of(), 0);
        assertEquals(1, empty.count());
        assertTrue(empty.slots().isEmpty());
        assertFalse(empty.hasPrevious());
        assertFalse(empty.hasNext());

        ChallengeGuiLayout.Page<Integer> shortPage = ChallengeGuiLayout.page(List.of(10, 20), 0);
        assertEquals(10, shortPage.slots().get(0));
        assertEquals(20, shortPage.slots().get(1));
        assertFalse(shortPage.slots().containsKey(ChallengeGuiLayout.INFO_SLOT));
    }

    @Test
    void paginatesFortyFiveEntriesAndClampsRequestedPage()
    {
        List<Integer> entries = IntStream.range(0, 91).boxed().toList();
        ChallengeGuiLayout.Page<Integer> first = ChallengeGuiLayout.page(entries, -5);
        assertEquals(0, first.index());
        assertEquals(3, first.count());
        assertEquals(45, first.slots().size());
        assertFalse(first.hasPrevious());
        assertTrue(first.hasNext());

        ChallengeGuiLayout.Page<Integer> last = ChallengeGuiLayout.page(entries, 99);
        assertEquals(2, last.index());
        assertEquals(1, last.slots().size());
        assertEquals(90, last.slots().get(0));
        assertTrue(last.hasPrevious());
        assertFalse(last.hasNext());
    }
}
