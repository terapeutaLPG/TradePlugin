package pl.yourname.tradeplugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
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
            // Obsłuż kliknięcia w przyciski kontrolne
            if (TradeGUI.isReadyButton(slot)) {
                event.setCancelled(true);
                handleReadyButton(player, session);
                return;
            } else if (TradeGUI.isAcceptButton(slot)) {
                event.setCancelled(true);
                handleAcceptButton(player, session);
                return;
            } else if (TradeGUI.isCancelButton(slot)) {
                event.setCancelled(true);
                handleCancelButton(player, session);
                return;
            } else if (canPlayerModifySlot(player, session, slot)) {
                // Pozwól na naturalne przenoszenie przedmiotów
                // NIE anulujemy eventu - pozwalamy na standardową obsługę
                handleTradeSlotUpdate(player, session);
                return;
            } else {
                // Zablokuj kliknięcia w inne sloty (separatory, etykiety itp.)
                event.setCancelled(true);
            }
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
            // Sprawdź czy sesja nadal istnieje
            if (plugin.getTradeManager().getPlayerTradeSession(player) != session) {
                return; // Sesja już nie istnieje (handel został zakończony lub anulowany)
            }
            
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

    private void handleTradeSlotUpdate(Player player, TradeSession session) {
        // Po każdej zmianie w slotach handlu, resetuj status gotowości
        session.setPlayerReady(player, false);
        session.setPlayerReady(session.getOtherPlayer(player), false);

        // Odśwież GUI z opóźnieniem, żeby standardowy event się wykonał
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Synchronizuj przedmioty z inventory do sesji
            syncInventoryToSession(session);
            updateBothPlayersGUI(session);
        }, 1L);
    }

    private void syncInventoryToSession(TradeSession session) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();

        // Sprawdź czy gracze mają otwarte GUI
        if (player1.getOpenInventory().getTopInventory().getSize() != 54
                || player2.getOpenInventory().getTopInventory().getSize() != 54) {
            return;
        }

        Inventory inv1 = player1.getOpenInventory().getTopInventory();
        Inventory inv2 = player2.getOpenInventory().getTopInventory();

        // Synchronizuj przedmioty gracza 1
        ItemStack[] player1Items = session.getPlayerItems(player1);
        for (int i = 0; i < TradeGUI.PLAYER1_SLOTS.length && i < player1Items.length; i++) {
            ItemStack item = inv1.getItem(TradeGUI.PLAYER1_SLOTS[i]);
            player1Items[i] = (item != null && !item.getType().isAir()) ? item.clone() : null;
        }

        // Synchronizuj przedmioty gracza 2  
        ItemStack[] player2Items = session.getPlayerItems(player2);
        for (int i = 0; i < TradeGUI.PLAYER2_SLOTS.length && i < player2Items.length; i++) {
            ItemStack item = inv2.getItem(TradeGUI.PLAYER2_SLOTS[i]);
            player2Items[i] = (item != null && !item.getType().isAir()) ? item.clone() : null;
        }
    }

    private void handleReadyButton(Player player, TradeSession session) {
        boolean currentReady = session.isPlayerReady(player);
        session.setPlayerReady(player, !currentReady);

        Player otherPlayer = session.getOtherPlayer(player);

        if (!currentReady) {
            player.sendMessage(ChatColor.GREEN + "Jesteś gotowy do handlu!");
            otherPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " jest gotowy!");
            
            // Sprawdź czy obaj gracze są teraz gotowi
            if (session.areBothPlayersReady()) {
                startTradeCountdown(session);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Nie jesteś już gotowy!");
            otherPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " nie jest już gotowy!");
        }

        updateBothPlayersGUI(session);
    }

    private void startTradeCountdown(TradeSession session) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();
        
        // Wyślij wiadomość o rozpoczęciu odliczania
        player1.sendMessage(ChatColor.GOLD + "Obaj gracze są gotowi! Automatyczna akceptacja za 4 sekundy...");
        player2.sendMessage(ChatColor.GOLD + "Obaj gracze są gotowi! Automatyczna akceptacja za 4 sekundy...");
        
        // Rozpocznij odliczanie
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Sprawdź czy sesja nadal istnieje i obaj gracze są gotowi
            if (plugin.getTradeManager().getPlayerTradeSession(player1) != session || 
                plugin.getTradeManager().getPlayerTradeSession(player2) != session) {
                return; // Handel został anulowany
            }
            
            if (!session.areBothPlayersReady()) {
                return; // Ktoś przestał być gotowy
            }
            
            // Automatycznie potwierdź dla obydwu graczy
            session.setPlayerConfirmed(player1, true);
            session.setPlayerConfirmed(player2, true);
            
            // Wyślij wiadomości
            player1.sendMessage(ChatColor.GREEN + "Automatyczne potwierdzenie handlu!");
            player2.sendMessage(ChatColor.GREEN + "Automatyczne potwierdzenie handlu!");
            
            // Zakończ handel
            completeTrade(session);
            
        }, 80L); // 4 sekundy (80 ticks)
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

        // Synchronizuj najpierw przedmioty z inventory
        syncInventoryToSession(session);

        // Pobierz przedmioty do wymiany (PRZED zamknięciem GUI)
        ItemStack[] player1Items = session.getPlayerItems(player1).clone();
        ItemStack[] player2Items = session.getPlayerItems(player2).clone();

        // Zamknij GUI dla obydwu graczy
        player1.closeInventory();
        player2.closeInventory();

        // Wykonaj wymianę przedmiotów - każdy gracz dostaje przedmioty DRUGIEGO gracza
        for (ItemStack item : player2Items) { // Gracz 1 dostaje przedmioty gracza 2
            if (item != null && !item.getType().isAir()) {
                player1.getInventory().addItem(item.clone());
            }
        }

        for (ItemStack item : player1Items) { // Gracz 2 dostaje przedmioty gracza 1
            if (item != null && !item.getType().isAir()) {
                player2.getInventory().addItem(item.clone());
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
