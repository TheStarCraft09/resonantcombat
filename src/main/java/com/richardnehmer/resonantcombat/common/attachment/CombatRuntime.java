package com.richardnehmer.resonantcombat.common.attachment;

import com.richardnehmer.resonantcombat.common.combat.CombatActionState;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/** Transient server-side combat state: never saved, never copied on death. */
public class CombatRuntime {
    public CombatActionState state = CombatActionState.NEUTRAL;

    // heavy
    public long heavyChargeStartTick;
    public int heavyTier;
    public long heavyImpactTick = -1; // -1 = no pending own-sweep impact (also used for plunge landing recovery)
    public long recoveryEndTick;

    // air
    public boolean doubleJumpUsed;
    public long plungeWindowEndTick;
    public int airTicks;

    // server-side velocity estimate (blocks/tick) - the server does not own player movement
    public boolean hasLast;
    public double lastX, lastY, lastZ;
    public double velX, velY, velZ;

    /** True while our own strike() is dealing damage, so it does not feed the normal resource/posture/multiplier hooks. */
    public boolean damageGuard;

    /** When an Epic Fight animation drives the hits, these scale the damage/posture it deals until hitMultEnd. */
    public float hitDamageMult = 1.0F, hitPostureMult = 1.0F;
    public long hitMultEnd;

    // abilities
    public ResourceLocation activeAbility;
    public long abilityEndTick, abilityDashEnd, nextBurstTick;
    public int burstsLeft;
    public double abilityDirX, abilityDirZ;
    public boolean abilityAnimated;
    public final Set<Integer> abilityHit = new HashSet<>();
    public long skillCooldownEnd, echoCooldownEnd, echoRechargeEnd;

    // timed effects
    public long stanceEnd;
    public float stanceDamageMult = 1.0F, stanceTakenMult = 1.0F;
    public boolean stanceHeavyFree;
    public long counterEnd;
    public float counterDamageMult = 2.0F, counterPostureMult = 4.0F;
    public int markedEntityId = -1;
    public long markEnd;
    public float markBonus = 1.25F;

    /** Game time until which a class respec (Class Sigil) may be confirmed. 0 = none pending. */
    public long pendingClassRespecUntil;

    /** Per-intent rate limiting. */
    public final long[] lastIntentTick = new long[8];

    public void reset() {
        state = CombatActionState.NEUTRAL;
        heavyImpactTick = -1;
        recoveryEndTick = 0L;
        doubleJumpUsed = false;
        plungeWindowEndTick = 0L;
        damageGuard = false;
        hitMultEnd = 0L;
        activeAbility = null;
        abilityEndTick = 0L;
        burstsLeft = 0;
        stanceEnd = 0L;
        counterEnd = 0L;
        markEnd = 0L;
        markedEntityId = -1;
    }
}
