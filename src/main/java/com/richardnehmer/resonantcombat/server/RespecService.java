package com.richardnehmer.resonantcombat.server;

import com.richardnehmer.resonantcombat.Config;
import com.richardnehmer.resonantcombat.common.attachment.CombatRuntime;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.data.WeaponClassDefinition;
import com.richardnehmer.resonantcombat.common.item.ModItems;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Server-authoritative respecs (design doc 5.7). Limits and cooldowns come from {@link Config}. */
public final class RespecService {
    /** A class respec must be confirmed within this many ticks of using the sigil. */
    private static final int PENDING_TICKS = 1200;

    public enum Result { OK, NO_CIRCUIT, COOLDOWN, LIMIT, FAILED, SAME_CLASS, NOT_PENDING, NO_ITEM }

    public static Result checkAllowed(ServerPlayer player) {
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        if (!profile.hasCircuit()) return Result.NO_CIRCUIT;
        int max = Config.MAX_RESPECS.get();
        if (max >= 0 && profile.respecCount() >= max) return Result.LIMIT;
        long now = player.level().getGameTime();
        if (profile.respecCount() > 0 && now - profile.lastRespecTick() < Config.RESPEC_COOLDOWN_TICKS.get()) return Result.COOLDOWN;
        return Result.OK;
    }

    /** Circuit Reroll: same class, new Circuit. The caller consumes the Prism only when this returns OK. */
    public static Result reroll(ServerPlayer player) {
        Result allowed = checkAllowed(player);
        if (allowed != Result.OK) return allowed;
        if (!CircuitAssignmentService.reroll(player)) return Result.FAILED; // also marks the respec and syncs
        PacketDistributor.sendToPlayer(player, new ModPayloads.CircuitAssigned(
                player.getData(ModAttachments.PROFILE).assignedCircuit().orElseThrow()));
        return Result.OK;
    }

    /** Step 1 of a class respec: open the selection screen in respec mode. Nothing is consumed yet. */
    public static Result beginClassRespec(ServerPlayer player) {
        Result allowed = checkAllowed(player);
        if (allowed != Result.OK) return allowed;
        player.getData(ModAttachments.RUNTIME).pendingClassRespecUntil = player.level().getGameTime() + PENDING_TICKS;
        PacketDistributor.sendToPlayer(player, new ModPayloads.OpenClassSelection(true));
        return Result.OK;
    }

    /** Step 2: the player confirmed a class. Validates everything again; the sigil is consumed only on success. */
    public static Result completeClassRespec(ServerPlayer player, ResourceLocation classId) {
        CombatRuntime rt = player.getData(ModAttachments.RUNTIME);
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        long now = player.level().getGameTime();
        if (now > rt.pendingClassRespecUntil) return Result.NOT_PENDING;

        Result allowed = checkAllowed(player);
        if (allowed != Result.OK) return allowed;
        if (profile.selectedClass().equals(Optional.of(classId))) return Result.SAME_CLASS;

        WeaponClassDefinition def = ModRegistries.weaponClasses(player.level().registryAccess()).get(classId);
        if (def == null) return Result.FAILED;
        long seed = player.getRandom().nextLong();
        Optional<ResourceLocation> circuit = CircuitAssignmentService.roll(player.level().registryAccess(), classId, seed, Set.of());
        if (circuit.isEmpty()) return Result.FAILED; // checked before consuming anything

        if (!player.getAbilities().instabuild
                && player.getInventory().clearOrCountMatchingItems(s -> s.is(ModItems.CLASS_SIGIL.get()), 1, player.inventoryMenu.getCraftSlots()) < 1) {
            return Result.NO_ITEM;
        }

        rt.pendingClassRespecUntil = 0L;
        CircuitAssignmentService.assign(player, classId, circuit.get(), seed);
        profile.markRespec(now);
        if (Config.GRANT_STARTER_ON_CLASS_RESPEC.get()) StarterKitService.grant(player, def);

        ModNetwork.syncProfile(player);
        PacketDistributor.sendToPlayer(player, new ModPayloads.CircuitAssigned(circuit.get()));
        return Result.OK;
    }

    public static void notify(ServerPlayer player, Result result) {
        player.displayClientMessage(Component.translatable("message.resonantcombat.respec." + result.name().toLowerCase(Locale.ROOT)), true);
    }

    private RespecService() {}
}
