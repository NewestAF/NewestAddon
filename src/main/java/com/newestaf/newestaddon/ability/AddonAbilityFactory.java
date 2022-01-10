package com.newestaf.newestaddon.ability;

import com.newestaf.newestaddon.ability.list.Alan;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.annotations.Beta;

import java.lang.annotation.*;
import java.util.*;

public class AddonAbilityFactory {

    protected static final Map<String, Class<? extends NewestAbility>> abilities = new HashMap<>();
    protected static final Map<String, Class<? extends NewestAbility>> test_abilities = new HashMap<>();

    static {
        registerAbility(Alan.class);
    }

    public static void registerAbility(Class<? extends NewestAbility> clazz) {
        if (!abilities.containsValue(clazz)) {
            AbilityFactory.registerAbility(clazz);
            if (AbilityFactory.isRegistered(clazz)) {
                AbilityList.registerAbility(clazz);
                AbilityManifest am = clazz.getAnnotation(AbilityManifest.class);
                if (clazz.getAnnotation(Beta.class) == null) abilities.put(am.name(), clazz);
            } else {
                System.out.println("등록에 실패하였습니다. : " + clazz.getName());
            }
        } else {
            System.out.println("이미 애드온에 등록된 능력입니다 : " + clazz.getName());
        }
    }

    public static void registerTestAbility(Class<? extends NewestAbility> clazz) {
        if (!test_abilities.containsValue(clazz)) {
            AbilityFactory.registerAbility(clazz);
            if (AbilityFactory.isRegistered(clazz)) {
                AbilityManifest am = clazz.getAnnotation(AbilityManifest.class);
                if (clazz.getAnnotation(Beta.class) == null) test_abilities.put(am.name(), clazz);
            } else {
                System.out.println("등록에 실패하였습니다. : " + clazz.getName());
            }
        } else {
            System.out.println("이미 애드온에 등록된 능력입니다 : " + clazz.getName());
        }
    }

    public static List<String> nameValues() {
        return new ArrayList<>(abilities.keySet());
    }

    public static Class<? extends NewestAbility> getTestAbilityByName(String name) {
        return test_abilities.get(name);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface SupportNMS { }
}
