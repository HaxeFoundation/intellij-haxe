package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.codeInsight.generation.PsiElementMemberChooserObject;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeNamedElementNode extends PsiElementMemberChooserObject implements ClassMember {
  public HaxeNamedElementNode(final HaxeNamedComponent haxeNamedComponent) {
    super(haxeNamedComponent, buildPresentationText(haxeNamedComponent), haxeNamedComponent.getIcon(Iconable.ICON_FLAG_VISIBILITY));
  }

  @Nullable
  private static String buildPresentationText(HaxeNamedComponent haxeNamedComponent) {
    final ItemPresentation presentation = haxeNamedComponent.getPresentation();
    if (presentation == null) {
      return haxeNamedComponent.getName();
    }
    final StringBuilder result = new StringBuilder();
    if (haxeNamedComponent instanceof HaxeClass) {
      result.append(haxeNamedComponent.getName());
      final String location = presentation.getLocationString();
      if (location != null && !location.isEmpty()) {
        result.append(" (").append(location).append(")");
      }
    }
    else {
      result.append(presentation.getPresentableText());
    }
    return result.toString();
  }

  @Nullable
  @Override
  public MemberChooserObject getParentNodeDelegate() {
    final HaxeNamedComponent result = PsiTreeUtil.getParentOfType(getPsiElement(), HaxeNamedComponent.class);
    return result == null ? null : new HaxeNamedElementNode(result);
  }
}
