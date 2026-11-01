package at.petrak.hexcasting.api.casting.iota;

import at.petrak.hexcasting.api.casting.SpellList;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.api.utils.TreeList;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import at.petrak.hexcasting.api.mod.HexConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.lang.Math.max;

/**
 * This is a <i>wrapper</i> for {@link SpellList}.
 */
public class ListIota extends Iota {
    private final int depth;
    private final int size;

    public ListIota(@NotNull SpellList list) {
        super(HexIotaTypes.LIST, list);
        int maxChildDepth = 0;
        int totalSize = 1;
        for (Iota iota : list) {
            totalSize += iota.size();
            maxChildDepth = max(maxChildDepth, iota.depth());
        }
        depth = maxChildDepth + 1;
        size = totalSize;
    }

    public ListIota(@NotNull TreeList<Iota> list) {
        this(new SpellList.LList(list));
    }

    public ListIota(@NotNull List<Iota> list) {
        this(new SpellList.LList(list));
    }

    public SpellList getList() {
        return (SpellList) this.payload;
    }

    @Override
    public boolean isTruthy() {
        return this.getList().getNonEmpty();
    }

    @Override
    public boolean toleratesOther(Iota that) {
        if (!typesMatch(this, that)) {
            return false;
        }
        var a = this.getList();
        if (!(that instanceof ListIota list)) {
            return false;
        }
        var b = list.getList();

        SpellList.SpellListIterator aIter = a.iterator(), bIter = b.iterator();
        for (; ; ) {
            if (!aIter.hasNext() && !bIter.hasNext()) {
                // we ran out together!
                return true;
            }
            if (aIter.hasNext() != bIter.hasNext()) {
                // one remains full before the other
                return false;
            }
            Iota x = aIter.next(), y = bIter.next();
            if (!Iota.tolerates(x, y)) {
                return false;
            }
        }
    }

    @Override
    public @NotNull Tag serialize() {
        var out = new ListTag();
        for (var subdatum : this.getList()) {
            out.add(IotaType.serialize(subdatum));
        }
        return out;
    }

    @Override
    public @Nullable Iterable<Iota> subIotas() {
        return this.getList();
    }

    @Override
    protected Iota visitChildren(UnaryOperator<Iota> visitor) {
        var out = new ArrayList<Iota>();
        boolean conserve = true;
        for (Iota orig : getList()) {
            var replacement = orig.visit(visitor);
            conserve &= replacement == orig;
            out.add(replacement);
        }
        return conserve ? this : new ListIota(out);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int depth() {
        return depth;
    }

    private static int widthOf(@Nullable Font font, Component component) {
        return font == null ? 0 : font.width(component);
    }

    public static IotaType<ListIota> TYPE = new IotaType<>() {
        @Nullable
        @Override
        public ListIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            var listTag = HexUtils.downcast(tag, ListTag.TYPE);
            var out = new ArrayList<Iota>(listTag.size());

            for (var sub : listTag) {
                var csub = HexUtils.downcast(sub, CompoundTag.TYPE);
                var subiota = IotaType.deserialize(csub, world);
                if (subiota == null) {
                    return null;
                }
                out.add(subiota);
            }

            return new ListIota(out);
        }

        @Override
        public Component display(Tag tag) {
            var out = Component.empty();
            for (var line : displayLines(tag, null, 0)) {
                out.append(line);
            }
            return out;
        }

        @Override
        public List<Component> displayLines(Tag tag, @Nullable Font font, int maxWidth) {
            var list = HexUtils.downcast(tag, ListTag.TYPE);
            var chunks = new ArrayList<Component>();
            var chunk = Component.empty();
            var chunkWidth = 0;
            var alwaysShowCommas = HexConfig.client() != null && HexConfig.client().alwaysShowListCommas();
            for (int i = 0; i < list.size(); i++) {
                var csub = HexUtils.downcast(list.get(i), CompoundTag.TYPE);
                var sub = IotaType.getDisplay(csub);

                // only add a comma between 2 non-patterns (commas don't look good with Inline patterns)
                var needsComma = false;
                if (i < list.size() - 1) {
                    var thisType = IotaType.getTypeFromTag(csub);
                    var nextType = IotaType.getTypeFromTag(HexUtils.downcast(list.get(i+1), CompoundTag.TYPE));
                    var thisIotaNeedsComma = thisType == null || thisType.usesListCommas();
                    var nextIotaNeedsComma = nextType == null || nextType.usesListCommas();
                    needsComma = thisIotaNeedsComma || nextIotaNeedsComma || alwaysShowCommas;
                }

                // wrap to a new line when this iota would push the line past the width budget
                var subWidth = widthOf(font, sub);
                var commaWidth = needsComma && font != null ? font.width(", ") : 0;
                if (maxWidth > 0 && chunkWidth > 0 && chunkWidth + subWidth + commaWidth > maxWidth) {
                    chunks.add(chunk);
                    chunk = Component.empty();
                    chunkWidth = 0;
                }

                chunk.append(sub);
                if (needsComma) {
                    chunk.append(", ");
                }
                chunkWidth += subWidth + commaWidth;
            }
            chunks.add(chunk);

            var out = new ArrayList<Component>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                var line = Component.empty();
                if (i == 0) {
                    line.append(Component.literal("["));
                }
                line.append(chunks.get(i));
                if (i == chunks.size() - 1) {
                    line.append(Component.literal("]"));
                }
                out.add(line.withStyle(ChatFormatting.DARK_PURPLE));
            }
            return out;
        }

        @Override
        public int color() {
            return 0xff_aa00aa;
        }
    };
}
