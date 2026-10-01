package com.richardnehmer.resonantcombat.common.registry;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.attachment.ModAttachments;
import com.richardnehmer.resonantcombat.common.attachment.PlayerProfile;
import com.richardnehmer.resonantcombat.common.data.CombatCircuitDefinition;
import com.richardnehmer.resonantcombat.common.data.WeaponClassDefinition;
import com.richardnehmer.resonantcombat.common.data.WeaponClassEntry;
import com.richardnehmer.resonantcombat.integration.epicfight.EpicFightBridge;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/** Design doc 2.5 resolution order: data map (items + tags) -> Epic Fight category -> vanilla fallback -> unsupported. */
public final class WeaponClassResolver {

    public static Optional<ResourceLocation> resolve(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        Item item = stack.getItem();

        // 1+2. Explicit override via data map (covers item ids and #tags)
        WeaponClassEntry mapped = item.builtInRegistryHolder().getData(ModDataMaps.WEAPON_CLASS);
        if (mapped != null) return Optional.of(mapped.weaponClass());

        // 3. Epic Fight weapon category -> class (declared in each weapon_class definition)
        Optional<String> category = EpicFightBridge.weaponCategory(stack);
        if (category.isPresent()) {
            Registry<WeaponClassDefinition> classes = ModRegistries.weaponClasses(access);
            Optional<ResourceLocation> hit = classes.entrySet().stream()
                    .sorted(Comparator.comparingInt((Map.Entry<ResourceKey<WeaponClassDefinition>, WeaponClassDefinition> e) -> e.getValue().order())
                            .thenComparing(e -> e.getKey().location()))
                    .filter(e -> e.getValue().epicFightCategories().contains(category.get()))
                    .map(e -> e.getKey().location())
                    .findFirst();
            if (hit.isPresent()) return hit;
        }

        // 4. Vanilla fallback
        if (item instanceof SwordItem) return Optional.of(ResonantCombat.id("sword"));
        if (item instanceof AxeItem) return Optional.of(ResonantCombat.id("broadblade"));
        if (item instanceof TridentItem) return Optional.of(ResonantCombat.id("spear"));
        if (item instanceof BowItem || item instanceof CrossbowItem) return Optional.of(ResonantCombat.id("bow"));

        // 5. UNSUPPORTED
        return Optional.empty();
    }

    /**
     * True when the player's full Circuit should be active: has a Circuit, is in Epic Fight battle mode and holds a
     * weapon of the selected class (and, if the Circuit restricts Epic Fight types, one of those types).
     */
    public static boolean isCircuitActive(ServerPlayer player) {
        PlayerProfile profile = player.getData(ModAttachments.PROFILE);
        if (!profile.hasCircuit() || !EpicFightBridge.isBattleMode(player)) return false;

        RegistryAccess access = player.level().registryAccess();
        ItemStack held = player.getMainHandItem();
        Optional<ResourceLocation> heldClass = resolve(access, held);
        if (heldClass.isEmpty() || !heldClass.equals(profile.selectedClass())) return false;

        CombatCircuitDefinition circuit = ModRegistries.circuits(access).get(profile.assignedCircuit().orElseThrow());
        if (circuit != null && !circuit.allowedEpicFightTypes().isEmpty()) {
            Optional<String> category = EpicFightBridge.weaponCategory(held);
            if (category.isPresent() && !circuit.allowedEpicFightTypes().contains(category.get())) return false;
        }
        return true;
    }

    private WeaponClassResolver() {}
}
