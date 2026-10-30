package at.petrak.hexcasting.fabric.cc;

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import org.ladysnake.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Collections;
import java.util.List;

public class CCPatterns implements Component {
    public static final String TAG_PATTERNS = "patterns";

    private final Player owner;

    private List<ResolvedPattern> patterns = Collections.emptyList();

    public CCPatterns(Player owner) {
        this.owner = owner;
    }


    public List<ResolvedPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<ResolvedPattern> patterns) {
        this.patterns = patterns;
    }

    @Override
    public void readData(ValueInput input) {
        this.patterns = input.listOrEmpty(TAG_PATTERNS, CompoundTag.CODEC).stream()
            .map(ResolvedPattern::fromNBT)
            .toList();
    }

    @Override
    public void writeData(ValueOutput output) {
        var list = output.list(TAG_PATTERNS, CompoundTag.CODEC);
        for (ResolvedPattern pattern : patterns) {
            list.add(pattern.serializeToNBT());
        }
    }
}
