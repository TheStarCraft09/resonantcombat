package com.richardnehmer.resonantcombat.client;

import java.util.HashMap;
import java.util.Map;

/** Client-side mirror of server feedback, used only for HUD display. Never authoritative. */
public final class ClientActionState {
    public record PostureView(float posture, float max, boolean staggered, long updatedAt) {}

    public static boolean holdActive;
    public static int chargeTicks;
    public static long plungeWindowEnd;
    /** Game-time cooldown end per HUD slot: 0 = skill, 2 = echo. */
    public static final long[] cooldownEnd = new long[3];
    public static final int[] cooldownLength = new int[3];
    public static final Map<Integer, PostureView> POSTURE = new HashMap<>();

    public static void clear() {
        holdActive = false;
        chargeTicks = 0;
        plungeWindowEnd = 0L;
        java.util.Arrays.fill(cooldownEnd, 0L);
        POSTURE.clear();
    }

    private ClientActionState() {}
}
