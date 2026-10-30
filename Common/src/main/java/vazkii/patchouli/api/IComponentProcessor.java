package vazkii.patchouli.api;

import net.minecraft.world.level.Level;

/**
 * Stub of patchouli's IComponentProcessor.
 */
public interface IComponentProcessor {
    void setup(Level level, IVariableProvider vars);

    IVariable process(Level level, String key);
}
