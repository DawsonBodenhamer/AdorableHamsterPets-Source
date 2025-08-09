package net.dawson.adorablehamsterpets;

import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.ComposterBlock;

/**
 * Handles miscellaneous registrations that need to occur at specific lifecycle events.
 */
public class ModRegistries {

    /**
     * Registers items with the vanilla composter.
     * This is called directly during the common setup phase.
     */
    private static void registerCompostables() {
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.GREEN_BEANS.get(), 0.5f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.CUCUMBER.get(), 0.5f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.GREEN_BEAN_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.CUCUMBER_SEEDS.get(), 0.25f);
        ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.put(ModItems.SUNFLOWER_SEEDS.get(), 0.25f);
    }
}