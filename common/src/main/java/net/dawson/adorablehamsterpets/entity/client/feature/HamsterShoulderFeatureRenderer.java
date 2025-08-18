package net.dawson.adorablehamsterpets.entity.client.feature;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.component.HamsterShoulderData;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.renderer.ShoulderHamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.Map;

/**
 * Renders a Hamster model on the player's shoulder if shoulder data is present.
 * Handles scaling for baby/adult hamsters and texture variations.
 */

public class HamsterShoulderFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    // --- 1. Constants ---
    private static final float HAMSTER_SHOULDER_SCALE = 0.8f;

    // --- 2. Fields (Lazy Initialization) ---
    private final Map<ShoulderLocation, HamsterEntity> dummyHamsters = new EnumMap<>(ShoulderLocation.class);
    private final Map<ShoulderLocation, ShoulderHamsterRenderer> hamsterRenderers = new EnumMap<>(ShoulderLocation.class);
    private final Map<ShoulderLocation, ShoulderPetState> petStates = new EnumMap<>(ShoulderLocation.class);
    private long lastTickTime = -1;

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
            if (!petStates.isEmpty()) petStates.clear();
            return;
        }

        // --- Lazy Initialization (checks if the map is empty) ---
        if (this.dummyHamsters.isEmpty()) {
            initializeDummy(player.getWorld());
        }

        // --- Tick-Based Animation Time Speed Gate ---
        long currentTime = player.getWorld().getTime();
        boolean isNewTick = currentTime > this.lastTickTime;
        this.lastTickTime = currentTime;

        // --- Render Hamster for Each Occupied Slot ---
        for (ShoulderLocation location : ShoulderLocation.values()) {
            NbtCompound shoulderNbt = playerAccessor.getShoulderHamster(location);
            if (!shoulderNbt.isEmpty()) {
                ShoulderPetState state = petStates.computeIfAbsent(location, l -> new ShoulderPetState());
                HamsterEntity dummy = this.dummyHamsters.get(location);

                if (isNewTick && dummy != null) {
                    // Pass the player's sprinting status to the state machine.
                    state.tick(dummy, player.isSprinting());
                    // Always tick the dummy's age and animation controller.
                    dummy.age++;
                    dummy.tick();
                }

                HamsterShoulderData.fromNbt(shoulderNbt).ifPresent(shoulderData ->
                        renderShoulderHamster(matrices, vertexConsumers, light, player, shoulderData, tickDelta, state, location)
                );
            } else {
                petStates.remove(location);
            }
        }
    }

    // --- 5. Private Helper Methods ---
    /**
     * Initializes the dummy hamster entities and their specialized renderers, one for each shoulder location.
     * This is called once, the first time the feature needs to be rendered.
     */
    private void initializeDummy(World world) {
        if (world == null) return; // Cannot proceed without a world instance

        // We need an EntityRendererFactory.Context to create the renderers.
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRendererFactory.Context context = new EntityRendererFactory.Context(
                client.getEntityRenderDispatcher(),
                client.getItemRenderer(),
                client.getBlockRenderManager(),
                client.getEntityRenderDispatcher().getHeldItemRenderer(),
                client.getResourceManager(),
                client.getEntityModelLoader(),
                client.textRenderer
        );

        for (ShoulderLocation location : ShoulderLocation.values()) {
            // Create a unique dummy entity for this location
            HamsterEntity dummy = new HamsterEntity(ModEntities.HAMSTER.get(), world);
            dummy.setNoGravity(true);
            this.dummyHamsters.put(location, dummy);

            // Create a unique renderer for this location
            this.hamsterRenderers.put(location, new ShoulderHamsterRenderer(context));
        }
    }

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
            ShoulderPetState state, ShoulderLocation location
    ) {
        // --- 1. Get the correct dummy and renderer for this specific location ---
        HamsterEntity dummyHamster = this.dummyHamsters.get(location);
        ShoulderHamsterRenderer hamsterRenderer = this.hamsterRenderers.get(location);
        if (dummyHamster == null || hamsterRenderer == null) return;

        // --- 2.Tell the dummy its location ---
        dummyHamster.shoulderLocation = location;

        // --- 3. Update Dummy Entity State from Data ---
        applyShoulderData(dummyHamster, shoulderData, player);

        matrices.push();

        // --- 4. Apply Transformations Based on Location ---
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

        // --- 5. Render the Dummy Entity ---
        hamsterRenderer.render(dummyHamster, renderYaw, tickDelta, matrices, vertexConsumers, light);

        matrices.pop();
    }
}