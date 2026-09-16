package com.sekailabs.kyouyuu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkSessionManagerTest {

    private LinkSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new LinkSessionManager(Duration.ofMillis(200));
    }

    @Test
    void testLinkSessionWorkflow() throws InterruptedException {
        UUID playerUuid = UUID.randomUUID();

        assertTrue(sessionManager.getSession(playerUuid).isEmpty());

        sessionManager.startLinkSession(playerUuid, "my-channel");
        Optional<LinkSessionManager.Session> sessionOpt = sessionManager.getSession(playerUuid);
        assertTrue(sessionOpt.isPresent());
        assertEquals(LinkSessionManager.SessionType.LINK, sessionOpt.get().type());
        assertEquals("my-channel", sessionOpt.get().targetChannelId());

        sessionManager.clearSession(playerUuid);
        assertTrue(sessionManager.getSession(playerUuid).isEmpty());

        sessionManager.startUnlinkSession(playerUuid);
        assertTrue(sessionManager.getSession(playerUuid).isPresent());
        Thread.sleep(250);
        assertTrue(sessionManager.getSession(playerUuid).isEmpty());
    }
}
