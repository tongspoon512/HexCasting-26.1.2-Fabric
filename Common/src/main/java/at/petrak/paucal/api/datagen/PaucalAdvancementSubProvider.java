package at.petrak.paucal.api.datagen;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class PaucalAdvancementSubProvider implements net.minecraft.data.advancements.AdvancementSubProvider {
    protected final String modid;

    protected PaucalAdvancementSubProvider(String modid) {
        this.modid = modid;
    }

    protected Identifier modLoc(String path) {
        return Identifier.fromNamespaceAndPath(modid, path);
    }

    protected String prefix(String id) {
        return modid + ":" + id;
    }

    protected DisplayInfo simpleDisplay(ItemLike item, String name, AdvancementType frame) {
        return new DisplayInfo(new ItemStackTemplate(item.asItem()),
            Component.translatable("advancement." + modid + ":" + name),
            Component.translatable("advancement." + modid + ":" + name + ".desc"),
            Optional.empty(), frame, true, true, false);
    }

    protected DisplayInfo simpleDisplayWithBackground(ItemLike item, String name, AdvancementType frame,
                                                      Identifier background) {
        return new DisplayInfo(new ItemStackTemplate(item.asItem()),
            Component.translatable("advancement." + modid + ":" + name),
            Component.translatable("advancement." + modid + ":" + name + ".desc"),
            Optional.of(new ClientAsset.ResourceTexture(background)), frame, true, true, false);
    }

    protected DisplayInfo display(ItemStackTemplate stack, String name, AdvancementType frame, Identifier background,
                                  boolean showToast, boolean announce, boolean hidden) {
        return new DisplayInfo(stack,
            Component.translatable("advancement." + modid + ":" + name),
            Component.translatable("advancement." + modid + ":" + name + ".desc"),
            Optional.ofNullable(background).map(ClientAsset.ResourceTexture::new), frame, showToast, announce, hidden);
    }

    @Override
    public abstract void generate(HolderLookup.Provider provider, Consumer<AdvancementHolder> consumer);
}
