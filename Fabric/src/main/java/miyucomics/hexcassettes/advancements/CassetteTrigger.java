package miyucomics.hexcassettes.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import miyucomics.hexcassettes.HexCassettes;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * 卡带进度触发器：吃过磁带、吃满磁带、以及嵌套深度达到 9 的守护进程。
 *
 * 三类进度条件都为空，因此共用同一份 codec；但每个条目使用独立实例，
 * 避免 SimpleCriterionTrigger 的监听器在不同 id 之间串味。
 */
public class CassetteTrigger extends SimpleCriterionTrigger<CassetteTrigger.Instance> {
    public static final Identifier QUINIO_ID = HexCassettes.id("quinio");
    public static final Identifier TAPE_WORM_ID = HexCassettes.id("tape_worm");
    public static final Identifier FULL_ARSENAL_ID = HexCassettes.id("full_arsenal");

    public static final CassetteTrigger QUINIO = new CassetteTrigger();
    public static final CassetteTrigger TAPE_WORM = new CassetteTrigger();
    public static final CassetteTrigger FULL_ARSENAL = new CassetteTrigger();

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        super.trigger(player, instance -> true);
    }

    public static void register() {
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, QUINIO_ID, QUINIO);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, TAPE_WORM_ID, TAPE_WORM);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, FULL_ARSENAL_ID, FULL_ARSENAL);
    }

    public record Instance(Optional<ContextAwarePredicate> player)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
        ).apply(i, Instance::new));
    }
}
