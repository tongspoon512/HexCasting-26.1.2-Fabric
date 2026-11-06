package at.petrak.hexcasting.fabric.cc;

import miyucomics.hexcassettes.data.CassetteState;
import org.ladysnake.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 玩家卡带状态：已拥有的槽位与排队中的咒术。
 */
public class CCCassettes implements Component {
    public static final String TAG_CASSETTES = "cassettes";

    private final CassetteState state = new CassetteState();

    public CCCassettes(Player owner) {
    }

    public CassetteState getState() {
        return this.state;
    }

    @Override
    public void readData(ValueInput input) {
        this.state.load(input.read(TAG_CASSETTES, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    @Override
    public void writeData(ValueOutput output) {
        output.store(TAG_CASSETTES, CompoundTag.CODEC, this.state.serialize());
    }
}
