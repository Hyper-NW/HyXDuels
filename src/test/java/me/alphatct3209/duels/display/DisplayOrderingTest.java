package me.alphatct3209.duels.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayOrderingTest
{
    @Test
    void sidebarUsesTheRequestedInvertedLineOrder()
    {
        assertEquals(1, SidebarDisplay.scoreForLine(0));
        assertEquals(15, SidebarDisplay.scoreForLine(14));
        assertThrows(IllegalArgumentException.class, () -> SidebarDisplay.scoreForLine(-1));
        assertThrows(IllegalArgumentException.class, () -> SidebarDisplay.scoreForLine(15));
    }

    @Test
    void firstTabCellReceivesTheHighestClientPriority()
    {
        assertEquals(80, FakePlayerTabList.listOrder(0));
        assertEquals(1, FakePlayerTabList.listOrder(79));
        assertTrue(FakePlayerTabList.listOrder(0) > FakePlayerTabList.listOrder(1));
        assertThrows(IllegalArgumentException.class, () -> FakePlayerTabList.listOrder(-1));
        assertThrows(IllegalArgumentException.class, () -> FakePlayerTabList.listOrder(80));
    }

    @Test
    void syntheticProfilesHaveNoVisibleOrSuggestibleName()
    {
        assertEquals("", FakePlayerTabList.profileName());
    }
}
