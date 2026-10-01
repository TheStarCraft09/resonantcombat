package com.richardnehmer.resonantcombat.common.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Persistent per-player data (design doc 2.3). Transient state (action state, heavy charge, double jump)
 * lives in {@link CombatRuntime} and is intentionally NOT serialized.
 * Mutable on purpose: mutate on the server, then call ModNetwork.syncProfile(player).
 */
public class PlayerProfile {
    public static final float MAX_RESOURCE = 100.0F;

    public static final Codec<PlayerProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("selected_class").forGetter(p -> Optional.ofNullable(p.selectedClass)),
            ResourceLocation.CODEC.optionalFieldOf("assigned_circuit").forGetter(p -> Optional.ofNullable(p.assignedCircuit)),
            Codec.LONG.optionalFieldOf("circuit_roll_seed", 0L).forGetter(p -> p.circuitRollSeed),
            Codec.LONG.optionalFieldOf("circuit_assigned_at", 0L).forGetter(p -> p.circuitAssignedAt),
            Codec.BOOL.optionalFieldOf("onboarding_complete", false).forGetter(p -> p.onboardingComplete),
            Codec.BOOL.optionalFieldOf("starter_kit_granted", false).forGetter(p -> p.starterKitGranted),
            Codec.INT.optionalFieldOf("respec_count", 0).forGetter(p -> p.respecCount),
            Codec.LONG.optionalFieldOf("last_respec_tick", 0L).forGetter(p -> p.lastRespecTick),
            Codec.FLOAT.optionalFieldOf("resonance", 0.0F).forGetter(p -> p.resonance),
            Codec.FLOAT.optionalFieldOf("liberation_energy", 0.0F).forGetter(p -> p.liberationEnergy),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("echo_charges", Map.of()).forGetter(p -> p.echoCharges)
    ).apply(i, PlayerProfile::new));

    public static final StreamCodec<ByteBuf, PlayerProfile> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private ResourceLocation selectedClass;
    private ResourceLocation assignedCircuit;
    private long circuitRollSeed;
    private long circuitAssignedAt;
    private boolean onboardingComplete;
    private boolean starterKitGranted;
    private int respecCount;
    private long lastRespecTick;
    private float resonance;
    private float liberationEnergy;
    private final Map<ResourceLocation, Integer> echoCharges = new HashMap<>();

    public PlayerProfile() {}

    private PlayerProfile(Optional<ResourceLocation> selectedClass, Optional<ResourceLocation> assignedCircuit,
                          long seed, long assignedAt, boolean onboardingComplete, boolean starterKitGranted,
                          int respecCount, long lastRespecTick, float resonance, float liberation,
                          Map<ResourceLocation, Integer> echoCharges) {
        this.selectedClass = selectedClass.orElse(null);
        this.assignedCircuit = assignedCircuit.orElse(null);
        this.circuitRollSeed = seed;
        this.circuitAssignedAt = assignedAt;
        this.onboardingComplete = onboardingComplete;
        this.starterKitGranted = starterKitGranted;
        this.respecCount = respecCount;
        this.lastRespecTick = lastRespecTick;
        this.resonance = resonance;
        this.liberationEnergy = liberation;
        this.echoCharges.putAll(echoCharges);
    }

    public Optional<ResourceLocation> selectedClass() { return Optional.ofNullable(selectedClass); }
    public Optional<ResourceLocation> assignedCircuit() { return Optional.ofNullable(assignedCircuit); }
    public boolean hasCircuit() { return selectedClass != null && assignedCircuit != null; }

    public void setClassAndCircuit(ResourceLocation cls, ResourceLocation circuit, long seed, long tick) {
        this.selectedClass = cls;
        this.assignedCircuit = circuit;
        this.circuitRollSeed = seed;
        this.circuitAssignedAt = tick;
    }

    public void clearClassAndCircuit() {
        this.selectedClass = null;
        this.assignedCircuit = null;
        this.circuitRollSeed = 0L;
        this.circuitAssignedAt = 0L;
    }

    public long circuitRollSeed() { return circuitRollSeed; }
    public long circuitAssignedAt() { return circuitAssignedAt; }
    public boolean isOnboardingComplete() { return onboardingComplete; }
    public void setOnboardingComplete(boolean v) { this.onboardingComplete = v; }
    public boolean isStarterKitGranted() { return starterKitGranted; }
    public void setStarterKitGranted(boolean v) { this.starterKitGranted = v; }
    public int respecCount() { return respecCount; }
    public long lastRespecTick() { return lastRespecTick; }
    public void markRespec(long tick) { this.respecCount++; this.lastRespecTick = tick; }

    public float resonance() { return resonance; }
    public void setResonance(float v) { this.resonance = clamp(v); }
    public float liberationEnergy() { return liberationEnergy; }
    public void setLiberationEnergy(float v) { this.liberationEnergy = clamp(v); }
    public Map<ResourceLocation, Integer> echoCharges() { return echoCharges; }

    private static float clamp(float v) { return Math.max(0.0F, Math.min(MAX_RESOURCE, v)); }
}
