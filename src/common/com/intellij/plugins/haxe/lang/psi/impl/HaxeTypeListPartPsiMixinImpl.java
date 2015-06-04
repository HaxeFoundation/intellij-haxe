/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.impl.source.tree.java.PsiTypeParameterImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ebishton on 10/22/14.
 */
public class HaxeTypeListPartPsiMixinImpl extends HaxePsiCompositeElementImpl implements HaxeTypeListPartPsiMixin {

  // XXX: TypeListPart might be better just being a private entry in the BNF.  I'm not sure.
  //
  // The thing I most dislike about subclassing PsiTypeParameter is that it implements
  // the PsiClass interface.  One of the (three possible)child elements that this class
  // holds is HaxeAnonymousType, which itself derives from HaxeClass (thus, PsiClass).
  // So, I'd prefer that HaxeTypePsiMixin implement the class interface.  Then we
  // would have better balance between the child implementations (and less special code)
  // and we can cleanly ignore this class in the hierarchy.
  //
  // The third child type that we can hold is HaxeFunctionType, which is simply a
  // construct for holding (effectively) a typedef, described via the arrow notation:
  //   type -> type -> return_type
  // This type doesn't really support a class, just gives a function pointer.  So, now
  // we've got a mostly empty stub class as well...

  static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeListPartPsiMixinImpl");
  static { LOG.setLevel(Level.DEBUG); }


  // The child type that implements an interface for one of these three parameter types.
  // We can't assign this type in the constructor because the children aren't known
  // (to us) at construction time.  We'll check it later when one of the interfaces
  // are hit.
  PsiClass myChildClass = null;

  HaxeTypeListPartPsiMixinImpl(ASTNode node) {
    super(node);
  }

  @NotNull
  private PsiClass getDelegate() {
    // We're going to try to cache our child.  If the code changes, then this reference
    // may refer to the wrong class.  In that case, it would be  better to do the
    // lookup (resolveHaxeClass()), which has a caching algorithm of its own and
    // deals with naming changes accordingly.

    if (null == myChildClass) {
      PsiElement child = getFirstChild();
      PsiElement target = null;
      ASTNode node = child.getNode();
      if ( null != node ) {

        IElementType type = node.getElementType();
        if (HaxeTokenTypes.TYPE_OR_ANONYMOUS.equals(type)) {

          target = child.getFirstChild();
          IElementType targetType = target.getNode().getElementType();

          if (HaxeTokenTypes.TYPE.equals(targetType)) {
            myChildClass = new HaxeClassDelegateForTypeChild((HaxeType)target);
          } else if (HaxeTokenTypes.ANONYMOUS_TYPE.equals(targetType)){
            myChildClass = (HaxeAnonymousType) target;
          } else {
            LOG.debug("Target: " + target.toString());
            LOG.assertTrue(false, "Unexpected token type for child of TYPE_OR_ANONYMOUS");
            myChildClass = AbstractHaxePsiClass.EMPTY_FACADE;
          }

        } else if (HaxeTokenTypes.FUNCTION_TYPE.equals(type)) {
          // TODO: Fix this.  Temporary hack in place so I can test.
          myChildClass = (HaxeAnonymousType) child;
        } else {
          myChildClass = AbstractHaxePsiClass.EMPTY_FACADE;
        }
      } else {
        // No child node?  How can this be?
        LOG.assertTrue(false, "No child node found for TYPE_LIST_PART");
        myChildClass = AbstractHaxePsiClass.EMPTY_FACADE;
      }
    }
    return myChildClass;

  }


  //
  // PsiTypeParameter overrides
  //

  @Override
  public PsiTypeParameterListOwner getOwner() {
    final PsiElement parent = getParent();
    if (parent == null) throw new PsiInvalidElementAccessException(this);
    return PsiTreeUtil.getParentOfType(this, PsiTypeParameterListOwner.class);
  }

