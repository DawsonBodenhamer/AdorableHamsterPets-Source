package net.dawson.adorablehamsterpets.client.sound;

import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.util.DistantSoundUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

/**
 * Plays one server-positioned distant sound with long-range client-side falloff.
 * A source entity, when supplied, updates the sound origin as it moves.
 */
public final class DistantSoundInstance extends MovingSoundInstance {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                              Instance State
     * ────────────────────────────────────────────────────────────────────────────*/

    private final Vec3d fallbackSourcePosition;
    private final int sourceEntityId;
    private final float baseVolume;
    private final float audioRange;

    public DistantSoundInstance(ModPackets.PlayDistantSoundS2CPacket packet) {
        super(
                SoundEvent.of(packet.soundId()),
                packet.category(),
                SoundInstance.createRandom()
        );
        this.repeat = false;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.baseVolume = packet.volume();
        this.audioRange = packet.audioRange();
        this.volume = this.baseVolume;
        this.pitch = packet.pitch();

        this.fallbackSourcePosition = packet.position().orElse(Vec3d.ZERO);
        this.sourceEntityId = packet.sourceEntityId();
        this.x = this.fallbackSourcePosition.x;
        this.y = this.fallbackSourcePosition.y;
        this.z = this.fallbackSourcePosition.z;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Lifecycle API
     * ────────────────────────────────────────────────────────────────────────────*/

    void markDone() {
        this.setDone();
    }

    @Override
    public void tick() {
        if (this.audioRange <= 0.0F) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            this.volume = 0.0F;
            return;
        }

        Vec3d sourcePosition = this.fallbackSourcePosition;
        if (this.sourceEntityId > 0 && client.world != null) {
            Entity sourceEntity = client.world.getEntityById(this.sourceEntityId);
            if (sourceEntity != null) {
                sourcePosition = sourceEntity.getPos();
            }
        }

        this.x = sourcePosition.x;
        this.y = sourcePosition.y;
        this.z = sourcePosition.z;
        double distance = client.player.getPos().distanceTo(sourcePosition);
        this.volume = this.baseVolume * DistantSoundUtil.calculateVolumeFalloff(distance, this.audioRange);
    }
}
