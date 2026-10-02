package com.richardnehmer.resonantcombat.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Datapack registry entry: data/&lt;ns&gt;/resonantcombat/weapon_class/&lt;name&gt;.json
 * Display name / description come from lang keys: weapon_class.&lt;ns&gt;.&lt;name&gt; and .desc
 */
public record WeaponClassDefinition(ResourceLocation icon, int order, List<String> epicFightCategories,
                                    List<StarterEntry> starterKit, List<String> heavyAnimations) {
    public static final Codec<WeaponClassDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("icon").forGetter(WeaponClassDefinition::icon),
            Codec.INT.optionalFieldOf("order", 0).forGetter(WeaponClassDefinition::order),
            Codec.STRING.listOf().optionalFieldOf("epicfight_categories", List.of()).forGetter(WeaponClassDefinition::epicFightCategories),
            StarterEntry.CODEC.listOf().optionalFieldOf("starter_kit", List.of()).forGetter(WeaponClassDefinition::starterKit),
            Codec.STRING.listOf().optionalFieldOf("heavy_animations", List.of()).forGetter(WeaponClassDefinition::heavyAnimations)
    ).apply(i, WeaponClassDefinition::new));
}
