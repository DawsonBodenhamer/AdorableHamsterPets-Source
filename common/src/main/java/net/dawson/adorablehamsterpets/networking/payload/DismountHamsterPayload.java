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
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DismountHamsterPayload() implements CustomPayload {
    public static final CustomPayload.Id<DismountHamsterPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "dismount_hamster"));
    public static final PacketCodec<RegistryByteBuf, DismountHamsterPayload> CODEC = PacketCodec.unit(new DismountHamsterPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}