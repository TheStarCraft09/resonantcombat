package com.richardnehmer.resonantcombat.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

/** One starter-kit line. The first item id that exists wins, so modded items can fall back to vanilla ones. */
public record StarterEntry(List<ResourceLocation> items, int count) {
    public static final Codec<StarterEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.listOf().fieldOf("items").forGetter(StarterEntry::items),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(StarterEntry::count)
    ).apply(i, StarterEntry::new));
}
