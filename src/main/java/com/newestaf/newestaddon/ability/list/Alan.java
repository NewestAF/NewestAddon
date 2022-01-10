package com.newestaf.newestaddon.ability.list;


import com.newestaf.newestaddon.ability.NewestAbility;
import com.newestaf.newestaddon.utils.glowing.GlowingUtil;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Predicate;


@AbilityManifest(name = "알란", rank = Rank.L, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c사냥꾼의 본능 §f표식이 남겨진 대상에게는 ",
        "$[MARK_MULTIPLE]배의 피해를 가합니다.",
        "§7철괴 좌클릭 §8- §c색출 §f상대방에게 \"표식\" 을 남깁니다. $[MARKING_COOL]",
        "자신은 표식이 남은 상대의 위치를 알 수 있습니다.",
        "§7철괴 우클릭 §8- §c사냥 시작 §f$[HUNT_RANGE]블럭 이내에 있는",
        "표식이 남겨진 플레이어의 뒤로 이동하며 $[HUNT_DAMAGE]의 피해를 입힙니다.",
        "시전시 10초간 힘 II를 받으며 체력이 $[HUNT_COST]% 감소합니다.",
        "지속시간동안 표식이 남은 적을 처치하거나 지속시간이 종료되면 ",
        "체력을 회복함과 동시에 시전위치로 복귀합니다. $[HUNT_COOL]"
})
public class Alan extends NewestAbility implements ActiveHandler {
    private static final Config<Double> MARK_MULTIPLE = new Config<>(Alan.class, "표식_배율", 2d, value -> value > 1),
        HUNT_DAMAGE = new Config<>(Alan.class, "샤냥_피해", 5d);

    private static final Config<Integer> HUNT_RANGE = new Config<>(Alan.class, "사냥_범위", 25, value -> value > 0),
        HUNT_COST = new Config<>(Alan.class, "사냥_소모(%)", 50),
        MARKING_COOL = new Config<>(Alan.class, "색출_쿨타임", 60, Config.Condition.COOLDOWN),
        HUNT_COOL = new Config<>(Alan.class, "사냥_쿨타임", 30, Config.Condition.COOLDOWN);

    private final Cooldown leftCool = new Cooldown(MARKING_COOL.getValue()),
        rightCool = new Cooldown(HUNT_COOL.getValue());

    private Player marked = null;

    private final ActionbarChannel channel = this.newActionbarChannel();

    private Location castLocation;
    private double huntCost;

    private final PotionEffect strength = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 1);

    private final Predicate<Entity> predicate = entity -> {
        if (entity.equals(getPlayer())) return false;
        if (entity instanceof Player) {
            if (!getGame().isParticipating(entity.getUniqueId())) return false;
            AbstractGame.Participant target = getGame().getParticipant(entity.getUniqueId());
            if (getGame() instanceof DeathManager.Handler) {
                DeathManager.Handler game = (DeathManager.Handler) getGame();
                if (game.getDeathManager().isExcluded(entity.getUniqueId())) return false;
            }
            if (getGame() instanceof Teamable) {
                Teamable game = (Teamable) getGame();
                return (!game.hasTeam(getParticipant()) || !game.hasTeam(target) || !game.getTeam(getParticipant()).equals(game.getTeam(target)));
            }
            return target.attributes().TARGETABLE.getValue();
        }
        return true;
    };

    private final HuntDuration huntDuration = new HuntDuration(10, rightCool) {
        @Override
        protected void onDurationProcess(int i) {}
    };

    public Alan(Participant participant) {
        super(participant);
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT) {
            if (clickType == ClickType.LEFT_CLICK && !leftCool.isCooldown()) {
                Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 50, predicate);
                if (player != null) {
                    Marking(player);
                }
            }
            else if (clickType == ClickType.RIGHT_CLICK && !rightCool.isCooldown() && !huntDuration.isDuration()) {
                final List<Player> players = LocationUtil.getNearbyEntities(
                        Player.class, getPlayer().getLocation(), HUNT_RANGE.getValue(), HUNT_RANGE.getValue(), predicate);
                if (!players.isEmpty()) {
                    for (Player player : players) {
                        if (player == marked.getPlayer()) {
                            double cost = HUNT_COST.getValue();
                            castLocation = getPlayer().getLocation();
                            huntCost = getPlayer().getHealth() * (cost / 100);
                            getPlayer().setHealth(getPlayer().getHealth() - huntCost);
                            getPlayer().addPotionEffect(strength);
                            shadowStep(getPlayer(), player);
                            player.damage(HUNT_DAMAGE.getValue());
                            huntDuration.start();
                            return true;
                        }
                    }
                }
                getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
                return false;
            }
        }

        return false;
    }


    @SubscribeEvent
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() == getPlayer() && event.getEntity() == marked){
            double damage = event.getDamage();
            event.setDamage(damage * MARK_MULTIPLE.getValue());
            
        }
    }

    private void Marking(Player player) {
        if (marked != null) {
            GlowingUtil.setGlowing(marked.getPlayer(), getPlayer(), false);
            marked = player;
            GlowingUtil.setGlowing(player, getPlayer(), true);
            channel.update("표식 지정 대상:" + marked.getPlayer().getName());
        }
        marked = player;
        GlowingUtil.setGlowing(player, getPlayer(), true);
        SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer().getLocation());
        channel.update("표식 지정 대상:" + marked.getPlayer().getName());
    }

    private void shadowStep(Player caster, Player target) {
        Location targetLoc = target.getLocation().clone();

        Vector startDir = targetLoc.getDirection().setY(0).normalize();
        Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();

        targetLoc.add(horizOffset).getBlock().getLocation();
        targetLoc.add(targetLoc.getDirection().setY(0).multiply(-1));

        targetLoc.setPitch(0);
        targetLoc.setYaw(targetLoc.getYaw());

        Line lineEffect = Line.between(caster.getLocation(), targetLoc, 100);
        for (Location loc : lineEffect.toLocations(caster.getLocation())) {
            ParticleLib.SMOKE_LARGE.spawnParticle(loc, 0d, 1d, 0d, 12, 0);
        }
        SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(caster.getLocation(), 2f, 0.75f);
        caster.teleport(targetLoc);
    }

    private class HuntDuration extends Duration implements Listener {

        public HuntDuration(int duration, Cooldown cooldown) {
            super(duration, cooldown);
        }


        @Override
        protected void onDurationStart() {
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        }

        @Override
        protected void onDurationProcess(int i) {

        }

        @EventHandler(ignoreCancelled = true)
        private void onDeath(PlayerDeathEvent event) {
            if (event.getEntity() == marked) {
                stop(false);
            }
        }

        @Override
        protected void onDurationEnd() {
            getPlayer().setHealth(getPlayer().getHealth() + huntCost);
            getPlayer().teleport(castLocation);
            getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
            onDurationSilentEnd();
        }

        @Override
        protected void onDurationSilentEnd() {
            HandlerList.unregisterAll(this);
            unregister();
        }

    }

}
