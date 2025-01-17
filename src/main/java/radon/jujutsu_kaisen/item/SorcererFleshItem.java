package radon.jujutsu_kaisen.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.data.JJKAttachmentTypes;
import radon.jujutsu_kaisen.data.capability.IJujutsuCapability;
import radon.jujutsu_kaisen.data.capability.JujutsuCapabilityHandler;
import radon.jujutsu_kaisen.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.config.ConfigHolder;

public class SorcererFleshItem extends CursedEnergyFleshItem {
    public SorcererFleshItem(Properties pProperties) {
        super(pProperties);
    }

    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack pStack, @NotNull Level pLevel, @NotNull LivingEntity pEntityLiving) {
        ItemStack stack = super.finishUsingItem(pStack, pLevel, pEntityLiving);

        IJujutsuCapability cap = pEntityLiving.getCapability(JujutsuCapabilityHandler.INSTANCE);

        if (cap == null) return stack;

        ISorcererData data = cap.getSorcererData();

        if (data.getType() == JujutsuType.CURSE) {
            data.addExtraEnergy((getGrade(pStack).ordinal() + 1) * ConfigHolder.SERVER.cursedObjectEnergyForGrade.get().floatValue());
        }
        return stack;
    }
}
