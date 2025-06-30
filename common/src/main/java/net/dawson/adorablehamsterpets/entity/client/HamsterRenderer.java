package net.dawson.adorablehamsterpets.entity.client;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.sound.HamsterCleaningSoundInstance;
import net.dawson.adorablehamsterpets.entity.client.layer.HamsterOverlayLayer;
import net.dawson.adorablehamsterpets.entity.client.layer.HamsterPinkPetalOverlayLayer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.HamsterVariant;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;


public class HamsterRenderer extends GeoEntityRenderer<HamsterEntity> {

    private final float adultShadowRadius;

    private static final Map<Integer, HamsterCleaningSoundInstance> activeCleaningSounds = new HashMap<>();

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
        // --- 1. Manage Cleaning Sound ---
        boolean isCleaning = entity.getDataTracker().get(HamsterEntity.IS_CLEANING);
        HamsterCleaningSoundInstance sound = activeCleaningSounds.get(entity.getId());

        if (isCleaning && (sound == null || sound.isDone())) {
            sound = new HamsterCleaningSoundInstance(entity);
            activeCleaningSounds.put(entity.getId(), sound);
            MinecraftClient.getInstance().getSoundManager().play(sound);
        } else if (!isCleaning && sound != null) {
            sound.stop();
            activeCleaningSounds.remove(entity.getId());
        }

        // --- 2. Set Shadow Radius ---
        if (entity.isBaby()) {
            this.shadowRadius = this.adultShadowRadius * 0.5f;
        } else {
            this.shadowRadius = this.adultShadowRadius;
        }

        // --- 3. Report to Client-Side Tracker ---
        // Adds the entity's ID to a set to determine which entities are no longer being rendered.
        AdorableHamsterPetsClient.onHamsterRendered(entity.getId());

        // --- 4. Call Superclass Method ---
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void preRender(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model, @Nullable VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        model.getBone("left_foot").ifPresent(bone -> bone.setTrackingMatrices(true));
        model.getBone("nose").ifPresent(bone -> bone.setTrackingMatrices(true));
    }

    /**
     * Performs the final rendering steps for the entity, including handling post-animation particle effects.
     * <p>
     * This method polls a transient {@code particleEffectId} flag on the animatable entity each frame.
     * If the flag is set (by a particle keyframe event), it spawns the corresponding particle effect
     * at the animated bone's calculated world position and then immediately resets the flag to {@code null}
     * to prevent re-triggering.
     */
    @Override
    public void renderFinal(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);

        if (animatable.particleEffectId != null) {
            Random random = animatable.getRandom();
            switch (animatable.particleEffectId) {
                case "attack_poof":
                    model.getBone("left_foot").ifPresent(bone -> {
                        Vector3d pos = bone.getWorldPosition();

                        for (int i = 0; i < 10; ++i) {
                            double d = random.nextGaussian() * 0.1;
                            double e = random.nextGaussian() * 0.2;
                            double f = random.nextGaussian() * 0.1;
                            animatable.getWorld().addParticle(ParticleTypes.WHITE_SMOKE,
                                    pos.x + d, pos.y + e, pos.z + f,
                                    random.nextGaussian() * 0.05,
                                    random.nextGaussian() * 0.05 + 0.1, // Slight upward bias
                                    random.nextGaussian() * 0.05);
                        }
                    });
                    break;
                case "seeking_dust":
                    model.getBone("nose").ifPresent(bone -> {
                        Vector3d pos = bone.getWorldPosition();
                        BlockPos blockBelow = BlockPos.ofFloored(pos.x, pos.y - 0.1, pos.z).down();
                        BlockState state = animatable.getWorld().getBlockState(blockBelow);
                        if (state.isAir()) state = Blocks.DIRT.getDefaultState();

                        for (int i = 0; i < 12; ++i) {
                            double d = random.nextGaussian() * 0.2;
                            double e = random.nextGaussian() * 0.03;
                            double f = random.nextGaussian() * 0.2;
                            animatable.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state),
                                    pos.x + d, pos.y + e, pos.z + f,
                                    0.0, 0.0, 0.0); // Speed is default
                        }
                    });
                    break;
            }
            animatable.particleEffectId = null;
        }
        // --- Handle Sound Spawning via Flag ---
        if (animatable.soundEffectId != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            switch (animatable.soundEffectId) {
                case "hamster_step_sound":
                    BlockPos pos = animatable.getBlockPos();
                    BlockState blockState = animatable.getWorld().getBlockState(pos.down());
                    if (blockState.isAir()) blockState = animatable.getWorld().getBlockState(pos.down(2));
                    if (!blockState.isAir()) {
                        BlockSoundGroup group = blockState.getSoundGroup();
                        float volume = blockState.isOf(Blocks.GRAVEL) ? (0.10F * 0.60F) : 0.10F;
                        client.getSoundManager().play(new PositionedSoundInstance(
                                group.getStepSound(), SoundCategory.NEUTRAL, volume,
                                group.getPitch() * 1.5F, animatable.getRandom(),
                                animatable.getX(), animatable.getY(), animatable.getZ()
                        ));
                    }
                    break;
                case "hamster_beg_bounce":
                    SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, animatable.getRandom());
                    if (bounceSound != null) {
                        float basePitch = animatable.getSoundPitch();
                        float randomPitchAddition = animatable.getRandom().nextFloat() * 0.2f;
                        float finalPitch = (basePitch * 1.2f) + randomPitchAddition;
                        client.getSoundManager().play(new PositionedSoundInstance(
                                bounceSound, SoundCategory.NEUTRAL, 0.6f, finalPitch,
                                animatable.getRandom(), animatable.getX(), animatable.getY(), animatable.getZ()
                        ));
                    }
                    break;
            }
            // Reset the flag
            animatable.soundEffectId = null;
        }
    }
}