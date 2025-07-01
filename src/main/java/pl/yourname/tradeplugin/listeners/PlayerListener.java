package pl.yourname.tradeplugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import pl.yourname.tradeplugin.TradePlugin;
import pl.yourname.tradeplugin.models.TradeRequest;
import pl.yourname.tradeplugin.models.TradeSession;

public class PlayerListener implements Listener {

    private final TradePlugin plugin;

    public PlayerListener(TradePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Sprawdź czy gracz ma aktywną sesję handlu
        TradeSession session = plugin.getTradeManager().getPlayerTradeSession(player);
        if (session != null) {
            Player otherPlayer = session.getOtherPlayer(player);

            // Anuluj handel
            plugin.getTradeManager().cancelTrade(session);

            // Zwróć przedmioty graczom
            returnItemsToPlayer(player, session.getPlayerItems(player));
            returnItemsToPlayer(otherPlayer, session.getPlayerItems(otherPlayer));

            // Powiadom drugiego gracza
            otherPlayer.sendMessage(ChatColor.RED + "Handel anulowany - " + player.getName() + " opuścił serwer");
            otherPlayer.closeInventory();
        }

        // Sprawdź czy gracz ma pending request jako requester
        TradeRequest outgoingRequest = plugin.getTradeManager().getPendingRequestByRequester(player);
        if (outgoingRequest != null) {
            plugin.getTradeManager().denyTradeRequest(outgoingRequest.getTarget());
            Player target = outgoingRequest.getTarget();
            if (target.isOnline()) {
                target.sendMessage(ChatColor.RED + "Prośba o handel od " + player.getName() + " została anulowana - gracz opuścił serwer");
            }
        }

        // Sprawdź czy gracz ma pending request jako target
        TradeRequest incomingRequest = plugin.getTradeManager().getPendingRequestForTarget(player);
        if (incomingRequest != null) {
            plugin.getTradeManager().denyTradeRequest(player);
            Player requester = incomingRequest.getRequester();
            if (requester.isOnline()) {
                requester.sendMessage(ChatColor.RED + "Prośba o handel została anulowana - " + player.getName() + " opuścił serwer");
            }
        }
    }

    private void returnItemsToPlayer(Player player, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item);
            }
        }
    }
}
