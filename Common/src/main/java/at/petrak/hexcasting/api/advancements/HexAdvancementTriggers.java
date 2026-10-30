package at.petrak.hexcasting.api.advancements;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class HexAdvancementTriggers {
    public static final OvercastTrigger OVERCAST_TRIGGER = new OvercastTrigger();
    public static final SpendMediaTrigger SPEND_MEDIA_TRIGGER = new SpendMediaTrigger();
    public static final FailToCastGreatSpellTrigger FAIL_GREAT_SPELL_TRIGGER = new FailToCastGreatSpellTrigger();

    public static void registerTriggers() {
        // 26.1: triggers live in a normal registry; no more CriteriaTriggers.register invoker.
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, OvercastTrigger.ID, OVERCAST_TRIGGER);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, SpendMediaTrigger.ID, SPEND_MEDIA_TRIGGER);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, FailToCastGreatSpellTrigger.ID, FAIL_GREAT_SPELL_TRIGGER);
    }
}
