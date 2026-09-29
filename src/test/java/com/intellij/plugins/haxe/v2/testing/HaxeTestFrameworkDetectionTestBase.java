package com.intellij.plugins.haxe.v2.testing;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** The fixture lookups every framework detection test uses. */
public abstract class HaxeTestFrameworkDetectionTestBase extends HaxeLightFixtureTestCase {

  protected HaxeClass classByQName(String qName) {
    HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(qName, getPsiManager(),
                                                           GlobalSearchScope.allScope(getProject()));
    assertNotNull(haxeClass, "fixture class not found: " + qName);
    return haxeClass;
  }

  protected HaxeMethod methodOf(HaxeClass haxeClass, String name) {
    for (PsiMethod method : haxeClass.getMethods()) {
      if (name.equals(method.getName()) && method instanceof HaxeMethod haxeMethod) {
        return haxeMethod;
      }
    }
    fail("fixture method not found: " + haxeClass.getQualifiedName() + "." + name);
    return null;
  }
}
