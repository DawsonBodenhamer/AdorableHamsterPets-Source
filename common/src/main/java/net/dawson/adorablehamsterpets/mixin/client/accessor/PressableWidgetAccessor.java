package net.dawson.adorablehamsterpets.mixin.client.accessor;

import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PressableWidget.class)
public interface PressableWidgetAccessor {
    @Accessor("height")
    void adorablehamsterpets$setHeight(int height);
}