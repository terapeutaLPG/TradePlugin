package pl.yourname.tradeplugin.managers;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import pl.yourname.tradeplugin.TradePlugin;
import pl.yourname.tradeplugin.models.TradeHistoryEntry;

public class TradeHistoryManager {

    private final TradePlugin plugin;
    private final File historyFile;
    private YamlConfiguration historyConfig;
    private final List<TradeHistoryEntry> historyCache;

    public TradeHistoryManager(TradePlugin plugin) {
        this.plugin = plugin;
        this.historyFile = new File(plugin.getDataFolder(), "trade_history.yml");
        this.historyCache = new ArrayList<>();
        loadHistory();

        // Uruchom zadanie czyszczenia co 24 godziny
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                this::cleanOldEntries,
                20L * 60 * 60 * 24, // 24 godziny w tickach
                20L * 60 * 60 * 24 // powtarzaj co 24 godziny
        );
    }

    public void addTradeEntry(String player1Name, UUID player1UUID, String player2Name, UUID player2UUID,
            ItemStack[] player1Items, ItemStack[] player2Items) {
        TradeHistoryEntry entry = new TradeHistoryEntry(player1Name, player1UUID, player2Name, player2UUID,
                player1Items, player2Items);
        historyCache.add(entry);
        saveEntryToFile(entry);

        plugin.getLogger().info("Zapisano handel: " + player1Name + " ↔ " + player2Name);
    }

    private void saveEntryToFile(TradeHistoryEntry entry) {
        try {
            if (!historyFile.exists()) {
                historyFile.getParentFile().mkdirs();
                historyFile.createNewFile();
                historyConfig = new YamlConfiguration();
            } else if (historyConfig == null) {
                historyConfig = YamlConfiguration.loadConfiguration(historyFile);
            }

            String path = "trades." + entry.getEntryId().toString();
            historyConfig.set(path + ".player1.name", entry.getPlayer1Name());
            historyConfig.set(path + ".player1.uuid", entry.getPlayer1UUID().toString());
            historyConfig.set(path + ".player2.name", entry.getPlayer2Name());
            historyConfig.set(path + ".player2.uuid", entry.getPlayer2UUID().toString());
            historyConfig.set(path + ".timestamp", entry.getTimestamp().toString());

            // Zapisz przedmioty jako base64
            historyConfig.set(path + ".player1.items", serializeItems(entry.getPlayer1Items()));
            historyConfig.set(path + ".player2.items", serializeItems(entry.getPlayer2Items()));

            historyConfig.save(historyFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas zapisywania historii handlu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadHistory() {
        try {
            if (!historyFile.exists()) {
                return;
            }

            historyConfig = YamlConfiguration.loadConfiguration(historyFile);
            historyCache.clear();

            if (historyConfig.getConfigurationSection("trades") == null) {
                return;
            }

            for (String key : historyConfig.getConfigurationSection("trades").getKeys(false)) {
                try {
                    String path = "trades." + key;
                    UUID entryId = UUID.fromString(key);
                    String player1Name = historyConfig.getString(path + ".player1.name");
                    UUID player1UUID = UUID.fromString(historyConfig.getString(path + ".player1.uuid"));
                    String player2Name = historyConfig.getString(path + ".player2.name");
                    UUID player2UUID = UUID.fromString(historyConfig.getString(path + ".player2.uuid"));
                    LocalDateTime timestamp = LocalDateTime.parse(historyConfig.getString(path + ".timestamp"));

                    ItemStack[] player1Items = deserializeItems(historyConfig.getString(path + ".player1.items"));
                    ItemStack[] player2Items = deserializeItems(historyConfig.getString(path + ".player2.items"));

                    TradeHistoryEntry entry = new TradeHistoryEntry(entryId, player1Name, player1UUID,
                            player2Name, player2UUID,
                            player1Items, player2Items, timestamp);
                    historyCache.add(entry);
                } catch (Exception e) {
                    plugin.getLogger().warning("Błąd podczas wczytywania wpisu historii: " + key);
                }
            }

            plugin.getLogger().info("Wczytano " + historyCache.size() + " wpisów historii handlu");
        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas wczytywania historii handlu: " + e.getMessage());
        }
    }

    private void cleanOldEntries() {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<TradeHistoryEntry> toRemove = new ArrayList<>();

        for (TradeHistoryEntry entry : historyCache) {
            if (entry.getTimestamp().isBefore(cutoffDate)) {
                toRemove.add(entry);
            }
        }

        if (!toRemove.isEmpty()) {
            historyCache.removeAll(toRemove);

            // Usuń z pliku
            for (TradeHistoryEntry entry : toRemove) {
                historyConfig.set("trades." + entry.getEntryId().toString(), null);
            }

            try {
                historyConfig.save(historyFile);
                plugin.getLogger().info("Usunięto " + toRemove.size() + " starych wpisów historii handlu");
            } catch (IOException e) {
                plugin.getLogger().severe("Błąd podczas usuwania starych wpisów: " + e.getMessage());
            }
        }
    }

    public List<TradeHistoryEntry> getHistory() {
        List<TradeHistoryEntry> sortedHistory = new ArrayList<>(historyCache);
        // Sortuj od najnowszych do najstarszych
        sortedHistory.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return sortedHistory;
    }

    public List<TradeHistoryEntry> getHistoryForPlayer(String playerName) {
        List<TradeHistoryEntry> result = new ArrayList<>();
        for (TradeHistoryEntry entry : historyCache) {
            if (entry.getPlayer1Name().equalsIgnoreCase(playerName)
                    || entry.getPlayer2Name().equalsIgnoreCase(playerName)) {
                result.add(entry);
            }
        }
        // Sortuj od najnowszych do najstarszych
        result.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return result;
    }

    public TradeHistoryEntry getEntry(UUID entryId) {
        return historyCache.stream()
                .filter(entry -> entry.getEntryId().equals(entryId))
                .findFirst()
                .orElse(null);
    }

    private String serializeItems(ItemStack[] items) {
        try {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            org.bukkit.util.io.BukkitObjectOutputStream dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(items);
            dataOutput.close();
            return java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas serializacji przedmiotów: " + e.getMessage());
            return "";
        }
    }

    private ItemStack[] deserializeItems(String data) {
        try {
            if (data == null || data.isEmpty()) {
                return new ItemStack[0];
            }

            byte[] bytes = java.util.Base64.getDecoder().decode(data);
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(bytes);
            org.bukkit.util.io.BukkitObjectInputStream dataInput = new org.bukkit.util.io.BukkitObjectInputStream(inputStream);
            ItemStack[] items = (ItemStack[]) dataInput.readObject();
            dataInput.close();
            return items;
        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas deserializacji przedmiotów: " + e.getMessage());
            return new ItemStack[0];
        }
    }
}