  @Override
  public int getIndex() {
    int ret = 0;
    PsiElement element = getPrevSibling();
    while (element != null) {
      if (element instanceof PsiTypeParameter) {
        ret++;
      }
      element = element.getPrevSibling();
    }
    return ret;
  }


  //
  // PsiClass overrides
  //

  @Nullable
  @Override
  public String getQualifiedName() {
    return getDelegate().getQualifiedName();
  }

  @Override
  public boolean isInterface() {
    return getDelegate().isInterface();
  }

  @Override
  public boolean isAnnotationType() {
    return getDelegate().isAnnotationType();
  }

  @Override
  public boolean isEnum() {
    return getDelegate().isEnum();
  }

  @Override
  @NotNull
  public PsiField[] getFields() {
    return getDelegate().getFields();
  }

  @Override
  @NotNull
  public PsiMethod[] getMethods() {
    return getDelegate().getMethods();
  }

  @Override
  public PsiMethod findMethodBySignature(PsiMethod patternMethod, boolean checkBases) {
    return getDelegate().findMethodBySignature(patternMethod, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
    return getDelegate().findMethodsBySignature(patternMethod, checkBases);
  }

  @Override
  public PsiField findFieldByName(String name, boolean checkBases) {
    return getDelegate().findFieldByName(name, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsByName(String name, boolean checkBases) {
    return getDelegate().findMethodsByName(name, checkBases);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(String name, boolean checkBases) {
    return getDelegate().findMethodsAndTheirSubstitutorsByName(name, checkBases);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
    return getDelegate().getAllMethodsAndTheirSubstitutors();
  }

  @Override
  public PsiClass findInnerClassByName(String name, boolean checkBases) {
    return getDelegate().findInnerClassByName(name, checkBases);
  }

  @Override
  public PsiTypeParameterList getTypeParameterList() {
    return getDelegate().getTypeParameterList();
  }

  @Override
  public boolean hasTypeParameters() {
    return getDelegate().hasTypeParameters();
  }

  @Override
  public PsiElement getScope() {
    return getDelegate().getScope();
  }

  @Override
  public boolean isInheritorDeep(PsiClass baseClass, PsiClass classToByPass) {
    return getDelegate().isInheritorDeep(baseClass, classToByPass);
  }

  @Override
  public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
    return getDelegate().isInheritor(baseClass, checkDeep);
  }

  @Override
  @Nullable
  public PsiIdentifier getNameIdentifier() {
    return getDelegate().getNameIdentifier();
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    return getDelegate().setName(name);
  }

  @Override
  @NotNull
  public PsiMethod[] getConstructors() {
    return getDelegate().getConstructors();
  }

  @Override
  public PsiDocComment getDocComment() {
    return getDelegate().getDocComment();
  }

  @Override
  public boolean isDeprecated() {
    return getDelegate().isDeprecated();
  }

  @Override
  @NotNull
  public PsiReferenceList getExtendsList() {
    return getDelegate().getExtendsList();
  }

  @Override
  public PsiReferenceList getImplementsList() {
    return getDelegate().getImplementsList();
  }

  @Override
  @NotNull
  public PsiClassType[] getExtendsListTypes() {
    return getDelegate().getExtendsListTypes();
  }

  @Override
  @NotNull
  public PsiClassType[] getImplementsListTypes() {
    return getDelegate().getImplementsListTypes();
  }

  @Override
  @NotNull
  public PsiClass[] getInnerClasses() {
    return getDelegate().getInnerClasses();
  }

  @Override
  @NotNull
  public PsiField[] getAllFields() {
    return getDelegate().getAllFields();
  }

  @Override
  @NotNull
  public PsiMethod[] getAllMethods() {
    return getDelegate().getAllMethods();
  }

  @Override
  @NotNull
  public PsiClass[] getAllInnerClasses() {
    return getDelegate().getAllInnerClasses();
  }

  @Override
  @NotNull
  public PsiClassInitializer[] getInitializers() {
    return getDelegate().getInitializers();
  }

  @Override
  @NotNull
  public PsiTypeParameter[] getTypeParameters() {
    return getDelegate().getTypeParameters();
  }

  @Override
  public PsiClass getSuperClass() {
    return getDelegate().getSuperClass();
  }

  @Override
  public PsiClass[] getInterfaces() {
    return getDelegate().getInterfaces();
  }

  @Override
  @NotNull
  public PsiClass[] getSupers() {
    return getDelegate().getSupers();
  }

  @Override
  @NotNull
  public PsiClassType[] getSuperTypes() {
    return getDelegate().getSuperTypes();
  }

  @Override
  public PsiClass getContainingClass() {
    return getDelegate().getContainingClass();
  }

  @Override
  @NotNull
  public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    return getDelegate().getVisibleSignatures();
  }

  @Override
  public HaxeModifierList getModifierList() {
    return (HaxeModifierList) getDelegate().getModifierList();
  }

  @Override
  public boolean hasModifierProperty(@NotNull String name) {
    return getDelegate().hasModifierProperty(name);
  }

  @Override
  public PsiJavaToken getLBrace() {
    // Casting this is only doable because HaxeAstFactory
    // creates PsiJavaTokens.
    return (PsiJavaToken)getDelegate().getLBrace();
  }

  @Override
  public PsiJavaToken getRBrace() {
    return (PsiJavaToken) getDelegate().getRBrace();
  }

  //
  // PsiAnnotationOwner
  //

  @Override
  @NotNull
  public PsiAnnotation[] getAnnotations() {
    // Type parameters don't get modifiers.
    return PsiAnnotation.EMPTY_ARRAY;
  }

  @Override
  public PsiAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
    // Type parameters don't get modifiers.
    return null;
  }

  @Override
  @NotNull
  public PsiAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
    throw new IncorrectOperationException();
  }

