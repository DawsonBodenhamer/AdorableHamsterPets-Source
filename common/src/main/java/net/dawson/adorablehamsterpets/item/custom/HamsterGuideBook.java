package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the Hamster Guide Book item.
 * This class contains only common (client and server) logic, such as tooltips.
 * The client-side screen opening logic is handled separately in AdorableHamsterPetsClient.
 */
public class HamsterGuideBook extends Item {

    public HamsterGuideBook(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (Configs.AHP.enableItemTooltips) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_guide_book.hint1").formatted(Formatting.GRAY));
        }
        // Check for NBT data to determine if the book has been written
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("title", 8)) { // 8 is the NBT type for String
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public boolean isUsedOnRelease(ItemStack stack) {
        return true;
    }
}