package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.plugins.haxe.util.HaxeModuleVariants;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class LookupUtil {

    static boolean isActiveTarget(HaxeComponentIndexData indexData, @NotNull Project project) {
        // TODO: the active build's defines answer for every requester; a per-file answer needs the requesting file's resolve context
        List<String> targets = indexData.getTargets();
        if (targets.isEmpty()) return true;

        Map<String, String> definitions = HaxeConditionalExpression.projectDefinitions(project);
        return definitions.keySet().containsAll(targets);
    }

    static boolean isActiveTarget(PsiElement element) {
        return !HaxeIndexUtil.belongToPlatformNotTargeted(element.getContainingFile());
    }

    /**
     * False when the component lives in a target-specific root package (flash.*,
     * js.*, cpp.* ...) of a target that is not active: those classes cannot be used
     * in the current build and must not be offered by completion.
     */
    public static boolean isActiveTargetPackage(@Nullable String packageName, @NotNull Project project) {
        // TODO: the active build's defines answer for every requester; a per-file answer needs the requesting file's resolve context
        if (packageName == null || packageName.isEmpty()) return true;
        int dot = packageName.indexOf('.');
        String root = dot < 0 ? packageName : packageName.substring(0, dot);
        return !HaxeModuleVariants.TARGET_ROOT_PACKAGES.contains(root) || HaxeModuleVariants.isActive(root, project);
    }
}
