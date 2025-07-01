package pl.yourname.tradeplugin.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import pl.yourname.tradeplugin.TradePlugin;
import pl.yourname.tradeplugin.models.TradeHistoryEntry;

public class TradeHistoryGUI {

    private static final String HISTORY_TITLE = ChatColor.DARK_BLUE + "Historia handlu";
    private static final String DETAIL_TITLE = ChatColor.DARK_GREEN + "Szczegóły handlu";

    public static void openHistoryGUI(Player player, List<TradeHistoryEntry> history, TradePlugin plugin) {
        int size = Math.min(54, ((history.size() + 8) / 9) * 9); // Zaokrąglij w górę do wielokrotności 9
        if (size < 9) {
            size = 9;
        }

        Inventory inventory = Bukkit.createInventory(null, size, HISTORY_TITLE);

        for (int i = 0; i < Math.min(history.size(), size - 1); i++) {
            TradeHistoryEntry entry = history.get(i);
            ItemStack item = createHistoryItem(entry);
            inventory.setItem(i, item);
        }

        // Dodaj przycisk zamknięcia
        if (size > history.size()) {
            ItemStack closeButton = createItem(Material.BARRIER,
                    ChatColor.RED + "Zamknij",
                    Arrays.asList(ChatColor.GRAY + "Kliknij aby zamknąć"));
            inventory.setItem(size - 1, closeButton);
        }

        player.openInventory(inventory);
    }

    public static void openTradeDetailGUI(Player player, TradeHistoryEntry entry, TradePlugin plugin) {
        Inventory inventory = Bukkit.createInventory(null, 54, DETAIL_TITLE);

        // Górny środek - informacje o handlu
        ItemStack infoItem = createItem(Material.BOOK,
                ChatColor.GOLD + "Informacje o handlu",
                Arrays.asList(
                        ChatColor.GRAY + "Data: " + entry.getFormattedTimestamp(),
                        ChatColor.BLUE + "Gracz 1: " + entry.getPlayer1Name(),
                        ChatColor.RED + "Gracz 2: " + entry.getPlayer2Name(),
                        "",
                        ChatColor.YELLOW + "ID: " + entry.getEntryId().toString().substring(0, 8) + "..."
                ));
        inventory.setItem(4, infoItem);

        // Etykieta gracza 1 (lewa strona - niebieska)
        ItemStack player1Label = createItem(Material.BLUE_CONCRETE,
                ChatColor.BLUE + entry.getPlayer1Name() + " oddał:",
                Arrays.asList(ChatColor.GRAY + "Przedmioty po lewej stronie"));
        inventory.setItem(0, player1Label);

        // Etykieta gracza 2 (prawa strona - czerwona)
        ItemStack player2Label = createItem(Material.RED_CONCRETE,
                ChatColor.RED + entry.getPlayer2Name() + " oddał:",
                Arrays.asList(ChatColor.GRAY + "Przedmioty po prawej stronie"));
        inventory.setItem(8, player2Label);

        // Przedmioty gracza 1 (lewa strona - sloty 1-3, 9-12, 18-21, 27-30)
        ItemStack[] player1Items = entry.getPlayer1Items();
        int[] player1Slots = {1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
        for (int i = 0; i < Math.min(player1Items.length, player1Slots.length); i++) {
            if (player1Items[i] != null && !player1Items[i].getType().isAir()) {
                inventory.setItem(player1Slots[i], player1Items[i]);
            }
        }

        // Przedmioty gracza 2 (prawa strona - sloty 5-7, 14-17, 23-26, 32-35)
        ItemStack[] player2Items = entry.getPlayer2Items();
        int[] player2Slots = {5, 6, 7, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
        for (int i = 0; i < Math.min(player2Items.length, player2Slots.length); i++) {
            if (player2Items[i] != null && !player2Items[i].getType().isAir()) {
                inventory.setItem(player2Slots[i], player2Items[i]);
            }
        }

        // Separatory czerwono-niebieskie (środkowa kolumna)
        ItemStack blueSeparator = createItem(Material.BLUE_STAINED_GLASS_PANE,
                ChatColor.BLUE + "Separator", null);
        ItemStack redSeparator = createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Separator", null);

        // Naprzemienne kolory w środkowej kolumnie (slot 4 ma już info)
        inventory.setItem(13, blueSeparator);
        inventory.setItem(22, redSeparator);
        inventory.setItem(31, blueSeparator);
        inventory.setItem(40, redSeparator);

        // Etykiety graczy na dole
        ItemStack player1Bottom = createItem(Material.BLUE_WOOL,
                ChatColor.BLUE + entry.getPlayer1Name(),
                Arrays.asList(ChatColor.GRAY + "Przedmioty po lewej stronie"));
        inventory.setItem(36, player1Bottom);

        ItemStack player2Bottom = createItem(Material.RED_WOOL,
                ChatColor.RED + entry.getPlayer2Name(),
                Arrays.asList(ChatColor.GRAY + "Przedmioty po prawej stronie"));
        inventory.setItem(44, player2Bottom);

        // Przycisk powrotu
        ItemStack backButton = createItem(Material.ARROW,
                ChatColor.YELLOW + "Powrót do historii",
                Arrays.asList(ChatColor.GRAY + "Kliknij aby wrócić"));
        inventory.setItem(49, backButton);

        player.openInventory(inventory);
    }

    private static ItemStack createHistoryItem(TradeHistoryEntry entry) {
        Material material = Material.WRITABLE_BOOK;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Data: " + entry.getFormattedTimestamp());
        lore.add(ChatColor.BLUE + "Gracz 1: " + entry.getPlayer1Name());
        lore.add(ChatColor.GREEN + "Gracz 2: " + entry.getPlayer2Name());
        lore.add("");

        // Pokaż niektóre przedmioty jako podgląd
        ItemStack[] player1Items = entry.getPlayer1Items();
        ItemStack[] player2Items = entry.getPlayer2Items();

        int itemCount1 = 0, itemCount2 = 0;
        for (ItemStack item : player1Items) {
            if (item != null && !item.getType().isAir()) {
                itemCount1++;
            }
        }
        for (ItemStack item : player2Items) {
            if (item != null && !item.getType().isAir()) {
                itemCount2++;
            }
        }

        lore.add(ChatColor.BLUE + entry.getPlayer1Name() + ": " + itemCount1 + " przedmiotów");
        lore.add(ChatColor.RED + entry.getPlayer2Name() + ": " + itemCount2 + " przedmiotów");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Kliknij aby zobaczyć szczegóły");

        return createItem(material, ChatColor.GOLD + entry.getTradeDescription(), lore);
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

    public static boolean isHistoryGUI(Inventory inventory) {
        return inventory.getSize() <= 54
                && inventory.getType().name().equals("CHEST");
    }

    public static boolean isDetailGUI(Inventory inventory) {
        return inventory.getSize() == 54;
    }
}
