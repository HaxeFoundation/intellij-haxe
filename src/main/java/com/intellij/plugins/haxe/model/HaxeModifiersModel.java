package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.ModifierConstant;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.StubWithModifiers;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataListOwner;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeModifiersModel {
  private PsiElement baseElement;

  public HaxeModifiersModel(PsiElement baseElement) {
    this.baseElement = baseElement;
  }

  public boolean hasModifier(@ModifierConstant String modifier) {
    if (baseElement instanceof StubBasedPsiElement<?> element) {
      if( element.getStub() instanceof StubWithModifiers stub) {
        Boolean result = stub.hasKeywordModifier(modifier);
        if (result != null) return result;
        result = stub.hasMetaModifier(modifier);
        if (result != null) return result;
      }
    }
    return getModifierPsi(modifier) != null;
  }

  public boolean hasAnyModifier(@ModifierConstant String... modifiers) {
    for (String modifier : modifiers) if (hasModifier(modifier)) return true;
    return false;
  }

  public boolean hasAllModifiers(@ModifierConstant String... modifiers) {
    for (String modifier : modifiers) if (!hasModifier(modifier)) return false;
    return true;
  }

  @Nullable
  public PsiElement getModifierPsi(@ModifierConstant String modifier) {
    PsiElement result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxePsiModifier.class, modifier);

    if (result == null && baseElement instanceof HaxeMetadataListOwner) {
      // Fast path: read metadata flags from stub to avoid expensive sibling PSI traversal.
      Boolean fromStub = getMetaModifierFromStub(modifier);
      if (fromStub != null) {
        return fromStub ? baseElement : null;
      }

      // Slow path: walk preceding siblings to find metadata annotations.
      HaxeMetadataList metas = ((HaxeMetadataListOwner)baseElement).getMetadataList(HaxeMeta.COMPILE_TIME);
      for (HaxeMeta meta : metas) {
        if (meta.isType(modifier)) {
          result = meta.getContainer();
          break;
        }
      }
    }

    return result;
  }

  /**
   * Checks whether the given modifier is present as a compile-time metadata annotation
   * using stub data, without touching the PSI tree.
   *
   * @return {@code true}/{@code false} if the stub has definitive info,
   *         {@code null} if no stub is available or the modifier is not tracked.
   */
  @Nullable
  private Boolean getMetaModifierFromStub(@ModifierConstant String modifier) {
    if (baseElement instanceof StubBasedPsiElement<?> element) {
      if( element.getStub() instanceof StubWithModifiers stub) {
        Boolean result = stub.hasMetaModifier(modifier);
        if (result != null) return result;
      }
    }
    return null;
  }


  public void replaceVisibility(@ModifierConstant String modifier) {
    PsiElement psi = getVisibilityPsi();
    if (psi != null) {
      getDocument().replaceElementText(psi, HaxePsiModifier.getStringWithSpace(modifier), StripSpaces.AFTER);
    } else {
      addModifier(modifier);
    }
  }

  public void removeModifier(@ModifierConstant String modifier) {
    PsiElement psi = getModifierPsi(modifier);
    if (psi != null) {
      getDocument().replaceElementText(psi, "", StripSpaces.AFTER);
    }
  }


  private HaxeDocumentModel _document = null;

  @NotNull
  public HaxeDocumentModel getDocument() {
    if (_document == null) _document = new HaxeDocumentModel(baseElement);
    return _document;
  }

  public void addModifier(@ModifierConstant String modifier) {
    getDocument().addTextBeforeElement(baseElement, HaxePsiModifier.getStringWithSpace(modifier));
  }

  public PsiElement getVisibilityPsi() {
    PsiElement element = getModifierPsi(HaxePsiModifier.PUBLIC);
    if (element == null) element = getModifierPsi(HaxePsiModifier.PRIVATE);
    return element;
  }

  public @ModifierConstant
  String getVisibility() {
    if (getModifierPsi(HaxePsiModifier.PUBLIC) != null) return HaxePsiModifier.PUBLIC;
    if (getModifierPsi(HaxePsiModifier.PRIVATE) != null) return HaxePsiModifier.PRIVATE;

    return HaxePsiModifier.EMPTY;
  }
}
