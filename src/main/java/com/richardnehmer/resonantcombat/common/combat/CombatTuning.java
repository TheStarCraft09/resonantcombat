package com.richardnehmer.resonantcombat.common.combat;

/** Balance constants from the design doc (sections 3.2-3.4). Becomes a ModConfigSpec in Phase 9. Tier index: 0=I, 1=II, 2=III. */
public final class CombatTuning {
    // ---- Heavy attack ----
    public static final int HEAVY_MIN_TICKS = 10;      // below this: normal Epic Fight basic attack
    public static final int HEAVY_II_TICKS = 24;
    public static final int HEAVY_III_TICKS = 40;      // auto-executes
    public static final int HEAVY_TIMEOUT_TICKS = 100; // lost release packet safety net
    public static final float[] HEAVY_DAMAGE = {1.45F, 1.85F, 2.30F};
    public static final float[] HEAVY_POSTURE = {1.8F, 2.6F, 3.5F};
    public static final float[] HEAVY_STAMINA = {20F, 28F, 35F};
    public static final int[] HEAVY_WINDUP_TICKS = {6, 8, 10};
    public static final int[] HEAVY_RECOVERY_TICKS = {14, 18, 24};
    public static final double[] HEAVY_RANGE = {3.2, 3.6, 4.0};
    public static final double CHARGE_SPEED_MULT = 0.35;
    public static final double HEAVY_CONE_COS = 0.5;   // +-60 degrees
    public static final int HEAVY_MAX_TARGETS = 5;

    // ---- Double jump / plunge ----
    public static final float DOUBLE_JUMP_STAMINA = 12F;
    public static final double DOUBLE_JUMP_VERTICAL = 0.42;
    public static final double DOUBLE_JUMP_FORWARD = 0.18;
    public static final int PLUNGE_WINDOW_TICKS = 30;
    public static final double PLUNGE_MIN_DISTANCE = 2.5;
    public static final double PLUNGE_APEX_VY = 0.1;
    public static final float PLUNGE_STAMINA = 25F;
    public static final double PLUNGE_SPEED = 1.1;
    public static final float PLUNGE_DIRECT_MULT = 2.0F;
    public static final float PLUNGE_AREA_MULT = 0.75F;
    public static final double PLUNGE_RADIUS = 2.5;
    public static final float PLUNGE_POSTURE_MULT = 3.0F;
    public static final int PLUNGE_RECOVERY_TICKS = 30;

    // ---- Posture ----
    public static final float POSTURE_PER_DAMAGE = 0.5F;   // basic hit posture = damage * this
    public static final float POSTURE_MIN_MAX = 20F;
    public static final float POSTURE_HEALTH_FACTOR = 1.0F;
    public static final int POSTURE_REGEN_DELAY_TICKS = 80;
    public static final float POSTURE_REGEN_PER_TICK = 0.5F;
    public static final int STAGGER_TICKS = 40;
    public static final float STAGGER_DAMAGE_MULT = 1.3F;

    public static final int MIN_INTENT_INTERVAL_TICKS = 2;

    private CombatTuning() {}
}