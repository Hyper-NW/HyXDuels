package me.alphatct3209.duels.game.items;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoldenHeadTest
{
    @Test
    void usesRequestedUhcQuantityAndEffects()
    {
        assertEquals(3, GoldenHead.DEFAULT_UHC_AMOUNT);
        assertEquals(100, GoldenHead.REGENERATION_DURATION_TICKS);
        assertEquals(2, GoldenHead.REGENERATION_AMPLIFIER);
        assertEquals(2400, GoldenHead.ABSORPTION_DURATION_TICKS);
        assertEquals(0, GoldenHead.ABSORPTION_AMPLIFIER);
    }

    @Test
    void shippedConfigurationMatchesTheRequestedDefaultEffects()
    {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/advanced/golden-heads.yml"));
        assertEquals(true, yaml.getBoolean("Enabled"));
        assertEquals(3, yaml.getInt("UHC.Amount"));
        assertEquals(3, yaml.getInt("Effects.Regeneration.Level"));
        assertEquals(5.0, yaml.getDouble("Effects.Regeneration.Duration-Seconds"));
        assertEquals(1, yaml.getInt("Effects.Absorption.Level"));
        assertEquals(120.0, yaml.getDouble("Effects.Absorption.Duration-Seconds"));
        assertEquals("&6Golden Head", yaml.getString("Item.Name"));
    }
}
