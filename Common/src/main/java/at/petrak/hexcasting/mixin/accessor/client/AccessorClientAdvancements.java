package at.petrak.hexcasting.mixin.accessor.client;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ClientAdvancements.class)
public interface AccessorClientAdvancements {
    /**
     * 26.1 只把完成进度交给 GUI 监听器，没有公开的查询方法；指南书需要它来按进度锁定条目。
     */
    @Accessor("progress")
    Map<AdvancementHolder, AdvancementProgress> getProgress();
}
