package pl.yourname.tradeplugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import pl.yourname.tradeplugin.TradePlugin;
import pl.yourname.tradeplugin.gui.TradeGUI;
import pl.yourname.tradeplugin.models.TradeSession;

public class InventoryListener implements Listener {

    private final TradePlugin plugin;

    public InventoryListener(TradePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        TradeSession session = plugin.getTradeManager().getPlayerTradeSession(player);

        if (session == null) {
            return; // Gracz nie jest w handlu
        }

        // Sprawdź czy to GUI handlu
        if (event.getInventory().getSize() != 54) {
            return;
        }

        int slot = event.getRawSlot();

        // Jeśli kliknięcie w górnym inventory (GUI handlu)
        if (slot < 54) {
            event.setCancelled(true); // Zawsze anuluj standardowe zachowanie

            // Obsłuż kliknięcia w przyciski kontrolne
            if (TradeGUI.isReadyButton(slot)) {
                handleReadyButton(player, session);
                return;
            } else if (TradeGUI.isAcceptButton(slot)) {
                handleAcceptButton(player, session);
                return;
            } else if (TradeGUI.isCancelButton(slot)) {
                handleCancelButton(player, session);
                return;
            } else if (canPlayerModifySlot(player, session, slot)) {
                // Pozwól graczowi modyfikować swoje sloty handlu
                handleTradeSlotInteraction(player, session, slot, event);
                return;
            }
            // Wszystkie inne kliknięcia w GUI są zablokowane
        }
        // Kliknięcia w dolnym inventory (ekwipunek gracza) są dozwolone
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        TradeSession session = plugin.getTradeManager().getPlayerTradeSession(player);

        if (session == null) {
            return;
        }

        // Anuluj przeciąganie w GUI handlu
        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        TradeSession session = plugin.getTradeManager().getPlayerTradeSession(player);

        if (session == null) {
            return;
        }

        // Sprawdź czy to GUI handlu
        if (event.getInventory().getSize() != 54) {
            return;
        }

        // Dodaj małe opóźnienie żeby sprawdzić czy GUI nie jest po prostu odświeżane
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Sprawdź ponownie czy gracz nadal ma otwarte GUI handlu
            if (player.getOpenInventory().getTopInventory().getSize() != 54) {
                // Gracz rzeczywiście zamknął GUI handlu
                cancelTrade(session, player.getName() + " zamknął okno handlu");
            }
        }, 2L); // 2 ticki opóźnienia
    }

    private boolean canPlayerModifySlot(Player player, TradeSession session, int slot) {
        if (player.equals(session.getPlayer1())) {
            return TradeGUI.isPlayer1Slot(slot);
        } else if (player.equals(session.getPlayer2())) {
            return TradeGUI.isPlayer2Slot(slot);
        }
        return false;
    }

    private void handleTradeSlotInteraction(Player player, TradeSession session, int slot, InventoryClickEvent event) {
        boolean isPlayer1 = player.equals(session.getPlayer1());
        int tradeSlotIndex = TradeGUI.getTradeSlotIndex(slot, isPlayer1);

        if (tradeSlotIndex == -1) {
            return;
        }

        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();

        // Bezpieczne kopiowanie, jeśli przedmioty nie są null
        ItemStack cursorCopy = (cursorItem != null && !cursorItem.getType().isAir()) ? cursorItem.clone() : null;
        ItemStack clickedCopy = (clickedItem != null && !clickedItem.getType().isAir()) ? clickedItem.clone() : null;

        if (cursorCopy != null) {
            // Gracz ma przedmiot na kursorze - chce go włożyć
            session.setPlayerItem(player, tradeSlotIndex, cursorCopy);

            // Ustaw przedmiot na kursorze jako ten ze slotu (może być null)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.setItemOnCursor(clickedCopy);
                updateBothPlayersGUI(session);
            });

        } else if (clickedCopy != null) {
            // Gracz chce wziąć przedmiot ze slotu
            session.setPlayerItem(player, tradeSlotIndex, null);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.setItemOnCursor(clickedCopy);
                updateBothPlayersGUI(session);
            });
        }
    }

    private void handleReadyButton(Player player, TradeSession session) {
        boolean currentReady = session.isPlayerReady(player);
        session.setPlayerReady(player, !currentReady);

        Player otherPlayer = session.getOtherPlayer(player);

        if (!currentReady) {
            player.sendMessage(ChatColor.GREEN + "Jesteś gotowy do handlu!");
            otherPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " jest gotowy!");
        } else {
            player.sendMessage(ChatColor.RED + "Nie jesteś już gotowy!");
            otherPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " nie jest już gotowy!");
        }

        updateBothPlayersGUI(session);
    }

    private void handleAcceptButton(Player player, TradeSession session) {
        if (!session.areBothPlayersReady()) {
            player.sendMessage(ChatColor.RED + "Obaj gracze muszą być gotowi!");
            return;
        }

        boolean currentConfirmed = session.isPlayerConfirmed(player);
        session.setPlayerConfirmed(player, !currentConfirmed);

        Player otherPlayer = session.getOtherPlayer(player);

        if (!currentConfirmed) {
            player.sendMessage(ChatColor.GREEN + "Potwierdziłeś handel!");
            otherPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " potwierdził handel!");

            // Sprawdź czy obaj gracze potwierdzili
            if (session.areBothPlayersConfirmed()) {
                completeTrade(session);
                return;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Anulowałeś potwierdzenie!");
            otherPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " anulował potwierdzenie!");
        }

        updateBothPlayersGUI(session);
    }

    private void handleCancelButton(Player player, TradeSession session) {
        cancelTrade(session, player.getName() + " anulował handel");
    }

    private void completeTrade(TradeSession session) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();

        // Zamknij GUI dla obydwu graczy
        player1.closeInventory();
        player2.closeInventory();

        // Wykonaj wymianę przedmiotów
        ItemStack[] player1Items = session.getPlayerItems(player1);
        ItemStack[] player2Items = session.getPlayerItems(player2);

        // Dodaj przedmioty do ekwipunku graczy
        for (ItemStack item : player1Items) {
            if (item != null && !item.getType().isAir()) {
                player2.getInventory().addItem(item);
            }
        }

        for (ItemStack item : player2Items) {
            if (item != null && !item.getType().isAir()) {
                player1.getInventory().addItem(item);
            }
        }

        // Wyślij wiadomości o sukcesie
        player1.sendMessage(ChatColor.GREEN + "Handel zakończony pomyślnie!");
        player2.sendMessage(ChatColor.GREEN + "Handel zakończony pomyślnie!");

        // Usuń sesję handlu
        plugin.getTradeManager().cancelTrade(session);
    }

    private void cancelTrade(TradeSession session, String reason) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();

        // Zamknij GUI dla obydwu graczy
        player1.closeInventory();
        player2.closeInventory();

        // Zwróć przedmioty graczom
        returnItemsToPlayer(player1, session.getPlayerItems(player1));
        returnItemsToPlayer(player2, session.getPlayerItems(player2));

        // Wyślij wiadomości
        player1.sendMessage(ChatColor.RED + "Handel anulowany: " + reason);
        player2.sendMessage(ChatColor.RED + "Handel anulowany: " + reason);

        // Usuń sesję handlu
        plugin.getTradeManager().cancelTrade(session);
    }

    private void returnItemsToPlayer(Player player, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item);
            }
        }
    }

    private void updateBothPlayersGUI(TradeSession session) {
        TradeGUI.updateTradeGUI(session, session.getPlayer1());
        TradeGUI.updateTradeGUI(session, session.getPlayer2());
    }
}
