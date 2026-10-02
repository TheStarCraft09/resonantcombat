package com.richardnehmer.resonantcombat.common.attachment;

import com.richardnehmer.resonantcombat.common.combat.CombatActionState;

/** Transient server-side combat state: never saved, never copied on death. */
public class CombatRuntime {
    public CombatActionState state = CombatActionState.NEUTRAL;

    // heavy
    public long heavyChargeStartTick;
    public int heavyTier;
    public long heavyImpactTick = -1; // -1 = no pending impact (also used for plunge landing recovery)
    public long recoveryEndTick;

    // air
    public boolean doubleJumpUsed;
    public long plungeWindowEndTick;
    public int airTicks;

    // server-side velocity estimate (blocks/tick) - the server does not own player movement
    public boolean hasLast;
    public double lastX, lastY, lastZ;
    public double velX, velY, velZ;

    /** True while our own strike() is dealing damage, so it does not feed the normal resource/posture hooks. */
    public boolean damageGuard;

    /** Per-intent rate limiting. */
    public final long[] lastIntentTick = new long[8];

    public void reset() {
        state = CombatActionState.NEUTRAL;
        heavyImpactTick = -1;
        recoveryEndTick = 0L;
        doubleJumpUsed = false;
        plungeWindowEndTick = 0L;
        damageGuard = false;
    }
}
