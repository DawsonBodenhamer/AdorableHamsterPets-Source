package net.dawson.adorablehamsterpets.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class ModItemGroups {

    // --- 1. Create a DeferredRegister for ItemGroups ---
    public static final DeferredRegister<ItemGroup> ITEM_GROUPS = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.ITEM_GROUP);

    // --- 2. Register the ItemGroup using CreativeTabRegistry ---
    public static final RegistrySupplier<ItemGroup> ADORABLE_HAMSTER_PETS_GROUP = ITEM_GROUPS.register("adorable_hamster_pets", () ->
            CreativeTabRegistry.create(
                    Text.translatable("itemgroup.adorablehamsterpets.main"), // The tab's title
                    () -> new ItemStack(ModItems.HAMSTER_SPAWN_EGG.get()) // The icon supplier
            )
    );

    // --- 3. Main Registration Call ---
    public static void register() {
        ITEM_GROUPS.register();
    }
}