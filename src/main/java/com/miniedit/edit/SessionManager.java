package com.miniedit.edit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

public final class SessionManager {
	private SessionManager() {}

	private static final Map<UUID, PlayerSession> SESSIONS = new ConcurrentHashMap<>();

	public static PlayerSession get(ServerPlayer player) {
		return SESSIONS.computeIfAbsent(player.getUUID(), id -> new PlayerSession());
	}

	public static void clearAll() {
		SESSIONS.clear();
	}
}
