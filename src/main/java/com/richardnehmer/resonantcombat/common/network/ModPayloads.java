package com.richardnehmer.resonantcombat.common.network;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** All payloads (design doc 5.2, Phase 1-2 subset). The client only ever sends INTENT. */
public final class ModPayloads {

    /** S -> C: open the first-join class selection screen. */
    public record OpenClassSelection() implements CustomPacketPayload {
        public static final Type<OpenClassSelection> TYPE = new Type<>(ResonantCombat.id("open_class_selection"));
        public static final StreamCodec<ByteBuf, OpenClassSelection> CODEC = StreamCodec.unit(new OpenClassSelection());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** C -> S: confirmed class choice. */
    public record ClassSelectionIntent(ResourceLocation classId) implements CustomPacketPayload {
        public static final Type<ClassSelectionIntent> TYPE = new Type<>(ResonantCombat.id("class_selection_intent"));
        public static final StreamCodec<ByteBuf, ClassSelectionIntent> CODEC =
                ResourceLocation.STREAM_CODEC.map(ClassSelectionIntent::new, ClassSelectionIntent::classId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S -> C: reveal the server-rolled Circuit. */
    public record CircuitAssigned(ResourceLocation circuitId) implements CustomPacketPayload {
        public static final Type<CircuitAssigned> TYPE = new Type<>(ResonantCombat.id("circuit_assigned"));
        public static final StreamCodec<ByteBuf, CircuitAssigned> CODEC =
                ResourceLocation.STREAM_CODEC.map(CircuitAssigned::new, CircuitAssigned::circuitId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S -> C: full profile snapshot (class, Circuit, resources, echo charges). */
    public record ProfileSync(PlayerProfile profile) implements CustomPacketPayload {
        public static final Type<ProfileSync> TYPE = new Type<>(ResonantCombat.id("profile_sync"));
        public static final StreamCodec<ByteBuf, ProfileSync> CODEC =
                PlayerProfile.STREAM_CODEC.map(ProfileSync::new, ProfileSync::profile);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private ModPayloads() {}
}
