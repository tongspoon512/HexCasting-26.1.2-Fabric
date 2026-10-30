package dev.emi.emi.api.widget;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;

/**
 * Stub of emi's SlotWidget.
 */
public class SlotWidget extends Widget {
    protected final EmiIngredient stack;
    protected int x, y;

    protected boolean drawBack = true;
    protected Identifier textureId;
    protected int u, v;
    protected boolean output;
    protected boolean catalyst;

    public SlotWidget(EmiIngredient stack, int x, int y) {
        this.stack = stack;
        this.x = x;
        this.y = y;
    }

    public EmiStack getStack() {
        var stacks = this.stack.getEmiStacks();
        return stacks.isEmpty() ? new EmiStack() : stacks.getFirst();
    }

    public Bounds getBounds() {
        return new Bounds(this.x, this.y, 18, 18);
    }

    public SlotWidget drawBack(boolean draw) {
        this.drawBack = draw;
        return this;
    }

    public SlotWidget customBackground(Object texture, int u, int v, int width, int height) {
        if (texture instanceof Identifier id) {
            this.textureId = id;
        }
        this.u = u;
        this.v = v;
        return this;
    }

    public SlotWidget large(boolean large) {
        this.output = large;
        return this;
    }

    public SlotWidget recipeContext(EmiRecipe recipe) {
        return this;
    }

    public SlotWidget useOffset(boolean offset) {
        return this;
    }

    public SlotWidget customShift(float x, float y) {
        return this;
    }
}
