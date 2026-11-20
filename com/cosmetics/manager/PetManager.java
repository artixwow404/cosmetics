package com.cosmetics.manager;

import com.cosmetics.CosmeticsPlugin;
import com.cosmetics.cosmetic.PetCosmetic;
import com.cosmetics.cosmetic.CompanionCosmetic;
import com.cosmetics.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PetManager {

    private final CosmeticsPlugin plugin;
    private final Map<UUID, Entity> activePets = new ConcurrentHashMap<>();
    private final Map<UUID, Entity> activeCompanions = new ConcurrentHashMap<>();
    private final Map<UUID, Location> companionLocations = new ConcurrentHashMap<>();

    public PetManager(CosmeticsPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnPet(Player player, PetCosmetic cosmetic) {
        despawnPet(player);

        EntityType entityType = cosmetic.getEntityType();
        Location spawnLoc = player.getLocation().add(1, 0, 1);

        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);
        if (entity instanceof Animals animal) {
            configurePet(animal, cosmetic, player);
        } else if (entity instanceof Tameable tameable && entity instanceof Animals animal) {
            tameable.setOwner(player);
            configurePet(animal, cosmetic, player);
        } else if (entity instanceof LivingEntity living) {
            configurePet(living, cosmetic, player);
        }

        activePets.put(player.getUniqueId(), entity);
    }

    private void configurePet(Animals animal, PetCosmetic cosmetic, Player owner) {
        animal.setCustomName(MessageUtil.color(cosmetic.getName()));
        animal.setCustomNameVisible(true);
        animal.setRemoveWhenFarAway(false);
        animal.setInvulnerable(true);
        animal.setCollidable(false);
        animal.setGravity(true);
        animal.setSilent(true);

        if (animal instanceof Tameable tameable) {
            tameable.setOwner(owner);
        }

        if (animal instanceof Ageable ageable) {
            ageable.setAdult();
        }

        String variant = cosmetic.getVariant();
        if (animal instanceof Cat cat && !variant.isEmpty()) {
            try {
                cat.setCatType(Cat.Type.valueOf(variant.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        } else if (animal instanceof Fox fox && !variant.isEmpty()) {
            try {
                fox.setFoxType(Fox.Type.valueOf(variant.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        } else if (animal instanceof Wolf wolf && !variant.isEmpty()) {
            Wolf.Variant wolfVariant = Registry.WOLF_VARIANT.get(NamespacedKey.minecraft(variant.toLowerCase()));
            if (wolfVariant != null) {
                wolf.setVariant(wolfVariant);
            }
        } else if (animal instanceof Rabbit rabbit && !variant.isEmpty()) {
            try {
                rabbit.setRabbitType(Rabbit.Type.valueOf(variant.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        } else if (animal instanceof Panda panda && !variant.isEmpty()) {
            try {
                panda.setMainGene(Panda.Gene.valueOf(variant.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        } else if (animal instanceof Axolotl axolotl && !variant.isEmpty()) {
            try {
                axolotl.setVariant(Axolotl.Variant.valueOf(variant.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        double scale = cosmetic.getScale();
        if (scale != 1.0) {
            setEntityScale(animal, (float) scale);
        }
    }

    private void configurePet(LivingEntity entity, PetCosmetic cosmetic, Player owner) {
        entity.setCustomName(MessageUtil.color(cosmetic.getName()));
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        entity.setInvulnerable(true);
        entity.setCollidable(false);
        entity.setGravity(true);
        entity.setSilent(true);
        entity.setAI(false);

        double scale = cosmetic.getScale();
        if (scale != 1.0) {
            setEntityScale(entity, (float) scale);
        }
    }

    public void despawnPet(Player player) {
        if (player == null) return;
        despawnPet(player.getUniqueId());
    }

    public void despawnPet(UUID playerId) {
        Entity entity = activePets.remove(playerId);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    public void spawnCompanion(Player player, CompanionCosmetic cosmetic) {
        despawnCompanion(player.getUniqueId());

        Location spawnLoc = player.getLocation().add(0, 1.5, 0);
        EntityType entityType = cosmetic.getEntityType();

        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);
        configureCompanion(entity, cosmetic);

        activeCompanions.put(player.getUniqueId(), entity);
        companionLocations.put(player.getUniqueId(), entity.getLocation());
    }

    private void configureCompanion(Entity entity, CompanionCosmetic cosmetic) {
        entity.setCustomName(MessageUtil.color(cosmetic.getName()));
        entity.setCustomNameVisible(false);
        entity.setInvulnerable(true);
        entity.setGravity(false);
        entity.setSilent(true);
        entity.setGlowing(true);

        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
            living.setCollidable(false);
            living.setAI(false);
            living.setInvisible(false);

            double scale = cosmetic.getScale();
            if (scale != 1.0) {
                setEntityScale(living, (float) scale);
            }
        } else if (entity instanceof Display display) {
            display.setTeleportDuration(2);
            double scale = cosmetic.getScale();
            if (scale != 1.0) {
                Transformation tr = display.getTransformation();
                tr.getScale().set((float) scale, (float) scale, (float) scale);
                display.setTransformation(tr);
            }
        }
    }

    private void setEntityScale(LivingEntity entity, float scale) {
        AttributeInstance attr = entity.getAttribute(Attribute.SCALE);
        if (attr != null) {
            attr.setBaseValue(scale);
        }
    }

    public void despawnCompanion(UUID playerId) {
        Entity entity = activeCompanions.remove(playerId);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        companionLocations.remove(playerId);
    }

    public void updateCompanionLocation(UUID playerId, Location location) {
        companionLocations.put(playerId, location);
        Entity entity = activeCompanions.get(playerId);
        if (entity != null && entity.isValid()) {
            entity.teleport(location);
        }
    }

    public void updateAll() {
        for (Map.Entry<UUID, Entity> entry : activePets.entrySet()) {
            UUID playerId = entry.getKey();
            Entity pet = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);

            if (player == null || !player.isOnline() || !pet.isValid()) {
                despawnPet(playerId);
                continue;
            }

            updatePetPosition(pet, player);
        }

        for (Map.Entry<UUID, Entity> entry : activeCompanions.entrySet()) {
            UUID playerId = entry.getKey();
            Entity companion = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);

            if (player == null || !player.isOnline() || !companion.isValid()) {
                despawnCompanion(playerId);
                continue;
            }

            Location targetLoc = companionLocations.get(playerId);
            if (targetLoc != null) {
                companion.teleport(targetLoc);
            }
        }
    }

    private void updatePetPosition(Entity pet, Player player) {
        Location playerLoc = player.getLocation();
        Location petLoc = pet.getLocation();

        if (!petLoc.getWorld().equals(playerLoc.getWorld())) {
            pet.teleport(playerLoc);
            return;
        }

        double distance = petLoc.distance(playerLoc);
        if (distance > 15) {
            pet.teleport(playerLoc.clone().add(1, 0, 1));
            return;
        }

        if (distance > 5) {
            Location target = playerLoc.clone().add(
                (Math.random() - 0.5) * 2,
                0,
                (Math.random() - 0.5) * 2
            );
            Vector direction = target.toVector().subtract(petLoc.toVector()).normalize().multiply(0.5);
            pet.setVelocity(pet.getVelocity().add(direction));
        }

        if (pet instanceof Mob mob) {
            mob.getPathfinder().moveTo(playerLoc, 1.0);
        }
    }

    public void cleanup() {
        for (Entity entity : activePets.values()) {
            if (entity.isValid()) entity.remove();
        }
        activePets.clear();

        for (Entity entity : activeCompanions.values()) {
            if (entity.isValid()) entity.remove();
        }
        activeCompanions.clear();
        companionLocations.clear();
    }

    public boolean hasActivePet(UUID playerId) {
        Entity entity = activePets.get(playerId);
        return entity != null && entity.isValid();
    }

    public boolean hasActiveCompanion(UUID playerId) {
        Entity entity = activeCompanions.get(playerId);
        return entity != null && entity.isValid();
    }
}
