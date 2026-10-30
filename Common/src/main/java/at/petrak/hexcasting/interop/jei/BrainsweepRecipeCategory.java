package at.petrak.hexcasting.interop.jei;

import at.petrak.hexcasting.common.recipe.BrainsweepRecipe;
import at.petrak.hexcasting.interop.patchouli.PatchouliUtils;
import at.petrak.hexcasting.interop.utils.EntityDrawingUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class BrainsweepRecipeCategory implements IRecipeCategory<BrainsweepRecipe> {
    public static final Identifier UID = modLoc("brainsweep");

    private final IDrawableStatic background;
    private final IDrawable icon;
    private final Component localizedName;

    public BrainsweepRecipeCategory(IGuiHelper guiHelper) {
        Identifier location = modLoc("textures/gui/brainsweep_recipe.png");
        background = guiHelper.drawableBuilder(location, 0, 0, 118, 86).setTextureSize(128, 128).build();
        var brainsweep = modLoc("brainsweep");
        localizedName = Component.translatable("hexcasting.action." + brainsweep);
        icon = new PatternDrawable(brainsweep, 16, 16);
    }

    @Override
    public @NotNull
    Component getTitle() {
        return localizedName;
    }

    @Override
    public int getWidth() {
        return 118;
    }

    @Override
    public int getHeight() {
        return 86;
    }

    @Override
    public @NotNull
    IDrawable getIcon() {
        return icon;
    }

    @Override
    public void getTooltip(@NotNull ITooltipBuilder tooltip, @NotNull BrainsweepRecipe recipe,
        @NotNull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (37 <= mouseX && mouseX <= 37 + 26 && 19 <= mouseY && mouseY <= 19 + 48) {
            Minecraft mc = Minecraft.getInstance();
            tooltip.addAll(recipe.entityIn().getTooltip(mc.options.advancedItemTooltips));
        }
    }

    @Override
    public void draw(@NotNull BrainsweepRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            var examples = recipe.entityIn().exampleEntities(level);
            Entity example = null;
            if (!examples.isEmpty()) {
                long seconds = System.currentTimeMillis() / 1000;
                example = examples.get((int) (seconds % examples.size()));
            }
            if (example == null)
                return;

            EntityDrawingUtil.drawEntity(graphics, example, 50, 62.5f, 20f);
        }
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull BrainsweepRecipe recipe,
        @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 35)
            .addItemStacks(recipe.blockIn().getDisplayedStacks());
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 12, 55)
            .addItemStacks(PatchouliUtils.mediaItems(recipe.mediaCost()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 87, 35)
            .addItemStack(new ItemStack(recipe.result().getBlock()));
    }

    @Override
    public @NotNull
    RecipeType<BrainsweepRecipe> getRecipeType() {
        return HexJEIPlugin.BRAINSWEEPING;
    }
}
