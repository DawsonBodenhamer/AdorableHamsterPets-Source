package net.dawson.adorablehamsterpets.entity.client.model;

import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Vanilla-style (ModelPart-based) model for rendering the hamster on a player's shoulder.
 * Defines geometry and UV mapping for a 32x32 texture.
 * Model definitions updated from Blockbench export.
 */
public class HamsterShoulderModel extends EntityModel<AbstractClientPlayerEntity> {

    // --- Fields ---
    public final ModelPart root;
    public final ModelPart left_cheek_deflated;
    public final ModelPart left_cheek_inflated;
    public final ModelPart right_cheek_deflated;
    public final ModelPart right_cheek_inflated;
    public final ModelPart closed_eyes;
    // --- End Fields ---

    // --- Constructor ---
    /**
     * Constructs the shoulder model and initializes direct references to controllable child parts.
     * @param root The root ModelPart generated from {@link #getTexturedModelData()}.
     */
    public HamsterShoulderModel(ModelPart root) {
        // The 'root' parameter IS the part from modelData.getRoot().
        // The actual visual model's root bone (named "root" in getTexturedModelData) is a child of this.
        this.root = root.getChild("root");

        // Fetch specific child parts for dynamic control (e.g., visibility by FeatureRenderer)
        try {
            ModelPart body_parent = this.root.getChild("body_parent");
            ModelPart body_child = body_parent.getChild("body_child");
            ModelPart head_parent = body_child.getChild("head_parent");
            ModelPart head_child = head_parent.getChild("head_child");
            ModelPart cheeks = head_child.getChild("cheeks");

            this.left_cheek_deflated = cheeks.getChild("left_cheek_deflated");
            this.left_cheek_inflated = cheeks.getChild("left_cheek_inflated");
            this.right_cheek_deflated = cheeks.getChild("right_cheek_deflated");
            this.right_cheek_inflated = cheeks.getChild("right_cheek_inflated");
            this.closed_eyes = head_child.getChild("closed_eyes");
        } catch (Exception e) {
            String errorMessage = "Failed to get child ModelParts in HamsterShoulderModel: " + e.getMessage();
            net.dawson.adorablehamsterpets.AdorableHamsterPets.LOGGER.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
    // --- End Constructor ---

    // --- Public Static Methods ---
    /**
     * Creates the {@link TexturedModelData} defining the hamster shoulder model's geometry and UVs.
     * Texture dimensions: 32x32.
     * @return TexturedModelData for the hamster shoulder model.
     */
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData partdefinition = modelData.getRoot();

        // --- Model Definition (from Blockbench Export, 32x32 texture) ---
        // Using addChild instead of addOrReplaceChild as that returns an error.
        ModelPartData root = partdefinition.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData body_parent = root.addChild("body_parent", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -1.5F, 3.0F));

        ModelPartData body_child = body_parent.addChild("body_child", ModelPartBuilder.create().uv(3, 3).cuboid(-3.0F, -3.0F, -5.0F, 6.0F, 4.0F, 6.0F, new Dilation(0.01F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData head_parent = body_child.addChild("head_parent", ModelPartBuilder.create(), ModelTransform.pivot(-0.5F, -2.0F, -3.0F));

        ModelPartData head_child = head_parent.addChild("head_child", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        head_child.addChild("nose", ModelPartBuilder.create().uv(23, 6).cuboid(-0.5F, -4.0F, -3.1F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.5F, 3.5F, -1.0F));

        head_child.addChild("head_skull", ModelPartBuilder.create().uv(3, 14).cuboid(-2.5F, -4.0F, -2.0F, 5.0F, 4.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.5F, 1.5F, -2.0F));

        ModelPartData ears = head_child.addChild("ears", ModelPartBuilder.create(), ModelTransform.pivot(2.0F, -1.5F, -2.0F));

        ears.addChild("left_ear", ModelPartBuilder.create().uv(14, 25).cuboid(0.0F, -2.0F, 0.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.01F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ears.addChild("right_ear", ModelPartBuilder.create().uv(14, 25).mirrored().cuboid(-2.0F, -2.0F, 0.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.01F)).mirrored(false), ModelTransform.pivot(-3.0F, 0.0F, 0.0F));

        ModelPartData cheeks = head_child.addChild("cheeks", ModelPartBuilder.create(), ModelTransform.pivot(-4.0F, 0.0F, -2.0F));

        cheeks.addChild("left_cheek_deflated", ModelPartBuilder.create().uv(22, 15).cuboid(0.0F, -1.5F, -1.5F, 1.2F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(7.0F, 0.0F, -0.5F));
        cheeks.addChild("left_cheek_inflated", ModelPartBuilder.create().uv(3, 23).cuboid(0.0F, -1.6F, -1.5F, 2.0F, 3.1F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(7.0F, 0.0F, -0.5F));
        cheeks.addChild("right_cheek_deflated", ModelPartBuilder.create().uv(22, 15).mirrored().cuboid(-1.2F, -1.5F, -1.5F, 1.2F, 3.0F, 3.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.pivot(2.0F, 0.0F, -0.5F));
        cheeks.addChild("right_cheek_inflated", ModelPartBuilder.create().uv(3, 23).mirrored().cuboid(-2.0F, -1.6F, -1.5F, 2.0F, 3.1F, 3.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.pivot(2.0F, 0.0F, -0.5F));

        head_child.addChild("closed_eyes", ModelPartBuilder.create()
                        .uv(23, 3).cuboid(1.5F, -4.1F, -4.025F, 1.0F, 1.2F, 1.0F, new Dilation(0.01F))
                        .uv(23, 3).cuboid(-2.5F, -4.1F, -4.025F, 1.0F, 1.2F, 1.0F, new Dilation(0.01F)),
                ModelTransform.pivot(0.5F, 2.5F, 0.0F));

        body_child.addChild("tail", ModelPartBuilder.create().uv(21, 26).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -3.0F, 1.0F));
        body_child.addChild("right_hand", ModelPartBuilder.create().uv(21, 22).mirrored().cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.pivot(-1.5F, 0.5F, -4.0F));
        body_child.addChild("left_hand", ModelPartBuilder.create().uv(21, 22).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(1.5F, 0.5F, -4.0F));

        ModelPartData legs = body_child.addChild("legs", ModelPartBuilder.create(), ModelTransform.pivot(1.5F, 0.5F, -4.0F));
        legs.addChild("left_foot", ModelPartBuilder.create().uv(21, 22).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 4.0F));
        legs.addChild("right_foot", ModelPartBuilder.create().uv(21, 22).mirrored().cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.pivot(-3.0F, 0.0F, 4.0F));
        // --- End Model Definition ---

        return TexturedModelData.of(modelData, 32, 32);
    }
    // --- End Public Static Methods ---

    // --- Public Methods (Overrides from EntityModel) ---
    @Override
    public void setAngles(AbstractClientPlayerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // This model is static on the shoulder; no complex animation updates needed here.
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        this.root.render(matrices, vertexConsumer, light, overlay, color);
    }
    // --- End Public Methods ---
}