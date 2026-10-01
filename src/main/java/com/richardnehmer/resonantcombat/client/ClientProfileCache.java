package com.richardnehmer.resonantcombat.client;

import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;

/** Latest server-synced profile for the local player. Read-only on the client; never trusted by the server. */
public final class ClientProfileCache {
    private static PlayerProfile current = new PlayerProfile();

    public static PlayerProfile get() { return current; }
    public static void set(PlayerProfile profile) { current = profile; }
    public static void clear() { current = new PlayerProfile(); }

    private ClientProfileCache() {}
}
