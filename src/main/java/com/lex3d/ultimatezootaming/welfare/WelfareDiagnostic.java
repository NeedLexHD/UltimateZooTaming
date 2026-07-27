package com.lex3d.ultimatezootaming.welfare;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Genere le "diagnostic d'enclos" affiche quand on clique sur un Soigneur :
 * satisfaction moyenne + ce qui manque pour atteindre 100%, critere par critere.
 * On agrege sur tous les animaux de l'enclos et on pointe les criteres faibles.
 */
public class WelfareDiagnostic {

    public static void report(ServerLevel level, ZooZone zone, Player player) {
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, zone.boundingBox(),
                a -> a.isAlive() && zone.contains(a.blockPosition())
                        && a.getCapability(CapabilityHandler.TAMING_DATA)
                            .resolve().map(TamingData::isTamed).orElse(false));

        // En-tete
        player.displayClientMessage(Component.translatable("message.ultimatezootaming.diag_header", zone.getName())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        if (animals.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.ultimatezootaming.diag_empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return;
        }

        // Moyennes de chaque critere sur tous les animaux
        int n = animals.size();
        int sumSpace = 0, sumHab = 0, sumFood = 0, sumComp = 0, sumHealth = 0, sumTotal = 0;
        for (Animal a : animals) {
            WelfareCalculator.Breakdown b = WelfareCalculator.computeBreakdown(level, a, zone);
            sumSpace += b.space(); sumHab += b.habitat(); sumFood += b.food();
            sumComp += b.company(); sumHealth += b.health(); sumTotal += b.total();
        }
        int avgTotal = sumTotal / n;
        int sickN = 0, severeN = 0;
        for (Animal a : animals) {
            TamingData d = a.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
            if (d != null && d.isSick()) {
                sickN++;
                if (d.isSevereSick()) severeN++;
            }
        }

        // Satisfaction moyenne (avec couleur)
        ChatFormatting color = avgTotal > 75 ? ChatFormatting.GREEN
                : avgTotal < 25 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        player.displayClientMessage(Component.translatable("message.ultimatezootaming.diag_avg",
                n, avgTotal).withStyle(color), false);
        if (severeN > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.diag_severe", severeN)
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        } else if (sickN > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.diag_sick", sickN)
                    .withStyle(ChatFormatting.RED), false);
        }

        // Traits speciaux presents dans l'enclos (info sympa)
        java.util.Map<String, Integer> traits = new java.util.HashMap<>();
        for (Animal a : animals) {
            a.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(d -> {
                if (d.getTrait() != com.lex3d.ultimatezootaming.capability.TamingData.Trait.NONE) {
                    traits.merge(d.getTrait().name().toLowerCase(), 1, Integer::sum);
                }
            });
        }
        if (!traits.isEmpty()) {
            net.minecraft.network.chat.MutableComponent line =
                    Component.translatable("message.ultimatezootaming.diag_traits").withStyle(ChatFormatting.AQUA);
            boolean first = true;
            for (var e : traits.entrySet()) {
                if (!first) line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                line.append(Component.translatable("trait.ultimatezootaming." + e.getKey())
                        .withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" x" + e.getValue()).withStyle(ChatFormatting.GRAY));
                first = false;
            }
            player.displayClientMessage(line, false);
        }

        if (avgTotal >= 98) {
            player.displayClientMessage(Component.translatable("message.ultimatezootaming.diag_perfect")
                    .withStyle(ChatFormatting.GREEN), false);
            return;
        }

        // Conseils : on liste les criteres sous leur maximum, du plus deficitaire au moins
        boolean[] hab = WelfareCalculator.habitatOf(level, zone);
        addAdvice(player, "space", sumSpace / n, 30, zone.size(), n);
        addAdvice(player, "habitat", sumHab / n, 25, hab, animals);

        // Congeneres par espece : liste les especes SEULES (count == 1)
        java.util.Map<net.minecraft.world.entity.EntityType<?>, Integer> byType = new java.util.HashMap<>();
        for (Animal an : animals) byType.merge(an.getType(), 1, Integer::sum);
        java.util.List<String> lonely = new java.util.ArrayList<>();
        for (var e : byType.entrySet()) {
            if (e.getValue() == 1) lonely.add(Component.translatable(e.getKey().getDescriptionId()).getString());
        }
        if (!lonely.isEmpty()) {
            player.sendSystemMessage(Component.literal("  \u2716 ")
                    .withStyle(net.minecraft.ChatFormatting.RED)
                    .append(Component.translatable("diag.ultimatezootaming.lonely_species",
                            String.join(", ", lonely)).withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
        addAdvice(player, "food", sumFood / n, 20, 0, 0);
        addAdvice(player, "company", sumComp / n, 15, 0, 0);
        addAdvice(player, "health", sumHealth / n, 10, 0, 0);
    }

    private static void addAdvice(Player player, String key, int avg, int max, Object a, Object b) {
        if (avg >= max) {
            // critere au max : petit check vert
            player.displayClientMessage(Component.literal(" \u2714 ")
                    .withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.translatable("diag.ultimatezootaming." + key + "_ok")
                            .withStyle(ChatFormatting.GRAY)), false);
            return;
        }
        // critere insuffisant : croix + conseil
        Component advice;
        if (key.equals("space")) {
            int size = (int) a; int count = (int) b;
            int needed = count * 12; // ~12 cases/animal pour le max
            advice = Component.translatable("diag.ultimatezootaming.space_advice", size, needed);
        } else if (key.equals("habitat")) {
            // Conseils CIBLES : un par profil distinct des animaux presents
            @SuppressWarnings("unchecked")
            java.util.List<Animal> anims = (java.util.List<Animal>) b;
            java.util.Set<String> needs = new java.util.LinkedHashSet<>();
            for (Animal an : anims) {
                var profile = com.lex3d.ultimatezootaming.welfare.HabitatManager.profileOf(an);
                if (profile == com.lex3d.ultimatezootaming.welfare.HabitatProfile.AUTO) {
                    // heuristique : eau pour aquatiques, lave pour mobs du Nether, sinon vegetation
                    if ((net.minecraft.world.entity.LivingEntity) an
                            instanceof net.minecraft.world.entity.animal.WaterAnimal) needs.add("aquatic");
                    else if (an.fireImmune()) needs.add("nether");
                    else needs.add("vegetation");
                } else {
                    needs.add(profile.name().toLowerCase());
                }
                if (needs.size() >= 3) break; // max 3 conseils pour rester lisible
            }
            net.minecraft.network.chat.MutableComponent combined = Component.empty();
            boolean first = true;
            for (String need : needs) {
                if (!first) combined.append(Component.literal(" + "));
                combined.append(Component.translatable("diag.ultimatezootaming.habitat_need." + need));
                first = false;
            }
            advice = combined;
        } else {
            advice = Component.translatable("diag.ultimatezootaming." + key + "_advice");
        }
        player.displayClientMessage(Component.literal(" \u2717 ")
                .withStyle(ChatFormatting.RED)
                .append(advice.copy().withStyle(ChatFormatting.WHITE)), false);
    }
}
