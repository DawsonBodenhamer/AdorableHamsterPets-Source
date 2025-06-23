package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterVariant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

@Environment(EnvType.CLIENT)
public class HamsterOverlayLayer extends GeoRenderLayer<HamsterEntity> {

    public HamsterOverlayLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    // Helper method to construct the overlay texture identifier
    @Nullable
    private Identifier getOverlayTexture(HamsterEntity entity) {
        HamsterVariant variant = HamsterVariant.byId(entity.getVariant());
        String overlayName = variant.getOverlayTextureName();

        if (overlayName != null) {
            return Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/" + overlayName + ".png");
        }
        return null; // No overlay for this variant
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                       VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        Identifier overlayTexture = getOverlayTexture(animatable);

        // Only render if there's an overlay texture for this variant
        if (overlayTexture != null) {
            RenderLayer overlayRenderType = RenderLayer.getEntityTranslucent(overlayTexture); // Use translucent so base shows through

            // --- FIX: Correct reRender call with packed color ---
            getRenderer().reRender(
                    bakedModel, // 1
                    poseStack, // 2
                    bufferSource, // 3
                    animatable, // 4
                    overlayRenderType, // 5
                    bufferSource.getBuffer(overlayRenderType), // 6
                    partialTick, // 7
                    packedLight, // 8
                    OverlayTexture.DEFAULT_UV, // 9 - Use default overlay (for damage flash, etc.)
                    // 10: Provide packed white color (ARGB)
                    ColorHelper.Argb.getArgb(255, 255, 255, 255) // White, full alpha
            );
            // --- END FIX ---
        }
        // If overlayTexture is null, do nothing, only the base layer renders.
    }
}