package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.saveddata.ZooDormitory;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Selecteur de parcelle : clic sur 2 blocs = les 2 coins du DORTOIR du personnel. */
public class PlotSelectorItem extends Item {

    public PlotSelectorItem(Properties props) { super(props); }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player.isShiftKeyDown()) {
            CompoundTag tag = stack.getOrCreateTag();
            boolean enclosureMode = !tag.getBoolean("EnclosureMode");
            tag.putBoolean("EnclosureMode", enclosureMode);
            tag.remove("CornerA"); // reset la selection en cours
            player.displayClientMessage(Component.translatable(enclosureMode
                    ? "message.ultimatezootaming.plot_mode_enclosure"
                    : "message.ultimatezootaming.plot_mode_dorm"), true);
        }
        return net.minecraft.world.InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.PASS;
        var player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        BlockPos clicked = ctx.getClickedPos();

        boolean enclosureMode = tag.getBoolean("EnclosureMode");
        if (!tag.contains("CornerA")) {
            tag.putLong("CornerA", clicked.asLong());
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.plot_corner1",
                    clicked.getX(), clicked.getY(), clicked.getZ()), true);
        } else {
            BlockPos a = BlockPos.of(tag.getLong("CornerA"));
            tag.remove("CornerA");
            if (enclosureMode) {
                createRectEnclosure(level, player, a, clicked);
            } else {
                ZooDormitory.get(level).setCorners(a, clicked);
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.plot_done"), true);
            }
            level.playSound(null, clicked, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.4f);
        }
        return InteractionResult.CONSUME;
    }

    /** Cree un enclos RECTANGULAIRE : toutes les colonnes de sol du rectangle
     *  (le volume monte automatiquement a +40 via ZooZone). */
    private static void createRectEnclosure(ServerLevel level, net.minecraft.world.entity.player.Player player,
                                            BlockPos a, BlockPos b) {
        int x0 = Math.min(a.getX(), b.getX()), x1 = Math.max(a.getX(), b.getX());
        int z0 = Math.min(a.getZ(), b.getZ()), z1 = Math.max(a.getZ(), b.getZ());
        int y = Math.min(a.getY(), b.getY());
        Set<Long> floor = new HashSet<>();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                floor.add(BlockPos.asLong(x, y, z));
            }
        }
        ZooSavedData data = ZooSavedData.get(level);
        String name = Component.translatable("zone.ultimatezootaming.default_name",
                data.countZones() + 1).getString();
        ZooZone zone = new ZooZone(UUID.randomUUID(), name, player.getUUID(), floor);
        data.addZone(zone);
        player.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.plot_enclosure_done", (x1 - x0 + 1) * (z1 - z0 + 1)), true);
    }
}
