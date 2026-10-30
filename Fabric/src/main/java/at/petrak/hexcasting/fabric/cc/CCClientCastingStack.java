package at.petrak.hexcasting.fabric.cc;

import at.petrak.hexcasting.api.client.ClientCastingStack;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CCClientCastingStack implements Component, ClientTickingComponent {

    public CCClientCastingStack(Player owner) {
    }

    private final ClientCastingStack clientCastingStack = new ClientCastingStack();

    public ClientCastingStack getClientCastingStack() {
        return clientCastingStack;
    }

    @Override
    public void clientTick() {
        clientCastingStack.tick();
    }

    @Override
    public void readData(ValueInput input) { }

    @Override
    public void writeData(ValueOutput output) { }
}
