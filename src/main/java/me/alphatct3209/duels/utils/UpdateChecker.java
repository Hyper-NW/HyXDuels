package me.alphatct3209.duels.utils;

import me.alphatct3209.duels.Duels;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

public class UpdateChecker
{
    public static void updateCheck(Duels plugin, CommandSender sender, boolean sendGoodMessage)
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new URL("https://api.spigotmc.org/legacy/update.php?resource=44820").openStream())))
        {
            String latest = reader.readLine();
            if (latest == null) return;
            Map<String, Object> values = Map.of("<version>", plugin.getDescription().getVersion(),
                    "<latest_version>", latest);
            if (plugin.getDescription().getVersion().equals(latest))
            {
                if (sendGoodMessage)
                    MessageService.send(sender, plugin.getConfig(), "Messages.Update-Check-Good", values);
            }
            else MessageService.send(sender, plugin.getConfig(), "Messages.Update-Check-Bad", values);
        }
        catch (IOException exception)
        {
            MessageService.send(sender, plugin.getConfig(), "Messages.Update-Check-Failed", Map.of());
            exception.printStackTrace(System.err);
        }
    }
}
