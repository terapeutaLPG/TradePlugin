package pl.yourname.tradeplugin.gui;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import pl.yourname.tradeplugin.models.TradeSession;

public class TradeGUI {

    private static final int INVENTORY_SIZE = 54; // 6 rzędów
    private static final String INVENTORY_TITLE = ChatColor.DARK_GREEN + "Handel";

    // Sloty dla przedmiotów gracza 1 (lewa strona)
    public static final int[] PLAYER1_SLOTS = {
        0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30
    };

    // Sloty dla przedmiotów gracza 2 (prawa strona)
    public static final int[] PLAYER2_SLOTS = {
        5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35
    };

    // Separatory (środkowa kolumna)
    private static final int[] SEPARATOR_SLOTS = {4, 13, 22, 31, 40, 49};

    // Przyciski kontrolne
    private static final int PLAYER1_READY_SLOT = 36;
    private static final int PLAYER1_CONFIRM_SLOT = 45;
    private static final int PLAYER2_READY_SLOT = 44;
    private static final int PLAYER2_CONFIRM_SLOT = 53;
    private static final int CANCEL_SLOT = 48;
    private static final int ACCEPT_SLOT = 50;

    public static void openTradeGUI(TradeSession session, Player player) {
        if (session == null || player == null) {
            return;
        }

        try {
            Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, INVENTORY_TITLE);

            // Ustaw separatory
            ItemStack separator = createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Separator", null);
            for (int slot : SEPARATOR_SLOTS) {
                inventory.setItem(slot, separator);
            }

            // Dodaj przedmioty z sesji
            updateInventoryItems(inventory, session, player);

            // Dodaj przyciski kontrolne
            updateControlButtons(inventory, session, player);

            player.openInventory(inventory);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Błąd podczas otwierania GUI handlu!");
            e.printStackTrace();
        }
    }

    public static void updateTradeGUI(TradeSession session, Player player) {
        if (session == null || player == null) {
            return;
        }

        try {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory.getSize() != INVENTORY_SIZE) {
                return; // Gracz nie ma otwartego GUI handlu
            }

            updateInventoryItems(inventory, session, player);
            updateControlButtons(inventory, session, player);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Błąd podczas odświeżania GUI handlu!");
            e.printStackTrace();
        }
    }

    private static void updateInventoryItems(Inventory inventory, TradeSession session, Player viewer) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();

        // Wyczyść sloty przedmiotów
        for (int slot : PLAYER1_SLOTS) {
            inventory.setItem(slot, null);
        }
        for (int slot : PLAYER2_SLOTS) {
            inventory.setItem(slot, null);
        }

        // Dodaj przedmioty gracza 1
        ItemStack[] player1Items = session.getPlayerItems(player1);
        for (int i = 0; i < Math.min(player1Items.length, PLAYER1_SLOTS.length); i++) {
            if (player1Items[i] != null) {
                inventory.setItem(PLAYER1_SLOTS[i], player1Items[i]);
            }
        }

        // Dodaj przedmioty gracza 2
        ItemStack[] player2Items = session.getPlayerItems(player2);
        for (int i = 0; i < Math.min(player2Items.length, PLAYER2_SLOTS.length); i++) {
            if (player2Items[i] != null) {
                inventory.setItem(PLAYER2_SLOTS[i], player2Items[i]);
            }
        }

        // Dodaj etykiety graczy
        ItemStack player1Label = createItem(Material.PLAYER_HEAD,
                ChatColor.BLUE + player1.getName(),
                Arrays.asList(ChatColor.GRAY + "Przedmioty gracza " + player1.getName()));
        ItemStack player2Label = createItem(Material.PLAYER_HEAD,
                ChatColor.BLUE + player2.getName(),
                Arrays.asList(ChatColor.GRAY + "Przedmioty gracza " + player2.getName()));

        inventory.setItem(37, player1Label);
        inventory.setItem(43, player2Label);
    }

    private static void updateControlButtons(Inventory inventory, TradeSession session, Player viewer) {
        Player otherPlayer = session.getOtherPlayer(viewer);

        // Przycisk gotowości dla tego gracza
        boolean isReady = session.isPlayerReady(viewer);
        ItemStack readyButton = createItem(
                isReady ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                isReady ? ChatColor.GREEN + "Gotowy!" : ChatColor.RED + "Nie gotowy",
                Arrays.asList(ChatColor.GRAY + "Kliknij aby zmienić status gotowości")
        );

        // Przycisk gotowości drugiego gracza (tylko do wyświetlania)
        boolean otherReady = session.isPlayerReady(otherPlayer);
        ItemStack otherReadyButton = createItem(
                otherReady ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                otherReady ? ChatColor.GREEN + otherPlayer.getName() + " jest gotowy"
                        : ChatColor.RED + otherPlayer.getName() + " nie jest gotowy",
                null
        );

        // Ustaw przyciski w zależności od tego, który gracz przegląda
        if (viewer.equals(session.getPlayer1())) {
            inventory.setItem(PLAYER1_READY_SLOT, readyButton);
            inventory.setItem(PLAYER2_READY_SLOT, otherReadyButton);
        } else {
            inventory.setItem(PLAYER2_READY_SLOT, readyButton);
            inventory.setItem(PLAYER1_READY_SLOT, otherReadyButton);
        }

        // Przycisk anulowania
        ItemStack cancelButton = createItem(Material.BARRIER,
                ChatColor.RED + "Anuluj handel",
                Arrays.asList(ChatColor.GRAY + "Kliknij aby anulować handel"));
        inventory.setItem(CANCEL_SLOT, cancelButton);

        // Przycisk akceptacji (tylko jeśli obaj gracze są gotowi)
        if (session.areBothPlayersReady()) {
            boolean isConfirmed = session.isPlayerConfirmed(viewer);
            ItemStack acceptButton = createItem(
                    isConfirmed ? Material.EMERALD_BLOCK : Material.GOLD_BLOCK,
                    isConfirmed ? ChatColor.GREEN + "Potwierdzono!" : ChatColor.YELLOW + "Potwierdź handel",
                    Arrays.asList(ChatColor.GRAY + "Kliknij aby potwierdzić handel")
            );
            inventory.setItem(ACCEPT_SLOT, acceptButton);
        } else {
            // Jeśli nie obaj gracze są gotowi, pokaż zablokowany przycisk
            ItemStack blockedButton = createItem(Material.GRAY_CONCRETE,
                    ChatColor.GRAY + "Czekaj na gotowość",
                    Arrays.asList(ChatColor.GRAY + "Obaj gracze muszą być gotowi"));
            inventory.setItem(ACCEPT_SLOT, blockedButton);
        }
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // Metody pomocnicze dla sprawdzania slotów
    public static boolean isPlayer1Slot(int slot) {
        for (int playerSlot : PLAYER1_SLOTS) {
            if (playerSlot == slot) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlayer2Slot(int slot) {
        for (int playerSlot : PLAYER2_SLOTS) {
            if (playerSlot == slot) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReadyButton(int slot) {
        return slot == PLAYER1_READY_SLOT || slot == PLAYER2_READY_SLOT;
    }

    public static boolean isAcceptButton(int slot) {
        return slot == ACCEPT_SLOT;
    }

    public static boolean isCancelButton(int slot) {
        return slot == CANCEL_SLOT;
    }

    public static int getTradeSlotIndex(int inventorySlot, boolean isPlayer1) {
        int[] slots = isPlayer1 ? PLAYER1_SLOTS : PLAYER2_SLOTS;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == inventorySlot) {
                return i;
            }
        }
        return -1;
    }
}
