package at.petrak.hexcasting.interop.emi;

import at.petrak.hexcasting.common.recipe.ingredient.brainsweep.BrainsweepeeIngredient;
import at.petrak.hexcasting.interop.utils.EntityDrawingUtil;
import dev.emi.emi.api.stack.EmiStack;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.world.entity.Entity;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class BrainsweepeeEmiStack extends EmiStack {
    public final BrainsweepeeIngredient ingredient;
    private final Identifier id;

    public BrainsweepeeEmiStack(BrainsweepeeIngredient ingr) {
        this.ingredient = ingr;

        var bareId = this.ingredient.getSomeKindOfReasonableIDForEmi();
        this.id = modLoc(bareId);
    }

    @Override
    public EmiStack copy() {
        return new BrainsweepeeEmiStack(this.ingredient);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public CompoundTag getNbt() {
        return null;
    }

    @Override
    public Object getKey() {
        return id;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public List<Component> getTooltipText() {
        Minecraft mc = Minecraft.getInstance();
        boolean advanced = mc.options.advancedItemTooltips;

        return ingredient.getTooltip(advanced);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        return getTooltipText().stream()
            .map(Component::getVisualOrderText)
            .map(ClientTooltipComponent::create)
            .collect(Collectors.toList());
    }

    @Override
    public Component getName() {
        return ingredient.getName();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int x, int y, float delta, int flags) {
        if ((flags & RENDER_ICON) != 0) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level != null) {
                var examples = this.ingredient.exampleEntities(level);
                Entity example = null;
                if (!examples.isEmpty()) {
                    long seconds = System.currentTimeMillis() / 1000;
                    example = examples.get((int) (seconds % examples.size()));
                }

                EntityDrawingUtil.drawEntity(graphics, example, x + 8, y + 16, 8f);
            }
        }

//		if ((flags & RENDER_REMAINDER) != 0) {
//			EmiRender.renderRemainderIcon(this, poseStack, x, y);
//		}
    }
}
