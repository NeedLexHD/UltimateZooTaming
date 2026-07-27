package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.EnclosureScanner;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Le Baton d'arpenteur pilote TOUT le systeme d'enclos, sans GUI :
 *
 * - Clic droit sur le sol HORS zone -> detecte l'enclos physique autour du point
 *   clique (flood fill borne par les clotures/murs/denivelles, forme LIBRE) et
 *   le cree avec un nom automatique ("Enclos N"). Bordure montree en particules.
 * - Clic droit sur le sol DANS une de tes zones -> la SELECTIONNE (stockee dans
 *   le baton) + re-montre sa bordure.
 * - Clic droit sur un FAMILIER apprivoise -> l'assigne a la zone selectionnee
 *   (ou le retire s'il y est deja).
 * - Sneak + clic droit dans une zone -> la SUPPRIME (les animaux assignes
 *   repassent en mode libre).
 */
public class SurveyorStaffItem extends Item implements software.bernie.geckolib.animatable.GeoItem {
    // ---- GeckoLib ----
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private com.lex3d.ultimatezootaming.client.render.SurveyorStaffRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) this.renderer = new com.lex3d.ultimatezootaming.client.render.SurveyorStaffRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "idle", 0, state ->
                state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("animation.surveyor_staff.idle"))));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }


    private static final String SELECTED_KEY = "SelectedZone";

    public SurveyorStaffItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ServerLevel serverLevel = (ServerLevel) level;
        ZooSavedData data = ZooSavedData.get(serverLevel);
        BlockPos clicked = context.getClickedPos();
        ItemStack staff = context.getItemInHand();

        ZooZone existing = data.getZoneAt(player.getUUID(), clicked.above());

        // --- Sneak + clic dans une zone : suppression EN DEUX TEMPS ---
        // Un enclos represente beaucoup de travail : on demande confirmation.
        // Le premier sneak+clic arme la suppression, le second (dans les 10 s,
        // sur le MEME enclos) la valide.
        if (player.isShiftKeyDown()) {
            if (existing == null) return InteractionResult.SUCCESS;

            var tagStaff = staff.getOrCreateTag();
            String armedId = tagStaff.getString("uzt_delete_armed");
            long armedAt = tagStaff.getLong("uzt_delete_time");
            long now = serverLevel.getGameTime();
            boolean stillValid = existing.getId().toString().equals(armedId)
                    && (now - armedAt) < 200L; // 10 secondes

            if (!stillValid) {
                // Premier clic : on arme et on previent
                tagStaff.putString("uzt_delete_armed", existing.getId().toString());
                tagStaff.putLong("uzt_delete_time", now);
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.zone_delete_confirm", existing.getName())
                        .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                level.playSound(null, clicked, SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.PLAYERS, 0.7f, 0.6f);
                return InteractionResult.SUCCESS;
            }

            // Second clic : suppression effective
            tagStaff.remove("uzt_delete_armed");
            tagStaff.remove("uzt_delete_time");
            // Les employes qui avaient cet enclos en charge l'oublient
            for (var k : serverLevel.getEntitiesOfClass(
                    com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                    new net.minecraft.world.phys.AABB(-30000000, -64, -30000000,
                            30000000, 320, 30000000))) {
                k.forgetZone(existing.getId());
            }
            data.removeZone(existing.getId());
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.zone_removed", existing.getName()), true);
            level.playSound(null, clicked, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, 1.0f, 0.8f);
            return InteractionResult.SUCCESS;
        }

        // --- Clic dans une zone existante : selection (+ renommage si le baton est nomme) ---
        if (existing != null) {
            staff.getOrCreateTag().putUUID(SELECTED_KEY, existing.getId());

            // Si le Baton a ete renomme dans une enclume, ce nom devient celui de l'enclos.
            if (staff.hasCustomHoverName()) {
                String newName = staff.getHoverName().getString();
                existing.setName(newName);
                ZooSavedData.get(serverLevel).markChanged();
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.zone_renamed", newName), true);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.zone_selected", existing.getName(), existing.size()), true);
            }
            showBorder(serverLevel, existing);
            return InteractionResult.SUCCESS;
        }

        // --- Clic hors zone : detection d'un nouvel enclos ---
        Optional<Set<Long>> scan = EnclosureScanner.scan(serverLevel, clicked);
        if (scan.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.zone_scan_failed", EnclosureScanner.MAX_COLUMNS), true);
            level.playSound(null, clicked, SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        String name = Component.translatable("zone.ultimatezootaming.default_name",
                data.countZones() + 1).getString();
        ZooZone zone = new ZooZone(UUID.randomUUID(), name, player.getUUID(), scan.get());
        data.addZone(zone);
        staff.getOrCreateTag().putUUID(SELECTED_KEY, zone.getId());

        player.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.zone_created", name, zone.size()), true);
        level.playSound(null, clicked, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
        showBorder(serverLevel, zone);
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            net.minecraft.world.level.Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Clic droit dans le vide -> ouvre le GUI de gestion des enclos
        if (level.isClientSide()) {
            com.lex3d.ultimatezootaming.core.network.NetworkHandler.CHANNEL.sendToServer(
                    new com.lex3d.ultimatezootaming.core.network.RequestZonesC2SPacket());
        }
        return net.minecraft.world.InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel serverLevel = (ServerLevel) player.level();

        // Clic sur un SOIGNEUR : l'affecter a l'enclos selectionne
        if (target instanceof com.lex3d.ultimatezootaming.entities.ZooKeeperEntity keeper) {
            java.util.UUID selId = stack.hasTag() && stack.getTag().hasUUID(SELECTED_KEY)
                    ? stack.getTag().getUUID(SELECTED_KEY) : null;
            ZooZone zone = ZooSavedData.get(serverLevel).getZone(selId);
            if (zone == null) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.zone_none_selected"), true);
                return InteractionResult.SUCCESS;
            }
            keeper.setAssignedZone(zone.getId());
            keeper.setOwnerUUID(player.getUUID());
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.keeper_assigned", zone.getName()), true);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    keeper.getX(), keeper.getY() + 2.0, keeper.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            return InteractionResult.SUCCESS;
        }

        return target.getCapability(CapabilityHandler.TAMING_DATA).map(tamingData -> {
            if (!tamingData.isTamed() || !player.getUUID().equals(tamingData.getOwnerUUID())) {
                return InteractionResult.PASS;
            }

            UUID selectedId = stack.hasTag() && stack.getTag().hasUUID(SELECTED_KEY)
                    ? stack.getTag().getUUID(SELECTED_KEY) : null;
            ZooZone zone = ZooSavedData.get(serverLevel).getZone(selectedId);
            if (zone == null) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.zone_none_selected"), true);
                return InteractionResult.SUCCESS;
            }

            if (zone.getId().equals(tamingData.getZoneId())) {
                // Deja dans cet enclos -> on le retire
                tamingData.setZoneId(null);
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.zone_unassigned", target.getDisplayName()), true);
            } else {
                tamingData.setZoneId(zone.getId());
                // Un pensionnaire d'enclos ne despawn JAMAIS, meme sans nom :
                // on le marque des l'assignation sans attendre le prochain tick.
                if (target instanceof net.minecraft.world.entity.Mob m) {
                    m.setPersistenceRequired();
                }
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.zone_assigned", target.getDisplayName(), zone.getName()), true);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                        6, 0.3, 0.3, 0.3, 0.02);
            }
            return InteractionResult.SUCCESS;
        }).orElse(InteractionResult.PASS);
    }

    /** Bordure de la zone en particules (burst ponctuel, visible ~5s). */
    private static void showBorder(ServerLevel level, ZooZone zone) {
        for (BlockPos pos : zone.borderColumns()) {
            level.sendParticles(ParticleTypes.COMPOSTER,
                    pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5,
                    3, 0.1, 0.3, 0.1, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.surveyor_staff.1"));
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.surveyor_staff.2"));
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.surveyor_staff.3"));
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.surveyor_staff.4"));
    }
}
