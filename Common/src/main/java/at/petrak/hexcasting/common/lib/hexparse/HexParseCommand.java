package at.petrak.hexcasting.common.lib.hexparse;

import at.petrak.hexcasting.api.addldata.ADIotaHolder;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.common.msgs.MsgPullClipboardS2C;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * /hexParse —— 按 HexParseMod 的命令格式导入 / 导出 iota 列表文本。
 * <p>
 * {@code /hexParse <code> [rename]}、{@code /hexParse read}、{@code /hexParse read_signatures}、
 * {@code /hexParse share}、{@code /hexParse clipboard [rename]}、{@code /hexParse clipboard_angles [rename]}。
 */
public class HexParseCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var main = Commands.literal("hexParse");

        main.then(Commands.literal("read").executes(ctx -> read(ctx, false)));
        main.then(Commands.literal("read_signatures").executes(ctx -> read(ctx, true)));
        main.then(Commands.literal("share").executes(HexParseCommand::share));

        main.then(Commands.literal("clipboard")
            .executes(ctx -> pullClipboard(ctx, MsgPullClipboardS2C.MODE_DEFAULT))
            .then(Commands.argument("rename", StringArgumentType.string())
                .executes(ctx -> pullClipboard(ctx, MsgPullClipboardS2C.MODE_DEFAULT))));
        main.then(Commands.literal("clipboard_angles")
            .executes(ctx -> pullClipboard(ctx, MsgPullClipboardS2C.MODE_ANGLES))
            .then(Commands.argument("rename", StringArgumentType.string())
                .executes(ctx -> pullClipboard(ctx, MsgPullClipboardS2C.MODE_ANGLES))));

        main.then(Commands.argument("code", StringArgumentType.string())
            .executes(HexParseCommand::write)
            .then(Commands.argument("rename", StringArgumentType.string())
                .executes(HexParseCommand::write)));

        dispatcher.register(main);
    }

    private record Target(ItemStack stack, ADIotaHolder holder) {
    }

    @Nullable
    private static Target findTarget(ServerPlayer player, boolean write) {
        for (var hand : new InteractionHand[]{InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND}) {
            var stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                continue;
            }
            var holder = IXplatAbstractions.INSTANCE.findDataHolder(stack);
            if (holder == null) {
                continue;
            }
            if (write && !holder.writeable()) {
                continue;
            }
            return new Target(stack, holder);
        }
        return null;
    }

    // ------------------------------------------------------------------ write

    private static int write(CommandContext<CommandSourceStack> ctx) {
        var player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        var code = StringArgumentType.getString(ctx, "code");
        String rename = null;
        try {
            rename = StringArgumentType.getString(ctx, "rename");
        } catch (Exception ignored) {
        }
        applyCode(player, code, rename);
        return 1;
    }

    /**
     * 解析代码并写入玩家手中的 iota 容器。
     */
    public static void applyCode(ServerPlayer player, String code, @Nullable String rename) {
        var target = findTarget(player, true);
        if (target == null) {
            player.sendSystemMessage(Component.literal(
                "No writable focus, spellbook or thought knot found in either hand").withStyle(ChatFormatting.RED));
            return;
        }

        Iota root;
        try {
            root = IotaCode.parseCode(code, player);
        } catch (Throwable e) {
            player.sendSystemMessage(Component.literal("Parse error: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return;
        }

        if (!target.holder().writeIota(root, false)) {
            player.sendSystemMessage(Component.literal("Cannot write this code to "
                + target.stack().getItem().getName(target.stack()).getString()).withStyle(ChatFormatting.RED));
            return;
        }

        if (rename != null) {
            target.stack().set(DataComponents.CUSTOM_NAME, Component.literal(rename));
        }

        player.sendSystemMessage(Component.literal("Wrote code to "
            + target.stack().getItem().getName(target.stack()).getString()).withStyle(ChatFormatting.GREEN));
    }

    // ------------------------------------------------------------------- read

    private static int read(CommandContext<CommandSourceStack> ctx, boolean signaturesOnly) {
        var player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        var code = readHand(player, signaturesOnly);
        if (code == null) {
            return 0;
        }
        player.sendSystemMessage(clickable(code).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int share(CommandContext<CommandSourceStack> ctx) {
        var player = ctx.getSource().getPlayer();
        if (player == null || player.level().getServer() == null) {
            return 0;
        }
        var code = readHand(player, false);
        if (code == null) {
            return 0;
        }
        var shared = Component.literal(player.getName().getString() + " shared: ")
            .append(clickable(code));
        for (var target : player.level().getServer().getPlayerList().getPlayers()) {
            target.sendSystemMessage(shared);
        }
        return 1;
    }

    @Nullable
    private static String readHand(ServerPlayer player, boolean signaturesOnly) {
        var target = findTarget(player, false);
        if (target == null) {
            player.sendSystemMessage(Component.literal(
                "Hold a focus, spellbook, scroll or thought knot in either hand").withStyle(ChatFormatting.RED));
            return null;
        }
        var tag = target.holder().readIotaTag();
        if (tag == null) {
            player.sendSystemMessage(Component.literal("The item in your hand is empty")
                .withStyle(ChatFormatting.RED));
            return null;
        }

        var world = player.level() instanceof ServerLevel serverLevel
            ? serverLevel
            : player.level().getServer().overworld();
        var iota = IotaType.deserialize(tag, world);
        return IotaCode.iotaToCode(iota, player, signaturesOnly);
    }

    // --------------------------------------------------------------- clipboard

    private static int pullClipboard(CommandContext<CommandSourceStack> ctx, int mode) {
        var player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        String rename = null;
        try {
            rename = StringArgumentType.getString(ctx, "rename");
        } catch (Exception ignored) {
        }
        IXplatAbstractions.INSTANCE.sendPacketToPlayer(player, new MsgPullClipboardS2C(rename, mode));
        return 1;
    }

    // ------------------------------------------------------------------ util

    private static MutableComponent clickable(String code) {
        return Component.literal(code).withStyle(Style.EMPTY
            .withClickEvent(new ClickEvent.CopyToClipboard(code))
            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click"))));
    }
}
