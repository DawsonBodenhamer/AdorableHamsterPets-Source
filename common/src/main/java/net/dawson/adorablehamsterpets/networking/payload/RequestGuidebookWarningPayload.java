package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestGuidebookWarningPayload() implements CustomPayload {
    public static final CustomPayload.Id<RequestGuidebookWarningPayload> ID =
            new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "request_guidebook_warning"));
    public static final PacketCodec<RegistryByteBuf, RequestGuidebookWarningPayload> CODEC =
            PacketCodec.unit(new RequestGuidebookWarningPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
