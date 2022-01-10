package com.newestaf.newestaddon.ability.list;

import com.newestaf.newestaddon.ability.NewestAbility;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;


@AbilityManifest(name = "블랙 프라이어", rank = Rank.S, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c방벽 자세 §f보조 손에 방패를 든 채로 웅크리는 중에 방벽 자세를 취합니다.",
        "방벽자세를 취하고 있을때는 방패의 내구도가 지속적으로 초당 10 감소합니다.",
        "방벽자세에서 피해를 받으면 그 피해를 방어하며 방패의 내구도가 $[BULWARK_DURABILITY] 감소합니다.",
        "블랙 프라이어의 액티브스킬은 모두 방벽자세인 상태에서만 사용가능합니다.",
        "방벽자세 중에는 이동하지 못합니다.",
        "§7방패 우클릭 §8- §c방벽 역습 §f사용 후 $[COUNTER_DURATION]초 안에 공격을 받을 시",
        "공격한 대상을 $[COUNTER_STUN]초 기절시키고 다음 공격의 피해가 $[COUNTER_MULTIPLE]배 증가합니다.",
        "방어에 실패할 시 본인이 기절합니다. $[COUNTER_COOL]"
})
public class BlackPrior extends NewestAbility implements ActiveHandler {
    
    private static final Config<Integer> BULWARK_DURABILITY = new Config<>(BlackPrior.class, "방벽자세_소모내구도", 50),
        COUNTER_COOL = new Config<>(BlackPrior.class, "방벽역습_쿨타임", 7, Config.Condition.COOLDOWN);

    private static final Config<Double> COUNTER_DURATION = new Config<>(BlackPrior.class, "방벽역습_지속시간", 0.4, Config.Condition.TIME),
        COUNTER_STUN = new Config<>(BlackPrior.class, "방벽역습_기절", 2d, Config.Condition.TIME),
        COUNTER_MULTIPLE = new Config<>(BlackPrior.class, "방벽역습_배수", 2.5);

    private final Cooldown rightcool = new Cooldown(COUNTER_COOL.getValue());

    private boolean isBulwark = false;
    private final BulwarkPos bulwarkPos = new BulwarkPos();
    private final BulwarkCounter bulwarkCounterDuration = new BulwarkCounter((int) Math.round(20 * COUNTER_DURATION.getValue()));


    public BlackPrior(Participant participant) { super(participant); }

    @SubscribeEvent(onlyRelevant = true, ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            bulwarkPos.start();
        }
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (clickType == ClickType.RIGHT_CLICK && bulwarkPos.isRunning() && rightcool.isCooldown()) {
            bulwarkCounterDuration.start();
            bulwarkPos.stop(false);
            return true;
        }
        return false;
    }

    private class BulwarkPos extends AbilityTimer implements Listener {

        private final Material shield = Material.SHIELD;

        private int count;

        private ArmorStand armorStand1,
            armorStand2,
            armorStand3;

        private final EulerAngle eulerAngle1 = new EulerAngle(Math.toRadians(90), 0, 0),
            eulerAngle2 =  new EulerAngle(Math.toRadians(-90), 0, 0),
            eulerAngle3 =  new EulerAngle(Math.toRadians(180), 0, 0);

        private BulwarkPos() {
            super();
            setPeriod(TimeUnit.TICKS, 1);
            register();
            armorStand1 = newArmourstand(eulerAngle1);
            armorStand2 = newArmourstand(eulerAngle2);
            armorStand3 = newArmourstand(eulerAngle3);
        }

        @Override
        protected void onStart() {
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        }

        @Override
        protected void run(int i) {
            final PlayerInventory inventory = getPlayer().getInventory();
            final ItemStack shieldItem = inventory.getItemInOffHand();
            if (shield == shieldItem.getType() && getPlayer().isSneaking()) {
                if (count == 20) {
                    Damageable damageable = (Damageable) shieldItem.getItemMeta();
                    damageable.setDamage(damageable.getDamage() - 10);
                    shieldItem.setItemMeta(damageable);
                    count = 0;
                }
                else {
                    count ++;
                }
            }
            else {
                stop(false);
            }
        }

        private ArmorStand newArmourstand(EulerAngle angle) {
            ArmorStand armorStand = getPlayer().getWorld().spawn(getPlayer().getLocation(), ArmorStand.class);
            armorStand.setFireTicks(Integer.MAX_VALUE);
            NMS.removeBoundingBox(armorStand);
            armorStand.setMetadata("BlackPrior", new FixedMetadataValue(AbilityWar.getPlugin(), null));
            armorStand.setVisible(false);
            armorStand.setInvulnerable(true);
            armorStand.getEquipment().setItemInMainHand(new ItemStack(Material.SHIELD));
            armorStand.setGravity(false);
            armorStand.setRightArmPose(angle);
            return armorStand;
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            if (event.getEntity() == getPlayer() && isRunning()) {
                event.setCancelled(true);
                final PlayerInventory inventory = getPlayer().getInventory();
                final ItemStack shieldItem = inventory.getItemInOffHand();
                Damageable damageable = (Damageable) shieldItem.getItemMeta();
                damageable.setDamage(damageable.getDamage() - BULWARK_DURABILITY.getValue());
                shieldItem.setItemMeta(damageable);
                NMS.broadcastEntityEffect(getPlayer(), (byte) 29);
            }
        }

        @EventHandler
        private void onPlayerMove(PlayerMoveEvent event) {
            if (event.getPlayer() == getPlayer() && isRunning()) {
                event.setTo(event.getFrom());
            }
        }

        @Override
        protected void onEnd() {
            onSilentEnd();
        }

        @Override
        protected void onSilentEnd() {
            HandlerList.unregisterAll(this);
            unregister();
        }
    }

    private class BulwarkCounter extends AbilityTimer implements Listener {

        private int duration;

        private BulwarkCounter(int dur) {
            super();
            duration = dur;
            setPeriod(TimeUnit.TICKS, 1);
            register();
        }

        @Override
        protected void onStart() {
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        }

        @Override
        protected void run(int i) {
            if (i >= duration) {
                failParrying();
            }
        }

        @EventHandler(ignoreCancelled = true)
        private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            if (event.getEntity() == getPlayer() && isRunning()) {
                Stun.apply(getGame().getParticipant(event.getDamager().getUniqueId()), TimeUnit.TICKS, (int) Math.round(20 * COUNTER_STUN.getValue()));
                stop(false);
            }
        }

        private void failParrying() {
            Stun.apply(getGame().getParticipant(getPlayer().getUniqueId()), TimeUnit.TICKS, (int) Math.round(20 * COUNTER_STUN.getValue()));
            stop(false);
        }

        @Override
        protected void onEnd() {
            onSilentEnd();
        }

        @Override
        protected void onSilentEnd() {
            HandlerList.unregisterAll(this);
            unregister();
        }
    }
}
