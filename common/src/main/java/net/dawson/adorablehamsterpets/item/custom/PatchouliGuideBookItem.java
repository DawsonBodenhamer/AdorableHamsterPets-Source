package net.dawson.adorablehamsterpets.item.custom;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.List;

public class PatchouliGuideBookItem extends Item {
    public PatchouliGuideBookItem(Settings settings) {
        super(settings);
    }

    /**
     * Called when the player right-clicks with this item.
     * This opens the Patchouli book screen for the player.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity serverPlayer) {
            PatchouliAPI.get().openBookGUI(serverPlayer, Identifier.of("adorablehamsterpets", "hamster_tips_guide_book"));
        }
        return TypedActionResult.success(stack);
    }

    /**
     * Appends the custom tooltip, including a context-aware check to prevent
     * duplicating the mod name when another mod (like Jade) would also add it.
     * This method is annotated with @Environment to be stripped from dedicated servers.
     */
    @Override
    @Environment(EnvType.CLIENT)
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        // --- 1. Add the primary hint text unconditionally ---
        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_guide_book.hint").formatted(Formatting.GRAY));

        // --- 2. Get Contextual Information ---
        boolean isJadeLoaded = Platform.isModLoaded("jade");
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;

        // --- 3. Determine screen context ---
        // A tooltip is needed anywhere Jade does NOT add its own tooltip.
        boolean needsToolTip = (currentScreen == null || currentScreen.getClass() == CreativeInventoryScreen.class);

        // --- 4. Add the mod name line if needed ---
        // Add line if EITHER Jade is not installed OR we are in a screen that needs a tooltip.
        if (!isJadeLoaded || needsToolTip) {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }

        super.appendTooltip(stack, context, tooltip, type);
    }
}