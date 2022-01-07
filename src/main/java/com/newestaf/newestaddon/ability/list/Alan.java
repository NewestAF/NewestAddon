package com.newestaf.newestaddon.ability.list;


import com.newestaf.newestaddon.ability.NewestAbility;
import com.newestaf.newestaddon.utils.glowing.GlowingUtil;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Predicate;


@AbilityManifest(name = "알란", rank = Rank.L, species = Species.HUMAN, explain = {
        "&7패시브 &8- &c사냥꾼의 본능 &f표식이 남겨진 대상에게는 $[MARK_MULTIPLE]배의 피해를 가합니다.",
        "&7철괴 좌클릭 &8- &c색출 &f상대방에게 \"표식\" 을 남깁니다. $[MARKING_COOL]",
        "자신은 표식이 남은 상대의 위치를 알 수 있습니다.",
        "&7철괴 우클릭 &8- &c사냥 시작 &f$[HUNT_RANGE]블럭 이내에 있는",
        "표식이 남겨진 플레이어의 뒤로 이동하며 $[HUNT_DAMAGE]의 피해를 입힙니다.",
        "시전시 10초간 힘 II를 받으며 체력이 $[HUNT_COST]% 감소합니다.",
        "표식이 남은 적을 10초안에 처치하면 체력을 회복함과 동시에 시전위치로 복귀합니다. $[HUNT_COOL]"
})
public class Alan extends NewestAbility implements ActiveHandler {
    private static final Config<Double> MARK_MULTIPLE = new Config<>(Alan.class, "표식_배율", 2d, value -> value > 1),
        HUNT_DAMAGE = new Config<>(Alan.class, "샤냥_피해", 5d);

    private static final Config<Integer> HUNT_RANGE = new Config<>(Alan.class, "사냥_범위", 10, value -> value > 0),
        HUNT_COST = new Config<>(Alan.class, "사냥_소모(%)", 50),
        MARKING_COOL = new Config<>(Alan.class, "색출_쿨타임", 60, value -> value > 0),
        HUNT_COOL = new Config<>(Alan.class, "사냥_쿨타임", 30, value -> value > 0);

    private final Cooldown leftCool = new Cooldown(MARKING_COOL.getValue()),
        rightCool = new Cooldown(HUNT_COOL.getValue());

    private Participant marked = null;

    private final ActionbarChannel channel = this.newActionbarChannel();

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

    private final Duration huntDuration = new Duration(10, rightCool) {
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
                    marked = getGame().getParticipant(player.getUniqueId());
                    Marking(player);
                }
            }
            else if (clickType == ClickType.RIGHT_CLICK && !rightCool.isCooldown() && !huntDuration.isDuration()) {
                final List<Player> players = LocationUtil.getNearbyEntities(
                        Player.class, getPlayer().getLocation(), HUNT_RANGE.getValue(), HUNT_RANGE.getValue(), predicate);
                if (!players.isEmpty()) {
                    for (Player player : players) {
                        if (player == marked.getPlayer()) {

                        }
                    }
                }
                getPlayer().sendMessage("주변에 플레이어가 존재하지 않습니다.");
                return false;
            }
        }

        return false;
    }

    private void Marking(Player player) {
        if (marked != null) {
            GlowingUtil.setGlowing(marked.getPlayer(), getPlayer(), false);
            marked = getGame().getParticipant(player.getUniqueId());
            GlowingUtil.setGlowing(player, getPlayer(), true);
        }
        marked = getGame().getParticipant(player.getUniqueId());
        GlowingUtil.setGlowing(player, getPlayer(), true);
    }

}
