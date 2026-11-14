package at.petrak.hexcasting.common.misc;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

/**
 * Used for displaying patterns on the tooltips for scrolls and slates.
 *
 * @see at.petrak.hexcasting.client.gui.PatternTooltipComponent the client-side renderer for this
 */
public record PatternTooltip(HexPattern pattern, Identifier background) implements TooltipComponent {
    // Kept here instead of on the client renderer so common item code never touches a client class.
    public static final Identifier PRISTINE_BG = modLoc("textures/gui/scroll.png");
    public static final Identifier ANCIENT_BG = modLoc("textures/gui/scroll_ancient.png");
    public static final Identifier SLATE_BG = modLoc("textures/gui/slate.png");
}
