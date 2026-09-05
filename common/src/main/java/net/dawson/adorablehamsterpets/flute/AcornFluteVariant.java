package net.dawson.adorablehamsterpets.flute;

import java.util.random.RandomGenerator;

public enum AcornFluteVariant {
    LUSH(0x8AF2C9, 0x7AD1C5, 0xBC9AFC, 0x6B68D0),
    EMBER(0xD43C21, 0xF0814B, 0xECA65C, 0xF9DF56),
    HARMONY(
            0xD43C21,
            0xF0814B,
            0xECA65C,
            0xF9DF56,
            0x8BD486,
            0x4FC38E,
            0x8AF2C9,
            0x7AD1C5,
            0xBC9AFC,
            0x6B68D0,
            0x4C5397);

    private final int[] palette;

    AcornFluteVariant(int... palette) {
        this.palette = palette;
    }

    public int randomColor(RandomGenerator random) {
        return this.palette[random.nextInt(this.palette.length)];
    }

    public int[] palette() {
        return this.palette.clone();
    }
}
