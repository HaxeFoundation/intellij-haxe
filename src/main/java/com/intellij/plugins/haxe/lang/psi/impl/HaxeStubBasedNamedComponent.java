package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.StubPsiTreeUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeMethodStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.StubWithName;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * Stub-aware equivalent of AbstractHaxeNamedComponent.
 */
public abstract class HaxeStubBasedNamedComponent<T extends StubElement<?>> extends HaxeStubBasedPsiElementBase<T>
  implements HaxeNamedComponent, PsiNamedElement {

  private HaxeComponentType componentType = null;

  public HaxeStubBasedNamedComponent(@NotNull ASTNode node) {
    super(node);
  }

  public HaxeStubBasedNamedComponent(@NotNull T stub, @NotNull IStubElementType<?, ?> nodeType) {
    super(stub, nodeType);
  }

  @Override
  public HaxeComponentType getComponentType() {
    if (componentType == null) {
      // Try stub data first
      T stub = getGreenStub();
      if (stub instanceof HaxeClassStub classStub) {
        componentType = classStub.getComponentType();
      }
      if (componentType == null) {
        componentType = HaxeComponentType.typeOf(this);
      }
    }
    return componentType;
  }

  @Override
  @Nullable
  @NonNls
  public String getName() {
    // Try to get name from stub first — avoids PSI tree traversal
    T stub = getGreenStub();
    if (stub instanceof StubWithName stubWithName) {
      return stubWithName.getName();
    }
    return getName(this);
  }

  public boolean isMacroName() {
    return (this.getComponentName() != null && this.getComponentName().getIdentifier() instanceof HaxeMacroIdentifier);
  }

  private static String getName(HaxeStubBasedNamedComponent<?> namedComponent) {
    HaxeComponentName componentName = namedComponent.getComponentName();
    if (componentName == null) return null;
    return componentName.getText();
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    final HaxeComponentName componentName = getComponentName();
    if (componentName != null) {
      componentName.setName(name);
    }
    return this;
  }

  @Override
  public Icon getIcon(int flags) {
    final HaxeComponentType type = getComponentType();
    return type == null ? null : type.getIcon();
  }

  @Override
  @NotNull
  public ItemPresentation getPresentation() {
    return new ItemPresentation() {
      @Override
      public String getPresentableText() {
        final StringBuilder result = new StringBuilder();
        HaxeBaseMemberModel model = HaxeBaseMemberModel.fromPsi(HaxeStubBasedNamedComponent.this);

        if (model == null) {
          result.append(HaxeStubBasedNamedComponent.this.getName());
        }
        else {
          if (isFindUsageRequest()) {
            HaxeClassModel klass = model.getDeclaringClass();
            if (null != klass) {
              result.append(klass.getName());
              result.append('.');
            }
          }

          if (model instanceof HaxeEnumValueModel) {
            return model.getPresentableText(null);
          }

          result.append(model.getName());

          if (model instanceof HaxeMethodModel) {
            final String parameterList = HaxePresentableUtil.getPresentableParameterList(model.getNamedComponentPsi());
            result.append("(").append(parameterList).append(")");
          }

          final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(HaxeStubBasedNamedComponent.this, HaxeTypeTag.class);
          if (null != typeTag) {
            final String typeName = HaxePresentableUtil.buildTypeText(HaxeStubBasedNamedComponent.this, typeTag);
            if (!typeName.isEmpty()) {
              result.append(':');
              result.append(typeName);
            }
          }
          else if (model instanceof HaxeObjectLiteralMemberModel objectLiteralMemberModel) {
            ResultHolder type = objectLiteralMemberModel.getResultType(null);
            if (type != null && !type.isUnknown()) {
              result.append(':');
              result.append(type.getType().withoutConstantValue().toPresentationString());
            }
          }
        }

        return result.toString();
      }

      @Override
      public String getLocationString() {
        HaxeClass haxeClass = HaxeStubBasedNamedComponent.this instanceof HaxeClass
                              ? (HaxeClass)HaxeStubBasedNamedComponent.this
                              : PsiTreeUtil.getStubOrPsiParentOfType(HaxeStubBasedNamedComponent.this, HaxeClass.class);
        String path = "";
        if (haxeClass instanceof HaxeAnonymousType) {
          HaxeAnonymousTypeField field = PsiTreeUtil.getParentOfType(haxeClass, HaxeAnonymousTypeField.class);
          while (field != null) {
            boolean addDelimiter = !path.isEmpty();
            path = field.getName() + (addDelimiter ? "." : "") + path;
            field = PsiTreeUtil.getParentOfType(field, HaxeAnonymousTypeField.class);
          }
          final HaxeTypedefDeclaration typedefDeclaration = PsiTreeUtil.getStubOrPsiParentOfType(haxeClass, HaxeTypedefDeclaration.class);
          if (typedefDeclaration != null) {
            haxeClass = typedefDeclaration;
          }
        }

        if (haxeClass == null) {
          return "";
        }

        String qualifiedName = haxeClass.getQualifiedName();
        if (qualifiedName == null) {
          return "";
        }

        final Pair<String, String> qName = HaxeResolveUtil.splitQName(qualifiedName);
        if (haxeClass == HaxeStubBasedNamedComponent.this) {
          return qName.getFirst();
        }
        return qualifiedName + (path.isEmpty() ? "" : "." + path);
      }

      @Override
      public Icon getIcon(boolean open) {
        return HaxeStubBasedNamedComponent.this.getIcon(0);
      }

      private boolean isFindUsageRequest() {
        return HaxeDebugUtil.appearsOnStack(PsiElement2UsageTargetAdapter.class);
      }
    };
  }

  @Override
  public HaxeNamedComponent getTypeComponent() {
    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(getParent(), HaxeTypeTag.class);
    final HaxeTypeOrAnonymous typeOrAnonymous = typeTag != null ? typeTag.getTypeOrAnonymous() : null;
    final HaxeType type = typeOrAnonymous != null ? typeOrAnonymous.getType() : null;
    final PsiReference reference = type != null ? type.getReference() : null;
    if (reference != null) {
      final PsiElement result = reference.resolve();
      if (result instanceof HaxeNamedComponent) {
        return (HaxeNamedComponent)result;
      }
    }
    return null;
  }

  @Override
  public boolean isPublic() {
    // Try stub data first — avoids PSI tree traversal for modifier keywords
    T stub = getGreenStub();
    if (stub instanceof HaxeMethodStub methodStub) {
      return methodStub.isPublic();
    }
    if (stub instanceof HaxeFieldStub fieldStub) {
      return fieldStub.isPublic();
    }
    // For classes, isPublic is handled in AbstractHaxePsiClass override
    // Fall back to PSI-based logic for non-stubbed elements
    PsiElement parentClass = StubPsiTreeUtil.getStubOrPsiParentOfType(this,
            HaxeExternClassDeclaration.class,
            HaxeInterfaceDeclaration.class,
            HaxeEnumDeclaration.class,
            HaxeAnonymousType.class
    );
    if (parentClass != null) {
      return true;
    }

    final PsiElement parent = getParent();
    return hasPublicAccessor(this) || (parent instanceof HaxePsiCompositeElement compositeParent && hasPublicAccessor(compositeParent));
  }

  private static boolean hasPublicAccessor(HaxePsiCompositeElement element) {
    if (UsefulPsiTreeUtil.getChildOfType(element, HaxeTokenTypes.KPRIVATE) != null) {
      return false;
    }
    if (UsefulPsiTreeUtil.getChildOfType(element, HaxeTokenTypes.KPUBLIC) != null) {
      return true;
    }

    final HaxePsiModifier[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(element, HaxePsiModifier.class);
    if (declarationAttributeList != null) {
      final Set<IElementType> declarationTypes = HaxeResolveUtil.getDeclarationTypes(declarationAttributeList);
      if (declarationTypes.contains(HaxeTokenTypes.KPRIVATE)) {
        return false;
      }
      if (declarationTypes.contains(HaxeTokenTypes.KPUBLIC)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isStatic() {
    T stub = getGreenStub();
    if (stub instanceof HaxeMethodStub methodStub) {
      return methodStub.isStatic();
    }
    if (stub instanceof HaxeFieldStub fieldStub) {
      return fieldStub.isStatic();
    }
    final HaxePsiModifier[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(this, HaxePsiModifier.class);
    return HaxeResolveUtil.getDeclarationTypes(declarationAttributeList).contains(HaxeTokenTypes.KSTATIC);
  }

  @Override
  public boolean isOverride() {
    T stub = getGreenStub();
    if (stub instanceof HaxeMethodStub methodStub) {
      return methodStub.isOverride();
    }
    final HaxePsiModifier[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(this, HaxePsiModifier.class);
    return HaxeResolveUtil.getDeclarationTypes(declarationAttributeList).contains(HaxeTokenTypes.KOVERRIDE);
  }

  @Override
  public boolean isOverload() {
    final HaxePsiModifier[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(this, HaxePsiModifier.class);
    return HaxeResolveUtil.getDeclarationTypes(declarationAttributeList).contains(HaxeTokenTypes.KOVERLOAD);
  }

  @Override
  public boolean isInline() {
    T stub = getGreenStub();
    if (stub instanceof HaxeMethodStub methodStub) {
      return methodStub.isInline();
    }
    final HaxePsiModifier[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(this, HaxePsiModifier.class);
    return HaxeResolveUtil.getDeclarationTypes(declarationAttributeList).contains(HaxeTokenTypes.KINLINE);
  }

  @Nullable
  @Override
  public PsiElement getModiferPsi(IElementType tokenType) {
    final HaxePsiModifier[] declarationAttributeList = PsiTreeUtil.getChildrenOfType(this, HaxePsiModifier.class);
    if (declarationAttributeList != null) {
      for (HaxePsiModifier modifier : declarationAttributeList) {
        if (modifier.getFirstChild() instanceof LeafPsiElement psiElement) {
          if (psiElement.getElementType() == tokenType) return psiElement;
        }
      }
    }
    return null;
  }

  @Override
  public String filterName() {
    if (this instanceof HaxeClass haxeClass) {
      return haxeClass.getQualifiedName();
    }
    HaxeClass haxeClass = PsiTreeUtil.getStubOrPsiParentOfType(this, HaxeClass.class);
    if (haxeClass != null) {
      return haxeClass.getQualifiedName() + this.getText();
    }
    return getContainingFile().getName() + this.getText();
  }

  @Nullable
  public final PsiElement findChildByRoleAsPsiElement(int role) {
    ASTNode element = findChildByRole(role);
    if (element == null) return null;
    return SourceTreeToPsiMap.treeElementToPsi(element);
  }

  @Nullable
  public final PsiElement findChildByRoleAsPsiElementIn(PsiElement body, int role) {
    ASTNode element = findChildByRole(body.getFirstChild(), role);
    if (element == null) return null;
    return SourceTreeToPsiMap.treeElementToPsi(element);
  }

  @Nullable
  private ASTNode findChildByRole(PsiElement firstChild, int role) {
    if (firstChild == null) return null;
    for (ASTNode child = firstChild.getNode(); child != null; child = child.getTreeNext()) {
      if (getChildRole(child) == role) return child;
    }
    return null;
  }

  @Nullable
  private ASTNode findChildByRole(int role) {
    return findChildByRole(getFirstChild(), role);
  }

  private int getChildRole(ASTNode child) {
    if (child.getElementType() == HaxeTokenTypes.PLCURLY) {
      return ChildRole.LBRACE;
    }
    else if (child.getElementType() == HaxeTokenTypes.PRCURLY) {
      return ChildRole.RBRACE;
    }
    return 0;
  }

  protected final int getChildRole(ASTNode child, int roleCandidate) {
    if (findChildByRole(roleCandidate) == child) {
      return roleCandidate;
    }
    return 0;
  }
}

