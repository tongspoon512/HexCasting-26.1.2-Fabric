package pub.pigeon.yggdyy.hexcreating.create;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.common.blocks.akashic.BlockEntityAkashicBookshelf;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import at.petrak.hexcasting.common.lib.HexBlocks;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.zurrtum.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.zurrtum.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;

/**
 * Create display-link sources exposing HexCasting machine state.
 */
public final class HexCreatingDisplaySources {
    public static final DisplaySource IMPETUS_MEDIA = register("impetus_media_display_source", new ImpetusMedia());
    public static final DisplaySource AKASHIC_PATTERN =
        register("akashic_bookshelf_pattern_display_source", new AkashicPattern());
    public static final DisplaySource AKASHIC_IOTA =
        register("akashic_bookshelf_iota_display_source", new AkashicIota());
    public static final DisplaySource SLATE_PATTERN =
        register("slate_pattern_display_source", new SlatePattern());

    private HexCreatingDisplaySources() {
    }

    private static DisplaySource register(String name, DisplaySource source) {
        return Registry.register(CreateRegistries.DISPLAY_SOURCE, HexCreatingCompat.id(name), source);
    }

    public static void register() {
        DisplaySource.BY_BLOCK.add(HexBlocks.IMPETUS_RIGHTCLICK, IMPETUS_MEDIA);
        DisplaySource.BY_BLOCK.add(HexBlocks.IMPETUS_REDSTONE, IMPETUS_MEDIA);
        DisplaySource.BY_BLOCK.add(HexBlocks.IMPETUS_LOOK, IMPETUS_MEDIA);
        DisplaySource.BY_BLOCK.add(HexBlocks.AKASHIC_BOOKSHELF, AKASHIC_PATTERN);
        DisplaySource.BY_BLOCK.add(HexBlocks.AKASHIC_BOOKSHELF, AKASHIC_IOTA);
        DisplaySource.BY_BLOCK.add(HexBlocks.SLATE, SLATE_PATTERN);
    }

    private static final class ImpetusMedia extends NumericSingleLineDisplaySource {
        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            if (context.getSourceBlockEntity() instanceof BlockEntityAbstractImpetus impetus) {
                return Component.literal(Long.toString(impetus.getMedia()));
            }
            return Component.empty();
        }

        @Override
        public boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }
    }

    private static final class AkashicPattern extends SingleLineDisplaySource {
        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            if (context.getSourceBlockEntity() instanceof BlockEntityAkashicBookshelf shelf) {
                var pattern = shelf.getPattern();
                if (pattern == null) {
                    return Component.empty();
                }
                return Component.literal(pattern.toString());
            }
            return Component.empty();
        }

        @Override
        public boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }
    }

    private static final class AkashicIota extends SingleLineDisplaySource {
        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            if (context.getSourceBlockEntity() instanceof BlockEntityAkashicBookshelf shelf
                && shelf.getIotaTag() != null
                && context.level() instanceof ServerLevel serverLevel) {
                return IotaType.deserialize(shelf.getIotaTag(), serverLevel).display().copy();
            }
            return Component.empty();
        }

        @Override
        public boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }
    }

    private static final class SlatePattern extends SingleLineDisplaySource {
        @Override
        protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            if (context.getSourceBlockEntity() instanceof BlockEntitySlate slate && slate.pattern != null) {
                return Component.literal(slate.pattern.toString());
            }
            return Component.empty();
        }

        @Override
        public boolean allowsLabeling(DisplayLinkContext context) {
            return true;
        }
    }
}
