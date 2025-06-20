package net.dawson.adorablehamsterpets.client.option;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {

    public static final String KEY_CATEGORY_HAMSTERPETS = "key.category.adorablehamsterpets.main";
    public static final String KEY_THROW_HAMSTER = "key.adorablehamsterpets.throw_hamster";

    public static KeyBinding THROW_HAMSTER_KEY;

    public static void registerKeyInputs() {
        // Define the key binding object
        THROW_HAMSTER_KEY = new KeyBinding(
                KEY_THROW_HAMSTER,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KEY_CATEGORY_HAMSTERPETS
        );

        // Register it with Architectury's registry
        KeyMappingRegistry.register(THROW_HAMSTER_KEY);
    }
}