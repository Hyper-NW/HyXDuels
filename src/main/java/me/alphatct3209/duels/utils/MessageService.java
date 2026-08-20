package me.alphatct3209.duels.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Renders message configuration values that may be either one string or a YAML list. */
public final class MessageService
{
    private MessageService() {}

    public static List<String> lines(ConfigurationSection configuration, String path, String... fallback)
    {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(path, "path");
        Object raw = configuration.get(path);
        List<String> values = new ArrayList<>();
        if (raw instanceof List<?> list)
        {
            for (Object value : list)
            {
                if (value != null) values.add(value.toString());
            }
        }
        else if (raw != null)
        {
            values.add(raw.toString());
        }
        else
        {
            values.addAll(List.of(fallback));
        }
        return values.stream()
                .flatMap(value -> value.lines())
                .map(value -> ChatColor.translateAlternateColorCodes('&', value))
                .toList();
    }

    public static List<String> render(ConfigurationSection configuration, String path,
                                      Map<String, ?> replacements, String... fallback)
    {
        return lines(configuration, path, fallback).stream().map(line -> replace(line, replacements)).toList();
    }

    public static void send(CommandSender recipient, ConfigurationSection configuration, String path,
                            Map<String, ?> replacements, String... fallback)
    {
        for (String line : render(configuration, path, replacements, fallback))
        {
            if (!line.isEmpty()) recipient.sendMessage(line);
        }
    }

    public static String replace(String input, Map<String, ?> replacements)
    {
        String result = input;
        for (Map.Entry<String, ?> replacement : replacements.entrySet())
        {
            result = result.replace(replacement.getKey(), Objects.toString(replacement.getValue(), ""));
        }
        return result;
    }
}
