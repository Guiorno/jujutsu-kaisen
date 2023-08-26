package radon.jujutsu_kaisen.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;

import java.util.concurrent.atomic.AtomicReference;

public class AbilityHandler {
    public static Ability.Status trigger(LivingEntity owner, Ability ability) {
        AtomicReference<Ability.Status> result = new AtomicReference<>(Ability.Status.SUCCESS);

        owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            if (ability.getActivationType(owner) == Ability.ActivationType.INSTANT) {
                Ability.Status status;

                if ((status = ability.checkTriggerable(owner)) == Ability.Status.SUCCESS) {
                    MinecraftForge.EVENT_BUS.post(new AbilityTriggerEvent(owner, ability));
                    ability.run(owner);
                }
                result.set(status);
            } else if (ability.getActivationType(owner) == Ability.ActivationType.TOGGLED) {
                Ability.Status status;

                if ((status = ability.checkToggleable(owner)) == Ability.Status.SUCCESS || cap.hasToggled(ability)) {
                    if (!cap.hasToggled(ability)) {
                        MinecraftForge.EVENT_BUS.post(new AbilityTriggerEvent(owner, ability));
                    }
                    cap.toggle(owner, ability);
                }
                result.set(status);
            } else if (ability.getActivationType(owner) == Ability.ActivationType.CHANNELED) {
                Ability.Status status;

                if ((status = ability.checkChannelable(owner)) == Ability.Status.SUCCESS) {
                    cap.channel(owner, ability);
                }
                result.set(status);
            }
        });
        return result.get();
    }
}
