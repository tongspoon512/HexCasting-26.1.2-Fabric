package at.petrak.hexcasting.common.command;

import at.petrak.hexcasting.server.ScrungledPatternsSave;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RecalcPatternsCommand {
    public static void add(LiteralArgumentBuilder<CommandSourceStack> cmd) {
        cmd.then(Commands.literal("recalcPatterns")
            .requires(dp -> Commands.LEVEL_ADMINS.check(dp.permissions()))
            .executes(ctx -> {
                var world = ctx.getSource().getServer().overworld();
                var ds = world.getDataStorage();
                ds.set(ScrungledPatternsSave.TYPE,
                    ScrungledPatternsSave.createFromScratch(world.getSeed()));

                ctx.getSource().sendSuccess(() ->
                    Component.translatable("command.hexcasting.recalc"), true);
                return 1;
            }));
    }
}
