package radon.jujutsu_kaisen.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.client.particle.ParticleColors;
import radon.jujutsu_kaisen.client.particle.TravelParticle;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.base.JujutsuProjectile;
import radon.jujutsu_kaisen.util.HelperMethods;

public class BlueProjectile extends JujutsuProjectile {
    private static final double RANGE = 30.0D;
    private static final double PULL_STRENGTH = 0.25D;
    private static final int DELAY = 20;

    public BlueProjectile(EntityType<? extends BlueProjectile> pEntityType, Level level) {
        super(pEntityType, level);
    }

    public BlueProjectile(EntityType<? extends BlueProjectile> pEntityType, Level level, LivingEntity pShooter) {
        super(pEntityType, level, pShooter);
    }

    public BlueProjectile(LivingEntity pShooter) {
        this(JJKEntities.BLUE.get(), pShooter.level, pShooter);

        Vec3 look = HelperMethods.getLookAngle(pShooter);
        Vec3 spawn = new Vec3(pShooter.getX(), pShooter.getEyeY() - (this.getBbHeight() / 2.0F), pShooter.getZ()).add(look);
        this.moveTo(spawn.x(), spawn.y(), spawn.z(), pShooter.getYRot(), pShooter.getXRot());
    }

    public float getRadius() {
        return 3.0F;
    }
    protected int getDuration() {
        return 3 * 20;
    }

    protected float getDamage() {
        return 3.0F;
    }

    private void pullEntities() {
        AABB bounds = new AABB(this.getX() - this.getRadius(), this.getY() - this.getRadius(), this.getZ() - this.getRadius(),
                this.getX() + this.getRadius(), this.getY() + this.getRadius(), this.getZ() + this.getRadius());

        Vec3 center = new Vec3(this.getX(), this.getY() + (this.getBbHeight() / 2.0F), this.getZ());

        if (this.getOwner() instanceof LivingEntity owner) {
            for (Entity entity : this.level.getEntities(owner, bounds)) {
                if ((entity instanceof LivingEntity living && !owner.canAttack(living)) || (entity instanceof Projectile projectile && projectile.getOwner() == owner)) continue;

                Vec3 direction = center.subtract(entity.getX(), entity.getY() + (entity.getBbHeight() / 2.0D), entity.getZ()).scale(PULL_STRENGTH);
                entity.setDeltaMovement(direction);
                entity.hurtMarked = true;
            }
        }
    }

    private void hurtEntities() {
        AABB bounds = this.getBoundingBox();

        if (this.getOwner() instanceof LivingEntity owner) {
            owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                for (Entity entity : HelperMethods.getEntityCollisions(this.level, bounds)) {
                    if ((entity instanceof LivingEntity living && !owner.canAttack(living)) || entity == owner) continue;

                    entity.hurt(JJKDamageSources.indirectJujutsuAttack(this, owner, JJKAbilities.BLUE.get()), this.getDamage() * cap.getGrade().getPower(owner));
                }
            });
        }
    }

    private void breakBlocks() {
        AABB bounds = this.getBoundingBox();
        double centerX = bounds.getCenter().x();
        double centerY = bounds.getCenter().y();
        double centerZ = bounds.getCenter().z();

        for (int x = (int) bounds.minX; x <= bounds.maxX; x++) {
            for (int y = (int) bounds.minY; y <= bounds.maxY; y++) {
                for (int z = (int) bounds.minZ; z <= bounds.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = this.level.getBlockState(pos);

                    double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) + Math.pow(z - centerZ, 2));

                    if (distance <= this.getRadius()) {
                        if (state.getFluidState().isEmpty() && state.getBlock().defaultDestroyTime() > Block.INDESTRUCTIBLE) {
                            this.level.destroyBlock(pos, false);
                        }
                    }
                }
            }
        }
    }

    private void spawnParticles() {
        Vec3 center = new Vec3(this.getX(), this.getY() + (this.getBbHeight() / 2.0F), this.getZ());

        float radius = this.getRadius();
        int count = (int) (radius * 4);

        for (int i = 0; i < count; i++) {
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double phi = this.random.nextDouble() * Math.PI;

            double xOffset = radius * Math.sin(phi) * Math.cos(theta);
            double yOffset = radius * Math.sin(phi) * Math.sin(theta);
            double zOffset = radius * Math.cos(phi);

            double x = center.x() + xOffset * (radius * 0.1F);
            double y = center.y() + yOffset * (radius * 0.1F);
            double z = center.z() + zOffset * (radius * 0.1F);

            this.level.addParticle(new TravelParticle.TravelParticleOptions(center.toVector3f(), ParticleColors.DARK_BLUE_COLOR, 0.1F, 1.0F, 5),
                    x, y, z, 0.0D, 0.0D, 0.0D);
        }

        for (int i = 0; i < count; i++) {
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double phi = this.random.nextDouble() * Math.PI;

            double xOffset = radius * 0.5F * Math.sin(phi) * Math.cos(theta);
            double yOffset = radius * 0.5F * Math.sin(phi) * Math.sin(theta);
            double zOffset = radius * 0.5F * Math.cos(phi);

            double x = center.x() + xOffset * (radius * 0.5F * 0.1F);
            double y = center.y() + yOffset * (radius * 0.5F * 0.1F);
            double z = center.z() + zOffset * (radius * 0.5F * 0.1F);

            this.level.addParticle(new TravelParticle.TravelParticleOptions(center.toVector3f(), ParticleColors.LIGHT_BLUE_COLOR, 0.1F, 1.0F, 5),
                    x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pPose) {
        return EntityDimensions.fixed(this.getRadius(), this.getRadius());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getTime() >= this.getDuration()) {
            this.discard();
        } else {
            this.spawnParticles();

            if (this.getOwner() instanceof LivingEntity owner) {
                if (this.getTime() < DELAY) {
                    if (!owner.isAlive()) {
                        this.discard();
                    } else {
                        if (this.getTime() % 5 == 0) {
                            owner.swing(InteractionHand.MAIN_HAND);
                        }
                        Vec3 look = HelperMethods.getLookAngle(owner);
                        Vec3 spawn = new Vec3(owner.getX(), owner.getEyeY() - (this.getBbHeight() / 2.0F), owner.getZ()).add(look);
                        this.moveTo(spawn.x(), spawn.y(), spawn.z(), owner.getYRot(), owner.getXRot());
                    }
                } else {
                    if (this.getTime() == DELAY) {
                        Vec3 start = owner.getEyePosition();
                        Vec3 look = HelperMethods.getLookAngle(owner);
                        Vec3 end = start.add(look.scale(RANGE));
                        HitResult result = HelperMethods.getHitResult(owner, start, end);

                        Vec3 pos = result.getType() == HitResult.Type.MISS ? end : result.getLocation();
                        this.setPos(pos);
                    }
                    this.pullEntities();
                    this.hurtEntities();

                    if (!this.level.isClientSide) {
                        if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                            this.breakBlocks();
                        }
                    }
                }
            }
        }
    }
}