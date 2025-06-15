package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

public class HaxeIntroduceTypeUtil {

    protected static int countGenerics(@Nullable HaxeIdentifier element) {
        HaxeType parentOfType = PsiTreeUtil.getParentOfType(element, HaxeType.class);
        if (parentOfType != null && parentOfType.getTypeParam() != null) {
            return parentOfType.getTypeParam().getTypeList().size();
        }
        return 0;
    }
    protected static boolean requireImport(HaxeIdentifier sourceElement, String targetQname) {
        FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(targetQname); // make sure we filter out
        if(sourceElement.getContainingFile() instanceof HaxeFile haxeFile) {
            return !haxeFile.getPackageName().equals(qualifiedInfo.packagePath);
        }
        return true;
    }

}