  @Override
  @NotNull
  public PsiAnnotation[] getApplicableAnnotations() {
    return getAnnotations();
  }



  // //////////////////////////////////////////////////////////////////////////////////
  // //////////////////////////////////////////////////////////////////////////////////
  //
  // HaxeTypeListPartForTypeChild
  //
  // //////////////////////////////////////////////////////////////////////////////////
  // //////////////////////////////////////////////////////////////////////////////////

  /**
   * Deals with type references that appear as children to HaxeTypeOrAnonymous
   * child elements.
   */
  private class HaxeClassDelegateForTypeChild extends PsiTypeParameterImpl {

    private HaxeType myChildType = null;

    public HaxeClassDelegateForTypeChild(@NotNull HaxeType childType) {
      super(childType.getNode());
      myChildType = childType;
    }

    @NotNull
    protected HaxeReference getDelegate() {
      return myChildType.getReferenceExpression();
    }

    @Nullable
    protected HaxeClass getResolvedDelegate() {
      PsiElement elem = getDelegate().resolve();
      return elem instanceof HaxeClass? (HaxeClass) elem : null;
    }

    //
    // PsiTypeParameter overrides
    //

    // PsiTypeParameter extends PsiClass instead of a reference.  It just stubs
    // out most of the functionality.

    @Override
    @Nullable
    public PsiMethod findMethodBySignature(PsiMethod patternMethod, boolean checkBases) {
      // XXX: If it turns out that we need this method, we need to find the
      //      actual class implementation (getDelegate().resolveHaxeClass())
      //      and call that.
      LOG.warn("Unexpected call to findMethodBySignature()");
      return null;
    }

    @Override
    @NotNull
    public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
      LOG.warn("Unexpected call to findMethodsBySignature()");
      return PsiMethod.EMPTY_ARRAY;
    }

    @Override
    public PsiField findFieldByName(String name, boolean checkBases) {
      LOG.warn("Unexpected call to findFieldByName()");
      return null;
    }

