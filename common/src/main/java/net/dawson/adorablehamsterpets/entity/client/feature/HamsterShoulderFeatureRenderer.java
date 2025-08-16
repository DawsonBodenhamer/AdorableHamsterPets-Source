package net.dawson.adorablehamsterpets.entity.client.feature;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.component.HamsterShoulderData;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.renderer.ShoulderHamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a Hamster model on the player's shoulder if shoulder data is present.
 * Handles scaling for baby/adult hamsters and texture variations.
 */

public class HamsterShoulderFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    // --- 1. Constants ---
    private static final float HAMSTER_SCALE = 0.7f;
    private static final float Y_OFFSET_SNEAKING = 0.2F;

    // --- 2. Fields (Lazy Initialization) ---
    @Nullable private ModelPart closedEyesPart;
    private HamsterEntity adorablehamsterpets$dummyHamster;
    private ShoulderHamsterRenderer adorablehamsterpets$hamsterRenderer;
    private int adorablehamsterpets$lastPlayerAge = -1;

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

        NbtCompound shoulderNbt = ((PlayerEntityAccessor) player).getHamsterShoulderEntity();
        if (shoulderNbt.isEmpty()) {
            return;
        }

        HamsterShoulderData shoulderData = HamsterShoulderData.fromNbt(shoulderNbt).orElse(null);
        if (shoulderData == null) {
            return;
        }

        // --- Lazy Initialization ---
        if (this.adorablehamsterpets$dummyHamster == null) {
            initializeDummy(player.getWorld());
        }

        // This should not happen if initialization is correct, but it's a safe check.
        if (this.adorablehamsterpets$hamsterRenderer == null) {
            return;
        }

        // --- Animation Speed Control ---
        boolean shouldTickAnimation = player.age > this.adorablehamsterpets$lastPlayerAge;
        this.adorablehamsterpets$lastPlayerAge = player.age;

        // --- Render on the player's right shoulder ---
        renderShoulderHamster(matrices, vertexConsumers, light, player, shoulderData, tickDelta, shouldTickAnimation);
    }

    // --- 5. Private Helper Methods ---
    /**
     * Initializes the dummy hamster entity and its specialized renderer.
     * This is called once, the first time the feature needs to be rendered.
     */
    private void initializeDummy(World world) {
        if (world == null) return; // Cannot proceed without a world instance

        this.adorablehamsterpets$dummyHamster = new HamsterEntity(ModEntities.HAMSTER.get(), world);
        this.adorablehamsterpets$dummyHamster.setNoGravity(true);
        this.adorablehamsterpets$dummyHamster.setSilent(true);

        // We need an EntityRendererFactory.Context to create the renderer.
        // We can get this from the MinecraftClient instance.
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRendererFactory.Context context = new EntityRendererFactory.Context(
                MinecraftClient.getInstance().getEntityRenderDispatcher(),
                MinecraftClient.getInstance().getItemRenderer(),
                client.getBlockRenderManager(),
                client.getEntityRenderDispatcher().getHeldItemRenderer(),
                MinecraftClient.getInstance().getResourceManager(),
                MinecraftClient.getInstance().getEntityModelLoader(),
                MinecraftClient.getInstance().textRenderer
        );

        this.adorablehamsterpets$hamsterRenderer = new ShoulderHamsterRenderer(context);
    }

    /**
     * Applies visual data from the stored shoulder NBT to the dummy entity.
     * This ensures the rendered model has the correct appearance (variant, age, cheeks, etc.).
     */
    private void applyShoulderData(HamsterShoulderData data, PlayerEntity owner) {
        if (this.adorablehamsterpets$dummyHamster == null) return;

        // --- Apply Visual Data ---
        this.adorablehamsterpets$dummyHamster.setVariant(data.variantId());
        this.adorablehamsterpets$dummyHamster.setLeftCheekFull((data.hamsterFlags() & HamsterEntity.LEFT_CHEEK_FULL_FLAG) != 0);
        this.adorablehamsterpets$dummyHamster.setRightCheekFull((data.hamsterFlags() & HamsterEntity.RIGHT_CHEEK_FULL_FLAG) != 0);
        this.adorablehamsterpets$dummyHamster.getDataTracker().set(HamsterEntity.PINK_PETAL_TYPE, data.pinkPetalType());
        this.adorablehamsterpets$dummyHamster.getDataTracker().set(HamsterEntity.ANIMATION_PERSONALITY_ID, data.animationPersonalityId());
        this.adorablehamsterpets$dummyHamster.setBreedingAge(data.breedingAge());

        // --- Set Ownership for Animation Logic ---
        // This is crucial for ensuring tamed-state animations are used.
        this.adorablehamsterpets$dummyHamster.setOwnerUuid(owner.getUuid());
        this.adorablehamsterpets$dummyHamster.setTamed(true, false); // No attribute update needed
    }

    /**
     * Renders the GeckoLib model on the player's shoulder with appropriate transformations and animations.
     */
    private void renderShoulderHamster(
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            AbstractClientPlayerEntity player, HamsterShoulderData shoulderData, float tickDelta, boolean shouldTickAnimation
    ) {
        // --- 1. Update Dummy Entity State ---
        applyShoulderData(shoulderData, player);
        if (shouldTickAnimation) {
            this.adorablehamsterpets$dummyHamster.age++; // Advance age for animation ticks
            this.adorablehamsterpets$dummyHamster.tick(); // Tick animations only once per game tick
        }

        // --- 2. Apply Transformations ---
        matrices.push();
        // Apply a simple Y offset only when sneaking.
        float yOffset = player.isInSneakingPose() ? Y_OFFSET_SNEAKING : 0.0F;
        float xOffset = -0.4F; // Right shoulder; use 0.4F for left
        matrices.translate(xOffset, yOffset, 0.0F);
        // Rotate the model to be upright
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        // Scale the hamster down slightly so it fits on the shoulder better
        matrices.scale(HAMSTER_SCALE, HAMSTER_SCALE, HAMSTER_SCALE);
        // Pass a yaw that makes the hamster face forward relative to the player.
        float renderYaw = 180.0F - player.getBodyYaw();

        // --- 3. Render the Dummy Entity ---
        this.adorablehamsterpets$hamsterRenderer.render(this.adorablehamsterpets$dummyHamster, renderYaw, tickDelta, matrices, vertexConsumers, light);

        matrices.pop();
    }
}