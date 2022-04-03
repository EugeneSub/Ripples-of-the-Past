package com.github.standobyte.jojo.client.model.entity.stand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.actions.StandEntityAction;
import com.github.standobyte.jojo.action.actions.StandEntityAction.Phase;
import com.github.standobyte.jojo.client.model.pose.IModelPose;
import com.github.standobyte.jojo.client.model.pose.ModelPose;
import com.github.standobyte.jojo.client.model.pose.ModelPose.ModelAnim;
import com.github.standobyte.jojo.client.model.pose.ModelPoseTransition;
import com.github.standobyte.jojo.client.model.pose.RotationAngle;
import com.github.standobyte.jojo.client.model.pose.StandActionAnimation;
import com.github.standobyte.jojo.client.renderer.entity.stand.AdditionalArmSwing;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntity.StandPose;
import com.github.standobyte.jojo.util.MathUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.AgeableModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public abstract class StandEntityModel<T extends StandEntity> extends AgeableModel<T> {
    protected VisibilityMode visibilityMode = VisibilityMode.ALL;
    protected float yRotation;
    protected float xRotation;
    protected float ticks;

    protected StandPose poseType = StandPose.SUMMON;
    private float idleLoopTickStamp = 0;
    protected ModelPose<T> poseReset;
    protected ModelPose<T> idlePose;
    protected IModelPose<T> idleLoop;
    protected List<IModelPose<T>> summonPoses;
    protected final Map<StandPose, StandActionAnimation<T>> actionAnim = new HashMap<>();

    protected StandEntityModel(boolean scaleHead, float yHeadOffset, float zHeadOffset) {
        this(scaleHead, yHeadOffset, zHeadOffset, 2.0F, 2.0F, 24.0F);
    }

    protected StandEntityModel(boolean scaleHead, float yHeadOffset, float zHeadOffset, 
            float babyHeadScale, float babyBodyScale, float bodyYOffset) {
        this(RenderType::entityTranslucent, scaleHead, yHeadOffset, zHeadOffset, babyHeadScale, babyBodyScale, bodyYOffset);
    }

    protected StandEntityModel(Function<ResourceLocation, RenderType> renderType, boolean scaleHead, float yHeadOffset, float zHeadOffset, 
            float babyHeadScale, float babyBodyScale, float bodyYOffset) {
        super(renderType, scaleHead, yHeadOffset, zHeadOffset, babyHeadScale, babyBodyScale, bodyYOffset);
    }

    public void afterInit() {
        initPoses();
    }

    protected final void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }

    public void setVisibilityMode(VisibilityMode mode, boolean forearmOnly) {
        this.visibilityMode = mode;
        updatePartsVisibility(mode, forearmOnly);
    }

    protected abstract void updatePartsVisibility(VisibilityMode mode, boolean forearmOnly);

    @Override
    public void prepareMobModel(T entity, float walkAnimPos, float walkAnimSpeed, float partialTick) {
        StandPose currentPose = entity.getStandPose();
//        if (currentPose != poseType) {
//            resetPose(entity);
//        }
        initPoses();
        poseType = currentPose;
    }

    public StandPose getPose() {
        return poseType;
    }

    @Override
    public void setupAnim(T entity, float walkAnimPos, float walkAnimSpeed, float ticks, float yRotationOffset, float xRotation) {
        HandSide swingingHand = entity.getSwingingHand();
        
//        initPoses();

        // FIXME (!!) idle pose messing up summon poses
        idlePose.poseModel(1.0F, entity, ticks, yRotationOffset, xRotation, swingingHand);

        if (poseType == StandPose.SUMMON && (ticks > SUMMON_ANIMATION_LENGTH || entity.isArmsOnlyMode())) {
            entity.setStandPose(StandPose.IDLE);
            poseType = StandPose.IDLE;
        }

        if (attackTime > 0.0F) {
            swingArmBarrage(entity, this.attackTime, yRotationOffset, xRotation, ticks, 
                    swingingHand, 0);
        }
        else {
            poseStand(entity, ticks, yRotationOffset, xRotation, 
                    poseType, entity.getCurrentTaskPhase(), 
                    entity.getCurrentTaskCompletion(MathHelper.frac(ticks)), swingingHand);
        }
        this.yRotation = yRotationOffset;
        this.xRotation = xRotation;
        this.ticks = ticks;
        /*if (!Minecraft.getInstance().isPaused())*/ entity.clUpdateSwings(Minecraft.getInstance().getDeltaFrameTime());
    }

    protected void poseStand(T entity, float ticks, float yRotationOffset, float xRotation, 
            StandPose standPose, @Nullable Phase actionPhase, float actionCompletion, HandSide swingingHand) {
        if (actionAnim.containsKey(standPose)) {
            onPose(entity, ticks);
            StandActionAnimation<T> anim = getActionAnim(entity, standPose);
            if (anim != null) {
                anim.animate(actionPhase, actionCompletion, 
                        entity, ticks, yRotationOffset, xRotation, swingingHand);
            }
        }
        else if (standPose == StandPose.SUMMON && summonPoses.size() > 0) {
            onPose(entity, ticks);
            summonPoses.get(entity.getSummonPoseRandomByte() % summonPoses.size())
            .poseModel(ticks, entity, ticks, yRotationOffset, xRotation, swingingHand);
        }
        else {
            idleLoop.poseModel(ticks - idleLoopTickStamp, entity, ticks, yRotationOffset, xRotation, swingingHand);
        }
    }

    protected StandActionAnimation<T> getActionAnim(T entity, StandPose poseType) {
        return actionAnim.get(poseType);
    }

    private void onPose(T entity, float ticks) {
        entity.setYBodyRot(entity.yRot);
        idleLoopTickStamp = ticks;
    }

    private final ModelAnim<T> HEAD_ROTATION = (rotationAmount, entity, ticks, yRotationOffset, xRotation) -> {
        headParts().forEach(part -> {
            part.yRot = yRotationOffset * MathUtil.DEG_TO_RAD;
            part.xRot = xRotation * MathUtil.DEG_TO_RAD;
            part.zRot = 0;
        });
    };
    
    protected void initPoses() {
        poseReset = initPoseReset();

        idlePose = initIdlePose().setAdditionalAnim(HEAD_ROTATION);
        idleLoop = new ModelPoseTransition<T>(idlePose, initIdlePose2Loop()).setEasing(ticks -> MathHelper.sin(ticks / 20));

        summonPoses = initSummonPoses();

        actionAnim.put(StandPose.LIGHT_ATTACK, initLightAttackAnim());
        actionAnim.put(StandPose.HEAVY_ATTACK, initHeavyAttackAnim(false));
        actionAnim.put(StandPose.HEAVY_ATTACK_COMBO, initHeavyAttackAnim(true));
        actionAnim.put(StandPose.RANGED_ATTACK, initRangedAttackAnim());
        actionAnim.put(StandPose.BLOCK, initBlockAnim());
    }



    protected abstract ModelPose<T> initPoseReset();

    protected ModelPose<T> initIdlePose() {
        return initPoseReset();
    }

    protected ModelPose<T> initIdlePose2Loop() {
        return initIdlePose();
    }

    private static final float SUMMON_ANIMATION_LENGTH = 20.0F;
    private static final float SUMMON_ANIMATION_POSE_REVERSE_POINT = 0.75F;
    protected List<IModelPose<T>> initSummonPoses() {
        return Arrays.stream(initSummonPoseRotations())
                .map(rotationAngles -> new ModelPose<T>(rotationAngles).setEasing(ticks -> 
                MathHelper.clamp(
                        (SUMMON_ANIMATION_LENGTH - ticks) / (SUMMON_ANIMATION_LENGTH * (1 - SUMMON_ANIMATION_POSE_REVERSE_POINT)), 
                        0F, 1F)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    protected RotationAngle[][] initSummonPoseRotations() {
        return new RotationAngle[0][0];
    }

    protected StandActionAnimation<T> initLightAttackAnim() {
        return new StandActionAnimation.Builder<T>().addPose(StandEntityAction.Phase.PERFORM, initPoseReset()).build(idlePose);
    }

    protected StandActionAnimation<T> initHeavyAttackAnim(boolean combo) {
        return initLightAttackAnim();
    }

    protected StandActionAnimation<T> initBlockAnim() {
        return new StandActionAnimation.Builder<T>().addPose(StandEntityAction.Phase.BUTTON_HOLD, blockPose()).build(idlePose);
    }

    protected IModelPose<T> blockPose() {
        return initIdlePose();
    }

    protected StandActionAnimation<T> initRangedAttackAnim() {
        return new StandActionAnimation.Builder<T>().addPose(StandEntityAction.Phase.BUTTON_HOLD, rangedAttackPose()).build(idlePose);
    }

    protected IModelPose<T> rangedAttackPose() {
        return initIdlePose();
    }

    protected abstract void swingArmBarrage(T entity, float swingAmount, float yRotation, float xRotation, float ticks, HandSide swingingHand, float recovery);

    public void renderFirstPersonArms(HandSide handSide, MatrixStack matrixStack, 
            IVertexBuilder buffer, int packedLight, T entity, float partialTick, 
            int packedOverlay, float red, float green, float blue, float alpha) {}

    public void renderArmSwingHand(HandSide handSide, MatrixStack matrixStack, 
            IVertexBuilder buffer, int packedLight, T entity, float partialTick, 
            int packedOverlay, float red, float green, float blue, float alpha) {}

    public abstract ModelRenderer armModel(HandSide side);

    public void renderArmSwings(T entity, MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        List<AdditionalArmSwing> swings = entity.getSwingsWithOffsets();
        if (!swings.isEmpty()) {
            resetPose(entity);
            for (AdditionalArmSwing swing : swings) {
                matrixStack.pushPose();
                setVisibilityMode(swing.getSide() == HandSide.LEFT ? VisibilityMode.LEFT_ARM_ONLY : VisibilityMode.RIGHT_ARM_ONLY, true);
                matrixStack.translate(swing.offset.x, swing.offset.y, swing.offset.z);
                float anim = swing.getAnim();
                HandSide swingingHand;
                if (anim <= 1) {
                    attackTime = anim;
                    swingingHand = swing.getSide();
                }
                else {
                    attackTime = anim - 1;
                    swingingHand = swing.getSide().getOpposite();
                }
                swingArmBarrage(entity, attackTime, yRotation, xRotation, ticks, swingingHand, 0F);
                rotateAdditionalArmSwings();
                renderToBuffer(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha * 0.5F);
                matrixStack.popPose();
            }
        }
    }

    private void resetPose(T entity) {
        poseReset.poseModel(1, entity, 0, 0, 0, entity.getSwingingHand());
    }

    protected abstract void rotateAdditionalArmSwings();

    public enum VisibilityMode {
        ALL,
        ARMS_ONLY,
        LEFT_ARM_ONLY,
        RIGHT_ARM_ONLY
    }
}
