package vazkii.patchouli.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.UnaryOperator;

/**
 * Stub of patchouli's ICustomComponent.
 */
public interface ICustomComponent {
    void build(int componentX, int componentY, int pageNum);

    void render(GuiGraphicsExtractor graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY);

    void onVariablesAvailable(UnaryOperator<IVariable> lookup);
}
