package com.richardnehmer.resonantcombat.client;

import java.util.HashMap;
import java.util.Map;

/** Client-side mirror of server feedback, used only for HUD display. Never authoritative. */
public final class ClientActionState {
    public record PostureView(float posture, float max, boolean staggered, long updatedAt) {}

    public static boolean holdActive;
    public static int chargeTicks;
    public static long plungeWindowEnd;
    public static final Map<Integer, PostureView> POSTURE = new HashMap<>();

    public static void clear() {
        holdActive = false;
        chargeTicks = 0;
        plungeWindowEnd = 0L;
        POSTURE.clear();
    }

    private ClientActionState() {}
}
