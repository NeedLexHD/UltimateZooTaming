package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.RecruitmentBlock;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity;
import com.lex3d.ultimatezootaming.core.init.ModEntities;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le joueur embauche un candidat : un ZooKeeper apparait pres de l'ordinateur. */
public class HireCandidateC2SPacket {

    private final BlockPos pos;
    private final String name;
    private final int job;
    private final int cost;

    public HireCandidateC2SPacket(BlockPos pos, String name, int job, int cost) {
        this.pos = pos;
        this.name = name;
        this.job = job;
        this.cost = cost;
    }

    public static void encode(HireCandidateC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeUtf(p.name);
        buf.writeInt(p.job);
        buf.writeInt(p.cost);
    }

    public static HireCandidateC2SPacket decode(FriendlyByteBuf buf) {
        return new HireCandidateC2SPacket(buf.readBlockPos(), buf.readUtf(), buf.readInt(), buf.readInt());
    }

    public static void handle(HireCandidateC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            if (!(level.getBlockState(packet.pos).getBlock() instanceof RecruitmentBlock)) return;

            // Payer le cout d'embauche depuis la TRESORERIE TOTALE du zoo
            // (pas depuis une caisse specifique, sinon on peut avoir 500 billets
            // repartis mais chaque coffre n'a pas les 40 requis)
            int totalTreasury = ZooVaultBlock.totalBalance(level);
            if (totalTreasury < packet.cost) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.hire_no_money", packet.cost), true);
                return;
            }
            if (!ZooVaultBlock.withdrawFromTreasury(level, packet.cost)) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.hire_no_money", packet.cost), true);
                return;
            }

            // Faire apparaitre l'employe devant l'ordinateur
            Direction facing = level.getBlockState(packet.pos).getValue(RecruitmentBlock.FACING);
            BlockPos spawn = packet.pos.relative(facing);
            ZooKeeperEntity keeper = ModEntities.ZOO_KEEPER.get().create(level);
            if (keeper == null) return;
            keeper.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    facing.toYRot(), 0);
            keeper.setCustomName(Component.literal(packet.name));
            keeper.setJob(packet.job);
            keeper.setPersistenceRequired();
            keeper.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn),
                    MobSpawnType.EVENT, null, null);
            level.addFreshEntity(keeper);
            // Mission journaliere
            com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).addMissionProgress(
                    com.lex3d.ultimatezootaming.progression.DailyMission.HIRE_STAFF, 1);
            level.playSound(null, spawn, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.3f);
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.hire_ok", packet.name), true);
        });
        ctx.setPacketHandled(true);
    }
}
