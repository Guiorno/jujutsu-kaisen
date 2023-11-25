package radon.jujutsu_kaisen.client.render.entity.sorcerer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.client.layer.SukunaMarkingsLayer;
import radon.jujutsu_kaisen.entity.CloneEntity;
import radon.jujutsu_kaisen.entity.sorcerer.SukunaEntity;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SukunaRenderer extends HumanoidMobRenderer<SukunaEntity, PlayerModel<SukunaEntity>> {
    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/player/wide/steve.png");

    private final PlayerModel<SukunaEntity> normal;
    private final PlayerModel<SukunaEntity> slim;

    public SukunaRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, null, 0.5F);

        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(pContext.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(pContext.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), pContext.getModelManager()));
        this.addLayer(new SukunaMarkingsLayer<>(this));

        this.normal = new PlayerModel<>(pContext.bakeLayer(ModelLayers.PLAYER), false);
        this.slim = new PlayerModel<>(pContext.bakeLayer(ModelLayers.PLAYER), true);
    }

    @Override
    public void render(SukunaEntity pEntity, float pEntityYaw, float pPartialTicks, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {
        LivingEntity owner = pEntity.getOwner();

        LivingEntityRenderer<?, ?> renderer;

        if (!(owner instanceof Player)) {
            Minecraft mc = Minecraft.getInstance();
            assert mc.level != null;
            EntityType<?> type = pEntity.getKey();

            if (type == EntityType.PLAYER) return;

            LivingEntity entity = (LivingEntity) type.create(mc.level);

            if (entity == null) return;

            renderer = (LivingEntityRenderer<?, ?>) this.entityRenderDispatcher.getRenderer(entity);
        } else {
            renderer = (LivingEntityRenderer<?, ?>) this.entityRenderDispatcher.getRenderer(owner);
        }

        if (renderer.getModel() instanceof PlayerModel<?> player) {
            try {
                this.model = (boolean) FieldUtils.readField(player, "slim", true) ? this.normal : this.slim;
            } catch (IllegalAccessException ignored) {
                return;
            }
        }
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(SukunaEntity pEntity) {
        EntityType<?> type = pEntity.getKey();

        if (type == EntityType.PLAYER) {
            GameProfile profile = pEntity.getPlayer();
            AtomicReference<ResourceLocation> result = new AtomicReference<>(STEVE);
            SkullBlockEntity.updateGameprofile(profile, updated -> {
                Minecraft mc = Minecraft.getInstance();
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = mc.getSkinManager().getInsecureSkinInformation(updated);
                result.set(map.containsKey(MinecraftProfileTexture.Type.SKIN) ? mc.getSkinManager().registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN) : DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(updated)));
            });
            return result.get();
        } else {
            Minecraft mc = Minecraft.getInstance();
            assert mc.level != null;
            Entity entity = type.create(mc.level);
            assert entity != null;
            return this.entityRenderDispatcher.getRenderer(entity).getTextureLocation(entity);
        }
    }
}
