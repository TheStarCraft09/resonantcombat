package com.richardnehmer.resonantcombat.common.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum CircuitTier implements StringRepresentable {
    STANDARD("standard"), ADVANCED("advanced"), RARE("rare");

    public static final Codec<CircuitTier> CODEC = StringRepresentable.fromEnum(CircuitTier::values);
    private final String name;

    CircuitTier(String name) { this.name = name; }

    @Override
    public String getSerializedName() { return name; }
}
