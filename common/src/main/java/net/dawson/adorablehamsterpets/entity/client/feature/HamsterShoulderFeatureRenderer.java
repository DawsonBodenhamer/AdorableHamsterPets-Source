package net.dawson.adorablehamsterpets.entity.client.feature;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.client.ShoulderHamsterManager;
import net.dawson.adorablehamsterpets.component.HamsterShoulderData;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.renderer.ShoulderHamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders a Hamster model on the player's shoulder if shoulder data is present.
 * Handles scaling for baby/adult hamsters and texture variations.
 */

public class HamsterShoulderFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    // --- 1. Constants ---
    private static final float HAMSTER_SHOULDER_SCALE = 0.8f;

    // --- 3. Constructor ---
    public HamsterShoulderFeatureRenderer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context
    ) {
        super(context);
    }

    // --- 4. Public Methods (Overrides from FeatureRenderer) ---
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;
        if (!playerAccessor.hasAnyShoulderHamster()) {
            return;
        }

        // --- Render Hamster for Each Occupied Slot ---
        for (ShoulderLocation location : ShoulderLocation.values()) {
            NbtCompound shoulderNbt = playerAccessor.getShoulderHamster(location);
            if (!shoulderNbt.isEmpty()) {
                HamsterEntity dummy = ShoulderHamsterManager.getDummy(location);
                ShoulderHamsterRenderer renderer = ShoulderHamsterManager.getRenderer(location);

                if (dummy != null && renderer != null) {
                    // Sync the animation clock to the game's main clock.
                    dummy.age = player.age;

                    HamsterShoulderData.fromNbt(shoulderNbt).ifPresent(shoulderData ->
                            renderShoulderHamster(matrices, vertexConsumers, light, player, shoulderData, tickDelta, dummy, renderer, location)
                    );
                }
            }
        }
    }

    // --- 5. Private Helper Methods ---
    /**
     * Applies visual data from the stored shoulder NBT to a specific dummy entity.
     * This ensures the rendered model has the correct appearance (variant, age, cheeks, etc.).
     */
    private void applyShoulderData(HamsterEntity dummyHamster, HamsterShoulderData data, PlayerEntity owner) {
        // --- Mark this as a shoulder pet for the animation controller ---
        dummyHamster.setShoulderPet(true);

        // --- Apply Visual Data ---
        dummyHamster.setVariant(data.variantId());
        dummyHamster.setLeftCheekFull((data.hamsterFlags() & HamsterEntity.LEFT_CHEEK_FULL_FLAG) != 0);
        dummyHamster.setRightCheekFull((data.hamsterFlags() & HamsterEntity.RIGHT_CHEEK_FULL_FLAG) != 0);
        dummyHamster.getDataTracker().set(HamsterEntity.PINK_PETAL_TYPE, data.pinkPetalType());
        dummyHamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, data.animationPersonalityId());
        dummyHamster.setBreedingAge(data.breedingAge());

        // --- Set Ownership for Animation Logic ---
        dummyHamster.setOwnerUuid(owner.getUuid());
        dummyHamster.setTamed(true, false); // No attribute update needed
    }

    /**
     * Renders the GeckoLib model on the player's shoulder with appropriate transformations and animations.
     */
    private void renderShoulderHamster(
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            AbstractClientPlayerEntity player, HamsterShoulderData shoulderData, float tickDelta,
            HamsterEntity dummyHamster, ShoulderHamsterRenderer hamsterRenderer, ShoulderLocation location
    ) {
        // --- 1. Update Dummy Entity State from Data ---
        applyShoulderData(dummyHamster, shoulderData, player);
        dummyHamster.shoulderLocation = location; // Inform the dummy of its location for animation

        matrices.push();

        // --- 2. Apply Transformations Based on Location ---
        boolean isSlim = player.getSkinTextures().model() == SkinTextures.Model.SLIM;

        switch (location) {
            case RIGHT_SHOULDER:
                this.getContextModel().rightArm.rotate(matrices);
                float xOffsetRight = isSlim ? -0.08F : -0.12F; // Apply different offset for slim vs wide
                matrices.translate(xOffsetRight, -0.12F, -0.016F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F));
                break;
            case LEFT_SHOULDER:
                this.getContextModel().leftArm.rotate(matrices);
                float xOffsetLeft = isSlim ? 0.08F : 0.12F; // Apply different offset for slim vs wide
                matrices.translate(xOffsetLeft, -0.12F, -0.016F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-15.0F));
                break;
            case HEAD:
                this.getContextModel().head.rotate(matrices);
                matrices.translate(0.0F, -0.5F, -0.05F);
                break;
        }

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        matrices.scale(HAMSTER_SHOULDER_SCALE, HAMSTER_SHOULDER_SCALE, HAMSTER_SHOULDER_SCALE);
        float renderYaw = 180.0F - player.getBodyYaw();

        // --- 3. Render the Dummy Entity ---
        hamsterRenderer.render(dummyHamster, renderYaw, tickDelta, matrices, vertexConsumers, light);

        matrices.pop();
    }
}