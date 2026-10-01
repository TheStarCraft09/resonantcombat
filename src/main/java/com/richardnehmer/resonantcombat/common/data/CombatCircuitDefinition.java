package com.richardnehmer.resonantcombat.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

/**
 * Datapack registry entry: data/&lt;ns&gt;/resonantcombat/circuit/&lt;name&gt;.json (design doc 5.4).
 * The profile/skill/ultimate ids are plain references until their registries exist (Phase 3+).
 * weight is per-Circuit (resolves the 60/30/10 tier-vs-Circuit ambiguity: tiers are just default weights).
 */
public record CombatCircuitDefinition(ResourceLocation weaponClass, int weight, CircuitTier tier, List<String> tags,
                                      ResourceLocation basicCombo, ResourceLocation heavyProfile,
                                      ResourceLocation plungeProfile, ResourceLocation resonanceSkill,
                                      ResourceLocation ultimate, List<String> allowedEpicFightTypes) {
    public static final Codec<CombatCircuitDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("weapon_class").forGetter(CombatCircuitDefinition::weaponClass),
            ExtraCodecs.POSITIVE_INT.fieldOf("weight").forGetter(CombatCircuitDefinition::weight),
            CircuitTier.CODEC.fieldOf("tier").forGetter(CombatCircuitDefinition::tier),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(CombatCircuitDefinition::tags),
            ResourceLocation.CODEC.fieldOf("basic_combo").forGetter(CombatCircuitDefinition::basicCombo),
            ResourceLocation.CODEC.fieldOf("heavy_profile").forGetter(CombatCircuitDefinition::heavyProfile),
            ResourceLocation.CODEC.fieldOf("plunge_profile").forGetter(CombatCircuitDefinition::plungeProfile),
            ResourceLocation.CODEC.fieldOf("resonance_skill").forGetter(CombatCircuitDefinition::resonanceSkill),
            ResourceLocation.CODEC.fieldOf("ultimate").forGetter(CombatCircuitDefinition::ultimate),
            Codec.STRING.listOf().optionalFieldOf("allowed_epicfight_types", List.of()).forGetter(CombatCircuitDefinition::allowedEpicFightTypes)
    ).apply(i, CombatCircuitDefinition::new));
}
