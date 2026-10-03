package com.richardnehmer.resonantcombat.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** Entity-type data map value: which Echo Imprint this mob can drop when killed by a player, and how likely (0-1). */
public record EchoDropEntry(ResourceLocation echo, float chance) {
    public static final Codec<EchoDropEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("echo").forGetter(EchoDropEntry::echo),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 0.05F).forGetter(EchoDropEntry::chance)
    ).apply(i, EchoDropEntry::new));
}
