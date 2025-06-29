package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (Configs.AHP.enableItemTooltips) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_guide_book.hint1").formatted(Formatting.GRAY));
        }
        WrittenBookContentComponent content = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (content != null && !content.title().raw().isEmpty()) {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public boolean isUsedOnRelease(ItemStack stack) {
        return true;
    }
}