package net.dawson.adorablehamsterpets.tag;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModItemTags {
    public static final TagKey<Item> ALLOWED_POUCH_BLOCKS =
            TagKey.of(
                    RegistryKeys.ITEM,
                    Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_pouch_allowed")
            );
}
