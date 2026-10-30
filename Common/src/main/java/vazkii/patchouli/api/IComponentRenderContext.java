package vazkii.patchouli.api;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Stub of patchouli's IComponentRenderContext.
 */
public interface IComponentRenderContext {
    boolean isAreaHovered(int mouseX, int mouseY, int x, int y, int width, int height);

    void setHoverTooltipComponents(List<Component> components);
}
