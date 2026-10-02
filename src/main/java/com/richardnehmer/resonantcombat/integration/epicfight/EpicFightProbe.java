package com.richardnehmer.resonantcombat.integration.epicfight;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Pure-reflection dump of the Epic Fight API surface relevant to this addon. Cannot break compilation.
 * Run /resonantcombat debug probe and send me the generated resonantcombat-probe.txt (in the game directory).
 */
public final class EpicFightProbe {
    private static final String PKG = "yesman.epicfight.";
    private static final List<String> PATCH_CLASSES = List.of(
            PKG + "world.capabilities.entitypatch.player.ServerPlayerPatch",
            PKG + "world.capabilities.entitypatch.player.PlayerPatch",
            PKG + "world.capabilities.entitypatch.LivingEntityPatch");
    private static final List<String> METHOD_KEYWORDS = List.of("stamina", "mode", "animation", "play", "attack", "skill",
            "combo", "impact", "stun", "hold", "guard", "dodge", "motion", "damage");
    private static final List<String> FIELD_CLASSES = List.of(PKG + "gameasset.Animations", PKG + "skill.SkillSlots",
            PKG + "skill.SkillCategories", PKG + "world.capabilities.item.CapabilityItem$WeaponCategories");
    /** v2: full public method lists (animation registry) and public constructors (to write custom animation registration). */
    private static final List<String> FULL_METHOD_CLASSES = List.of(
            PKG + "api.animation.AnimationManager",
            PKG + "api.animation.AnimationManager$AnimationRegistryEvent",
            PKG + "api.animation.AnimationManager$AnimationBuilder",
            PKG + "api.animation.AnimationManager$AnimationAccessor");
    private static final List<String> CONSTRUCTOR_CLASSES = List.of(
            PKG + "api.animation.types.StaticAnimation",
            PKG + "api.animation.types.ActionAnimation",
            PKG + "api.animation.types.AttackAnimation",
            PKG + "api.animation.types.AttackAnimation$Phase",
            PKG + "api.animation.types.ComboAttackAnimation",
            PKG + "api.animation.types.AirSlashAnimation",
            PKG + "api.animation.types.DashAttackAnimation",
            PKG + "api.animation.types.MovementAnimation",
            PKG + "api.animation.types.InvincibleAnimation",
            PKG + "api.animation.types.LongHitAnimation");
    private static final List<String> ENUM_CLASSES = List.of(PKG + "world.damagesource.StunType");
    private static final List<String> SCAN_PREFIXES = List.of("yesman/epicfight/api/neoevent/", "yesman/epicfight/skill/",
            "yesman/epicfight/world/capabilities/item/", "yesman/epicfight/api/animation/types/");

    public static Path run() throws IOException {
        StringBuilder out = new StringBuilder("# Epic Fight API probe\n");

        for (String name : PATCH_CLASSES) {
            out.append("\n## methods of ").append(name).append("\n");
            try {
                Class<?> cls = Class.forName(name);
                Arrays.stream(cls.getMethods())
                        .filter(m -> METHOD_KEYWORDS.stream().anyMatch(k -> m.getName().toLowerCase(Locale.ROOT).contains(k)))
                        .map(EpicFightProbe::describe).collect(Collectors.toCollection(TreeSet::new))
                        .forEach(line -> out.append(line).append("\n"));
            } catch (Throwable t) { out.append("  (unavailable: ").append(t).append(")\n"); }
        }

        for (String name : FULL_METHOD_CLASSES) {
            out.append("\n## [v2] all public methods of ").append(name).append("\n");
            try {
                Arrays.stream(Class.forName(name).getMethods()).filter(m -> m.getDeclaringClass() != Object.class)
                        .map(EpicFightProbe::describe).collect(Collectors.toCollection(TreeSet::new))
                        .forEach(line -> out.append(line).append("\n"));
            } catch (Throwable t) { out.append("  (unavailable: ").append(t).append(")\n"); }
        }

        for (String name : CONSTRUCTOR_CLASSES) {
            out.append("\n## [v2] public constructors of ").append(name).append("\n");
            try {
                for (var c : Class.forName(name).getConstructors()) {
                    out.append("  (").append(Arrays.stream(c.getGenericParameterTypes()).map(java.lang.reflect.Type::getTypeName)
                            .collect(Collectors.joining(", "))).append(")\n");
                }
            } catch (Throwable t) { out.append("  (unavailable: ").append(t).append(")\n"); }
        }

        for (String name : ENUM_CLASSES) {
            out.append("\n## [v2] enum constants of ").append(name).append("\n");
            try {
                out.append(Arrays.stream(Class.forName(name).getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "))).append("\n");
            } catch (Throwable t) { out.append("  (unavailable: ").append(t).append(")\n"); }
        }

        for (String name : FIELD_CLASSES) {
            out.append("\n## public static fields of ").append(name).append("\n");
            try {
                Class<?> cls = Class.forName(name);
                List<String> names = new ArrayList<>();
                for (Field f : cls.getFields()) if (Modifier.isStatic(f.getModifiers())) names.add(f.getName());
                names.sort(String::compareTo);
                out.append(String.join(", ", names)).append("\n");
            } catch (Throwable t) { out.append("  (unavailable: ").append(t).append(")\n"); }
        }

        out.append("\n## classes in selected Epic Fight packages\n");
        try {
            Class<?> anchor = Class.forName(PKG + "gameasset.Animations");
            URL location = anchor.getProtectionDomain().getCodeSource().getLocation();
            Path path = Path.of(location.toURI());
            List<String> entries = new ArrayList<>();
            if (Files.isDirectory(path)) {
                try (var walk = Files.walk(path)) {
                    walk.map(p -> path.relativize(p).toString().replace('\\', '/')).forEach(entries::add);
                }
            } else {
                try (ZipFile zip = new ZipFile(path.toFile())) {
                    zip.stream().map(ZipEntry::getName).forEach(entries::add);
                }
            }
            for (String prefix : SCAN_PREFIXES) {
                out.append("\n### ").append(prefix).append("\n");
                entries.stream().filter(e -> e.startsWith(prefix) && e.endsWith(".class") && !e.contains("$"))
                        .map(e -> e.substring(prefix.length(), e.length() - 6)).sorted()
                        .forEach(e -> out.append(e).append("\n"));
            }
        } catch (Throwable t) { out.append("  (could not scan jar: ").append(t).append(")\n"); }

        Path file = FMLPaths.GAMEDIR.get().resolve("resonantcombat-probe.txt");
        Files.writeString(file, out.toString());
        return file;
    }

    private static String describe(Method m) {
        return "  " + m.getReturnType().getSimpleName() + " " + m.getName() + "("
                + Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")) + ")"
                + "   [" + m.getDeclaringClass().getSimpleName() + "]";
    }

    private EpicFightProbe() {}
}
