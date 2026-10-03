package com.richardnehmer.resonantcombat.server;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.data.WeaponClassDefinition;
import com.richardnehmer.resonantcombat.common.network.ModNetwork;
import com.richardnehmer.resonantcombat.common.network.ModPayloads;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.Set;

/** First-join flow (design doc 2.1). Everything authoritative happens here. */
public final class OnboardingService {
    private static final ResourceLocation DEFAULT_ECHO = ResonantCombat.id("ravager_echo");

    public static void onJoin(ServerPlayer player) {
        ModNetwork.syncProfile(player);
        if (!player.getData(ModAttachments.PROFILE).isOnboardingComplete()) {
            PacketDistributor.sendToPlayer(player, new ModPayloads.OpenClassSelection(false));
        }
    }

    public static void handleSelection(ServerPlayer player, ResourceLocation classId) {
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);

        // Duplicate/forged packet after onboarding finished: ignore (prevents rerolls and kit duplication).
        if (profile.isOnboardingComplete()) {
            // either a confirmed class respec (Class Sigil) or a stray/forged packet; RespecService decides
            RespecService.Result result = RespecService.completeClassRespec(player, classId);
            if (result == RespecService.Result.NOT_PENDING) {
                ResonantCombat.LOGGER.debug("{} sent a class selection with no pending respec; ignored", player.getGameProfile().getName());
            } else if (result != RespecService.Result.OK) {
                RespecService.notify(player, result);
            }
            return;
        }

        WeaponClassDefinition def = ModRegistries.weaponClasses(player.level().registryAccess()).get(classId);
        if (def == null) {
            ResonantCombat.LOGGER.warn("{} selected unknown class {}", player.getGameProfile().getName(), classId);
            PacketDistributor.sendToPlayer(player, new ModPayloads.OpenClassSelection(false));
            return;
        }

        long seed = player.getRandom().nextLong();
        Optional<ResourceLocation> circuit = CircuitAssignmentService.roll(player.level().registryAccess(), classId, seed, Set.of());
        if (circuit.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.resonantcombat.no_circuits", ModRegistries.className(classId)));
            PacketDistributor.sendToPlayer(player, new ModPayloads.OpenClassSelection(false));
            return;
        }

        CircuitAssignmentService.assign(player, classId, circuit.get(), seed);
        if (!profile.isStarterKitGranted()) {
            StarterKitService.grant(player, def);
            profile.setStarterKitGranted(true);
        }
        profile.setOnboardingComplete(true);
        if (ModRegistries.abilities(player.level().registryAccess()).containsKey(DEFAULT_ECHO)) {
            profile.equipEcho(DEFAULT_ECHO, 1); // vertical slice: start with the Ravager Echo (acquisition system comes later)
        }

        ModNetwork.syncProfile(player);
        PacketDistributor.sendToPlayer(player, new ModPayloads.CircuitAssigned(circuit.get()));
        ResonantCombat.LOGGER.info("{} -> class {}, circuit {} (seed {})", player.getGameProfile().getName(), classId, circuit.get(), seed);
    }

    private OnboardingService() {}
}
