package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ShowGuidebookWarningPayload() implements CustomPayload {
    public static final CustomPayload.Id<ShowGuidebookWarningPayload> ID =
            new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "show_guidebook_warning"));
    public static final PacketCodec<RegistryByteBuf, ShowGuidebookWarningPayload> CODEC =
            PacketCodec.unit(new ShowGuidebookWarningPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
