package pub.pigeon.yggdyy.hexcreating.client;

import com.zurrtum.create.client.ponder.api.registration.PonderPlugin;
import com.zurrtum.create.client.ponder.api.registration.PonderSceneRegistrationHelper;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

/**
 * Ponder integration for HexCreating, ported from the upstream ModPonderIndex.
 */
public final class HexCreatingPonder {
    private static final String INTRODUCTION = "hex/introduction";
    private static final String EMPTY_5_5 = "hex/empty_5_5";
    private static final String EMPTY_5_5_10 = "hex/empty_5_5_10";

    private HexCreatingPonder() {
    }

    public static void register() {
        PonderIndex.addPlugin(new Plugin());
    }

    private static final class Plugin implements PonderPlugin {
        @Override
        public String getModId() {
            return HexCreatingCompat.MOD_ID;
        }

        @Override
        public void registerScenes(PonderSceneRegistrationHelper<Identifier> helper) {
            PonderSceneRegistrationHelper<Item> items =
                helper.<Item>withKeyFunction(item -> BuiltInRegistries.ITEM.getKey(item));

            items.forComponents(HexCreatingItems.THOUGHT_KEY_HEXCASTING_INTRODUCTION)
                .addStoryBoard(INTRODUCTION, HexIntroductionScenes::hexcasting);

            items.forComponents(HexCreatingItems.THOUGHT_KEY_BASIC_IOTA)
                .addStoryBoard(EMPTY_5_5, HexIntroductionScenes::basicIotaEntity)
                .addStoryBoard(EMPTY_5_5, HexIntroductionScenes::basicIotaNumber)
                .addStoryBoard(EMPTY_5_5, HexIntroductionScenes::basicIotaVector)
                .addStoryBoard(EMPTY_5_5, HexIntroductionScenes::baseIotaBool);

            items.forComponents(HexCreatingItems.THOUGHT_KEY_VECTOR)
                .addStoryBoard(EMPTY_5_5, HexIntroductionScenes::vector);

            items.forComponents(HexCreatingItems.THOUGHT_KEY_STACK)
                .addStoryBoard(EMPTY_5_5_10, HexIntroductionScenes::stack);

            items.forComponents(HexCreatingItems.THOUGHT_KEY_PATTERN_INTRODUCTION)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::introduction)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::reset);

            items.forComponents(HexCreatingItems.THOUGHT_KEY_PATTERN_LOCATE)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::locateSelf)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::locateBlock)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::locateEntity);

            items.forComponents(HexCreatingItems.THOUGHT_KEY_PATTERN_CONSTANTS)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::numberConstants)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::vectorConstants)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::booleanConstants);

            items.forComponents(HexCreatingItems.THOUGHT_KEY_PATTERN_CALCULATE)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::calculate);

            items.forComponents(HexCreatingItems.THOUGHT_KEY_PATTERN_SPELL_1)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::edifyTreePattern)
                .addStoryBoard(EMPTY_5_5, PatternScenes1::edifyTreePractice);
        }
    }
}
