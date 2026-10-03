package com.richardnehmer.resonantcombat.common.network;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** All payloads (design doc 5.2). The client only ever sends INTENT; durations are measured by the server. */
public final class ModPayloads {

    // ================= onboarding / profile =================
    /** respec = true: opened by a Class Sigil, so the screen may be cancelled. */
    public record OpenClassSelection(boolean respec) implements CustomPacketPayload {
        public static final Type<OpenClassSelection> TYPE = new Type<>(ResonantCombat.id("open_class_selection"));
        public static final StreamCodec<ByteBuf, OpenClassSelection> CODEC =
                ByteBufCodecs.BOOL.map(OpenClassSelection::new, OpenClassSelection::respec);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClassSelectionIntent(ResourceLocation classId) implements CustomPacketPayload {
        public static final Type<ClassSelectionIntent> TYPE = new Type<>(ResonantCombat.id("class_selection_intent"));
        public static final StreamCodec<ByteBuf, ClassSelectionIntent> CODEC =
                ResourceLocation.STREAM_CODEC.map(ClassSelectionIntent::new, ClassSelectionIntent::classId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CircuitAssigned(ResourceLocation circuitId) implements CustomPacketPayload {
        public static final Type<CircuitAssigned> TYPE = new Type<>(ResonantCombat.id("circuit_assigned"));
        public static final StreamCodec<ByteBuf, CircuitAssigned> CODEC =
                ResourceLocation.STREAM_CODEC.map(CircuitAssigned::new, CircuitAssigned::circuitId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ProfileSync(PlayerProfile profile) implements CustomPacketPayload {
        public static final Type<ProfileSync> TYPE = new Type<>(ResonantCombat.id("profile_sync"));
        public static final StreamCodec<ByteBuf, ProfileSync> CODEC =
                PlayerProfile.STREAM_CODEC.map(ProfileSync::new, ProfileSync::profile);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ================= combat intents (client -> server) =================
    public record AttackHoldStart() implements CustomPacketPayload {
        public static final Type<AttackHoldStart> TYPE = new Type<>(ResonantCombat.id("attack_hold_start"));
        public static final StreamCodec<ByteBuf, AttackHoldStart> CODEC = StreamCodec.unit(new AttackHoldStart());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AttackRelease() implements CustomPacketPayload {
        public static final Type<AttackRelease> TYPE = new Type<>(ResonantCombat.id("attack_release"));
        public static final StreamCodec<ByteBuf, AttackRelease> CODEC = StreamCodec.unit(new AttackRelease());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DoubleJumpIntent() implements CustomPacketPayload {
        public static final Type<DoubleJumpIntent> TYPE = new Type<>(ResonantCombat.id("double_jump_intent"));
        public static final StreamCodec<ByteBuf, DoubleJumpIntent> CODEC = StreamCodec.unit(new DoubleJumpIntent());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Sent on an attack press while airborne; the server decides whether it becomes a Plunge. */
    public record BasicAttackIntent() implements CustomPacketPayload {
        public static final Type<BasicAttackIntent> TYPE = new Type<>(ResonantCombat.id("basic_attack_intent"));
        public static final StreamCodec<ByteBuf, BasicAttackIntent> CODEC = StreamCodec.unit(new BasicAttackIntent());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ================= server -> client feedback =================
    /** action: see ACTION_* ; value: action specific (double jump = plunge window length in ticks). */
    public record ActionResult(int action, boolean accepted, String reason, int value) implements CustomPacketPayload {
        public static final int ACTION_DOUBLE_JUMP = 0;
        public static final int ACTION_PLUNGE = 1;
        public static final int ACTION_HEAVY = 2;
        public static final int ACTION_ABILITY = 3;

        public static final Type<ActionResult> TYPE = new Type<>(ResonantCombat.id("action_result"));
        public static final StreamCodec<ByteBuf, ActionResult> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ActionResult::action,
                ByteBufCodecs.BOOL, ActionResult::accepted,
                ByteBufCodecs.STRING_UTF8, ActionResult::reason,
                ByteBufCodecs.VAR_INT, ActionResult::value,
                ActionResult::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PostureUpdate(int entityId, float posture, float max, boolean staggered) implements CustomPacketPayload {
        public static final Type<PostureUpdate> TYPE = new Type<>(ResonantCombat.id("posture_update"));
        public static final StreamCodec<ByteBuf, PostureUpdate> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PostureUpdate::entityId,
                ByteBufCodecs.FLOAT, PostureUpdate::posture,
                ByteBufCodecs.FLOAT, PostureUpdate::max,
                ByteBufCodecs.BOOL, PostureUpdate::staggered,
                PostureUpdate::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SkillIntent() implements CustomPacketPayload {
        public static final Type<SkillIntent> TYPE = new Type<>(ResonantCombat.id("skill_intent"));
        public static final StreamCodec<ByteBuf, SkillIntent> CODEC = StreamCodec.unit(new SkillIntent());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UltimateIntent() implements CustomPacketPayload {
        public static final Type<UltimateIntent> TYPE = new Type<>(ResonantCombat.id("ultimate_intent"));
        public static final StreamCodec<ByteBuf, UltimateIntent> CODEC = StreamCodec.unit(new UltimateIntent());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record EchoIntent() implements CustomPacketPayload {
        public static final Type<EchoIntent> TYPE = new Type<>(ResonantCombat.id("echo_intent"));
        public static final StreamCodec<ByteBuf, EchoIntent> CODEC = StreamCodec.unit(new EchoIntent());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: a cooldown just started. slot: 0 = skill, 2 = echo. */
    public record AbilityCooldown(int slot, int ticks) implements CustomPacketPayload {
        public static final Type<AbilityCooldown> TYPE = new Type<>(ResonantCombat.id("ability_cooldown"));
        public static final StreamCodec<ByteBuf, AbilityCooldown> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AbilityCooldown::slot,
                ByteBufCodecs.VAR_INT, AbilityCooldown::ticks,
                AbilityCooldown::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private ModPayloads() {}
}
