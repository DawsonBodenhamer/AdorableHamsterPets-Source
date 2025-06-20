package net.dawson.adorablehamsterpets.entity.client; // Adjust package if needed

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer HAMSTER_SHOULDER_LAYER =
            new EntityModelLayer(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_shoulder"), "main");

    // Add other model layers here if needed in the future
}