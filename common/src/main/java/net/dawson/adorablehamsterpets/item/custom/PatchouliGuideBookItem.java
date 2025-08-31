package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.config.Configs;
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
            PatchouliAPI.get().openBookGUI(serverPlayer, Identifier.of("adorablehamsterpets", "adh_guide"));
        }
        return TypedActionResult.success(stack);
    }

    /**
     * Appends the custom tooltip, respecting the config setting.
     */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (Configs.AHP.enableItemTooltips) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_guide_book.hint1").formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}