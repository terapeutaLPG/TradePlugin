package pl.yourname.tradeplugin.models;

import java.util.UUID;

import org.bukkit.entity.Player;

public class TradeRequest {

    private final UUID requestId;
    private final Player requester;
    private final Player target;
    private final long requestTime;
    private final long expireTime;

    public TradeRequest(Player requester, Player target) {
        this.requestId = UUID.randomUUID();
        this.requester = requester;
        this.target = target;
        this.requestTime = System.currentTimeMillis();
        this.expireTime = requestTime + (60 * 1000); // 60 sekund na odpowiedź
    }

    public UUID getRequestId() {
        return requestId;
    }

    public Player getRequester() {
        return requester;
    }

    public Player getTarget() {
        return target;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public long getTimeLeft() {
        return Math.max(0, expireTime - System.currentTimeMillis());
    }

    public boolean hasPlayer(Player player) {
        return player.equals(requester) || player.equals(target);
    }
}
