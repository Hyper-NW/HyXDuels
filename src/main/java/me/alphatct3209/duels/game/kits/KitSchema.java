package me.alphatct3209.duels.game.kits;

final class KitSchema
{
    static final int CURRENT_FORMAT_VERSION = 2;
    static final int STORAGE_SIZE = 36;

    private KitSchema()
    {
    }

    static KitFormat readFormat(boolean versionPresent, Object configuredVersion, String kitPath)
    {
        if (!versionPresent)
        {
            return KitFormat.LEGACY_V1;
        }

        String versionPath = kitPath + ".Format-Version";
        if (!(configuredVersion instanceof Number number))
        {
            throw new IllegalArgumentException("Invalid kit format at " + versionPath
                    + ": expected integer version 1 or 2, found " + String.valueOf(configuredVersion));
        }

        int version = number.intValue();
        if (!Double.isFinite(number.doubleValue()) || number.doubleValue() != version)
        {
            throw new IllegalArgumentException("Invalid kit format at " + versionPath
                    + ": expected integer version 1 or 2, found " + configuredVersion);
        }

        return switch (version)
        {
            case 1 -> KitFormat.LEGACY_V1;
            case CURRENT_FORMAT_VERSION -> KitFormat.POSITIONAL_V2;
            default -> throw new IllegalArgumentException("Invalid kit format at " + versionPath
                    + ": unknown version " + version + " (supported versions: 1, 2)");
        };
    }

    static void requireV2StorageSize(int size, String kitPath)
    {
        if (size != STORAGE_SIZE)
        {
            throw new IllegalArgumentException("Invalid kit inventory at " + kitPath + ".Inventory"
                    + ": Format-Version 2 requires exactly " + STORAGE_SIZE
                    + " entries, found " + size);
        }
    }
}
