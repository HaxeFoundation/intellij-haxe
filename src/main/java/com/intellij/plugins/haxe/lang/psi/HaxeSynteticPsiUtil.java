package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.fakes.impl.HaxeFakeComponentBindMethod;
import org.jspecify.annotations.NonNull;

import static com.intellij.plugins.haxe.lang.psi.fakes.HaxeSyntheticDeclarations.getTargetSpecificSyntax;
import static com.intellij.plugins.haxe.lang.psi.fakes.HaxeSyntheticDeclarations.getTraceDeclaration;
import static com.intellij.plugins.haxe.util.HaxeResolveUtil.findClassOrMemberByQName;

public class HaxeSynteticPsiUtil {

    public static @NonNull HaxeFakeComponentBindMethod createFakeForBind(HaxeIdentifier haxeIdentifier, HaxeNamedComponent namedComponent) {
        return new HaxeFakeComponentBindMethod(haxeIdentifier, namedComponent);
    }

    public static @NonNull HaxeMethod createSynteticForTrace(Project project) {
        return getTraceDeclaration(project);
    }

    public static @NonNull HaxeMethod createSynteticForTargetSpecificSyntax(String name, String qname, HaxeReference reference) {
        return getTargetSpecificSyntax(reference.getProject(), name);
    }

}
