package net.dawson.adorablehamsterpets.entity.client.renderer;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * A specialized renderer for the shoulder-mounted hamster.
 * It extends the base HamsterRenderer but overrides methods to suppress
 * sounds, particles, and other world-interactive effects that are not
 * needed for a purely cosmetic render.
 */
public class ShoulderHamsterRenderer extends HamsterRenderer {

    public ShoulderHamsterRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    /**
     * Overrides the main render method to bypass logic that is not relevant
     * for a shoulder-mounted entity, such as cleaning sounds and snow offset.
     */
    @Override
    public void render(HamsterEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                       VertexConsumerProvider bufferSource, int packedLight) {
        this.shadowRadius = entity.isBaby() ? 0.1F : 0.2F;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * Overrides the final render step to prevent keyframe-triggered particles
     * and sounds from being processed for the shoulder model.
     */
    @Override
    public void renderFinal(MatrixStack poseStack, HamsterEntity animatable, BakedGeoModel model,
                            VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer,
                            float partialTick, int packedLight, int packedOverlay, int colour) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);
    }
}