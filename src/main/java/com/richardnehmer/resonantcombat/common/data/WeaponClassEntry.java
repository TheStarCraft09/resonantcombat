package com.richardnehmer.resonantcombat.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** Item data-map value: explicit item -> class override (resolution step 1/2). */
public record WeaponClassEntry(ResourceLocation weaponClass) {
    public static final Codec<WeaponClassEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("weapon_class").forGetter(WeaponClassEntry::weaponClass)
    ).apply(i, WeaponClassEntry::new));
}
