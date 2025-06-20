package net.dawson.adorablehamsterpets;

import dev.architectury.event.events.common.LifecycleEvent;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.ComposterBlock;

/**
 * Handles miscellaneous registrations that need to occur at specific lifecycle events.
 */
public class ModRegistries {

    public static void initialize() {
        // Register a listener to the SETUP event to add compostables.
        // This ensures it runs after items are registered but before the world loads.
        LifecycleEvent.SETUP.register(ModRegistries::registerCompostables);
    }

    /**
     * Registers items with the vanilla composter.
     */
    private static void registerCompostables() {
        // CORRECTED: Use the correct field name from the ComposterBlock source
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.GREEN_BEANS.get(), 0.5f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.CUCUMBER.get(), 0.5f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.GREEN_BEAN_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.CUCUMBER_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.SUNFLOWER_SEEDS.get(), 0.25f);
    }
}