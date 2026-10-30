package at.petrak.hexcasting.fabric.mixin;


import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClipContext.class)
public abstract class FabricClipContextMixin {

    @Redirect(method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/shapes/CollisionContext;of" +
                            "(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/shapes/CollisionContext;"))
    // 26.1 builds the collision context in the super() argument list, i.e. before this() runs,
    // so the redirect handler has to be static.
    private static CollisionContext hex$getCollisionContext(Entity entity) {
        if (entity == null)
            return CollisionContext.empty();
        return CollisionContext.of(entity);
    }
}
