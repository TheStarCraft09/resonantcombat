package com.richardnehmer.resonantcombat.common.attachment;

import com.richardnehmer.resonantcombat.ResonantCombat;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ResonantCombat.MOD_ID);

    /** Persistent profile. copyOnDeath() so class/Circuit/resources survive respawn. */
    public static final Supplier<AttachmentType<PlayerProfile>> PROFILE = ATTACHMENTS.register("profile",
            () -> AttachmentType.builder(PlayerProfile::new)
                    .serialize(PlayerProfile.CODEC)
                    .copyOnDeath()
                    .build());

    /** Transient combat state (players). No serializer = not saved. */
    public static final Supplier<AttachmentType<CombatRuntime>> RUNTIME = ATTACHMENTS.register("runtime",
            () -> AttachmentType.builder(CombatRuntime::new).build());

    /** Transient posture state (target entities). */
    public static final Supplier<AttachmentType<PostureData>> POSTURE = ATTACHMENTS.register("posture",
            () -> AttachmentType.builder(PostureData::new).build());

    private ModAttachments() {}
}
