package me.alphatct3209.duels.world;

import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SlimeWorldOwnershipTest
{
    @Test
    void slimeLoadsAreNotPublishedToFolderOrientedWorldManagers()
    {
        assertFalse(SlimeWorldManager.PUBLISH_BUKKIT_WORLD_LOAD_EVENT);
    }

    @Test
    void rejectsWritableAspOwnedInstance()
    {
        World bukkit = world("Arena");
        SlimeWorldInstance instance = slimeWorld("arena", bukkit, false);

        assertNull(SlimeWorldManager.selectOwnedInstance(
                "Arena", bukkit, null, List.of(instance)));
    }

    @Test
    void acceptsReadOnlyAspOwnedInstanceFoundByCaseInsensitiveFallback()
    {
        World bukkit = world("Arena");
        SlimeWorldInstance instance = slimeWorld("arena", bukkit, true);

        assertSame(instance, SlimeWorldManager.selectOwnedInstance(
                "Arena", bukkit, null, List.of(instance)));
    }

    @Test
    void rejectsDifferentBukkitInstanceWithSameName()
    {
        World bukkit = world("Arena");
        SlimeWorldInstance instance = slimeWorld("Arena", world("Arena"), true);

        assertNull(SlimeWorldManager.selectOwnedInstance(
                "Arena", bukkit, instance, List.of(instance)));
    }

    private static World world(String name)
    {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[]{World.class}, (proxy, method, args) -> switch (method.getName())
                {
                    case "getName" -> name;
                    case "toString" -> "World[" + name + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static SlimeWorldInstance slimeWorld(String name, World bukkit, boolean readOnly)
    {
        return (SlimeWorldInstance) Proxy.newProxyInstance(
                SlimeWorldInstance.class.getClassLoader(),
                new Class<?>[]{SlimeWorldInstance.class},
                (proxy, method, args) -> switch (method.getName())
                {
                    case "getName" -> name;
                    case "getBukkitWorld" -> bukkit;
                    case "isReadOnly" -> readOnly;
                    case "toString" -> "SlimeWorld[" + name + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type)
    {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        throw new AssertionError("Unknown primitive " + type);
    }
}
