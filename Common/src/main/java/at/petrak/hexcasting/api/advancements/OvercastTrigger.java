package at.petrak.hexcasting.api.advancements;

import at.petrak.hexcasting.api.mod.HexConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

// https://github.com/TelepathicGrunt/Bumblezone/blob/latest-released/src/main/java/com/telepathicgrunt/the_bumblezone/advancements/CleanupStickyHoneyResidueTrigger.java
// https://github.com/VazkiiMods/Botania/blob/b8706e2e0bba20f67f1e103559a4ce39d63d48f9/src/main/java/vazkii/botania/common/advancements/CorporeaRequestTrigger.java

// 26.1: criterion triggers are codec-based and keyed by registry, not by a getId() on the trigger.
public class OvercastTrigger extends SimpleCriterionTrigger<OvercastTrigger.Instance> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("hexcasting", "overcast");

    private static final String TAG_MEDIA_GENERATED = "media_generated";
    private static final String TAG_HEALTH_USED = "health_used";
    // HEY KIDS DID YOYU KNOW THERE'S NOT A CRITERIA FOR HOW MUCH ***HEALTH*** AN ENTITY HAS
    private static final String TAG_HEALTH_LEFT =
        "mojang_i_am_begging_and_crying_please_add_an_entity_health_criterion";

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int mediaGenerated) {
        super.trigger(player, inst -> {
            var mediaToHealth = HexConfig.common().mediaToHealthRate();
            var healthUsed = mediaGenerated / mediaToHealth;
            return inst.test(mediaGenerated, healthUsed / player.getMaxHealth(), player.getHealth());
        });
    }

    public record Instance(
        Optional<ContextAwarePredicate> player,
        MinMaxBounds.Ints mediaGenerated,
        // This is the *proporttion* of the health bar.
        MinMaxBounds.Doubles healthUsed,
        // DID YOU KNOW THERES ONE TO CHECK THE WORLD TIME, BUT NOT THE HEALTH!?
        MinMaxBounds.Doubles healthLeft
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
            MinMaxBounds.Ints.CODEC.optionalFieldOf(TAG_MEDIA_GENERATED, MinMaxBounds.Ints.ANY)
                .forGetter(Instance::mediaGenerated),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf(TAG_HEALTH_USED, MinMaxBounds.Doubles.ANY)
                .forGetter(Instance::healthUsed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf(TAG_HEALTH_LEFT, MinMaxBounds.Doubles.ANY)
                .forGetter(Instance::healthLeft)
        ).apply(i, Instance::new));

        // convenience for datagen
        public Instance(ContextAwarePredicate predicate, MinMaxBounds.Ints mediaGenerated,
            MinMaxBounds.Doubles healthUsed, MinMaxBounds.Doubles healthLeft) {
            this(Optional.of(predicate), mediaGenerated, healthUsed, healthLeft);
        }

        private boolean test(int mediaGeneratedIn, double healthUsedIn, float healthLeftIn) {
            return this.mediaGenerated.matches(mediaGeneratedIn)
                && this.healthUsed.matches(healthUsedIn)
                // DID YOU KNOW ALL THE ENEITYT PREDICATES ARE HARD-CODED AND YOU CANT MAKE NEW ONES
                && this.healthLeft.matches(healthLeftIn);
        }
    }
}
