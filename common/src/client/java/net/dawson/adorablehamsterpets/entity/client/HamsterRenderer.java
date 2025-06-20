package net.dawson.adorablehamsterpets.entity.client;


import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.client.layer.HamsterOverlayLayer;
import net.dawson.adorablehamsterpets.entity.client.layer.HamsterPinkPetalOverlayLayer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterVariant;
import net.dawson.adorablehamsterpets.networking.payload.SpawnAttackParticlesPayload; // Added
import net.dawson.adorablehamsterpets.networking.payload.SpawnSeekingDustPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking; // Added
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class HamsterRenderer extends GeoEntityRenderer<HamsterEntity> {


    private final float adultShadowRadius;


    // --- 1. New Fields for Particle Spawning ---
    private boolean shouldSpawnAttackParticles = false;
    private boolean shouldSpawnSeekingDust = false;
    // Don't need to store particleSpawnPos here, calculation and sending will be immediate in renderFinal
    // --- End 1. New Fields ---


    public HamsterRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new HamsterModel());
        this.adultShadowRadius = 0.2F;
        this.shadowRadius = this.adultShadowRadius;


        addRenderLayer(new HamsterOverlayLayer(this));
        addRenderLayer(new HamsterPinkPetalOverlayLayer(this));
    }


    @Override
    public Identifier getTextureLocation(HamsterEntity entity) {
        HamsterVariant variant = HamsterVariant.byId(entity.getVariant());
        String baseTextureName = variant.getBaseTextureName();
        return Identifier.of(
                AdorableHamsterPets.MOD_ID,
                "textures/entity/hamster/" + baseTextureName + ".png"
        );
    }


    @Override
    public void render(HamsterEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        if (entity.isBaby()) {
            this.shadowRadius = this.adultShadowRadius * 0.5f;
        } else {
            this.shadowRadius = this.adultShadowRadius;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }


    // --- 2. Modified preRender Method ---
    @Override
    public void preRender(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model, @Nullable VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);


        model.getBone("left_foot").ifPresent(bone -> bone.setTrackingMatrices(true));
        model.getBone("nose").ifPresent(bone -> bone.setTrackingMatrices(true));
    }
    // --- End 2. Modified preRender Method ---


    // --- 3. New renderFinal Method ---
    @Override
    public void renderFinal(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);


        // Logic for Attack Particles
        if (this.shouldSpawnAttackParticles) {
            AdorableHamsterPets.LOGGER.debug("[Renderer {} Tick {}] renderFinal: shouldSpawnAttackParticles is true.", animatable.getId(), animatable.getWorld().getTime());
            model.getBone("left_foot").ifPresentOrElse(bone -> {
                Vector3d boneWorldPos = bone.getWorldPosition(); // Get current world position
                double boneX = boneWorldPos.x();
                double boneY = boneWorldPos.y();
                double boneZ = boneWorldPos.z();


                AdorableHamsterPets.LOGGER.debug("[Renderer {}] renderFinal: Found bone 'left_foot'. Calculated Pos: ({}, {}, {}). Sending packet.", animatable.getId(), boneX, boneY, boneZ);
                ClientPlayNetworking.send(new SpawnAttackParticlesPayload(boneX, boneY, boneZ));


            }, () -> AdorableHamsterPets.LOGGER.error("[Renderer {}] renderFinal: Could not find 'left_foot' bone to spawn particles.", animatable.getId()));


            this.shouldSpawnAttackParticles = false; // Reset the flag
        }


        // Logic for Seeking Dust Particles
        if (this.shouldSpawnSeekingDust) {
            AdorableHamsterPets.LOGGER.debug("[Renderer {} Tick {}] renderFinal: shouldSpawnSeekingDust is true.", animatable.getId(), animatable.getWorld().getTime());
            model.getBone("nose").ifPresentOrElse(bone -> {
                Vector3d boneWorldPos = bone.getWorldPosition();
                double boneX = boneWorldPos.x();
                double boneY = boneWorldPos.y();
                double boneZ = boneWorldPos.z();


                AdorableHamsterPets.LOGGER.debug("[Renderer {}] renderFinal: Found bone 'nose'. Particle Pos: ({}, {}, {}). Sending dust packet.", animatable.getId(), boneX, boneY, boneZ);
                // Send hamster's entity ID along with particle coordinates
                ClientPlayNetworking.send(new SpawnSeekingDustPayload(animatable.getId(), boneX, boneY, boneZ)); // UPDATED


            }, () -> AdorableHamsterPets.LOGGER.error("[Renderer {}] renderFinal: Could not find 'nose' bone to spawn seeking dust.", animatable.getId()));
            this.shouldSpawnSeekingDust = false;
        }
    }
    // --- End 3. New renderFinal Method ---


    // --- 4. Public Methods to Set Particle Flags ---
    /**
     * Called by the HamsterEntity's particle keyframe handler to signal
     * that particles should be spawned in the next renderFinal call.
     */
    public void triggerAttackParticleSpawn() {
        this.shouldSpawnAttackParticles = true;
    }


    /**
     * Called by the HamsterEntity's particle keyframe handler to signal
     * that seeking dust particles should be spawned in the next renderFinal call.
     */
    public void triggerSeekingDustSpawn() {
        this.shouldSpawnSeekingDust = true;
    }
    // --- End 4. Public Methods ---
}
