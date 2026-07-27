package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.config.ZooServerConfig;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Salaires : periodiquement, chaque employe (soigneur ou vendeur) coute des
 * emeraudes, prelevees dans la Caisse du Zoo la plus proche. Un employe impaye
 * se met EN GREVE et cesse de travailler jusqu'a la prochaine paie reussie.
 */
public class SalaryHandler {

    /**
     * Paie tous les employes. Appele UNE FOIS PAR JOUR a la fermeture du zoo
     * (ZooDayHandler), pour que le bilan quotidien affiche le vrai montant.
     * Retourne le total preleve.
     */
    public static int payAllKeepers(ServerLevel level) {
        int wage = ZooServerConfig.SALARY_AMOUNT.get();
        if (wage <= 0) return 0;
        int paid = 0, unpaid = 0;
        for (ZooKeeperEntity keeper : level.getEntitiesOfClass(ZooKeeperEntity.class,
                new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 320, 30000000))) {
            // Preleve sur la TRESORERIE TOTALE du zoo (toutes les Tresoreries cumulees)
            boolean ok = ZooVaultBlock.withdrawFromTreasury(level, wage);
            keeper.setOnStrike(!ok);
            if (ok) paid++; else unpaid++;
        }
        int total = paid * wage;
        if (total > 0) {
            com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).addSalaries(total);
        }
        if (unpaid > 0) {
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.literal("\u26A0 ").withStyle(ChatFormatting.RED)
                        .append(Component.translatable("message.ultimatezootaming.salary_unpaid", unpaid)
                                .withStyle(ChatFormatting.GRAY)));
            }
        } else if (paid > 0) {
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.translatable(
                        "message.ultimatezootaming.salary_paid", paid, total)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return total;
    }
}
