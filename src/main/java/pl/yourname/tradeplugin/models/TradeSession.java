package pl.yourname.tradeplugin.models;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TradeSession {

    private final UUID sessionId;
    private final Player player1;
    private final Player player2;
    private final ItemStack[] player1Items;
    private final ItemStack[] player2Items;
    private boolean player1Ready;
    private boolean player2Ready;
    private boolean player1Confirmed;
    private boolean player2Confirmed;
    private final long createdTime;
    private int countdownSeconds; // -1 = brak countdown'u, 0-4 = sekundy pozostałe
    private boolean countdownActive;

    public TradeSession(Player player1, Player player2) {
        this.sessionId = UUID.randomUUID();
        this.player1 = player1;
        this.player2 = player2;
        this.player1Items = new ItemStack[27]; // 3 rzędy po 9 slotów
        this.player2Items = new ItemStack[27];
        this.player1Ready = false;
        this.player2Ready = false;
        this.player1Confirmed = false;
        this.player2Confirmed = false;
        this.createdTime = System.currentTimeMillis();
        this.countdownSeconds = -1; // Brak countdown'u na początku
        this.countdownActive = false;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Player getOtherPlayer(Player player) {
        return player.equals(player1) ? player2 : player1;
    }

    public ItemStack[] getPlayerItems(Player player) {
        return player.equals(player1) ? player1Items : player2Items;
    }

    public void setPlayerItem(Player player, int slot, ItemStack item) {
        if (player.equals(player1)) {
            player1Items[slot] = item;
        } else {
            player2Items[slot] = item;
        }
        // Resetuj gotowość przy zmianie itemów
        resetReadiness();
    }

    public boolean isPlayerReady(Player player) {
        return player.equals(player1) ? player1Ready : player2Ready;
    }

    public void setPlayerReady(Player player, boolean ready) {
        if (player.equals(player1)) {
            this.player1Ready = ready;
        } else {
            this.player2Ready = ready;
        }
    }

    public boolean areBothPlayersReady() {
        return player1Ready && player2Ready;
    }

    public boolean isPlayerConfirmed(Player player) {
        return player.equals(player1) ? player1Confirmed : player2Confirmed;
    }

    public void setPlayerConfirmed(Player player, boolean confirmed) {
        if (player.equals(player1)) {
            this.player1Confirmed = confirmed;
        } else {
            this.player2Confirmed = confirmed;
        }
    }

    public boolean areBothPlayersConfirmed() {
        return player1Confirmed && player2Confirmed;
    }

    public void resetReadiness() {
        this.player1Ready = false;
        this.player2Ready = false;
        this.player1Confirmed = false;
        this.player2Confirmed = false;
        this.countdownSeconds = -1; // Zresetuj countdown
        this.countdownActive = false;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public boolean hasPlayer(Player player) {
        return player.equals(player1) || player.equals(player2);
    }

    public void clearPlayerItems(Player player) {
        ItemStack[] items = getPlayerItems(player);
        Arrays.fill(items, null);
        resetReadiness();
    }

    // Metody do obsługi countdown'u
    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int seconds) {
        this.countdownSeconds = seconds;
    }

    public boolean isCountdownActive() {
        return countdownActive;
    }

    public void setCountdownActive(boolean active) {
        this.countdownActive = active;
        if (!active) {
            this.countdownSeconds = -1;
        }
    }

    public void startCountdown() {
        this.countdownActive = true;
        this.countdownSeconds = 4;
    }

    public void decrementCountdown() {
        if (countdownActive && countdownSeconds > 0) {
            countdownSeconds--;
        }
    }
}
