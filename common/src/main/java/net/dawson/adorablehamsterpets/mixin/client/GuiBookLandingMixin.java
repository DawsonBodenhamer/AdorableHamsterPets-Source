package net.dawson.adorablehamsterpets.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookLanding;
import vazkii.patchouli.common.book.Book;

@Mixin(value = GuiBookLanding.class, remap = false)
public abstract class GuiBookLandingMixin extends GuiBook {

    // This constructor is required by the compiler because GuiBook has a constructor.
    public GuiBookLandingMixin(Book book, Text title) {
        super(book, title);
    }

    /**
     * A simple helper method to check if the currently rendered book is the
     * Adorable Hamster Pets guide book. All mixin modifications will be gated
     * behind this check to prevent them from affecting other mods' books.
     *
     * @return True if the current book is the hamster guide, false otherwise.
     */
    private boolean isHamsterBook() {
        // The 'book' field is inherited from the parent GuiBook class.
        return this.book != null && this.book.id.equals(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
    }

    /**
     * Injects at the beginning of the drawHeader method to completely replace its rendering logic
     * for the Adorable Hamster Pets guide book. This allows for custom positioning and text wrapping
     * of the title and subtitle without affecting other Patchouli books.
     *
     * @param graphics The DrawContext for rendering.
     * @param ci       The CallbackInfo, used to cancel the original method.
     */
    @Inject(method = "drawHeader", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$onDrawHeader(DrawContext graphics, CallbackInfo ci) {
        // --- SAFETY CHECK ---
        // If this is not the Hamster Tips guide book, do nothing and let the original method run.
        if (!isHamsterBook()) {
            return;
        }

        // --- 1. Set Render Color ---
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // --- 2. Render the Header Ribbon Texture ---
        // We use a taller texture to fit the wrapped subtitle.
        int ribbonX = -8;
        int ribbonY = 12;
        int ribbonU = 0;
        int ribbonV = 180;
        int ribbonWidth = 140;
        int ribbonHeight = 43; // MODIFIED: Was 31
        drawFromTexture(graphics, this.book, ribbonX, ribbonY, ribbonU, ribbonV, ribbonWidth, ribbonHeight);

        // --- 3. Render the Main Book Title ---
        int titleColor = this.book.nameplateColor;
        Text titleText = this.book.getBookItem().getName();
        int titleX = 13 + 7; // MODIFIED: Shifted right by 7
        int titleY = 16;
        graphics.drawText(this.textRenderer, titleText, titleX, titleY, titleColor, false);

        // --- 4. Render the Wrapped Subtitle ---
        Text subtitleText = this.book.getSubtitle().fillStyle(this.book.getFontStyle());
        int subtitleX = 24 - 5; // MODIFIED: Shifted left by 5
        int subtitleY = 24 + 2; // MODIFIED: Shifted down by 2
        int wrapWidth = 100;    // The available width for the text
        graphics.drawTextWrapped(this.textRenderer, subtitleText, subtitleX, subtitleY, wrapWidth, titleColor);

        // --- 5. Cancel the Original Method ---
        ci.cancel();
    }
}