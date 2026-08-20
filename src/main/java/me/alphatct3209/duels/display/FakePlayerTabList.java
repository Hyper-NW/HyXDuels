package me.alphatct3209.duels.display;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Sends player-info-only profiles; these entries never create entities or join the server. */
final class FakePlayerTabList
{
    private static final String UPDATE_PACKET =
            "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket";
    private static final String REMOVE_PACKET =
            "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket";
    private static final String GAME_PROFILE = "com.mojang.authlib.GameProfile";
    private static final String CRAFT_CHAT_MESSAGE = "org.bukkit.craftbukkit.util.CraftChatMessage";

    private final Class<?> updatePacketType;
    private final Class<?> entryType;
    private final Class<? extends Enum> actionType;
    private final Constructor<?> profileConstructor;
    private final Constructor<?> entryConstructor;
    private final Constructor<?> updateConstructor;
    private final Constructor<?> removeConstructor;
    private final Method chatComponent;
    private final Set<Enum> addActions;
    private final Set<Enum> displayAction;
    private final Map<UUID, State> viewers = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    FakePlayerTabList()
    {
        try
        {
            updatePacketType = Class.forName(UPDATE_PACKET);
            entryType = Class.forName(UPDATE_PACKET + "$Entry");
            actionType = (Class<? extends Enum>) Class.forName(UPDATE_PACKET + "$Action");
            profileConstructor = Class.forName(GAME_PROFILE).getConstructor(UUID.class, String.class);
            entryConstructor = canonicalConstructor(entryType);
            updateConstructor = collectionConstructor(updatePacketType, EnumSet.class);
            removeConstructor = collectionConstructor(Class.forName(REMOVE_PACKET), List.class);
            chatComponent = Class.forName(CRAFT_CHAT_MESSAGE)
                    .getMethod("fromStringOrNull", String.class);
            addActions = actions("ADD_PLAYER", "UPDATE_LISTED", "UPDATE_DISPLAY_NAME",
                    "UPDATE_LATENCY", "UPDATE_GAME_MODE", "UPDATE_LIST_ORDER", "UPDATE_HAT");
            displayAction = actions("UPDATE_DISPLAY_NAME");
        }
        catch (ReflectiveOperationException exception)
        {
            throw new IllegalStateException(
                    "The 1.21.11 synthetic tab packet contract is unavailable", exception);
        }
    }

    void show(Player viewer, List<String> cells)
    {
        Objects.requireNonNull(viewer, "viewer");
        if (cells.size() != TabGridLayout.CELL_COUNT)
        {
            throw new IllegalArgumentException("Synthetic tab requires exactly 80 cells");
        }
        try
        {
            State state = viewers.computeIfAbsent(viewer.getUniqueId(), this::state);
            if (!state.added)
            {
                send(viewer, packet(addActions, entries(state, allIndexes(), cells)));
                state.added = true;
                state.cells = List.copyOf(cells);
                return;
            }

            List<Integer> changed = new ArrayList<>();
            for (int index = 0; index < cells.size(); index++)
            {
                if (!Objects.equals(cells.get(index), state.cells.get(index)))
                {
                    changed.add(index);
                }
            }
            if (!changed.isEmpty())
            {
                send(viewer, packet(displayAction, entries(state, changed, cells)));
                state.cells = List.copyOf(cells);
            }
        }
        catch (ReflectiveOperationException exception)
        {
            throw new IllegalStateException("Could not update synthetic tab entries for "
                    + viewer.getName(), exception);
        }
    }

    void remove(Player viewer)
    {
        State state = viewers.remove(viewer.getUniqueId());
        if (state == null || !state.added || !viewer.isOnline())
        {
            return;
        }
        try
        {
            send(viewer, removeConstructor.newInstance(state.ids));
        }
        catch (ReflectiveOperationException exception)
        {
            throw new IllegalStateException("Could not remove synthetic tab entries for "
                    + viewer.getName(), exception);
        }
    }

    private State state(UUID viewerId)
    {
        List<UUID> ids = new ArrayList<>(TabGridLayout.CELL_COUNT);
        List<Object> profiles = new ArrayList<>(TabGridLayout.CELL_COUNT);
        try
        {
            for (int slot = 0; slot < TabGridLayout.CELL_COUNT; slot++)
            {
                UUID id = UUID.nameUUIDFromBytes(("hyxduels:tab:" + viewerId + ':' + slot)
                        .getBytes(StandardCharsets.UTF_8));
                ids.add(id);
                profiles.add(profileConstructor.newInstance(id, profileName()));
            }
        }
        catch (ReflectiveOperationException exception)
        {
            throw new IllegalStateException("Could not create synthetic tab profiles", exception);
        }
        return new State(List.copyOf(ids), List.copyOf(profiles));
    }

    private List<Object> entries(State state, Collection<Integer> indexes, List<String> cells)
            throws ReflectiveOperationException
    {
        List<Object> entries = new ArrayList<>(indexes.size());
        for (int index : indexes)
        {
            entries.add(entry(index, state.ids.get(index), state.profiles.get(index), cells.get(index)));
        }
        return entries;
    }

