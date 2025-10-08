package net.dawson.adorablehamsterpets.networking.payload;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 *
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateHamsterRenderStatePayload(int hamsterEntityId, boolean isRendering) implements CustomPayload {
    public static final CustomPayload.Id<UpdateHamsterRenderStatePayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "update_hamster_render_state"));

    public static final PacketCodec<RegistryByteBuf, UpdateHamsterRenderStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, UpdateHamsterRenderStatePayload::hamsterEntityId,
            PacketCodecs.BOOL, UpdateHamsterRenderStatePayload::isRendering,
            UpdateHamsterRenderStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}