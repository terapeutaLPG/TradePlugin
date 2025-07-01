package pl.yourname.tradeplugin.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pl.yourname.tradeplugin.TradePlugin;
import pl.yourname.tradeplugin.gui.TradeHistoryGUI;
import pl.yourname.tradeplugin.models.TradeHistoryEntry;

public class TradeHistoryCommand implements CommandExecutor {

    private final TradePlugin plugin;

    public TradeHistoryCommand(TradePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda może być używana tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("tradeplugin.admin")) {
            player.sendMessage(ChatColor.RED + "Nie masz uprawnień do tej komendy!");
            return true;
        }

        if (args.length == 0) {
            // Pokaż wszystkie historie
            List<TradeHistoryEntry> history = plugin.getHistoryManager().getHistory();
            if (history.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Brak historii handlu.");
                return true;
            }

            TradeHistoryGUI.openHistoryGUI(player, history, plugin);
        } else if (args.length == 1) {
            // Pokaż historię dla konkretnego gracza
            String targetPlayer = args[0];
            List<TradeHistoryEntry> history = plugin.getHistoryManager().getHistoryForPlayer(targetPlayer);

            if (history.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Brak historii handlu dla gracza: " + targetPlayer);
                return true;
            }

            TradeHistoryGUI.openHistoryGUI(player, history, plugin);
        } else {
            player.sendMessage(ChatColor.RED + "Użycie: /wymianahist [gracz]");
        }

        return true;
    }
}
