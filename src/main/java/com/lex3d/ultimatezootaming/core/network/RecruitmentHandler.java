package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Genere les candidats affiches par l'ordinateur de recrutement. */
public final class RecruitmentHandler {

    /** Prenoms francais pour les candidats. */
    private static final String[] NAMES = {
            "Lucas", "Emma", "Hugo", "Lea", "Nathan", "Chloe", "Louis", "Manon",
            "Jules", "Sarah", "Tom", "Camille", "Enzo", "Ines", "Theo", "Jade"};

    private RecruitmentHandler() {}

    public static void openFor(ServerPlayer player, BlockPos pos) {
        long seed = (player.level().getDayTime() / 24000L) * 31 + pos.hashCode();
        Random rng = new Random(seed); // candidats stables pour la journee
        List<OpenRecruitmentS2CPacket.Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String name = NAMES[rng.nextInt(NAMES.length)];
            int job = rng.nextInt(5);         // 0 polyvalent .. 4 vendeur
            int skill = 1 + rng.nextInt(5);   // etoiles 1..5
            int cost = 20 + skill * 10;       // salaire d'embauche
            candidates.add(new OpenRecruitmentS2CPacket.Candidate(name, job, skill, cost));
        }
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenRecruitmentS2CPacket(pos, candidates));
    }
}
