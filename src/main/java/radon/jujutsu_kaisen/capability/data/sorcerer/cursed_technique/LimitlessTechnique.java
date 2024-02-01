package radon.jujutsu_kaisen.capability.data.sorcerer.cursed_technique;

import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.cursed_technique.base.ICursedTechnique;

import java.util.Optional;
import java.util.Set;

public class LimitlessTechnique implements ICursedTechnique {
    @Override
    public @Nullable Ability getDomain() {
        return JJKAbilities.UNLIMITED_VOID.get();
    }

    @Override
    public Ability getImbuement() {
        return JJKAbilities.LIMITLESS_IMBUEMENT.get();
    }

    @Override
    public Set<Ability> getAbilities() {
        return Set.of(JJKAbilities.INFINITY.get(),
                JJKAbilities.RED.get(),
                JJKAbilities.BLUE_STILL.get(),
                JJKAbilities.BLUE_MOTION.get(),
                JJKAbilities.BLUE_FISTS.get(),
                JJKAbilities.HOLLOW_PURPLE.get(),
                JJKAbilities.TELEPORT.get(),
                JJKAbilities.FLY.get());
    }
}