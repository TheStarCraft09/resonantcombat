package com.richardnehmer.resonantcombat.server;

import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.data.CombatCircuitDefinition;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Server-only weighted Circuit rolls restricted to the selected class (design doc 2.8). */
public final class CircuitAssignmentService {

    public static Optional<ResourceLocation> roll(RegistryAccess access, ResourceLocation classId, long seed, Set<ResourceLocation> exclude) {
        Registry<CombatCircuitDefinition> registry = ModRegistries.circuits(access);
        List<Map.Entry<ResourceLocation, Integer>> pool = collect(registry, classId, exclude);
        if (pool.isEmpty() && !exclude.isEmpty()) pool = collect(registry, classId, Set.of()); // only one Circuit in class
        if (pool.isEmpty()) return Optional.empty();

        int total = pool.stream().mapToInt(Map.Entry::getValue).sum();
        int roll = RandomSource.create(seed).nextInt(total);
        for (Map.Entry<ResourceLocation, Integer> e : pool) {
            roll -= e.getValue();
            if (roll < 0) return Optional.of(e.getKey());
        }
        return Optional.of(pool.get(pool.size() - 1).getKey()); // unreachable
    }

    private static List<Map.Entry<ResourceLocation, Integer>> collect(Registry<CombatCircuitDefinition> registry,
                                                                      ResourceLocation classId, Set<ResourceLocation> exclude) {
        List<Map.Entry<ResourceLocation, Integer>> pool = new ArrayList<>();
        registry.entrySet().forEach(e -> {
            ResourceLocation id = e.getKey().location();
            if (e.getValue().weaponClass().equals(classId) && !exclude.contains(id)) {
                pool.add(Map.entry(id, e.getValue().weight()));
            }
        });
        pool.sort(Map.Entry.comparingByKey(Comparator.naturalOrder())); // deterministic for a given seed
        return pool;
    }

    /** Writes class + Circuit to the profile. Does not sync; callers decide when. */
    public static void assign(ServerPlayer player, ResourceLocation classId, ResourceLocation circuitId, long seed) {
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        profile.setClassAndCircuit(classId, circuitId, seed, player.level().getGameTime());
    }

    /** Admin/respec: same class, new Circuit (different from the current one when possible). */
    public static boolean reroll(ServerPlayer player) {
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        if (profile.selectedClass().isEmpty()) return false;
        long seed = player.getRandom().nextLong();
        Set<ResourceLocation> exclude = profile.assignedCircuit().map(Set::of).orElse(Set.of());
        Optional<ResourceLocation> circuit = roll(player.level().registryAccess(), profile.selectedClass().get(), seed, exclude);
        if (circuit.isEmpty()) return false;
        assign(player, profile.selectedClass().get(), circuit.get(), seed);
        profile.markRespec(player.level().getGameTime());
        ModNetwork.syncProfile(player);
        return true;
    }

    private CircuitAssignmentService() {}
}
