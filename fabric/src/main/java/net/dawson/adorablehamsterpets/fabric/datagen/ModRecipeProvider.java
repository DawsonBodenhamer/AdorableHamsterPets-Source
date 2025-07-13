package net.dawson.adorablehamsterpets.fabric.datagen;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends FabricRecipeProvider {

    // --- 1. Constants ---
    // For the current recipes, don't need a shared list here.

    // --- 2. Constructor ---
    public ModRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    // --- 3. Public Methods (generate) ---
    @Override
    public void generate(Consumer<RecipeJsonProvider> exporter) {
        // Smelting recipes now use offerSmelting instead of a list
        offerSmelting(exporter, List.of(ModItems.GREEN_BEANS.get()), RecipeCategory.FOOD, ModItems.STEAMED_GREEN_BEANS.get(),
                0.35f, 200, "steamed_green_beans");

        // Shaped Crafting Recipes
        ShapedRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.HAMSTER_FOOD_MIX.get(), 1)
                .pattern("SSS")
                .pattern("PCP")
                .pattern("WWW")
                .input('S', ModItems.SUNFLOWER_SEEDS.get())
                .input('P', Items.PUMPKIN_SEEDS)
                .input('C', Items.CARROT)
                .input('W', Items.WHEAT_SEEDS)
                .criterion("has_sunflower_seeds", conditionsFromItem(ModItems.SUNFLOWER_SEEDS.get()))
                .offerTo(exporter, Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_food_mix_from_ingredients"));

        // Shapeless Crafting Recipes
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.SLICED_CUCUMBER.get(), 3)
                .input(ModItems.CUCUMBER.get())
                .criterion("has_cucumber", conditionsFromItem(ModItems.CUCUMBER.get()))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.CHEESE.get(), 3)
                .input(Items.MILK_BUCKET)
                .criterion("has_milk_bucket", conditionsFromItem(Items.MILK_BUCKET))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HAMSTER_GUIDE_BOOK.get(), 1)
                .input(Items.BOOK)
                .input(ModItems.SLICED_CUCUMBER.get())
                .criterion("has_sliced_cucumber", conditionsFromItem(ModItems.SLICED_CUCUMBER.get()))
                .offerTo(exporter, Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_guide_book_from_crafting"));
    }
}