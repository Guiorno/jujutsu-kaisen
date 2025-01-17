package radon.jujutsu_kaisen.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.item.veil.modifier.Modifier;
import radon.jujutsu_kaisen.item.veil.modifier.PlayerModifier;

import javax.annotation.Nullable;

public class VeilBlockEntity extends BlockEntity {
    private int counter;

    private boolean initialized;

    @Nullable
    private BlockPos parent;

    @Nullable
    private BlockState original;

    private CompoundTag deferred;

    @Nullable
    private CompoundTag saved;

    private int size;

    public VeilBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(JJKBlockEntities.VEIL.get(), pPos, pBlockState);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, VeilBlockEntity pBlockEntity) {
        if (++pBlockEntity.counter != VeilRodBlockEntity.INTERVAL * 2) return;

        pBlockEntity.counter = 0;

        if (pBlockEntity.parent == null || !(pLevel.getBlockEntity(pBlockEntity.parent) instanceof VeilRodBlockEntity be) || !be.isActive() || be.getSize() != pBlockEntity.size) {
            pBlockEntity.destroy();
        }
    }

    public void destroy() {
        if (this.level == null) return;

        BlockState original = this.getOriginal();

        if (original != null) {
            if (original.isAir()) {
                this.level.setBlockAndUpdate(this.getBlockPos(), Blocks.AIR.defaultBlockState());
            } else {
                this.level.setBlockAndUpdate(this.getBlockPos(), original);

                if (this.saved != null) {
                    BlockEntity be = this.level.getBlockEntity(this.getBlockPos());

                    if (be != null) {
                        be.load(this.saved);
                    }
                }
            }
        } else {
            this.level.setBlockAndUpdate(this.getBlockPos(), Blocks.AIR.defaultBlockState());
        }
    }

    public @Nullable BlockPos getParent() {
        return this.parent;
    }

    public @Nullable BlockState getOriginal() {
        if (this.level == null) return this.original;

        if (this.original == null && this.deferred != null) {
            this.original = NbtUtils.readBlockState(this.level.holderLookup(Registries.BLOCK), this.deferred);
            this.deferred = null;
            this.setChanged();
        }
        return this.original;
    }

    public void create(BlockPos parent, int size, BlockState original, CompoundTag saved) {
        this.initialized = true;
        this.parent = parent;
        this.size = size;
        this.original = original;
        this.saved = saved;
        this.sendUpdates();
    }

    public void sendUpdates() {
        if (this.level != null) {
            this.level.setBlocksDirty(this.worldPosition, this.level.getBlockState(this.worldPosition), this.level.getBlockState(this.worldPosition));
            this.level.sendBlockUpdated(this.worldPosition, this.level.getBlockState(this.worldPosition), this.level.getBlockState(this.worldPosition), 3);
            this.level.updateNeighborsAt(this.worldPosition, this.level.getBlockState(this.worldPosition).getBlock());
            this.setChanged();
        }
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);

        pTag.putBoolean("initialized", this.initialized);

        if (this.initialized) {
            if (this.parent != null) {
                pTag.put("parent", NbtUtils.writeBlockPos(this.parent));
            }

            pTag.putInt("size", this.size);

            if (this.original != null) {
                pTag.put("original", NbtUtils.writeBlockState(this.original));
            } else {
                pTag.put("original", this.deferred);
            }

            if (this.saved != null) {
                pTag.put("saved", this.saved);
            }
        }
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);

        this.size = pTag.getInt("size");

        this.initialized = pTag.getBoolean("initialized");

        if (this.initialized) {
            if (pTag.contains("parent")) {
                this.parent = NbtUtils.readBlockPos(pTag.getCompound("parent"));
            }
            this.deferred = pTag.getCompound("original");
        }

        if (pTag.contains("saved")) {
            this.saved = pTag.getCompound("saved");
        }
    }
}