    @Override
    @NotNull
    public PsiMethod[] findMethodsByName(String name, boolean checkBases) {
      LOG.warn("Unexpected call to findMethodsByName()");
      return PsiMethod.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(String name, boolean checkBases) {
      LOG.warn("Unexpected call to findMethodsAndTheirSubstitutorsByName()");
      return new ArrayList<Pair<PsiMethod,PsiSubstitutor>>();
    }

    @Override
    @NotNull
    public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
      LOG.warn("Unexpected call to getAllMethodsAndTheirSubstitutors");
      return new ArrayList<Pair<PsiMethod,PsiSubstitutor>>();
    }

    @Override
    public PsiClass findInnerClassByName(String name, boolean checkBases) {
      return null; // No named inner classes in Haxe.
    }

    @Override
    public PsiElement getScope() {
      /**
       * Returns the PSI member in which the class has been declared (for example,
       * the method containing the anonymous inner class, or the file containing a regular
       * class, or the class owning a type parameter).
       */
      // As a TypeListPart, we are only children of extends, implements, and generic parameters.
      // In each of these cases, the class for which we are being declared (which may be
      // an anonymous type) is the scope.
      PsiElement parent = getParent();
      while (parent != null) {
        IElementType parentType = parent.getNode().getElementType();
        if (HaxeTokenTypes.ANONYMOUS_TYPE.equals(parentType)
        || (HaxeTokenTypes.CLASS_DECLARATION.equals(parentType))
        || parent instanceof HaxeFile )  // HaxeFile is a catchall..
        {
          break;
        }
        parent = parent.getParent();
      }

      return parent;
    }

    @NotNull
    @Override
    public PsiIdentifier getNameIdentifier() {
      // For a HaxeType, the identifier is two children below.  The first is
      // a reference.
      HaxeReferenceExpression ref = PsiTreeUtil.getRequiredChildOfType(this, HaxeReferenceExpression.class);
      HaxeIdentifier id = PsiTreeUtil.getRequiredChildOfType(ref, HaxeIdentifier.class);
      return id;
    }

    @Override
    public boolean isInheritorDeep(PsiClass baseClass, PsiClass classToByPass) {
      HaxeClass resolved = getResolvedDelegate();
      return null != resolved && resolved.isInheritorDeep(baseClass, classToByPass);
    }

    @Override
    public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
      HaxeClass resolved = getResolvedDelegate();
      return null != resolved && resolved.isInheritor(baseClass, checkDeep);
    }

    @Override
    @NotNull
    public PsiReferenceList getExtendsList() {
      // Haxe BNF doesn't allow for extends in this position.
      return new HaxeExtendsDeclarationImpl(new HaxeDummyASTNode("Empty Extends List"));
    }

    @Override
    public PsiReferenceList getImplementsList() {
      // Haxe BNF doesn't allow for implements in this position.
      return new HaxeImplementsDeclarationImpl(new HaxeDummyASTNode("Empty Implements List"));
    }

    @Override
    @NotNull
    public PsiClassType[] getExtendsListTypes() {
      return PsiClassType.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public PsiClassType[] getImplementsListTypes() {
      return PsiClassType.EMPTY_ARRAY;
    }

    @Override
    public PsiClass getSuperClass() {
      HaxeClass resolved = getResolvedDelegate();
      return null == resolved ? null : resolved.getSuperClass();
    }

    @Override
    public PsiClass[] getInterfaces() {
      HaxeClass resolved = getResolvedDelegate();
      return null == resolved ? PsiClass.EMPTY_ARRAY : resolved.getInterfaces();
    }

    @Override
    @NotNull
    public PsiClass[] getSupers() {
      HaxeClass resolved = getResolvedDelegate();
      return null == resolved ? PsiClass.EMPTY_ARRAY : resolved.getSupers();
    }

    @Override
    @NotNull
    public PsiClassType[] getSuperTypes() {
      HaxeClass resolved = getResolvedDelegate();
      return null == resolved ? PsiClassType.EMPTY_ARRAY : resolved.getSuperTypes();
    }

    @Override
    @NotNull
    public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
      HaxeClass resolved = getResolvedDelegate();
      return PsiSuperMethodImplUtil.getVisibleSignatures(resolved);
    }

  }

}
