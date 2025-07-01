package pl.yourname.tradeplugin.managers;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import pl.yourname.tradeplugin.models.TradeRequest;
import pl.yourname.tradeplugin.models.TradeSession;

public class TradeManager {

    private final Map<UUID, TradeRequest> pendingRequests;
    private final Map<UUID, TradeSession> activeSessions;
    private final Map<UUID, TradeSession> playerSessions; // UUID gracza -> sesja

    public TradeManager() {
        this.pendingRequests = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.playerSessions = new ConcurrentHashMap<>();
    }

    public TradeRequest createTradeRequest(Player requester, Player target) {
        // Sprawdź czy gracze nie są już w handlu
        if (isPlayerInTrade(requester) || isPlayerInTrade(target)) {
            return null;
        }

        // Sprawdź czy nie ma już pending request między tymi graczami
        if (hasPendingRequest(requester, target)) {
            return null;
        }

        TradeRequest request = new TradeRequest(requester, target);
        pendingRequests.put(request.getRequestId(), request);
        return request;
    }

    public boolean acceptTradeRequest(Player player) {
        TradeRequest request = getPendingRequestForTarget(player);
        if (request == null || request.isExpired()) {
            return false;
        }

        // Usuń request z pending
        pendingRequests.remove(request.getRequestId());

        // Utwórz sesję handlu
        TradeSession session = new TradeSession(request.getRequester(), request.getTarget());
        activeSessions.put(session.getSessionId(), session);
        playerSessions.put(request.getRequester().getUniqueId(), session);
        playerSessions.put(request.getTarget().getUniqueId(), session);

        return true;
    }

    public boolean denyTradeRequest(Player player) {
        TradeRequest request = getPendingRequestForTarget(player);
        if (request == null) {
            return false;
        }

        pendingRequests.remove(request.getRequestId());
        return true;
    }

    public TradeSession getPlayerTradeSession(Player player) {
        return playerSessions.get(player.getUniqueId());
    }

    public boolean isPlayerInTrade(Player player) {
        return playerSessions.containsKey(player.getUniqueId());
    }

    public void cancelTrade(TradeSession session) {
        activeSessions.remove(session.getSessionId());
        playerSessions.remove(session.getPlayer1().getUniqueId());
        playerSessions.remove(session.getPlayer2().getUniqueId());
    }

    public void cancelAllTrades() {
        activeSessions.clear();
        playerSessions.clear();
        pendingRequests.clear();
    }

    public TradeRequest getPendingRequestForTarget(Player target) {
        return pendingRequests.values().stream()
                .filter(request -> request.getTarget().equals(target) && !request.isExpired())
                .findFirst()
                .orElse(null);
    }

    public TradeRequest getPendingRequestByRequester(Player requester) {
        return pendingRequests.values().stream()
                .filter(request -> request.getRequester().equals(requester) && !request.isExpired())
                .findFirst()
                .orElse(null);
    }

    private boolean hasPendingRequest(Player requester, Player target) {
        return pendingRequests.values().stream()
                .anyMatch(request
                        -> (request.getRequester().equals(requester) && request.getTarget().equals(target))
                || (request.getRequester().equals(target) && request.getTarget().equals(requester))
                );
    }

    public void cleanupExpiredRequests() {
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public Collection<TradeSession> getActiveSessions() {
        return activeSessions.values();
    }

    public Collection<TradeRequest> getPendingRequests() {
        return pendingRequests.values();
    }
}
