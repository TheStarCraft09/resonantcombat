package com.richardnehmer.resonantcombat.common.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum AbilityKind implements StringRepresentable {
    SKILL("skill"), ULTIMATE("ultimate"), ECHO("echo");

    public static final Codec<AbilityKind> CODEC = StringRepresentable.fromEnum(AbilityKind::values);
    private final String name;

    AbilityKind(String name) { this.name = name; }

    @Override
    public String getSerializedName() { return name; }
}
