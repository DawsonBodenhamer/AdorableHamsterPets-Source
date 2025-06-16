package net.dawson.adorablehamsterpets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdorableHamsterPets {
    public static final String MOD_ID = "adorablehamsterpets";

    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        // Write common init code here.
        LOGGER.info("Initializing Adorable Hamster Pets");
    }
}
