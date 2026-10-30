package at.petrak.hexcasting.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;

// https://github.com/VazkiiMods/Botania/blob/1.19.x/Xplat/src/main/java/vazkii/botania/client/model/armor/ArmorModel.java
public class MyOwnArmorModelWithBlackjackAndHookers extends HumanoidModel<HumanoidRenderState> {
    protected final EquipmentSlot slot;

    public MyOwnArmorModelWithBlackjackAndHookers(ModelPart root, EquipmentSlot slot) {
        super(root);
        this.slot = slot;
    }

    // [VanillaCopy] ArmorStandArmorModel.setupAnim because armor stands are dumb
    // This fixes the armor "breathing" and helmets always facing south on armor stands
    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if (!(state instanceof ArmorStandRenderState entityIn)) {
            return;
        }

        this.head.xRot = ((float) Math.PI / 180F) * entityIn.headPose.x();
        this.head.yRot = ((float) Math.PI / 180F) * entityIn.headPose.y();
        this.head.zRot = ((float) Math.PI / 180F) * entityIn.headPose.z();
        this.head.setPos(0.0F, 1.0F, 0.0F);
        this.body.xRot = ((float) Math.PI / 180F) * entityIn.bodyPose.x();
        this.body.yRot = ((float) Math.PI / 180F) * entityIn.bodyPose.y();
        this.body.zRot = ((float) Math.PI / 180F) * entityIn.bodyPose.z();
        this.leftArm.xRot = ((float) Math.PI / 180F) * entityIn.leftArmPose.x();
        this.leftArm.yRot = ((float) Math.PI / 180F) * entityIn.leftArmPose.y();
        this.leftArm.zRot = ((float) Math.PI / 180F) * entityIn.leftArmPose.z();
        this.rightArm.xRot = ((float) Math.PI / 180F) * entityIn.rightArmPose.x();
        this.rightArm.yRot = ((float) Math.PI / 180F) * entityIn.rightArmPose.y();
        this.rightArm.zRot = ((float) Math.PI / 180F) * entityIn.rightArmPose.z();
        this.leftLeg.xRot = ((float) Math.PI / 180F) * entityIn.leftLegPose.x();
        this.leftLeg.yRot = ((float) Math.PI / 180F) * entityIn.leftLegPose.y();
        this.leftLeg.zRot = ((float) Math.PI / 180F) * entityIn.leftLegPose.z();
        this.leftLeg.setPos(1.9F, 11.0F, 0.0F);
        this.rightLeg.xRot = ((float) Math.PI / 180F) * entityIn.rightLegPose.x();
        this.rightLeg.yRot = ((float) Math.PI / 180F) * entityIn.rightLegPose.y();
        this.rightLeg.zRot = ((float) Math.PI / 180F) * entityIn.rightLegPose.z();
        this.rightLeg.setPos(-1.9F, 11.0F, 0.0F);
        this.hat.xRot = this.head.xRot;
        this.hat.yRot = this.head.yRot;
        this.hat.zRot = this.head.zRot;
        this.hat.setPos(this.head.x, this.head.y, this.head.z);
    }

    // [VanillaCopy] HumanoidArmorLayer
    @SuppressWarnings("unused")
    private void setPartVisibility(EquipmentSlot slot) {
        setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                head.visible = true;
                hat.visible = true;
            }
            case CHEST -> {
                body.visible = true;
                rightArm.visible = true;
                leftArm.visible = true;
            }
            case LEGS -> {
                body.visible = true;
                rightLeg.visible = true;
                leftLeg.visible = true;
            }
            case FEET -> {
                rightLeg.visible = true;
                leftLeg.visible = true;
            }
        }
    }

    private void setAllVisible(boolean visible) {
        this.head.visible = visible;
        this.hat.visible = visible;
        this.body.visible = visible;
        this.rightArm.visible = visible;
        this.leftArm.visible = visible;
        this.rightLeg.visible = visible;
        this.leftLeg.visible = visible;
    }
}
