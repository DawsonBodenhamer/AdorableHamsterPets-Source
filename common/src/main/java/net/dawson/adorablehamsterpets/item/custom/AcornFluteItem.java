package net.dawson.adorablehamsterpets.item.custom;

import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.flute.AcornFluteVariant;
import net.dawson.adorablehamsterpets.flute.FlutePerformanceManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Cosmetic Acorn Flute variant that starts one server-authoritative performance.
 */
public final class AcornFluteItem extends Item {

    private final AcornFluteVariant variant;

    public AcornFluteItem(Settings settings, AcornFluteVariant variant) {
        super(settings);
        this.variant = variant;
    }

    public AcornFluteVariant variant() {
        return this.variant;
    }

    public AcornFluteVariant getVariant() {
        return this.variant;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hand != Hand.MAIN_HAND) {
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            FlutePerformanceManager.startPerformance(player, stack, this.variant);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            @NotNull TooltipContext context,
            @NotNull List<Text> tooltip,
            @NotNull TooltipType type) {
        if (Configs.AHP_UI.enableItemTooltips) {
            tooltip.add(Text.translatable("tooltip.adorablehamsterpets.acorn_flute.common")
                    .formatted(Formatting.GOLD));
            tooltip.add(Text.translatable(
                            "tooltip.adorablehamsterpets.acorn_flute_"
                                    + this.variant.name().toLowerCase(Locale.ROOT)
                                    + ".variant")
                    .formatted(Formatting.GRAY));
        } else if (!Platform.isModLoaded("emi")) {
            tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}
