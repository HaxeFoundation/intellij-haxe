package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LookupUtil {

    /**
     * Root packages that mirror compiler targets: the std library's target
     * externs (std/flash is package flash) and libraries shipping per-target
     * externs use exactly these names.
     */
    private static final Set<String> TARGET_ROOT_PACKAGES =
      Set.of("cpp", "cs", "flash", "hl", "java", "js", "jvm", "lua", "php", "python", "neko");

    static boolean isActiveTarget(HaxeComponentIndexData indexData, @NotNull Project project) {
        List<String> targets = indexData.getTargets();
        if(targets.isEmpty()) return true;

        Map<String, String> definitions = HaxeDefineDetectionManager.getInstance(project).getAllDefinitions();
        for (String target : targets) {
            if(!definitions.containsKey(target)) {
                return false;
            }
        }
        return  true;
    }

    static boolean isActiveTarget(PsiElement element) {
        return !HaxeIndexUtil.belongToPlatformNotTargeted(element.getContainingFile());
    }

    /**
     * False when the component lives in a target-specific root package (flash.*,
     * js.*, cpp.* …) of a target that is not active — those classes cannot be used
     * in the current build and must not be offered by completion.
     */
    public static boolean isActiveTargetPackage(@Nullable String packageName, @NotNull Project project) {
        if (packageName == null || packageName.isEmpty()) return true;
        int dot = packageName.indexOf('.');
        String root = dot < 0 ? packageName : packageName.substring(0, dot);
        if (!TARGET_ROOT_PACKAGES.contains(root)) return true;
        return HaxeDefineDetectionManager.getInstance(project).getAllDefinitions().containsKey(root);
    }
}
