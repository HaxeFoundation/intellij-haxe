package com.intellij.plugins.haxe.model.type;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import org.jetbrains.annotations.NotNull;

public class HaxeConstraintsTypeUtil {

    private static final Key<ParameterizedCachedValue<HaxeClass, PsiElement>> CONSTRUCTABLE_KEY = Key.create("CONSTRUCTABLE_KEY");
    public static final String QNAME_CONSTRUCTIBLE = "haxe.Constraints.Constructible";

    public static SpecificHaxeClassReference createConstructable(@NotNull PsiElement context, @NotNull ResultHolder specific) {
        HaxeClass classByQName = getConstructableType(context);
        HaxeClassReference reference = classByQName != null
                ? new HaxeClassReference(classByQName.getModel(), context)
                : HaxeClassReference.createNoModelClassReference(QNAME_CONSTRUCTIBLE, context);

        return SpecificHaxeClassReference.withGenerics(reference, new ResultHolder[]{specific});
    }

    public static HaxeClass getConstructableType(@NotNull PsiElement context) {
        return getCachedConstructable(context, context.getProject());
    }

    private static HaxeClass getCachedConstructable(@NotNull PsiElement context, Project project) {
        return CachedValuesManager.getManager(project).getParameterizedCachedValue(project, CONSTRUCTABLE_KEY, c -> {
            HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(QNAME_CONSTRUCTIBLE, c);
            return new CachedValueProvider.Result<>(haxeClass, ModificationTracker.EVER_CHANGED);
        }, false, context);
    }
}
