package me.alphatct3209.duels.commands.subcommands;

import me.alphatct3209.duels.Duels;
import me.alphatct3209.duels.commands.HelpPagination;
import me.alphatct3209.duels.utils.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class HelpDuelsSubCmd extends DuelsSubCommand
{

    public HelpDuelsSubCmd(Duels plugin)
    {
        super(plugin, "help");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("duels.help"))
        {
            noPerm(sender);
            return true;
        }
        if(args.length <= 1)
        {
            int requestedPage = 1;
            if (args.length == 1)
            {
                try { requestedPage = Integer.parseInt(args[0]); }
                catch (NumberFormatException exception)
                {
                    incorrectArgs(sender, "/duels help [page]");
                    return true;
                }
            }
            List<String> entries = MessageService.render(plugin.getConfig(),
                    "Messages.Help-Menu", Map.of());
            HelpPagination.Page page = HelpPagination.page(entries, requestedPage,
                    plugin.getConfig().getInt("Messages.Help-Page-Size", 6));
            MessageService.send(sender, plugin.getConfig(), "Messages.Help-Header",
                    Map.of("<page>", page.number(), "<pages>", page.count()),
                    "&9&lHyXDuels Help &7(Page <page>/<pages>)");
            page.entries().forEach(sender::sendMessage);
            if (page.count() > 1)
                MessageService.send(sender, plugin.getConfig(), "Messages.Help-Footer",
                        Map.of("<page>", page.number(), "<pages>", page.count()),
                        "&7Use &e/duels help <page> &7to change pages.");
            return true;
        }
        else
        {
            incorrectArgs(sender, "/duels help [page]");
            return true;
        }
    }
}
