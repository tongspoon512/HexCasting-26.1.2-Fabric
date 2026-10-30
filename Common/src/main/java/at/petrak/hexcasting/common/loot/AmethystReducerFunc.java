package at.petrak.hexcasting.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class AmethystReducerFunc extends LootItemConditionalFunction {
    public static final MapCodec<AmethystReducerFunc> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        commonFields(inst).t1(),
        Codec.DOUBLE.fieldOf("delta").forGetter(f -> f.delta)
    ).apply(inst, AmethystReducerFunc::new));

    public final double delta;

    public AmethystReducerFunc(List<LootItemCondition> lootItemConditions, double delta) {
        super(lootItemConditions);
        this.delta = delta;
    }

    public static ItemStack doStatic(ItemStack stack, LootContext ctx, double amount) {
        if (stack.is(Items.AMETHYST_SHARD)) {
            stack.setCount((int) (stack.getCount() * (1 + amount)));
        }
        return stack;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext ctx) {
        return doStatic(stack, ctx, this.delta);
    }

    @Override
    public MapCodec<? extends LootItemConditionalFunction> codec() {
        return CODEC;
    }
}
