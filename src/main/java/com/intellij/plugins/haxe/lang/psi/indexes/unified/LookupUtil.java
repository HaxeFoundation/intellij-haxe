package com.intellij.plugins.haxe.lang.psi.indexes.unified;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class LookupUtil {

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
}