    private Object entry(int slot, UUID id, Object profile, String text)
            throws ReflectiveOperationException
    {
        RecordComponent[] components = entryType.getRecordComponents();
        Class<?>[] types = entryConstructor.getParameterTypes();
        Object[] values = new Object[types.length];
        for (int index = 0; index < types.length; index++)
        {
            String name = components == null ? "" : components[index].getName().toLowerCase();
            Class<?> type = types[index];
            if (type == UUID.class)
            {
                values[index] = id;
            }
            else if (type.getName().equals(GAME_PROFILE))
            {
                values[index] = profile;
            }
            else if (type == boolean.class)
            {
                values[index] = name.contains("listed");
            }
            else if (type == int.class)
            {
                values[index] = name.contains("order") ? listOrder(slot) : 0;
            }
            else if (type.isEnum())
            {
                values[index] = enumValue(type, "SURVIVAL");
            }
            else if (type.getName().equals("net.minecraft.network.chat.Component"))
            {
                values[index] = chatComponent.invoke(null, text);
            }
            else
            {
                values[index] = null;
            }
        }
        return entryConstructor.newInstance(values);
    }

    /**
     * Modern clients prioritize larger list-order values, so slot zero needs the largest value.
     */
    static int listOrder(int slot)
    {
        if (slot < 0 || slot >= TabGridLayout.CELL_COUNT)
        {
            throw new IllegalArgumentException("Synthetic tab slot must be from 0 through 79");
        }
        return TabGridLayout.CELL_COUNT - slot;
    }

    /** Blank profile names keep internal layout entries out of chat and command suggestions. */
    static String profileName()
    {
        return "";
    }

    private Object packet(Set<Enum> actions, List<Object> entries)
            throws ReflectiveOperationException
    {
        EnumSet<?> set = EnumSet.copyOf(actions);
        return updateConstructor.newInstance(set, entries);
    }

    private void send(Player viewer, Object packet) throws ReflectiveOperationException
    {
        Object handle = viewer.getClass().getMethod("getHandle").invoke(viewer);
        Object connection = connection(handle);
        Method send = sendMethod(connection.getClass(), packet.getClass());
        send.invoke(connection, packet);
    }

    private Object connection(Object handle) throws ReflectiveOperationException
    {
        for (Class<?> type = handle.getClass(); type != null; type = type.getSuperclass())
        {
            for (Field field : type.getDeclaredFields())
            {
                if (field.getName().equals("connection")
                        || field.getType().getSimpleName().contains("GamePacketListener"))
                {
                    field.setAccessible(true);
                    Object value = field.get(handle);
                    if (value != null)
                    {
                        return value;
                    }
                }
            }
        }
        throw new NoSuchFieldException("ServerPlayer connection");
    }

    private Method sendMethod(Class<?> connectionType, Class<?> packetType)
            throws NoSuchMethodException
    {
        for (Method method : connectionType.getMethods())
        {
            if (method.getName().equals("send") && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(packetType))
            {
                return method;
            }
        }
        throw new NoSuchMethodException("Connection send(Packet)");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<Enum> actions(String... names)
    {
        Set<Enum> actions = new LinkedHashSet<>();
        for (String name : names)
        {
            try
            {
                actions.add(Enum.valueOf(actionType, name));
            }
            catch (IllegalArgumentException ignored)
            {
                // Some actions were introduced after the base player-info packet.
            }
        }
        if (actions.isEmpty())
        {
            throw new IllegalStateException("No supported synthetic tab actions were found");
        }
        return Set.copyOf(actions);
    }

    private static Constructor<?> canonicalConstructor(Class<?> record)
            throws NoSuchMethodException
    {
        if (!record.isRecord())
        {
            throw new NoSuchMethodException(record.getName() + " is not a record");
        }
        Class<?>[] types = java.util.Arrays.stream(record.getRecordComponents())
                .map(RecordComponent::getType).toArray(Class<?>[]::new);
        Constructor<?> constructor = record.getDeclaredConstructor(types);
        constructor.setAccessible(true);
        return constructor;
    }

    private static Constructor<?> collectionConstructor(Class<?> owner, Class<?> firstType)
            throws NoSuchMethodException
    {
        for (Constructor<?> constructor : owner.getDeclaredConstructors())
        {
            Class<?>[] types = constructor.getParameterTypes();
            if ((types.length == 2 && firstType.isAssignableFrom(types[0])
                    && Collection.class.isAssignableFrom(types[1]))
                    || (types.length == 1 && Collection.class.isAssignableFrom(types[0])))
            {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        throw new NoSuchMethodException(owner.getName() + " collection constructor");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String preferred)
    {
        try
        {
            return Enum.valueOf((Class<? extends Enum>) type, preferred);
        }
        catch (IllegalArgumentException ignored)
        {
            return type.getEnumConstants()[0];
        }
    }

    private static List<Integer> allIndexes()
    {
        List<Integer> indexes = new ArrayList<>(TabGridLayout.CELL_COUNT);
        for (int index = 0; index < TabGridLayout.CELL_COUNT; index++)
        {
            indexes.add(index);
        }
        return indexes;
    }

    private static final class State
    {
        private final List<UUID> ids;
        private final List<Object> profiles;
        private List<String> cells = List.of();
        private boolean added;

        private State(List<UUID> ids, List<Object> profiles)
        {
            this.ids = ids;
            this.profiles = profiles;
        }
    }
}
