package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.capability.TamingUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Item representant une Cage ou un Filet PLEIN (le mob capture est dans le NBT,
 * comme un seau a poisson generalise).
 *
 * NOUVEAU SYSTEME (demande Lex) : le contenant se souvient de quel bloc il vient
 * (SourceItem + SourceDamage). A la liberation du mob, le joueur RECUPERE l'item
 * de la cage/du filet d'origine avec +1 point d'usure (vraie barre de durabilite
 * sur l'item, visible dans l'inventaire). Quand la durabilite est epuisee, l'item
 * casse au lieu d'etre rendu.
 *
 * ANTI-DUPLICATION conserve : 1 capture = 1 liberation, le contenant plein est
 * consomme a la liberation (remplace par l'item source use, ou rien s'il casse).
 * Cas sans source (canne a peche) : le contenant est simplement consomme.
 */
public class OccupiedContainerItem extends Item {

    private static final String ROOT_KEY = "ultimatezootaming:container";

    public OccupiedContainerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack capture(ItemStack emptyContainer, LivingEntity target, boolean preTamed,
                                    @Nullable UUID ownerUUID, @Nullable Item sourceItem, int sourceDamage) {
        ItemStack filled = emptyContainer.copy();
        CompoundTag rootTag = new CompoundTag();

        CompoundTag entityTag = new CompoundTag();
        target.save(entityTag);
        rootTag.put("CapturedEntity", entityTag);

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        rootTag.putString("EntityType", typeId.toString());
        rootTag.putBoolean("PreTamed", preTamed);
        if (preTamed && ownerUUID != null) {
            rootTag.putUUID("OwnerUUID", ownerUUID);
        }
        if (sourceItem != null) {
            rootTag.putString("SourceItem", BuiltInRegistries.ITEM.getKey(sourceItem).toString());
            rootTag.putInt("SourceDamage", sourceDamage);
        }

        filled.getOrCreateTag().put(ROOT_KEY, rootTag);
        return filled;
    }

    public static boolean isFilled(ItemStack stack) {
        return stack.hasTag()
                && stack.getTag().contains(ROOT_KEY)
                && stack.getTag().getCompound(ROOT_KEY).contains("CapturedEntity");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isFilled(stack) || level.isClientSide()) {
            return InteractionResultHolder.pass(stack);
        }

        CompoundTag rootTag = stack.getTag().getCompound(ROOT_KEY);
        ResourceLocation typeId = ResourceLocation.tryParse(rootTag.getString("EntityType"));
        if (typeId == null) return InteractionResultHolder.fail(stack);

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
        if (type == null) return InteractionResultHolder.fail(stack);

        Entity entity = type.create(level);
        if (entity == null) return InteractionResultHolder.fail(stack);

        CompoundTag entityTag = rootTag.getCompound("CapturedEntity");
        entity.load(entityTag);
        entity.setUUID(UUID.randomUUID()); // evite les collisions d'UUID
        entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);

        boolean preTamed = rootTag.getBoolean("PreTamed");
        if (preTamed && entity instanceof Mob mob) {
            UUID owner = rootTag.hasUUID("OwnerUUID") ? rootTag.getUUID("OwnerUUID") : player.getUUID();
            TamingUtil.tame(mob, owner, true);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(serverLevel)
                        .addFamiliar(owner, entity.getUUID());
            }
        }

        level.addFreshEntity(entity);

        // --- Rendre la cage/le filet d'origine avec +1 d'usure ---
        ItemStack returned = ItemStack.EMPTY;
        if (rootTag.contains("SourceItem")) {
            ResourceLocation sourceId = ResourceLocation.tryParse(rootTag.getString("SourceItem"));
            Item sourceItem = sourceId != null
                    ? BuiltInRegistries.ITEM.getOptional(sourceId).orElse(null) : null;
            if (sourceItem != null) {
                int newDamage = rootTag.getInt("SourceDamage") + 1;
                ItemStack source = new ItemStack(sourceItem);
                if (newDamage >= source.getMaxDamage()) {
                    // Le contenant a fait son temps : il casse
                    level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK,
                            SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    source.setDamageValue(newDamage);
                    returned = source;
                }
            }
        }

        player.setItemInHand(hand, returned);
        return InteractionResultHolder.success(returned);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isFilled(stack)) {
            CompoundTag rootTag = stack.getTag().getCompound(ROOT_KEY);
            ResourceLocation typeId = ResourceLocation.tryParse(rootTag.getString("EntityType"));
            if (typeId != null) {
                tooltip.add(Component.translatable(
                        "entity." + typeId.getNamespace() + "." + typeId.getPath()));
            }
            if (rootTag.getBoolean("PreTamed")) {
                tooltip.add(Component.translatable("tooltip.ultimatezootaming.container.pretamed"));
            }
            if (rootTag.contains("SourceItem")) {
                ResourceLocation sourceId = ResourceLocation.tryParse(rootTag.getString("SourceItem"));
                Item sourceItem = sourceId != null
                        ? BuiltInRegistries.ITEM.getOptional(sourceId).orElse(null) : null;
                if (sourceItem != null) {
                    tooltip.add(Component.translatable("tooltip.ultimatezootaming.container.source",
                            Component.translatable(sourceItem.getDescriptionId())));
                }
            }
        } else {
            tooltip.add(Component.translatable("tooltip.ultimatezootaming.container.empty"));
        }
    }
}
