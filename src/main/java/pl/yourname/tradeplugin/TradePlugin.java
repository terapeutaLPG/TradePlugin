package pl.yourname.tradeplugin;

import org.bukkit.plugin.java.JavaPlugin;

import pl.yourname.tradeplugin.commands.TradeCommand;
import pl.yourname.tradeplugin.commands.TradeHistoryCommand;
import pl.yourname.tradeplugin.listeners.InventoryListener;
import pl.yourname.tradeplugin.listeners.PlayerListener;
import pl.yourname.tradeplugin.managers.TradeHistoryManager;
import pl.yourname.tradeplugin.managers.TradeManager;

public class TradePlugin extends JavaPlugin {

    private TradeManager tradeManager;
    private TradeHistoryManager historyManager;

    @Override
    public void onEnable() {
        // Inicjalizacja managerów
        this.tradeManager = new TradeManager();
        this.historyManager = new TradeHistoryManager(this);

        // Rejestracja komend
        TradeCommand tradeCommand = new TradeCommand(this);
        if (getCommand("wymiana") != null) {
            getCommand("wymiana").setExecutor(tradeCommand);
        }
        if (getCommand("wymianazaakceptuj") != null) {
            getCommand("wymianazaakceptuj").setExecutor(tradeCommand);
        }
        if (getCommand("wymianaodrzuc") != null) {
            getCommand("wymianaodrzuc").setExecutor(tradeCommand);
        }

        // Rejestracja komendy historii
        TradeHistoryCommand historyCommand = new TradeHistoryCommand(this);
        if (getCommand("wymianahist") != null) {
            getCommand("wymianahist").setExecutor(historyCommand);
        }

        // Rejestracja listenerów
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("TradePlugin został włączony!");
    }

    @Override
    public void onDisable() {
        // Zamknij wszystkie aktywne sesje handlu
        if (tradeManager != null) {
            tradeManager.cancelAllTrades();
        }

        getLogger().info("TradePlugin został wyłączony!");
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public TradeHistoryManager getHistoryManager() {
        return historyManager;
    }
}
