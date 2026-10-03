package com.richardnehmer.resonantcombat.common.item;

import com.richardnehmer.resonantcombat.ResonantCombat;
import com.richardnehmer.resonantcombat.common.data.AbilityKind;
import com.richardnehmer.resonantcombat.common.registry.ModRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ResonantCombat.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ResonantCombat.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ResonantCombat.MOD_ID);

    /** Which Echo an Echo Imprint stack contains (an ability id of kind ECHO). */
    public static final Supplier<DataComponentType<ResourceLocation>> ECHO = COMPONENTS.register("echo",
            () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    public static final DeferredItem<CircuitPrismItem> CIRCUIT_PRISM = ITEMS.registerItem("circuit_prism",
            CircuitPrismItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
    public static final DeferredItem<ClassSigilItem> CLASS_SIGIL = ITEMS.registerItem("class_sigil",
            ClassSigilItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<EchoImprintItem> ECHO_IMPRINT = ITEMS.registerItem("echo_imprint",
            EchoImprintItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.resonantcombat"))
            .icon(() -> new ItemStack(CIRCUIT_PRISM.get()))
            .displayItems((params, output) -> {
                output.accept(CIRCUIT_PRISM.get());
                output.accept(CLASS_SIGIL.get());
                // one imprint per Echo defined by loaded datapacks
                params.holders().lookup(ModRegistries.ABILITY).ifPresent(lookup -> lookup.listElements().forEach(holder -> {
                    if (holder.value().kind() == AbilityKind.ECHO) output.accept(imprint(holder.key().location()));
                }));
            })
            .build());

    public static ItemStack imprint(ResourceLocation echoId) {
        ItemStack stack = new ItemStack(ECHO_IMPRINT.get());
        stack.set(ECHO.get(), echoId);
        return stack;
    }

    private ModItems() {}
}
