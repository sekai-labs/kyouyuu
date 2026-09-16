package com.sekailabs.kyouyuu.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LinkSessionManager {

    public enum SessionType {
        LINK,
        UNLINK
    }

    public record Session(
            UUID playerUuid,
            SessionType type,
            String targetChannelId,
            Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Duration sessionTimeout;

    public LinkSessionManager() {
        this(Duration.ofSeconds(30));
    }

    public LinkSessionManager(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void startLinkSession(UUID playerUuid, String channelId) {
        Instant expiresAt = Instant.now().plus(sessionTimeout);
        sessions.put(playerUuid, new Session(playerUuid, SessionType.LINK, channelId.toLowerCase().trim(), expiresAt));
    }

    public void startUnlinkSession(UUID playerUuid) {
        Instant expiresAt = Instant.now().plus(sessionTimeout);
        sessions.put(playerUuid, new Session(playerUuid, SessionType.UNLINK, null, expiresAt));
    }

    public Optional<Session> getSession(UUID playerUuid) {
        Session session = sessions.get(playerUuid);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired()) {
            sessions.remove(playerUuid);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void clearSession(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    public void pruneExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
