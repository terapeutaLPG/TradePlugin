package pl.yourname.tradeplugin.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class TradeHistoryEntry {

    private final UUID entryId;
    private final String player1Name;
    private final UUID player1UUID;
    private final String player2Name;
    private final UUID player2UUID;
    private final ItemStack[] player1Items;
    private final ItemStack[] player2Items;
    private final LocalDateTime timestamp;

    public TradeHistoryEntry(String player1Name, UUID player1UUID, String player2Name, UUID player2UUID,
            ItemStack[] player1Items, ItemStack[] player2Items) {
        this.entryId = UUID.randomUUID();
        this.player1Name = player1Name;
        this.player1UUID = player1UUID;
        this.player2Name = player2Name;
        this.player2UUID = player2UUID;
        this.player1Items = player1Items.clone();
        this.player2Items = player2Items.clone();
        this.timestamp = LocalDateTime.now();
    }

    // Konstruktor do wczytywania z bazy danych
    public TradeHistoryEntry(UUID entryId, String player1Name, UUID player1UUID, String player2Name, UUID player2UUID,
            ItemStack[] player1Items, ItemStack[] player2Items, LocalDateTime timestamp) {
        this.entryId = entryId;
        this.player1Name = player1Name;
        this.player1UUID = player1UUID;
        this.player2Name = player2Name;
        this.player2UUID = player2UUID;
        this.player1Items = player1Items;
        this.player2Items = player2Items;
        this.timestamp = timestamp;
    }

    public UUID getEntryId() {
        return entryId;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public UUID getPlayer1UUID() {
        return player1UUID;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public UUID getPlayer2UUID() {
        return player2UUID;
    }

    public ItemStack[] getPlayer1Items() {
        return player1Items.clone();
    }

    public ItemStack[] getPlayer2Items() {
        return player2Items.clone();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.toString().replace("T", " ").substring(0, 16);
    }

    public String getTradeDescription() {
        return player1Name + " ↔ " + player2Name;
    }
}
