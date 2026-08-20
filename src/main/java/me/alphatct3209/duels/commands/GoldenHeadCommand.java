package me.alphatct3209.duels.commands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.game.items.GoldenHead;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class GoldenHeadCommand implements CommandExecutor, TabCompleter
{
    private final Duels plugin;

    public GoldenHeadCommand(Duels plugin) { this.plugin = Objects.requireNonNull(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) return reload(sender);
        if (args.length < 1 || !args[0].equalsIgnoreCase("give"))
        {
            usage(sender);
            return true;
        }
        if (!sender.hasPermission("duels.goldenhead.give"))
        {
            MessageService.send(sender, plugin.getConfig(), "Messages.No-Permission", Map.of());
            return true;
        }
        GoldenHead goldenHead = plugin.getGoldenHead();
        if (!goldenHead.enabled())
        {
            MessageService.send(sender, plugin.getConfig(), "Messages.Golden-Head-Disabled", Map.of(),
                    "&cGolden Heads are disabled.");
            return true;
        }

        Player target;
        int amount = goldenHead.commandDefaultAmount();
        if (args.length == 1)
        {
            if (!(sender instanceof Player player)) { usage(sender); return true; }
            target = player;
        }
        else if (args.length == 2 && sender instanceof Player player && integer(args[1]) != null)
        {
            target = player;
            amount = integer(args[1]);
        }
        else if (args.length == 2 || args.length == 3)
        {
            target = exactVisiblePlayer(sender, args[1]);
            if (target == null)
            {
                MessageService.send(sender, plugin.getConfig(), "Messages.Golden-Head-Player-Not-Found",
                        Map.of("<player>", args[1]), "&cThat exact player is not available.");
                return true;
            }
            if (args.length == 3)
            {
                Integer parsed = integer(args[2]);
                if (parsed == null) { usage(sender); return true; }
                amount = parsed;
            }
        }
        else { usage(sender); return true; }

        if (amount < 1 || amount > goldenHead.commandMaximumAmount())
        {
            MessageService.send(sender, plugin.getConfig(), "Messages.Golden-Head-Amount",
                    Map.of("<maximum>", goldenHead.commandMaximumAmount()),
                    "&cAmount must be between 1 and <maximum>.");
            return true;
        }
        int remaining = amount;
        while (remaining > 0)
        {
            int stackAmount = Math.min(64, remaining);
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(
                    goldenHead.create(target, stackAmount));
            overflow.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            remaining -= stackAmount;
        }
        MessageService.send(sender, plugin.getConfig(), "Messages.Golden-Head-Given",
                Map.of("<player>", target.getName(), "<amount>", amount),
                "&aGave &e<amount> Golden Head(s) &ato &e<player>&a.");
        if (!sender.equals(target))
            MessageService.send(target, plugin.getConfig(), "Messages.Golden-Head-Received",
                    Map.of("<amount>", amount), "&aYou received &e<amount> Golden Head(s)&a.");
        return true;
    }

    private boolean reload(CommandSender sender)
    {
        if (!sender.hasPermission("duels.goldenhead.reload"))
        {
            MessageService.send(sender, plugin.getConfig(), "Messages.No-Permission", Map.of());
            return true;
        }
        try
        {
            plugin.getGoldenHead().reload();
            MessageService.send(sender, plugin.getConfig(), "Messages.Golden-Head-Reloaded", Map.of(),
                    "&aReloaded advanced/golden-heads.yml.");
        }
        catch (RuntimeException exception)
        {
            MessageService.send(sender, plugin.getConfig(), "Messages.Golden-Head-Reload-Failed",
                    Map.of("<error>", exception.getMessage()), "&cGolden Head reload failed: <error>");
        }
        return true;
    }

    private void usage(CommandSender sender)
    {
        MessageService.send(sender, plugin.getConfig(), "Messages.Golden-Head-Usage", Map.of(),
                "&e/goldenhead give [player] [amount]", "&e/goldenhead reload");
    }

    private Player exactVisiblePlayer(CommandSender sender, String name)
    {
        Player player = Bukkit.getOnlinePlayers().stream()
                .filter(candidate -> candidate.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        return player != null && (!(sender instanceof Player viewer) || viewer.canSee(player)) ? player : null;
    }

    private static Integer integer(String value)
    {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { return null; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        List<String> values = new ArrayList<>();
        if (args.length == 1)
        {
            if (sender.hasPermission("duels.goldenhead.give")) values.add("give");
            if (sender.hasPermission("duels.goldenhead.reload")) values.add("reload");
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("give")
                && sender.hasPermission("duels.goldenhead.give"))
        {
            Bukkit.getOnlinePlayers().stream()
                    .filter(player -> !(sender instanceof Player viewer) || viewer.canSee(player))
                    .map(Player::getName).forEach(values::add);
            if (sender instanceof Player) values.add(Integer.toString(plugin.getGoldenHead().commandDefaultAmount()));
        }
        else if (args.length == 3 && args[0].equalsIgnoreCase("give"))
            values.addAll(List.of("1", "3", "8", "16", "32", "64"));
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().distinct().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
