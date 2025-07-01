package pl.yourname.tradeplugin.listeners;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import pl.yourname.tradeplugin.gui.TradeHistoryGUI;
import pl.yourname.tradeplugin.models.TradeHistoryEntry;
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

        // Sprawdź czy to GUI historii handlu - użyjemy prostego rozpoznawania na podstawie tego, czy gracz nie jest w sesji handlu
        // i inventory ma odpowiedni rozmiar
        if (session == null && TradeHistoryGUI.isHistoryGUI(event.getInventory())) {
            event.setCancelled(true);
            handleHistoryGUIClick(player, event);
            return;
        }

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

        // Sprawdź czy drag jest tylko w dozwolonych slotach
        boolean allowDrag = true;
        for (int slot : event.getRawSlots()) {
            if (slot < 54) { // Slot w GUI handlu
                if (!canPlayerModifySlot(player, session, slot)) {
                    allowDrag = false;
                    break;
                }
            }
        }

        if (!allowDrag) {
            event.setCancelled(true);
        } else {
            // Pozwól na drag, ale zresetuj gotowość i odśwież po dragowaniu
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                session.setPlayerReady(player, false);
                session.setPlayerReady(session.getOtherPlayer(player), false);
                syncInventoryToSession(session);
                updateBothPlayersGUI(session);
            }, 1L);
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

        // Rozpocznij countdown w sesji
        session.startCountdown();

        // Wyślij wiadomość o rozpoczęciu odliczania
        player1.sendMessage(ChatColor.GOLD + "Obaj gracze są gotowi! Automatyczna akceptacja za 4 sekundy...");
        player2.sendMessage(ChatColor.GOLD + "Obaj gracze są gotowi! Automatyczna akceptacja za 4 sekundy...");

        // Odśwież GUI aby pokazać countdown
        updateBothPlayersGUI(session);

        // Uruchom zadanie countdown'u z odświeżaniem co sekundę
        runCountdownTask(session, 4);
    }

    private void runCountdownTask(TradeSession session, int secondsLeft) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();

        // Sprawdź czy sesja nadal istnieje i countdown jest aktywny
        if (plugin.getTradeManager().getPlayerTradeSession(player1) != session
                || plugin.getTradeManager().getPlayerTradeSession(player2) != session
                || !session.isCountdownActive()
                || !session.areBothPlayersReady()) {

            // Anuluj countdown
            session.setCountdownActive(false);
            updateBothPlayersGUI(session);
            return;
        }

        // Ustaw sekundy w sesji
        session.setCountdownSeconds(secondsLeft);
        updateBothPlayersGUI(session);

        // Odtwórz dźwięk countdown'u
        if (secondsLeft > 0) {
            // Dźwięk tick podczas odliczania
            player1.playSound(player1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            player2.playSound(player2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        } else {
            // Dźwięk sukcesu na końcu
            player1.playSound(player1.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player2.playSound(player2.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        if (secondsLeft <= 0) {
            // Countdown skończony - wykonaj handel
            session.setPlayerConfirmed(player1, true);
            session.setPlayerConfirmed(player2, true);

            player1.sendMessage(ChatColor.GREEN + "Automatyczne potwierdzenie handlu!");
            player2.sendMessage(ChatColor.GREEN + "Automatyczne potwierdzenie handlu!");

            completeTrade(session);
        } else {
            // Kontynuuj countdown za sekundę
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                runCountdownTask(session, secondsLeft - 1);
            }, 20L); // 20 ticks = 1 sekunda
        }
    }

    private void handleCancelButton(Player player, TradeSession session) {
        cancelTrade(session, player.getName() + " anulował handel");
    }

    private void completeTrade(TradeSession session) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();

        // Synchronizuj najpierw przedmioty z inventory
        syncInventoryToSession(session);

        // Pobierz przedmioty do wymiany
        ItemStack[] player1Items = session.getPlayerItems(player1).clone();
        ItemStack[] player2Items = session.getPlayerItems(player2).clone();

        // Zapisz handel do historii
        plugin.getHistoryManager().addTradeEntry(
                player1.getName(), player1.getUniqueId(),
                player2.getName(), player2.getUniqueId(),
                player1Items, player2Items
        );

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

        // Zamknij GUI dla obydwu graczy po zakończeniu handlu
        player1.closeInventory();
        player2.closeInventory();

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

    private void handleHistoryGUIClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        // Sprawdź czy to GUI szczegółów handlu
        if (TradeHistoryGUI.isDetailGUI(event.getInventory())) {
            // W GUI szczegółów tylko przycisk powrotu (slot 49)
            if (slot == 49 && clickedItem.getType() == Material.ARROW) {
                // Powrót do historii
                List<TradeHistoryEntry> history = plugin.getHistoryManager().getHistory();
                TradeHistoryGUI.openHistoryGUI(player, history, plugin);
            }
            return;
        }

        // GUI głównej historii - sprawdź rozmiar i typ inventory
        if (event.getInventory().getSize() <= 54) {
            // Sprawdź czy to przycisk zamknięcia
            if (clickedItem.getType() == Material.BARRIER) {
                player.closeInventory();
                return;
            }

            // Sprawdź czy to item historii (książka)
            if (clickedItem.getType() == Material.WRITABLE_BOOK) {
                List<TradeHistoryEntry> history = plugin.getHistoryManager().getHistory();
                if (slot >= 0 && slot < history.size()) {
                    TradeHistoryEntry entry = history.get(slot);
                    TradeHistoryGUI.openTradeDetailGUI(player, entry, plugin);
                }
            }
        }
    }
}
