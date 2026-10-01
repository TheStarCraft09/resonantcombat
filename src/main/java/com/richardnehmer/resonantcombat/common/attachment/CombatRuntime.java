package com.richardnehmer.resonantcombat.common.attachment;

import com.richardnehmer.resonantcombat.common.combat.CombatActionState;

/** Transient: never saved, never copied on death. Reserved for Phase 3+ combat actions. */
public class CombatRuntime {
    public CombatActionState state = CombatActionState.NEUTRAL;
    public int heavyChargeTicks;
    public boolean doubleJumpUsed;
    public long plungeWindowEndTick;

    public void reset() {
        state = CombatActionState.NEUTRAL;
        heavyChargeTicks = 0;
        doubleJumpUsed = false;
        plungeWindowEndTick = 0L;
    }
}
