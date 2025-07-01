package pl.yourname.tradeplugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pl.yourname.tradeplugin.TradePlugin;
import pl.yourname.tradeplugin.gui.TradeGUI;
import pl.yourname.tradeplugin.models.TradeRequest;
import pl.yourname.tradeplugin.models.TradeSession;

public class TradeCommand implements CommandExecutor {

    private final TradePlugin plugin;

    public TradeCommand(TradePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą używać tej komendy!");
            return true;
        }

        Player player = (Player) sender;

        try {
            switch (command.getName().toLowerCase()) {
                case "wymiana":
                    return handleTradeCommand(player, args);
                case "wymianaakceptuj":
                    return handleTradeAccept(player);
                case "wymianaodrzuc":
                    return handleTradeDeny(player);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas wykonywania komendy " + command.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Wystąpił błąd podczas wykonywania komendy!");
            return true;
        }

        return false;
    }

    private boolean handleTradeCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Użycie: /wymiana <gracz>");
            return true;
        }

        // Sprawdź czy gracz nie jest już w handlu
        if (plugin.getTradeManager().isPlayerInTrade(player)) {
            player.sendMessage(ChatColor.RED + "Jesteś już w trakcie handlu!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Gracz nie jest online!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "Nie możesz handlować sam ze sobą!");
            return true;
        }

        // Sprawdź czy docelowy gracz nie jest w handlu
        if (plugin.getTradeManager().isPlayerInTrade(target)) {
            player.sendMessage(ChatColor.RED + "Ten gracz jest już w trakcie handlu!");
            return true;
        }

        // Sprawdź czy nie ma już pending request
        TradeRequest existingRequest = plugin.getTradeManager().getPendingRequestByRequester(player);
        if (existingRequest != null) {
            player.sendMessage(ChatColor.RED + "Masz już wysłaną prośbę o handel!");
            return true;
        }

        // Sprawdź czy docelowy gracz nie ma już prośby od tego gracza
        TradeRequest targetRequest = plugin.getTradeManager().getPendingRequestForTarget(target);
        if (targetRequest != null && targetRequest.getRequester().equals(player)) {
            player.sendMessage(ChatColor.RED + "Już wysłałeś prośbę o handel do tego gracza!");
            return true;
        }

        // Utwórz prośbę o handel
        TradeRequest request = plugin.getTradeManager().createTradeRequest(player, target);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "Nie można utworzyć prośby o handel!");
            return true;
        }

        // Wyślij wiadomości
        player.sendMessage(ChatColor.GREEN + "Wysłano prośbę o handel do " + target.getName());
        target.sendMessage(ChatColor.YELLOW + player.getName() + " chce z Tobą handlować!");
        target.sendMessage(ChatColor.YELLOW + "Wpisz " + ChatColor.GREEN + "/wymianaakceptuj" + ChatColor.YELLOW + " aby zaakceptować lub " + ChatColor.RED + "/wymianaodrzuc" + ChatColor.YELLOW + " aby odrzucić");

        return true;
    }

    private boolean handleTradeAccept(Player player) {
        TradeRequest request = plugin.getTradeManager().getPendingRequestForTarget(player);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "Nie masz żadnych próśb o handel!");
            return true;
        }

        if (request.isExpired()) {
            plugin.getTradeManager().denyTradeRequest(player);
            player.sendMessage(ChatColor.RED + "Prośba o handel wygasła!");
            return true;
        }

        Player requester = request.getRequester();
        if (!requester.isOnline()) {
            plugin.getTradeManager().denyTradeRequest(player);
            player.sendMessage(ChatColor.RED + "Gracz, który wysłał prośbę, nie jest już online!");
            return true;
        }

        // Zaakceptuj handel
        if (plugin.getTradeManager().acceptTradeRequest(player)) {
            player.sendMessage(ChatColor.GREEN + "Zaakceptowałeś handel z " + requester.getName());
            requester.sendMessage(ChatColor.GREEN + player.getName() + " zaakceptował handel!");

            // Otwórz GUI handlu dla obydwu graczy
            TradeSession session = plugin.getTradeManager().getPlayerTradeSession(player);
            TradeGUI.openTradeGUI(session, player);
            TradeGUI.openTradeGUI(session, requester);
        } else {
            player.sendMessage(ChatColor.RED + "Nie można zaakceptować handlu!");
        }

        return true;
    }

    private boolean handleTradeDeny(Player player) {
        TradeRequest request = plugin.getTradeManager().getPendingRequestForTarget(player);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "Nie masz żadnych próśb o handel!");
            return true;
        }

        Player requester = request.getRequester();

        if (plugin.getTradeManager().denyTradeRequest(player)) {
            player.sendMessage(ChatColor.GREEN + "Odrzuciłeś prośbę o handel od " + requester.getName());
            if (requester.isOnline()) {
                requester.sendMessage(ChatColor.RED + player.getName() + " odrzucił Twoją prośbę o handel!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Nie można odrzucić prośby o handel!");
        }

        return true;
    }
}
