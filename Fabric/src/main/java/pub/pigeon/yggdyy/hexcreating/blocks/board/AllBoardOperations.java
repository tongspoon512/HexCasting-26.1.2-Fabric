package pub.pigeon.yggdyy.hexcreating.blocks.board;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;
import pub.pigeon.yggdyy.hexcreating.items.PrintedPaperItem;

import java.util.ArrayList;
import java.util.List;

/**
 * The redstone-driven typesetting operations of the board. The signal level decides what a click
 * does, mirroring the upstream behaviour:
 * <ul>
 *   <li>0 - put / take a single square</li>
 *   <li>1 - take the whole connected run</li>
 *   <li>2 / 3 - shift the run left / right</li>
 *   <li>4 / 5 - shift the run up / down</li>
 *   <li>6..14 - put only</li>
 *   <li>15 - static, copying to a paper reel still works</li>
 * </ul>
 * Holding a paper reel copies the run into a printed paper; holding a printed paper or a slate
 * pastes its patterns back onto the board.
 */
public final class AllBoardOperations {
    private AllBoardOperations() {
    }

    public static boolean operate(ServerLevel level, BlockPos pos, BoardBlockEntity board,
        int slot, ItemStack target, BoardConnected connected, int condition, Player player, ItemStack held) {
        boolean emptyHand = held.isEmpty();
        int index = connected.getIndex(pos, slot);
        if (index < 0) {
            return false;
        }

        // Take one square.
        if (condition == 0 && emptyHand && !target.isEmpty()) {
            giveOrDrop(player, board.removeSquare(slot));
            return true;
        }
        // Take the whole run.
        if (condition == 1 && emptyHand && !target.isEmpty()) {
            BoardConnected.Section section = connected.getLeftAndRight(index);
            if (section != null) {
                for (int i = section.l; i <= section.r; i++) {
                    BoardConnected.SlotHandler handler = connected.getSlotHandler(i);
                    if (handler == null || handler.getStack().isEmpty()) {
                        continue;
                    }
                    giveOrDrop(player, handler.getStack().copy());
                    handler.setStack(ItemStack.EMPTY);
                }
                return true;
            }
        }
        // Shift left / right.
        if (condition == 2 && emptyHand && !target.isEmpty()) {
            BoardConnected.Section section = connected.getLeft(index);
            if (section != null) {
                section.moveLeft();
                return true;
            }
        }
        if (condition == 3 && emptyHand && !target.isEmpty()) {
            BoardConnected.Section section = connected.getRight(index);
            if (section != null) {
                section.moveRight();
                return true;
            }
        }
        // Shift up / down: keep moving until the run leaves its current row.
        if (condition == 4 && emptyHand && !target.isEmpty()) {
            BoardConnected.Section section = connected.getLeft(index);
            if (section != null) {
                int row = section.rightSlotRow();
                while (section.canMoveLeft() && connected.getSlotHandler(section.r).slot / 4 == row) {
                    section.moveLeft();
                }
                return true;
            }
        }
        if (condition == 5 && emptyHand && !target.isEmpty()) {
            BoardConnected.Section section = connected.getRight(index);
            if (section != null) {
                int row = section.leftSlotRow();
                while (section.canMoveRight() && connected.getSlotHandler(section.l).slot / 4 == row) {
                    section.moveRight();
                }
                return true;
            }
        }
        // Copy the run into a printed paper.
        if (held.is(HexCreatingItems.PAPER_REEL) && !target.isEmpty()) {
            BoardConnected.Section section = connected.getLeftAndRight(index);
            if (section != null) {
                List<at.petrak.hexcasting.api.casting.iota.Iota> patterns = section.getPatterns();
                if (!patterns.isEmpty()
                    && held.getMaxDamage() - held.getDamageValue() >= patterns.size()) {
                    held.setDamageValue(held.getDamageValue() + patterns.size());
                    ItemStack copied = new ItemStack(HexCreatingItems.PRINTED_PAPER);
                    if (copied.getItem() instanceof PrintedPaperItem paper) {
                        paper.writeDatum(copied,
                            new at.petrak.hexcasting.api.casting.iota.ListIota(patterns));
                    }
                    giveOrDrop(player, copied);
                    return true;
                }
            }
        }
        // Put one square.
        if (condition <= 14 && target.isEmpty() && held.is(HexCreatingItems.SQUARE)) {
            board.setSquare(slot, held.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return true;
        }
        // Paste a printed paper / slate, taking squares from the player or the container behind.
        if (condition != 15 && !emptyHand) {
            for (AllBoardPasters.BoardPaster paster : AllBoardPasters.all()) {
                if (!paster.canHandle(held)) {
                    continue;
                }
                List<AllBoardPasters.SquareMatcher> matchers = paster.getResult(held, level);
                if (matchers.isEmpty()) {
                    return true;
                }
                List<Container> sources = new ArrayList<>(connected.containersBehind());
                sources.add(player.getInventory());
                for (int step = 0; step < matchers.size(); step++) {
                    AllBoardPasters.SquareMatcher matcher = matchers.get(step);
                    BoardConnected.SlotHandler handler = connected.getSlotHandler(index + step);
                    if (handler == null || !handler.getStack().isEmpty() && !matcher.matches(handler.getStack())) {
                        player.sendSystemMessage(Component.translatable("hexcreating.board.paster.no_space", step));
                        break;
                    }
                    if (matcher.matches(handler.getStack())) {
                        continue;
                    }
                    if (!takeMatching(sources, matcher)) {
                        player.sendSystemMessage(Component.translatable("hexcreating.board.paster.no_item", step));
                        break;
                    }
                    handler.setStack(matcher.result());
                }
                return true;
            }
        }
        return false;
    }

    private static boolean takeMatching(List<Container> sources, AllBoardPasters.SquareMatcher matcher) {
        for (Container container : sources) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && matcher.matches(stack)) {
                    container.removeItem(i, 1);
                    return true;
                }
            }
        }
        return false;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
