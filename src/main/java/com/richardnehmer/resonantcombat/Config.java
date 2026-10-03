package com.richardnehmer.resonantcombat;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config (serverconfig/resonantcombat-server.toml, per world). Combat balance stays in CombatTuning until Phase 9. */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue RESPEC_COOLDOWN_TICKS = BUILDER
            .comment("Minimum ticks between two respecs (Circuit rerolls and class respecs). 0 disables the cooldown.")
            .defineInRange("respecCooldownTicks", 6000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_RESPECS = BUILDER
            .comment("Maximum respecs per player (Circuit rerolls + class respecs). -1 = unlimited.")
            .defineInRange("maxRespecs", -1, -1, 10000);

    public static final ModConfigSpec.BooleanValue GRANT_STARTER_ON_CLASS_RESPEC = BUILDER
            .comment("Give the new class's starter weapons after a class respec. Default false: players keep what they own.")
            .define("grantStarterKitOnClassRespec", false);

    public static final ModConfigSpec.DoubleValue ECHO_DROP_MULTIPLIER = BUILDER
            .comment("Multiplier on every Echo Imprint drop chance (see data_maps/entity_type/echo_drop.json). 0 disables drops.")
            .defineInRange("echoDropChanceMultiplier", 1.0, 0.0, 100.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
