package net.dawson.adorablehamsterpets.entity.client.layer;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class HamsterStolenItemLayer extends GeoRenderLayer<HamsterEntity> {
    private final ItemStack diamondStack = new ItemStack(Items.DIAMOND);

    public HamsterStolenItemLayer(GeoRenderer<HamsterEntity> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(MatrixStack poseStack, HamsterEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
                       net.minecraft.client.render.RenderLayer renderType, VertexConsumerProvider bufferSource,
                       VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (!animatable.isStealingDiamond()) {
            return;
        }

        GeoBone noseBone = this.getGeoModel().getBone("nose").orElse(null);
        if (noseBone == null) {
            return;
        }

        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

        poseStack.push();

        // Apply the transformations from the nose bone
        poseStack.translate(noseBone.getWorldPosition().x, noseBone.getWorldPosition().y, noseBone.getWorldPosition().z);

        // Apply bone rotations
        poseStack.multiply(new Quaternionf().rotateZ(noseBone.getRotZ()));
        poseStack.multiply(new Quaternionf().rotateY(noseBone.getRotY()));
        poseStack.multiply(new Quaternionf().rotateX(noseBone.getRotX()));

        // Manual adjustments to position and scale the item correctly
        poseStack.translate(0, -0.1F, -0.1F);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.multiply(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0))); // Rotate to face forward

        // Render the item
        itemRenderer.renderItem(diamondStack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, packedLight, packedOverlay, poseStack, bufferSource, animatable.getWorld(), animatable.getId());

        poseStack.pop();
    }
}