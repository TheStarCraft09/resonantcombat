package com.richardnehmer.resonantcombat.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * Datapack registry entry: data/&lt;ns&gt;/resonantcombat/ability/&lt;name&gt;.json - Resonance Skills, Ultimates and Echoes.
 * "type" selects a server-side executor (dash_strike, area_burst, stance, counter, mark, blink); "params" tune it.
 * If "animation" is set and Epic Fight can play it, the animation's own hit phases deal the damage and the executor
 * only scales damage/posture. Without an animation the executor performs its own server-side hit sweep.
 */
public record AbilityDefinition(AbilityKind kind, String type, float resonanceCost, float staminaCost, int cooldownTicks,
                                int maxCharges, int rechargeTicks, Optional<String> animation, Map<String, Double> params) {
    public static final Codec<AbilityDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            AbilityKind.CODEC.fieldOf("kind").forGetter(AbilityDefinition::kind),
            Codec.STRING.fieldOf("type").forGetter(AbilityDefinition::type),
            Codec.FLOAT.optionalFieldOf("resonance_cost", 0.0F).forGetter(AbilityDefinition::resonanceCost),
            Codec.FLOAT.optionalFieldOf("stamina_cost", 0.0F).forGetter(AbilityDefinition::staminaCost),
            Codec.INT.optionalFieldOf("cooldown_ticks", 0).forGetter(AbilityDefinition::cooldownTicks),
            Codec.INT.optionalFieldOf("max_charges", 1).forGetter(AbilityDefinition::maxCharges),
            Codec.INT.optionalFieldOf("recharge_ticks", 1200).forGetter(AbilityDefinition::rechargeTicks),
            Codec.STRING.optionalFieldOf("animation").forGetter(AbilityDefinition::animation),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("params", Map.of()).forGetter(AbilityDefinition::params)
    ).apply(i, AbilityDefinition::new));

    public double param(String key, double fallback) { return params.getOrDefault(key, fallback); }
}
