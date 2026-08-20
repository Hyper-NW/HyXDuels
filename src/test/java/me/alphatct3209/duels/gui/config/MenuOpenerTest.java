package me.alphatct3209.duels.gui.config;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuOpenerTest
{
    @Test
    void acceptsSafeConfiguredOpenerAndCopiesLore()
    {
        List<String> lore = new ArrayList<>(List.of("line"));
        MenuOpener opener = new MenuOpener("duel-menu", true, Material.DIAMOND_SWORD,
                0, "Duels", lore, MenuAction.DUEL_MENU, true, true, false, null);
        lore.clear();
        assertEquals(List.of("line"), opener.lore());
        assertThrows(UnsupportedOperationException.class, () -> opener.lore().add("other"));
    }

    @Test
    void rejectsUnsafeIdsSlotsAndModelData()
    {
        assertThrows(IllegalArgumentException.class, () -> new MenuOpener("Duel Menu", true,
                Material.DIAMOND_SWORD, 0, "Duels", List.of(), MenuAction.DUEL_MENU,
                false, true, false, null));
        assertThrows(IllegalArgumentException.class, () -> new MenuOpener("duels", true,
                Material.DIAMOND_SWORD, 9, "Duels", List.of(), MenuAction.DUEL_MENU,
                false, true, false, null));
        assertThrows(IllegalArgumentException.class, () -> new MenuOpener("duels", true,
                Material.DIAMOND_SWORD, 0, "Duels", List.of(), MenuAction.DUEL_MENU,
                false, true, false, -1));
    }
}
