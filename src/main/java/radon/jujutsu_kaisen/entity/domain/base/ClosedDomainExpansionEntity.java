package radon.jujutsu_kaisen.entity.domain.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.VeilHandler;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
import radon.jujutsu_kaisen.block.JJKBlocks;
import radon.jujutsu_kaisen.block.entity.DomainBlockEntity;
import radon.jujutsu_kaisen.block.entity.VeilBlockEntity;
import radon.jujutsu_kaisen.data.ability.IAbilityData;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.sorcerer.Trait;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.base.DomainExpansionEntity;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;
import radon.jujutsu_kaisen.util.RotationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClosedDomainExpansionEntity extends DomainExpansionEntity {
    private static final EntityDataAccessor<Integer> DATA_RADIUS = SynchedEntityData.defineId(ClosedDomainExpansionEntity.class, EntityDataSerializers.INT);

    private int total;

    public ClosedDomainExpansionEntity(EntityType<? > pType, Level pLevel) {
        super(pType, pLevel);
    }

    public ClosedDomainExpansionEntity(LivingEntity owner, DomainExpansion ability, int radius) {
        this(JJKEntities.CLOSED_DOMAIN_EXPANSION.get(), owner, ability, radius);
    }

    public ClosedDomainExpansionEntity(EntityType<? > pType, LivingEntity owner, DomainExpansion ability, int radius) {
        super(pType, owner, ability);

        Vec3 direction = RotationUtil.getTargetAdjustedLookAngle(owner);
        Vec3 behind = owner.position().subtract(0.0D, radius, 0.0D).add(direction.scale(radius - OFFSET));
        this.moveTo(behind.x, behind.y, behind.z, RotationUtil.getTargetAdjustedYRot(owner), RotationUtil.getTargetAdjustedXRot(owner));

        this.entityData.set(DATA_RADIUS, radius);
    }

    @Override
    public boolean shouldCollapse(float strength) {
        int radius = this.getRadius();
        boolean completed = this.getTime() >= radius;
        return completed && super.shouldCollapse(strength);
    }

    @Override
    public AABB getBounds() {
        return this.getBoundingBox();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_RADIUS, 0);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        this.entityData.set(DATA_RADIUS, pCompound.getInt("radius"));
        this.total = pCompound.getInt("total");
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putInt("radius", this.getRadius());
        pCompound.putInt("total", this.total);
    }

    public int getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    @Override
    public boolean isInsideBarrier(BlockPos pos) {
        if (this.level().getBlockEntity(pos) instanceof DomainBlockEntity be && be.getIdentifier() != null && be.getIdentifier().equals(this.uuid))
            return true;

        int radius = this.getRadius();
        BlockPos center = this.blockPosition().offset(0, radius, 0);
        BlockPos relative = pos.subtract(center);
        return relative.distSqr(Vec3i.ZERO) < (radius - 1) * (radius - 1);
    }

    protected void createBlock(int delay, BlockPos pos, int radius, double distance) {
        if (this.isRemoved()) return;

        if (distance >= radius) return;

        LivingEntity owner = this.getOwner();

        if (owner == null) return;

        BlockState state = this.level().getBlockState(pos);

        if (state.is(Blocks.BEDROCK)) return;

        BlockEntity existing = this.level().getBlockEntity(pos);

        CompoundTag saved = null;

        if (existing instanceof VeilBlockEntity be) {
            be.destroy();

            state = this.level().getBlockState(pos);
        } else if (existing instanceof DomainBlockEntity be) {
            BlockState original = be.getOriginal();

            if (original == null) return;

            state = original;
            saved = be.getSaved();
        } else if (existing != null) {
            saved = existing.saveWithFullMetadata();
        }

        DomainExpansion.IClosedDomain domain = ((DomainExpansion.IClosedDomain) this.ability);
        List<Block> blocks = domain.getBlocks();
        List<Block> fill = domain.getFillBlocks();
        List<Block> floor = domain.getFloorBlocks();
        List<Block> decoration = domain.getDecorationBlocks();

        Block block = null;

        if (distance >= radius - 1) {
            block = JJKBlocks.DOMAIN.get();
        } else if (!state.isAir() && state.getCollisionShape(this.level(), pos).isEmpty() || !state.getFluidState().isEmpty()) {
            block = distance >= radius - 2 ? blocks.get(this.random.nextInt(blocks.size())) : JJKBlocks.DOMAIN_AIR.get();
        } else {
            if (distance >= radius - 2) {
                block = blocks.get(this.random.nextInt(blocks.size()));
            } else if (!state.isAir()) {
                if (!floor.isEmpty() && domain.canPlaceFloor(this, pos)) {
                    block = floor.get(this.random.nextInt(floor.size()));
                } else {
                    block = fill.get(this.random.nextInt(fill.size()));
                }
            } else {
                if (!decoration.isEmpty() && domain.canPlaceDecoration(this, pos)) {
                    block = decoration.get(this.random.nextInt(decoration.size()));
                }
            }
        }

        if (block == null) return;

        owner.level().removeBlockEntity(pos);

        if (!this.level().getBlockState(pos.above()).isAir() && !(this.level().getBlockEntity(pos.above()) instanceof DomainBlockEntity)) {
            BlockPos center = BlockPos.containing(this.position().add(0.0D, radius, 0.0D));
            this.createBlock(delay, pos.above(), radius, Math.sqrt(pos.above().distSqr(center)));
        }

        boolean success = owner.level().setBlock(pos, block.defaultBlockState(),
                Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);

        if (distance >= radius - 1 && success) this.total++;

        if (this.level().getBlockEntity(pos) instanceof DomainBlockEntity be) {
            be.create(this.uuid, delay, state, saved);
        }
    }

    protected List<BlockPos> getFloor() {
        int radius = this.getRadius() - 1;

        BlockPos center = BlockPos.containing(this.position().add(0.0D, radius, 0.0D));

        List<BlockPos> floor = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance > radius) continue;

                    BlockPos pos = center.offset(x, y, z);

                    if (this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty() || !this.level().getBlockState(pos.above()).isAir() ||
                            !this.level().getBlockState(pos).getFluidState().isEmpty()) continue;

                    floor.add(pos);
                }
            }
        }
        return floor;
    }

    protected void createBarrier(boolean instant) {
        this.total = 0;

        LivingEntity owner = this.getOwner();

        if (owner == null) return;

        int radius = this.getRadius();

        BlockPos center = BlockPos.containing(this.position().add(0.0D, radius, 0.0D));

        Vec3 direction = this.getLookAngle();
        Vec3 behind = this.position().subtract(direction.scale(radius - OFFSET)).add(0.0D, radius, 0.0D);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance > radius) continue;

                    BlockPos pos = center.offset(x, y, z);

                    int delay = (int) Math.round(pos.getCenter().distanceTo(behind)) / 2;

                    IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

                    if (cap == null) return;

                    if (instant) {
                        this.createBlock(radius - delay, pos, radius, distance);
                    } else {
                        if (delay == 0) {
                            this.createBlock(radius - delay, pos, radius, distance);
                        } else {
                            IAbilityData data = cap.getAbilityData();
                            data.delayTickEvent(() -> this.createBlock(radius - delay, pos, radius, distance), delay);
                        }
                    }
                }
            }
        }
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pPose) {
        int radius = this.getRadius() * 2;
        return EntityDimensions.fixed(radius, radius);
    }

    protected void doSureHitEffect(@NotNull LivingEntity owner) {
        for (LivingEntity entity : this.getAffected()) {
            IJujutsuCapability cap = entity.getCapability(JujutsuCapabilityHandler.INSTANCE);

            if (cap == null) continue;

            ISorcererData data = cap.getSorcererData();

            if (data.hasTrait(Trait.HEAVENLY_RESTRICTION)) {
                this.ability.onHitBlock(this, owner, entity.blockPosition());
            } else {
                this.ability.onHitEntity(this, owner, entity, false);
            }
        }

        int radius = this.getRadius();
        BlockPos center = this.blockPosition().offset(0, radius / 2, 0);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance < radius - 1) {
                        BlockPos pos = center.offset(x, y, z);
                        this.ability.onHitBlock(this, owner, pos);
                    }
                }
            }
        }
    }

    @Override
    public boolean checkSureHitEffect() {
        int radius = this.getRadius();
        boolean completed = this.getTime() >= radius * 2;

        if (!completed) return false;

        Set<DomainExpansionEntity> domains = VeilHandler.getDomains((ServerLevel) this.level(), this.getBounds());

        for (DomainExpansionEntity domain : domains) {
            if (domain == this) continue;

            if (this.shouldCollapse(domain.getStrength())) {
                this.discard();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();

        if (this.level().isClientSide) return;

        LivingEntity owner = this.getOwner();

        if (owner == null) return;

        IJujutsuCapability cap = owner.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (cap == null) return;

        ISorcererData data = cap.getSorcererData();

        data.setBurnout(DomainExpansion.BURNOUT);

        if (owner instanceof ServerPlayer player) {
            PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(data.serializeNBT()), player);
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();

        this.refreshDimensions();
    }

    private void check() {
        int radius = this.getRadius();

        Vec3 behind = this.position().add(0.0D, radius, 0.0D);
        BlockPos center = BlockPos.containing(behind);

        int count = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    if (distance < radius && distance >= radius - 1) {
                        BlockPos pos = center.offset(x, y, z);

                        if (this.level().getBlockEntity(pos) instanceof DomainBlockEntity be && be.getIdentifier() != null && be.getIdentifier().equals(this.getUUID())) count++;
                    }
                }
            }
        }

        if ((float) count / this.total < 0.75F) {
            this.discard();
        }
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
        super.remove(pReason);

        Set<DomainExpansionEntity> domains = VeilHandler.getDomains((ServerLevel) this.level(), this.getBounds());

        for (DomainExpansionEntity domain : domains) {
            if (domain == this || !(domain instanceof ClosedDomainExpansionEntity closed)) continue;

            closed.createBarrier(true);
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.refreshDimensions();

        LivingEntity owner = this.getOwner();

        if (owner == null) return;

        if (this.level().isClientSide) return;

        int radius = this.getRadius();
        boolean completed = this.getTime() >= radius * 2;

        if (this.checkSureHitEffect()) {
            this.doSureHitEffect(owner);
        }

        if (completed) {
            if (this.getTime() % 20 == 0) {
                this.check();
            }
        }

        ParticleOptions particle = ((DomainExpansion.IClosedDomain) this.ability).getEnvironmentParticle();

        if (particle != null) {
            AABB bounds = this.getBounds();

            for (BlockPos pos : BlockPos.randomBetweenClosed(this.random, 16, (int) bounds.minX, (int) bounds.minY, (int) bounds.minZ,
                    (int) bounds.maxX, (int) bounds.maxY, (int) bounds.maxZ)) {
                if (!this.isInsideBarrier(pos)) continue;
                Vec3 center = pos.getCenter();
                ((ServerLevel) this.level()).sendParticles(particle, center.x, center.y, center.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        if (this.getTime() - 1 == 0) {
            this.createBarrier(false);
        } else if (completed && !this.isInsideBarrier(owner.blockPosition())) {
            this.discard();
        }
    }
}
