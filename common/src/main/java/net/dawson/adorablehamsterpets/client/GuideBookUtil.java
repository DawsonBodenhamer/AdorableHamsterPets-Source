package net.dawson.adorablehamsterpets.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.item.ItemStack;

/**
 * Client-side utility for handling actions related to the Hamster Guide Book.
 * This class is annotated to ensure it is only loaded on the physical client,
 * preventing server crashes.
 */
@Environment(EnvType.CLIENT)
public class GuideBookUtil {

    /**
     * Opens the book GUI for the given ItemStack.
     * This method should only be called from the client thread.
     * @param stack The ItemStack of the written book.
     */
    public static void openScreen(ItemStack stack) {
        BookScreen.Contents contents = BookScreen.Contents.create(stack);
        if (contents != null) {
            MinecraftClient.getInstance().setScreen(new BookScreen(contents));
        }
    }
}