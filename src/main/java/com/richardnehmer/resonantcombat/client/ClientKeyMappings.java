package com.richardnehmer.resonantcombat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Defaults from the design doc (R / V / G). NOTE: Epic Fight's optional "toggle mode" key also defaults to R -
 * check for conflicts in-game. Skill/Ultimate/Echo are registered now but wired to actions in Phase 5.
 * DOUBLE_JUMP is an optional extra binding; by default the double jump uses Epic Fight's own dodge key (found by name).
 */
public final class ClientKeyMappings {
    public static final String CATEGORY = "key.categories.resonantcombat";

    public static final KeyMapping SKILL = key("skill", GLFW.GLFW_KEY_R);
    public static final KeyMapping ULTIMATE = key("ultimate", GLFW.GLFW_KEY_V);
    public static final KeyMapping ECHO = key("echo", GLFW.GLFW_KEY_G);
    public static final KeyMapping INSPECT = key("inspect", GLFW.GLFW_KEY_K);
    public static final KeyMapping DOUBLE_JUMP = key("double_jump", GLFW.GLFW_KEY_UNKNOWN);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.resonantcombat." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    private ClientKeyMappings() {}
}
