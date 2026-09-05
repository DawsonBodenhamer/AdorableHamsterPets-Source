package net.dawson.adorablehamsterpets.flute;

/**
 * Immutable per-tick note-emission envelope for one Acorn Flute sound.
 */
public record AcornFluteNoteProfile(
        String key,
        String sourceSha256,
        int durationTicks,
        double peakRms,
        int[] intensities) {

    public AcornFluteNoteProfile {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Profile key must not be blank");
        }
        if (sourceSha256 == null || sourceSha256.isBlank()) {
            throw new IllegalArgumentException("Profile source hash must not be blank");
        }
        if (durationTicks <= 0 || !Double.isFinite(peakRms)) {
            throw new IllegalArgumentException("Profile metadata is invalid");
        }
        if (intensities == null || intensities.length != durationTicks) {
            throw new IllegalArgumentException("Profile duration does not match its intensity count");
        }
        intensities = intensities.clone();
    }

    @Override
    public int[] intensities() {
        return this.intensities.clone();
    }

    public double normalizedIntensityAt(long elapsedTick) {
        if (elapsedTick < 0L || elapsedTick >= this.intensities.length) {
            return 0.0D;
        }
        return this.intensities[(int) elapsedTick] / 255.0D;
    }
}
