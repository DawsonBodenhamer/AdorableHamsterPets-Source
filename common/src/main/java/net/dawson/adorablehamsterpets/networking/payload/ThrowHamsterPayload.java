package net.dawson.adorablehamsterpets.networking.payload;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import net.dawson.adorablehamsterpets.AdorableHamsterPets; // Import main mod class
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier; // Import Identifier

public record ThrowHamsterPayload() implements CustomPayload {
    public static final CustomPayload.Id<ThrowHamsterPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "throw_hamster"));
    public static final PacketCodec<RegistryByteBuf, ThrowHamsterPayload> CODEC = PacketCodec.unit(new ThrowHamsterPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}