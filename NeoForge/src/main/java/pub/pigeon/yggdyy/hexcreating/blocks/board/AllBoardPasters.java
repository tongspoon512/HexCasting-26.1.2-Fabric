package pub.pigeon.yggdyy.hexcreating.blocks.board;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.common.lib.HexItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a held "reference" (a printed paper produced by the board, or a slate) into the sequence of
 * squares that should be laid down on the board.
 */
public final class AllBoardPasters {
    public interface BoardPaster {
        boolean canHandle(ItemStack stack);

        List<SquareMatcher> getResult(ItemStack stack, ServerLevel level);
    }

    /**
     * One board slot to fill: any of {@code candidates} may be punched out of a container, and the
     * square is replaced with {@code result} (the exact pattern that belongs in that slot).
     */
    public static final class SquareMatcher {
        private final List<ItemStack> candidates;
        private final ItemStack result;

        public SquareMatcher(ItemStack result) {
            this.candidates = new ArrayList<>();
            this.result = result;
        }

        public SquareMatcher accept(ItemStack stack) {
            candidates.add(stack);
            return this;
        }

        public boolean matches(ItemStack stack) {
            for (ItemStack candidate : candidates) {
                if (ItemStack.isSameItemSameComponents(candidate, stack)) {
                    return true;
                }
            }
            return false;
        }

        public ItemStack result() {
            return result.copy();
        }
    }

    private static final List<BoardPaster> PASTERS = new ArrayList<>();

    static {
        // Printed paper: one square per pattern in the list.
        PASTERS.add(new BoardPaster() {
            @Override
            public boolean canHandle(ItemStack stack) {
                return stack.is(HexCreatingItems.PRINTED_PAPER) && stack.getItem() instanceof IotaHolderItem holder
                    && holder.readIotaTag(stack) != null;
            }

            @Override
            public List<SquareMatcher> getResult(ItemStack stack, ServerLevel level) {
                List<SquareMatcher> out = new ArrayList<>();
                if (!(stack.getItem() instanceof IotaHolderItem holder)) {
                    return out;
                }
                Iota iota = holder.readIota(stack, level);
                if (!(iota instanceof ListIota list)) {
                    return out;
                }
                for (Iota element : list.getList()) {
                    if (element instanceof PatternIota pattern) {
                        out.add(matcherFor(pattern.getPattern()));
                    }
                }
                return out;
            }
        });
        // Slate: a single pattern.
        PASTERS.add(new BoardPaster() {
            @Override
            public boolean canHandle(ItemStack stack) {
                return stack.is(HexItems.SLATE);
            }

            @Override
            public List<SquareMatcher> getResult(ItemStack stack, ServerLevel level) {
                Iota iota = HexItems.SLATE.readIota(stack, level);
                if (!(iota instanceof PatternIota pattern)) {
                    return List.of();
                }
                return List.of(matcherFor(pattern.getPattern()));
            }
        });
    }

    private AllBoardPasters() {
    }

    /**
     * Accepts the exact square, any rotation of the same stroke, or an empty square (which gets
     * etched into the required pattern afterwards).
     */
    private static SquareMatcher matcherFor(HexPattern pattern) {
        ItemStack result = BoardBlock.iotaToSquare(new PatternIota(pattern));
        SquareMatcher matcher = new SquareMatcher(result);
        String stroke = pattern.anglesSignature();
        for (HexDir dir : HexDir.values()) {
            matcher.accept(BoardBlock.iotaToSquare(new PatternIota(HexPattern.fromAngles(stroke, dir))));
        }
        matcher.accept(new ItemStack(HexCreatingItems.SQUARE));
        return matcher;
    }

    public static List<BoardPaster> all() {
        return PASTERS;
    }
}
