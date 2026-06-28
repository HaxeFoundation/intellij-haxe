package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.lang.psi.fakes.impl.HaxeFakeComponentBindMethod;
import com.intellij.plugins.haxe.lang.psi.fakes.impl.HaxeFakeComponentTrace;
import com.intellij.plugins.haxe.lang.psi.fakes.impl.HaxeFakeTargetSpecificSyntax;
import org.jspecify.annotations.NonNull;

import static com.intellij.plugins.haxe.util.HaxeResolveUtil.findClassOrMemberByQName;

public class HaxeFakePsiUtil {

    public static @NonNull HaxeFakeTargetSpecificSyntax createFakeForSyntax(String name, String qname, HaxeReference reference) {
        HaxeMethod member = findClassOrMemberByQName(qname, reference) instanceof HaxeMethod method ? method : null;
        return new HaxeFakeTargetSpecificSyntax(name, reference, member);
    }
    public static @NonNull HaxeFakeComponentBindMethod createFakeForBind(HaxeIdentifier haxeIdentifier, HaxeNamedComponent namedComponent) {
        return new HaxeFakeComponentBindMethod(haxeIdentifier, namedComponent);
    }

    public static @NonNull HaxeFakeComponentTrace createFakeForTrace(HaxeIdentifier haxeIdentifier) {
        return new HaxeFakeComponentTrace(haxeIdentifier);
    }

}
