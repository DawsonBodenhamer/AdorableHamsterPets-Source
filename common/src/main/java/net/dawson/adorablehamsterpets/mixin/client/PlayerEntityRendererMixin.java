package net.dawson.adorablehamsterpets.mixin.client;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.client.feature.HamsterShoulderFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Target PlayerEntityRenderer, but ONLY when on Fabric using remap=false.
@Mixin(value = PlayerEntityRenderer.class, remap = false)
public abstract class PlayerEntityRendererMixin {

    // Inject into the constructor
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        // --- Diagnostic Logging ---
        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] PlayerEntityRendererMixin constructor injection is RUNNING.");
        // Cast 'this' (PlayerEntityRenderer) to LivingEntityRenderer first
        LivingEntityRenderer<?, ?> livingRenderer = (LivingEntityRenderer<?, ?>) (Object) this;
        // Then cast the LivingEntityRenderer instance to our separate Invoker interface
        LivingEntityRendererInvoker invoker = (LivingEntityRendererInvoker) livingRenderer;

        EntityModelLoader modelLoader = ctx.getModelLoader();

        // Cast 'this' to PlayerEntityRenderer to pass to the FeatureRenderer constructor
        PlayerEntityRenderer thisRenderer = (PlayerEntityRenderer)(Object)this;

        AdorableHamsterPets.LOGGER.debug("[PlayerRendererMixin] Adding HamsterShoulderFeatureRenderer via Invoker...");
        // Call the protected method using the invoker interface
        boolean added = invoker.callAddFeature(new HamsterShoulderFeatureRenderer(thisRenderer, modelLoader));
        // --- Diagnostic Logging ---
        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] Attempted to add HamsterShoulderFeatureRenderer. Success: {}", added);
        if (!added) {
            AdorableHamsterPets.LOGGER.debug("[AHP Mixin] FAILED to add HamsterShoulderFeatureRenderer!");
        }
    }
}