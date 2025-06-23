package net.dawson.adorablehamsterpets.item;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.client.GuideBookUtil;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.dawson.adorablehamsterpets.sound.ModSounds;

import java.util.List;
import java.util.function.Supplier;

public class ModItems {

    // --- 1. Create a DeferredRegister for Items ---
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.ITEM);

    // --- 2. Change Item fields to RegistrySuppliers ---
    public static final RegistrySupplier<Item> HAMSTER_GUIDE_BOOK = registerItem("hamster_guide_book",
            () -> new Item(new Item.Settings().maxCount(1)) {
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

                @Override
                public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
                    ItemStack itemStack = user.getStackInHand(hand);
                    if (itemStack.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
                        // Defer client-side screen opening to a helper class
                        if (world.isClient) {
                            GuideBookUtil.openScreen(itemStack);
                        }
                        user.incrementStat(Stats.USED.getOrCreateStat(this));
                        return TypedActionResult.success(itemStack, world.isClient());
                    }
                    return TypedActionResult.pass(itemStack);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_SPAWN_EGG = registerItem("hamster_spawn_egg",
            () -> new SpawnEggItem(ModEntities.HAMSTER.get(), 0x9c631f, 0xffffff, new Item.Settings()));

    public static final RegistrySupplier<Item> GREEN_BEAN_SEEDS = registerItem("green_bean_seeds",
            () -> new AliasedBlockItem(ModBlocks.GREEN_BEANS_CROP.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_bean_seeds.hint1").formatted(Formatting.AQUA));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_bean_seeds.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> CUCUMBER_SEEDS = registerItem("cucumber_seeds",
            () -> new AliasedBlockItem(ModBlocks.CUCUMBER_CROP.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber_seeds.hint1").formatted(Formatting.AQUA));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber_seeds.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> SUNFLOWER_SEEDS = registerItem("sunflower_seeds",
            () -> new Item(new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sunflower_seeds.hint1").formatted(Formatting.YELLOW));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sunflower_seeds.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> CUCUMBER = registerItem("cucumber",
            () -> new Item(new Item.Settings().food(ModFoodComponents.CUCUMBER)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber.hint1").formatted(Formatting.YELLOW));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cucumber.hint2").formatted(Formatting.AQUA));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> SLICED_CUCUMBER = registerItem("sliced_cucumber",
            () -> new Item(new Item.Settings().food(ModFoodComponents.SLICED_CUCUMBER)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sliced_cucumber.hint1").formatted(Formatting.GREEN));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.sliced_cucumber.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> GREEN_BEANS = registerItem("green_beans",
            () -> new Item(new Item.Settings().food(ModFoodComponents.GREEN_BEANS)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_beans.hint1").formatted(Formatting.YELLOW));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.green_beans.hint2").formatted(Formatting.AQUA));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> STEAMED_GREEN_BEANS = registerItem("steamed_green_beans",
            () -> new Item(new Item.Settings().food(ModFoodComponents.STEAMED_GREEN_BEANS)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.steamed_green_beans.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.steamed_green_beans.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> HAMSTER_FOOD_MIX = registerItem("hamster_food_mix",
            () -> new Item(new Item.Settings().food(ModFoodComponents.HAMSTER_FOOD_MIX).maxCount(16)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_food_mix.hint1").formatted(Formatting.GREEN));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.hamster_food_mix.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> CHEESE = registerItem("cheese",
            () -> new Item(new Item.Settings().food(ModFoodComponents.CHEESE)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.hint1").formatted(Formatting.GOLD));
                        tooltip.add(Text.translatable("tooltip.adorablehamsterpets.cheese.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }

                @Override
                public SoundEvent getEatSound() {
                    return ModSounds.CHEESE_EAT_SOUND.get();
                }

                @Override
                public UseAction getUseAction(ItemStack stack) {
                    return UseAction.EAT;
                }

                @Override
                public int getMaxUseTime(ItemStack stack, LivingEntity user) {
                    return 20; // Custom eating time
                }
            });

    // --- Block Item Registrations ---
    public static final RegistrySupplier<Item> WILD_GREEN_BEAN_BUSH_ITEM = registerBlockItem("wild_green_bean_bush",
            () -> new BlockItem(ModBlocks.WILD_GREEN_BEAN_BUSH.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint1").formatted(Formatting.YELLOW));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_green_bean_bush.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> WILD_CUCUMBER_BUSH_ITEM = registerBlockItem("wild_cucumber_bush",
            () -> new BlockItem(ModBlocks.WILD_CUCUMBER_BUSH.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint1").formatted(Formatting.YELLOW));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.wild_cucumber_bush.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });

    public static final RegistrySupplier<Item> SUNFLOWER_BLOCK_ITEM = registerBlockItem("sunflower_block",
            () -> new BlockItem(ModBlocks.SUNFLOWER_BLOCK.get(), new Item.Settings()) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    if (Configs.AHP.enableItemTooltips) {
                        tooltip.add(Text.translatable("block.adorablehamsterpets.sunflower_block.hint1").formatted(Formatting.YELLOW));
                        tooltip.add(Text.translatable("block.adorablehamsterpets.sunflower_block.hint2").formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.literal("Adorable Hamster Pets").formatted(Formatting.BLUE, Formatting.ITALIC));
                    }
                    super.appendTooltip(stack, context, tooltip, type);
                }
            });
// --- End (New) Block Item Registrations ---

    // --- 3. Helper methods for registration ---
    private static RegistrySupplier<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(Identifier.of(AdorableHamsterPets.MOD_ID, name), itemSupplier);
    }

    private static RegistrySupplier<Item> registerBlockItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
    }

    // --- 4. Main registration call ---
    public static void register() {
        ITEMS.register();
    }
}