package com.intellij.plugins.haxe.lang.psi;

/**
 * @author: Fedor.Korotkov
 */

import com.intellij.psi.PsiLanguageInjectionHost;
import org.intellij.lang.regexp.RegExpLanguageHost;

public interface HaxeRegularExpression
  extends HaxePsiCompositeElement, PsiLanguageInjectionHost, RegExpLanguageHost, HaxeLiteralExpression {
}
