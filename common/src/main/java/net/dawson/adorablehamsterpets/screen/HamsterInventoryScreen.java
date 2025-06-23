package net.dawson.adorablehamsterpets.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class HamsterInventoryScreen extends HandledScreen<HamsterInventoryScreenHandler> {
    // Path to the background texture for the GUI
    private static final Identifier TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/gui/hamster_inventory_gui.png");

    // Store the player entity for easy access
    private final PlayerEntity player;

    public HamsterInventoryScreen(HamsterInventoryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.player = inventory.player; // Store the player

        // Adjust background height based on texture file
        this.backgroundHeight = 222; // Needs to match texture height

        // Adjust player inventory label Y position based on the new backgroundHeight and layout
        this.playerInventoryTitleY = 139 - 11; // Position it just above the player inventory (Y=139 - approx text height)
    }

    @Override
    protected void init() {
        super.init();
        // Restore default title centering (or adjust as needed for your specific texture)
        this.titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        this.titleY = 6; // Default Y position near the top

        // Set player inventory title position explicitly based on your layout
        this.playerInventoryTitleX = 7;
        // this.playerInventoryTitleY is set in the constructor
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2; // Centered X
        int y = (this.height - this.backgroundHeight) / 2; // Centered Y
        // Draw the background texture
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the background and slots
        super.render(context, mouseX, mouseY, delta);

        // --- Draw the Hamster Entity ---
        int boxX = this.x + 8;
        int boxY = this.y + 12;
        int boxWidth = 59 - 8;
        int boxHeight = 69 - 18;
        int size = 60;

        // Get the entity instance directly from the handler
        HamsterEntity hamster = this.handler.getHamsterEntity();

        if (hamster != null) {
            // Call the static helper method using the box coordinates
            InventoryScreen.drawEntity(
                    context,
                    boxX,
                    boxY,
                    boxX + boxWidth,
                    boxY + boxHeight,
                    size,
                    0.0625F,
                    (float)mouseX,
                    (float)mouseY,
                    hamster // Pass the entity instance
            );
        }

        // Draw tooltips last
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Restore drawing the screen title using default positioning fields
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);

        // Draw the player inventory title (remains standard)
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);

        // --- "Left Cheek" and "Right Cheek" Text ---
        String customTextLeft = "Left Cheek";
        int customTextLeftX = 25;
        int customTextLeftY = 80;
        context.drawText(this.textRenderer, customTextLeft, customTextLeftX, customTextLeftY, 4210752, false);

        String customTextRight = "Right Cheek";
        int customTextRightX = 95;
        int customTextRightY = 80;
        context.drawText(this.textRenderer, customTextRight, customTextRightX, customTextRightY, 4210752, false);
    }
}