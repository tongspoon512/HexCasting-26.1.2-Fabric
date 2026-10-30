package vazkii.patchouli.api;

/**
 * Stub of patchouli's IVariableProvider.
 */
public interface IVariableProvider {
    IVariable get(String key);

    boolean has(String key);
}
