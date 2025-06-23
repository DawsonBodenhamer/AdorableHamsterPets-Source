package net.dawson.adorablehamsterpets.datagen;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {

    // --- 1. Constants ---
    // For the current recipes, don't need a shared list here.

    // --- 2. Constructor ---
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    // --- 3. Public Methods (generate) ---
    @Override
    public void generate(RecipeExporter recipeExporter) {
        // --- Smelting Recipes ---
        // Smelting Green Beans to Steamed Green Beans
        // The list should only contain items that can be smelted into the result.
        offerSmelting(recipeExporter, List.of(ModItems.GREEN_BEANS.get()), RecipeCategory.FOOD, ModItems.STEAMED_GREEN_BEANS.get(),
                0.35f, 200, "steamed_green_beans");

        // --- Shaped Crafting Recipes ---
        // Hamster Food Mix
        ShapedRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.HAMSTER_FOOD_MIX.get(), 1)
                .pattern("SSS")
                .pattern("PCP")
                .pattern("WWW")
                .input('S', ModItems.SUNFLOWER_SEEDS.get())
                .input('P', Items.PUMPKIN_SEEDS)
                .input('C', Items.CARROT)
                .input('W', Items.WHEAT_SEEDS)
                .criterion("has_sunflower_seeds", conditionsFromItem(ModItems.SUNFLOWER_SEEDS.get()))
                .offerTo(recipeExporter, Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_food_mix_from_ingredients"));

        // --- Shapeless Crafting Recipes ---
        // Sliced Cucumber
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.SLICED_CUCUMBER.get(), 3)
                .input(ModItems.CUCUMBER.get())
                .criterion("has_cucumber", conditionsFromItem(ModItems.CUCUMBER.get()))
                .offerTo(recipeExporter); // Will use default ID: adorablehamsterpets:sliced_cucumber

        // Cheese
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.CHEESE.get(), 3)
                .input(Items.MILK_BUCKET)
                .criterion("has_milk_bucket", conditionsFromItem(Items.MILK_BUCKET))
                .offerTo(recipeExporter); // Will use default ID: adorablehamsterpets:cheese

        // Hamster Guide Book Recipe (NO NBT in result here)
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HAMSTER_GUIDE_BOOK.get(), 1)
                .input(Items.BOOK)
                .input(ModItems.SLICED_CUCUMBER.get())
                .criterion("has_sliced_cucumber", conditionsFromItem(ModItems.SLICED_CUCUMBER.get()))
                .offerTo(recipeExporter, Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_guide_book_from_crafting"));
    }
}