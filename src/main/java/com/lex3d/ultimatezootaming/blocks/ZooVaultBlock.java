package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Caisse du Zoo : tresorerie centrale (emeraudes). Paie les salaires, encaisse les ventes. */
public class ZooVaultBlock extends BaseEntityBlock {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> VAULTS = new ConcurrentHashMap<>();

    public ZooVaultBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZooVaultBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) {
            register(level, pos);
        }
    }

    /** Enregistre un coffre dans le registre en memoire (pose OU chargement du chunk). */
    public static void register(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            VAULTS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide() && !state.is(next.getBlock())) {
            Set<BlockPos> s = VAULTS.get(level.dimension());
            if (s != null) s.remove(pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ZooVaultBlockEntity vault)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(com.lex3d.ultimatezootaming.core.init.ModItems.PARK_TICKET.get())) {
            vault.deposit(held.getCount());
            held.setCount(0);
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.vault_balance", vault.getBalance()), true);
            return InteractionResult.CONSUME;
        }
        // Main vide (ou autre item) : le GUI de la tresorerie
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.lex3d.ultimatezootaming.core.network.NetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new com.lex3d.ultimatezootaming.core.network.OpenVaultS2CPacket(
                            pos, vault.getBalance(), totalBalance(level)));
        }
        return InteractionResult.CONSUME;
    }

    /** La caisse la plus proche (pour salaires et ventes). */
    @Nullable
    public static ZooVaultBlockEntity nearestVault(Level level, BlockPos from, double maxDist) {
        Set<BlockPos> set = VAULTS.get(level.dimension());
        if (set == null) return null;
        ZooVaultBlockEntity best = null;
        double bd = maxDist * maxDist;
        for (BlockPos p : set) {
            if (!(level.getBlockEntity(p) instanceof ZooVaultBlockEntity v)) { set.remove(p); continue; }
            double d = from.distSqr(p);
            if (d < bd) { bd = d; best = v; }
        }
        return best;
    }

    /** N'importe quelle caisse valide du monde (pour les primes d'objectifs). */
    @Nullable
    public static ZooVaultBlockEntity anyVault(Level level) {
        Set<BlockPos> set = VAULTS.get(level.dimension());
        if (set == null) return null;
        for (BlockPos p : set) {
            if (level.getBlockEntity(p) instanceof ZooVaultBlockEntity v) return v;
        }
        return null;
    }

    /** N'importe quelle caisse du monde qui a AU MOINS 'min' emeraudes (paiement
     *  des salaires : evite les greves injustifiees). */
    @Nullable
    public static ZooVaultBlockEntity anyVaultWithFunds(Level level, int min) {
        Set<BlockPos> set = VAULTS.get(level.dimension());
        if (set == null) return null;
        for (BlockPos p : set) {
            if (level.getBlockEntity(p) instanceof ZooVaultBlockEntity v && v.getBalance() >= min) return v;
        }
        return null;
    }

    /** Tresorerie totale du monde (toutes les caisses connues). */
    public static int totalBalance(Level level) {
        Set<BlockPos> set = VAULTS.get(level.dimension());
        if (set == null) return 0;
        int total = 0;
        for (BlockPos p : set) {
            if (level.getBlockEntity(p) instanceof ZooVaultBlockEntity v) total += v.getBalance();
        }
        return total;
    }

    /** Preleve 'amount' sur la TRESORERIE totale (peu importe le coffre). Retourne
     *  true si la somme etait disponible. C'est ce que le joueur voit comme "Caisse".
     *  On repartit le retrait sur les coffres jusqu'a atteindre le montant. */
    public static boolean withdrawFromTreasury(Level level, int amount) {
        if (amount <= 0) return true;
        if (totalBalance(level) < amount) return false; // pas assez au total
        Set<BlockPos> set = VAULTS.get(level.dimension());
        if (set == null) return false;
        int remaining = amount;
        for (BlockPos p : set) {
            if (remaining <= 0) break;
            if (level.getBlockEntity(p) instanceof ZooVaultBlockEntity v) {
                int take = Math.min(remaining, v.getBalance());
                if (take > 0 && v.withdraw(take)) remaining -= take;
            }
        }
        return remaining <= 0;
    }

    /** Scan de secours apres un redemarrage (le registre est vide). */
    @Nullable
    public static ZooVaultBlockEntity scanForVault(Level level, BlockPos center, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -8, -radius), center.offset(radius, 8, radius))) {
            if (level.getBlockEntity(p) instanceof ZooVaultBlockEntity v) return v;
        }
        return null;
    }
}
