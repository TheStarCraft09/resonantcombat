package com.richardnehmer.resonantcombat.common.registry;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.data.CombatCircuitDefinition;
import com.richardnehmer.resonantcombat.common.data.WeaponClassDefinition;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class ModRegistries {
    public static final ResourceKey<Registry<WeaponClassDefinition>> WEAPON_CLASS =
            ResourceKey.createRegistryKey(ResonantCombat.id("weapon_class"));
    public static final ResourceKey<Registry<CombatCircuitDefinition>> CIRCUIT =
            ResourceKey.createRegistryKey(ResonantCombat.id("circuit"));

    public static void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        // Same codec for disk and network: clients need names/starter previews/circuit info for the GUI.
        event.dataPackRegistry(WEAPON_CLASS, WeaponClassDefinition.CODEC, WeaponClassDefinition.CODEC);
        event.dataPackRegistry(CIRCUIT, CombatCircuitDefinition.CODEC, CombatCircuitDefinition.CODEC);
    }

    public static Registry<WeaponClassDefinition> weaponClasses(RegistryAccess access) {
        return access.registryOrThrow(WEAPON_CLASS);
    }

    public static Registry<CombatCircuitDefinition> circuits(RegistryAccess access) {
        return access.registryOrThrow(CIRCUIT);
    }

    // ---- lang-key helpers ----
    public static Component className(ResourceLocation id) { return Component.translatable(id.toLanguageKey("weapon_class")); }
    public static Component classDesc(ResourceLocation id) { return Component.translatable(id.toLanguageKey("weapon_class") + ".desc"); }
    public static Component circuitName(ResourceLocation id) { return Component.translatable(id.toLanguageKey("circuit")); }
    public static Component circuitDesc(ResourceLocation id) { return Component.translatable(id.toLanguageKey("circuit") + ".desc"); }

    private ModRegistries() {}
}
