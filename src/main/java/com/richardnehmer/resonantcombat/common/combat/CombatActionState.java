package com.richardnehmer.resonantcombat.common.combat;

/** Design doc 1.5. Only NEUTRAL is used in Phase 0-2; the rest are reserved for Phase 3+. */
public enum CombatActionState {
    NEUTRAL, BASIC_COMBO, CHARGING_HEAVY, HEAVY_RECOVERY, DOUBLE_JUMP_READY, DOUBLE_JUMP_ACTIVE,
    PLUNGE_ACTIVE, SKILL_ACTIVE, ULTIMATE_ACTIVE, ECHO_ACTIVE, STAGGERED, INCOMPATIBLE_WEAPON
}
