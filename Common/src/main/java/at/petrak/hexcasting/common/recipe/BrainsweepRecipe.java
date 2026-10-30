package at.petrak.hexcasting.common.recipe;

import at.petrak.hexcasting.common.recipe.ingredient.StateIngredient;
import at.petrak.hexcasting.common.recipe.ingredient.StateIngredientHelper;
import at.petrak.hexcasting.common.recipe.ingredient.brainsweep.BrainsweepeeIngredient;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.stream.Stream;

// God I am a horrible person
// 26.1: recipes no longer carry their id (the ResourceKey on the RecipeHolder is the id),
// and serializers are just a record around a MapCodec + StreamCodec.
public record BrainsweepRecipe(
	StateIngredient blockIn,
	BrainsweepeeIngredient entityIn,
	long mediaCost,
	BlockState result
) implements Recipe<RecipeInput> {
	public boolean matches(BlockState blockIn, Entity victim, ServerLevel level) {
		return this.blockIn.test(blockIn) && this.entityIn.test(victim, level);
	}

	// in order to get this to be a "Recipe" we need to do a lot of bending-over-backwards
	// to get the implementation to be satisfied even though we never use it
	@Override
	public boolean matches(RecipeInput input, Level level) {
		return false;
	}

	@Override
	public ItemStack assemble(RecipeInput input) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
		return HexRecipeStuffRegistry.BRAINSWEEP;
	}

	@Override
	public RecipeType<? extends Recipe<RecipeInput>> getType() {
		return HexRecipeStuffRegistry.BRAINSWEEP_TYPE;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		// never shows up in the recipe book anyway
		return new RecipeBookCategory();
	}

	// Because kotlin doesn't like doing raw, unchecked types
	// Can't blame it, but that's what we need to do
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static BlockState copyProperties(BlockState original, BlockState copyTo) {
		for (Property prop : original.getProperties()) {
			if (copyTo.hasProperty(prop)) {
				copyTo = copyTo.setValue(prop, original.getValue(prop));
			}
		}

		return copyTo;
	}

	// Codec bridging: the datapack JSON format is unchanged, so adapt the old Gson-based
	// (de)serialization into a MapCodec, and the old FriendlyByteBuf logic into a StreamCodec.
	public static final MapCodec<BrainsweepRecipe> MAP_CODEC = new MapCodec<>() {
		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops) {
			return Stream.of("blockIn", "entityIn", "cost", "result").map(ops::createString);
		}

		@Override
		public <T> DataResult<BrainsweepRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
			try {
				JsonObject json = new JsonObject();
				input.entries().forEach(pair -> {
					var key = ops.getStringValue(pair.getFirst()).result().orElseThrow();
					json.add(key, ops.convertTo(JsonOps.INSTANCE, pair.getSecond()));
				});
				var blockIn = StateIngredientHelper.deserialize(GsonHelper.getAsJsonObject(json, "blockIn"));
				var villagerIn = BrainsweepeeIngredient.deserialize(GsonHelper.getAsJsonObject(json, "entityIn"));
				var cost = GsonHelper.getAsLong(json, "cost");
				var result = StateIngredientHelper.readBlockState(GsonHelper.getAsJsonObject(json, "result"));
				return DataResult.success(new BrainsweepRecipe(blockIn, villagerIn, cost, result));
			} catch (Exception e) {
				return DataResult.error(() -> "Malformed brainsweep recipe: " + e.getMessage());
			}
		}

		@Override
		public <T> RecordBuilder<T> encode(BrainsweepRecipe input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
			JsonObject json = new JsonObject();
			json.add("blockIn", input.blockIn.serialize());
			json.add("entityIn", input.entityIn.serialize());
			json.addProperty("cost", input.mediaCost);
			json.add("result", StateIngredientHelper.serializeBlockState(input.result));
			for (var entry : json.entrySet()) {
				prefix.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
			}
			return prefix;
		}
	};

	public static final StreamCodec<RegistryFriendlyByteBuf, BrainsweepRecipe> STREAM_CODEC = StreamCodec.of(
		(buf, recipe) -> {
			recipe.blockIn.write(buf);
			recipe.entityIn.wrapWrite(buf);
			buf.writeVarLong(recipe.mediaCost);
			buf.writeVarInt(Block.getId(recipe.result));
		},
		buf -> new BrainsweepRecipe(
			StateIngredientHelper.read(buf),
			BrainsweepeeIngredient.read(buf),
			buf.readVarLong(),
			Block.stateById(buf.readVarInt())
		)
	);

	public static final RecipeSerializer<BrainsweepRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
}
