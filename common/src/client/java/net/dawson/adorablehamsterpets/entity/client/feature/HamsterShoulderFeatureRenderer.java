package net.dawson.adorablehamsterpets.entity.client.feature;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.attachment.HamsterShoulderData;
import net.dawson.adorablehamsterpets.attachment.ModEntityAttachments;
import net.dawson.adorablehamsterpets.entity.client.ModModelLayers;
import net.dawson.adorablehamsterpets.entity.client.model.HamsterShoulderModel;
import net.dawson.adorablehamsterpets.entity.custom.HamsterVariant;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a Hamster model on the player's shoulder if shoulder data is present.
 * Handles scaling for baby/adult hamsters and texture variations.
 */
public class HamsterShoulderFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    // --- 1. Constants ---
    private static final float BABY_SCALE = 0.5f;
    private static final float ADULT_SCALE = 0.8f;
    private static final float BABY_Y_OFFSET_SNEAKING = -0.55F;
    private static final float BABY_Y_OFFSET_STANDING = -0.75F;
    private static final float ADULT_Y_OFFSET_SNEAKING = -0.85F;
    private static final float ADULT_Y_OFFSET_STANDING = -1.13F;
    // --- End 1. Constants ---

    // --- 2. Fields ---
    private final HamsterShoulderModel hamsterShoulderModel;
    /** Reference to the 'closed_eyes' model part for visibility control. May be null if not found. */
    @Nullable
    private final ModelPart closedEyesPart;
    // --- End 2. Fields ---

    // --- 3. Constructor ---
    public HamsterShoulderFeatureRenderer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
            EntityModelLoader modelLoader
    ) {
        super(context);
        this.hamsterShoulderModel = new HamsterShoulderModel(modelLoader.getModelPart(ModModelLayers.HAMSTER_SHOULDER_LAYER));

        ModelPart tempClosedEyes = null;
        try {
            // Path from the HamsterShoulderModel's 'root' bone to 'closed_eyes':
            // root -> body_parent -> body_child -> head_parent -> head_child -> closed_eyes
            tempClosedEyes = this.hamsterShoulderModel.root
                    .getChild("body_parent")
                    .getChild("body_child")
                    .getChild("head_parent")
                    .getChild("head_child")
                    .getChild("closed_eyes");
        } catch (Exception e) {
            AdorableHamsterPets.LOGGER.error("[ShoulderRender] Failed to get 'closed_eyes' ModelPart in constructor. Eyes may not hide as intended.", e);
            // tempClosedEyes remains null
        }
        this.closedEyesPart = tempClosedEyes;
    }
    // --- End 3. Constructor ---

    // --- 4. Public Methods (Overrides from FeatureRenderer) ---
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        HamsterShoulderData shoulderData = player.getAttached(ModEntityAttachments.HAMSTER_SHOULDER_DATA);
        if (shoulderData == null) {
            return; // No hamster data, nothing to render
        }

        // --- Prepare model for rendering ---
        setCheekVisibility(shoulderData);
        if (this.closedEyesPart != null) {
            this.closedEyesPart.visible = false; // Always hide closed eyes for shoulder model
        }
        // --- End Prepare model ---

        // Render on the player's right shoulder (can be adapted for left if needed)
        renderShoulderHamster(matrices, vertexConsumers, light, player, shoulderData);
    }
    // --- End 4. Public Methods ---

    // --- 5. Private Helper Methods ---
    /**
     * Renders the hamster model on the player's shoulder with appropriate transformations and textures.
     */
    private void renderShoulderHamster(
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            AbstractClientPlayerEntity player, HamsterShoulderData shoulderData
    ) {
        matrices.push();

        // --- Determine Scale and Position based on Age and Player Pose ---
        boolean isBaby = shoulderData.breedingAge() < 0;
        float scaleFactor = isBaby ? BABY_SCALE : ADULT_SCALE;
        float yOffset = player.isInSneakingPose()
                ? (isBaby ? BABY_Y_OFFSET_SNEAKING : ADULT_Y_OFFSET_SNEAKING)
                : (isBaby ? BABY_Y_OFFSET_STANDING : ADULT_Y_OFFSET_STANDING);
        float xOffset = -0.4F; // Right shoulder; use 0.4F for left

        matrices.translate(xOffset, yOffset, 0.0F);
        matrices.scale(scaleFactor, scaleFactor, scaleFactor);
        // --- End Scale and Position ---

        // --- Get Variant and Textures ---
        HamsterVariant variant = HamsterVariant.byId(shoulderData.variantId());
        Identifier baseTextureId = getTextureId(variant.getBaseTextureName());
        @Nullable String overlayTextureName = variant.getOverlayTextureName();
        int pinkPetalType = shoulderData.pinkPetalType();
        // --- End Get Variant ---

        // --- Render Base Model ---
        RenderLayer baseRenderLayer = RenderLayer.getEntityCutoutNoCull(baseTextureId);
        this.hamsterShoulderModel.render(matrices, vertexConsumers.getBuffer(baseRenderLayer), light, OverlayTexture.DEFAULT_UV);
        // --- End Render Base Model ---

        // --- Render Overlay Model (if applicable) ---
        if (overlayTextureName != null) {
            Identifier overlayTextureId = getTextureId(overlayTextureName);
            RenderLayer overlayRenderLayer = RenderLayer.getEntityTranslucent(overlayTextureId); // Translucent for overlays
            this.hamsterShoulderModel.render(matrices, vertexConsumers.getBuffer(overlayRenderLayer), light, OverlayTexture.DEFAULT_UV);
        }
        // --- End Render Overlay Model ---

        // --- Render Pink Petal Overlay (if applicable) ---
        if (pinkPetalType > 0 && pinkPetalType <= 3) {
            Identifier petalTextureId = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/overlay_pink_petal" + pinkPetalType + ".png");
            RenderLayer petalRenderLayer = RenderLayer.getEntityTranslucent(petalTextureId); // Use translucent
            this.hamsterShoulderModel.render(matrices, vertexConsumers.getBuffer(petalRenderLayer), light, OverlayTexture.DEFAULT_UV);
        }
        // --- End Render Pink Petal Overlay ---

        matrices.pop(); // Restore matrix state
    }

    /**
     * Sets the visibility of the hamster's cheek model parts based on shoulder data.
     * @param data The hamster's shoulder data containing cheek fullness state.
     */
    private void setCheekVisibility(HamsterShoulderData data) {
        // Ensure model parts were successfully fetched in constructor
        if (this.hamsterShoulderModel.left_cheek_deflated == null || this.hamsterShoulderModel.left_cheek_inflated == null ||
                this.hamsterShoulderModel.right_cheek_deflated == null || this.hamsterShoulderModel.right_cheek_inflated == null) {
            // This might happen if getChild failed in constructor, error already logged there.
            return;
        }

        boolean leftCheekFull = data.leftCheekFull();
        boolean rightCheekFull = data.rightCheekFull();

        this.hamsterShoulderModel.left_cheek_deflated.visible = !leftCheekFull;
        this.hamsterShoulderModel.left_cheek_inflated.visible = leftCheekFull;
        this.hamsterShoulderModel.right_cheek_deflated.visible = !rightCheekFull;
        this.hamsterShoulderModel.right_cheek_inflated.visible = rightCheekFull;
    }

    /**
     * Constructs an Identifier for a hamster texture.
     * @param textureName The base name of the texture file (e.g., "orange", "overlay3").
     * @return The full Identifier for the texture.
     */
    private Identifier getTextureId(String textureName) {
        return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/" + textureName + ".png");
    }
    // --- End 5. Private Helper Methods ---
}