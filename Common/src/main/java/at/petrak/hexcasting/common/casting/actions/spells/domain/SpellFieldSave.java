package at.petrak.hexcasting.common.casting.actions.spells.domain;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-level storage for effects that live outside the entity/chunk systems: cast domains and the
 * three suns field. Keeping them here means quitting and re-entering a world restores the domains
 * and the remaining daylight instead of silently dropping them.
 */
public class SpellFieldSave extends SavedData {
    public static final String TAG_SAVED_DATA = "hexcasting.spell-fields.0.1.0";
    private static final String TAG_DOMAINS = "domains";
    private static final String TAG_SUNS = "suns";

    public static final SavedDataType<SpellFieldSave> TYPE = new SavedDataType<>(
        Identifier.parse(TAG_SAVED_DATA),
        SpellFieldSave::new,
        CompoundTag.CODEC.xmap(SpellFieldSave::load, SpellFieldSave::saveToTag),
        DataFixTypes.SAVED_DATA_WORLD_CLOCKS);

    private ListTag domains;
    private ListTag suns;

    public SpellFieldSave() {
        this(new ListTag(), new ListTag());
    }

    private SpellFieldSave(ListTag domains, ListTag suns) {
        this.domains = domains;
        this.suns = suns;
    }

    public ListTag getDomains() {
        return this.domains;
    }

    public void setDomains(ListTag domains) {
        this.domains = domains;
        this.setDirty();
    }

    public ListTag getSuns() {
        return this.suns;
    }

    public void setSuns(ListTag suns) {
        this.suns = suns;
        this.setDirty();
    }

    private static SpellFieldSave load(CompoundTag tag) {
        return new SpellFieldSave(
            tag.getListOrEmpty(TAG_DOMAINS),
            tag.getListOrEmpty(TAG_SUNS));
    }

    private CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(TAG_DOMAINS, this.domains);
        tag.put(TAG_SUNS, this.suns);
        return tag;
    }
}
