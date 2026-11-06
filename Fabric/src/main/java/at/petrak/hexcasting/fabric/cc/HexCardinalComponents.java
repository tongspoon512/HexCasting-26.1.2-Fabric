package at.petrak.hexcasting.fabric.cc;

import at.petrak.hexcasting.api.addldata.ItemDelegatingEntityIotaHolder;
import at.petrak.hexcasting.common.entities.EntityWallScroll;
import at.petrak.hexcasting.fabric.cc.adimpl.CCEntityIotaHolder;
import at.petrak.hexcasting.fabric.cc.adimpl.CCIotaHolder;
import org.ladysnake.cca.api.v3.component.ComponentFactory;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.function.Function;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class HexCardinalComponents implements EntityComponentInitializer {
    // entities
    public static final ComponentKey<CCBrainswept> BRAINSWEPT = ComponentRegistry.getOrCreate(modLoc("brainswept"),
        CCBrainswept.class);
    public static final ComponentKey<CCFavoredPigment> FAVORED_PIGMENT = ComponentRegistry.getOrCreate(
        modLoc("favored_pigment"), CCFavoredPigment.class);
    public static final ComponentKey<CCSentinel> SENTINEL = ComponentRegistry.getOrCreate(modLoc("sentinel"),
        CCSentinel.class);
    public static final ComponentKey<CCFlight> FLIGHT = ComponentRegistry.getOrCreate(modLoc("flight"),
        CCFlight.class);

    public static final ComponentKey<CCAltiora> ALTIORA = ComponentRegistry.getOrCreate(modLoc("altiora"),
        CCAltiora.class);
    public static final ComponentKey<CCStaffcastImage> STAFFCAST_IMAGE = ComponentRegistry.getOrCreate(modLoc(
        "harness"),
        CCStaffcastImage.class);
    public static final ComponentKey<CCPatterns> PATTERNS = ComponentRegistry.getOrCreate(modLoc("patterns"),
        CCPatterns.class);

    public static final ComponentKey<CCClientCastingStack> CLIENT_CASTING_STACK = ComponentRegistry.getOrCreate(modLoc("client_casting_stack"),
            CCClientCastingStack.class);

    // hexcassettes
    public static final ComponentKey<CCCassettes> CASSETTES = ComponentRegistry.getOrCreate(
        net.minecraft.resources.Identifier.fromNamespaceAndPath("hexcassettes", "cassettes"), CCCassettes.class);

    // 26.1: CCA v8 dropped item components; item-based addldata is constructed directly in FabricXplatImpl.
    public static final ComponentKey<CCIotaHolder> IOTA_HOLDER = ComponentRegistry.getOrCreate(modLoc("iota_holder"),
        CCIotaHolder.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerFor(Mob.class, BRAINSWEPT, CCBrainswept::new);
        registry.registerForPlayers(FAVORED_PIGMENT, CCFavoredPigment::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(SENTINEL, CCSentinel::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(ALTIORA, CCAltiora::new, RespawnCopyStrategy.LOSSLESS_ONLY);
        registry.registerForPlayers(CLIENT_CASTING_STACK, CCClientCastingStack::new, RespawnCopyStrategy.NEVER_COPY);
        // Fortunately these are all both only needed on the server and don't want to be copied across death
        registry.registerFor(ServerPlayer.class, FLIGHT, CCFlight::new);
        registry.registerFor(ServerPlayer.class, STAFFCAST_IMAGE, CCStaffcastImage::new);
        registry.registerFor(ServerPlayer.class, PATTERNS, CCPatterns::new);
        // 死亡时清空排队咒术，但保留已购买的槽位（重生事件中单独复制 owned）
        registry.registerForPlayers(CASSETTES, CCCassettes::new, RespawnCopyStrategy.NEVER_COPY);


        registry.registerFor(ItemEntity.class, IOTA_HOLDER, wrapItemEntityDelegate(
            ItemDelegatingEntityIotaHolder.ToItemEntity::new));
        registry.registerFor(ItemFrame.class, IOTA_HOLDER, wrapItemEntityDelegate(
            ItemDelegatingEntityIotaHolder.ToItemFrame::new));
        registry.registerFor(EntityWallScroll.class, IOTA_HOLDER,
            wrapItemEntityDelegate(ItemDelegatingEntityIotaHolder.ToWallScroll::new));
    }

    private <E extends Entity> ComponentFactory<E, CCEntityIotaHolder.Wrapper> wrapItemEntityDelegate(Function<E,
        ItemDelegatingEntityIotaHolder> make) {
        return e -> new CCEntityIotaHolder.Wrapper(make.apply(e));
    }
}
