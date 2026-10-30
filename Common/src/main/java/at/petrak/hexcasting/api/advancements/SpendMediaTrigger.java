package at.petrak.hexcasting.api.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

// 26.1: criterion triggers are codec-based and keyed by registry, not by a getId() on the trigger.
// MinMaxLongs was removed, so the media bounds use Doubles (same JSON shape).
public class SpendMediaTrigger extends SimpleCriterionTrigger<SpendMediaTrigger.Instance> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("hexcasting", "spend_media");

    private static final String TAG_MEDIA_SPENT = "media_spent";
    private static final String TAG_MEDIA_WASTED = "media_wasted";

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, long mediaSpent, long mediaWasted) {
        super.trigger(player, inst -> inst.test(mediaSpent, mediaWasted));
    }

    public record Instance(
        Optional<ContextAwarePredicate> player,
        MinMaxBounds.Doubles mediaSpent,
        MinMaxBounds.Doubles mediaWasted
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf(TAG_MEDIA_SPENT, MinMaxBounds.Doubles.ANY)
                .forGetter(Instance::mediaSpent),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf(TAG_MEDIA_WASTED, MinMaxBounds.Doubles.ANY)
                .forGetter(Instance::mediaWasted)
        ).apply(i, Instance::new));

        // convenience for datagen
        public Instance(ContextAwarePredicate predicate, MinMaxBounds.Doubles mediaSpent,
            MinMaxBounds.Doubles mediaWasted) {
            this(Optional.of(predicate), mediaSpent, mediaWasted);
        }

        private boolean test(long mediaSpentIn, long mediaWastedIn) {
            return this.mediaSpent.matches(mediaSpentIn) && this.mediaWasted.matches(mediaWastedIn);
        }
    }
}
