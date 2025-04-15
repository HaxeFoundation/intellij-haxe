/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataListOwner;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataListOwnerImpl;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
 * This class, ideally, must not derive from HaxeModifierListOwner because every element cannot be prefixed with modifiers/annotations.
 * E.g. try/catch blocks, Interface body, Class body etc. do not have annotations attached to them.
 *
 * Unfortunately, it is observed that individual words read from the file are being validated whether they are methods, fields or have
 * annotations attached to them. This is causing .findMethodByName("void"), .findFieldByName("var"), .hasModifierByName("catch") etc
 * calls to be made and resulting in runtime errors / class-cast exceptions.
 *
 * To work around that, this 'is-a' relationship is introduced :(
 */

public class HaxePsiCompositeElementImpl extends ASTWrapperPsiElement implements HaxePsiCompositeElement, HaxeModifierListOwner,
                                                                                 HaxeMetadataListOwner {
  private HaxeMetadataListOwner metaImpl;

  public HaxePsiCompositeElementImpl(@NotNull ASTNode node) {
    super(node);
    metaImpl = new HaxeMetadataListOwnerImpl(node);
  }

  public IElementType getTokenType() {
    return getNode().getElementType();
  }

  public String getDebugName() {
    String name = null;
    String text = null;
    try {
      text = getText();
      name = getName();
    } catch (ProcessCanceledException e) {
      // ignore it.
    }
    StringBuilder sb = new StringBuilder();
    if (null != name) {
      sb.append('\'');
      sb.append(name);
      sb.append('\'');
    }
    if (null != text) {
      if (null != name) {
        sb.append(' ');
      }
      sb.append('"');
      sb.append(text);
      sb.append('"');
    }
    return sb.toString();
  }

  public String toDebugString() {
    return getTokenType().toString() + getDebugName();
  }

  public String toString() {
    String out = getTokenType().toString();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      out += " " + getDebugName();
    }
    return out;
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {

    // makes  sure we resolve the local function if referenced from inside
    if (lastParent instanceof  HaxeLocalFunctionDeclaration) {
      if (!processor.execute(lastParent, state)) {
        return false;
      }
    }

    for (PsiElement element : getDeclarationElementToProcess(lastParent)) {
      if (!processor.execute(element, state)) {
        return false;
      }
    }
    return super.processDeclarations(processor, state, lastParent, place);
  }

  private List<PsiElement> getDeclarationElementToProcess(PsiElement lastParent) {
    final boolean isBlock = this instanceof HaxeBlockStatement || this instanceof HaxeSwitchCaseBlock;
    final PsiElement stopper = isBlock ? lastParent : null;
    final List<PsiElement> result = new ArrayList<PsiElement>();
    addVarDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeFieldDeclaration.class));
    addLocalVarDeclarations(result, UsefulPsiTreeUtil.getChildrenOfType(this, HaxeLocalVarDeclarationList.class, stopper));

    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeMethodDeclaration.class));
    addDeclarations(result, UsefulPsiTreeUtil.getChildrenOfType(this, HaxeLocalFunctionDeclaration.class, stopper));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeClassDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeExternClassDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeEnumDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeInterfaceDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeTypedefDeclaration.class));

    if(this instanceof HaxeSwitchCase switchCase) {
      List<HaxeSwitchCaseExpr> list = switchCase.getSwitchCaseExprList();
      for (HaxeSwitchCaseExpr expr : list) {
        addDeclarations(result, PsiTreeUtil.findChildrenOfType(expr, HaxeSwitchCaseCapture.class));
        addDeclarations(result, PsiTreeUtil.findChildrenOfType(expr, HaxeExtractorMatchAssignExpression.class));
        addDeclarations(result, getObjectLiteralReferences(expr));
        addCaptureVariableDeclarations(expr, result);
      }
    }

    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(this, HaxeParameterList.class);
    if (parameterList != null) {
      result.addAll(parameterList.getParameterList());
    }
    final HaxeOpenParameterList openParameterList = PsiTreeUtil.getChildOfType(this, HaxeOpenParameterList.class);
    if (openParameterList != null) {
      result.add(openParameterList);
    }
    final HaxeGenericParam tygenericParameParam = PsiTreeUtil.getChildOfType(this, HaxeGenericParam.class);
    if (tygenericParameParam != null) {
      result.addAll(tygenericParameParam.getGenericListPartList());
    }

    if (this instanceof HaxeForStatement forStatement) {
      HaxeKeyValueIterator keyValueIterator = forStatement.getKeyValueIterator();
      HaxeValueIterator valueIterator = forStatement.getValueIterator();
      // any reference in HaxeIterable is always defined outside its current loop  (avoid problems like var x:Array<String>; for (x in x))
      if (!(lastParent instanceof HaxeIterable)) {
        if (keyValueIterator != null && keyValueIterator != lastParent) {
          result.add(keyValueIterator.getIteratorkey());
          result.add(keyValueIterator.getIteratorValue());
        }
        else if (valueIterator != null && valueIterator != lastParent) {
          result.add(valueIterator);
        }
      }
    }
    if (this instanceof  HaxeSwitchCase switchCase) {
      for (HaxeSwitchCaseExpr expr : switchCase.getSwitchCaseExprList()) {
        HaxeSwitchCaseCaptureVar captureVar = expr.getSwitchCaseCaptureVar();
        if (captureVar!= null) {
          result.add(captureVar.getComponentName());
        }
        Collection<HaxeEnumArgumentExtractor> extractors = PsiTreeUtil.findChildrenOfType(expr, HaxeEnumArgumentExtractor.class);
        for (HaxeEnumArgumentExtractor extractor : extractors) {
          Collection<HaxeEnumExtractedValueReference> extractedValues = PsiTreeUtil.findChildrenOfType(extractor, HaxeEnumExtractedValueReference.class);

          List<HaxeComponentName> list = extractedValues.stream()
            .map(HaxeEnumExtractedValueReference::getComponentName)
            .toList();
          result.addAll(list);

        }
      }
    }

    if (this instanceof HaxeSwitchCaseCaptureVar captureVar) {
      HaxeComponentName componentName = captureVar.getComponentName();
      result.add(componentName);
    }
    if (this instanceof HaxeEnumExtractedValueReference extractedValue) {
      HaxeComponentName componentName = extractedValue.getComponentName();
      result.add(componentName);
    }

    if (this instanceof HaxeCatchStatement) {
      final HaxeParameter catchParameter = PsiTreeUtil.getChildOfType(this, HaxeParameter.class);
      if (catchParameter != null) {
        result.add(catchParameter);
      }
    }
    return result;
  }

  private static void addCaptureVariableDeclarations(HaxeSwitchCaseExpr expr, List<PsiElement> result) {
    List<PsiElement> captureVars = PsiTreeUtil.findChildrenOfType(expr, HaxeReferenceExpression.class).stream()
            .filter(HaxeReferenceUtil::isCaptureVar)
            .map(PsiElement.class::cast)
            .toList();
    addDeclarations(result, captureVars);
  }


  private static @NotNull Collection<PsiElement> getObjectLiteralReferences(HaxeSwitchCaseExpr expr) {
    Collection<HaxeEnumObjectLiteralElement> objectLiterals = PsiTreeUtil.findChildrenOfType(expr, HaxeEnumObjectLiteralElement.class);
    return objectLiterals.stream()
      .map(HaxeEnumObjectLiteralElement::getExpression)
      .filter(expression -> expression instanceof HaxeReferenceExpression)
      // check casing to prevent issues separating variables without "var" and types
      // Compiler message when Uppercase:  "Val, pattern variables must be lower-case or with `var ` prefix"
      .filter(haxeExpression -> Character.isLowerCase(haxeExpression.getText().charAt(0)))
      .map(HaxeReferenceExpression.class::cast)
      .filter(expression -> expression.getChildren().length == 1)
      .map(PsiElement.class::cast)
      .toList();
  }

  private static void addLocalVarDeclarations(@NotNull List<PsiElement> result,
                                              @Nullable HaxeLocalVarDeclarationList[] items) {
    if (items == null) {
      return;
    }
    // reversed to correctly resolve variable shadowing
    // (declarations after element are not included, see getDeclarationElementToProcess and "stoppers")
    List<HaxeLocalVarDeclarationList> declarationLists = Arrays.asList(items);
    Collections.reverse(declarationLists);

    declarationLists.forEach(list -> result.addAll(list.getLocalVarDeclarationList()));
  }

  private static void addVarDeclarations(@NotNull List<PsiElement> result, @Nullable HaxeFieldDeclaration[] items) {
    if (items == null) {
      return;
    }

    result.addAll(Arrays.asList(items));
  }

  private static void addDeclarations(@NotNull List<PsiElement> result, @Nullable PsiElement[] items) {
    if (items != null) {
      result.addAll(Arrays.asList(items));
    }
  }
  private static void addDeclarations(@NotNull List<PsiElement> result, @Nullable Collection<PsiElement> items) {
    if (items != null) {
      result.addAll(items);
    }
  }


  // HaxeModifierListOwner implementations

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    HaxeModifierList list = getModifierList();
    return null == list ? false : list.hasModifierProperty(name);
  }

  @Nullable
  @Override
  public HaxeModifierList getModifierList() {
    return null;  // This list is built in sub-classes.
  }

  // HaxeMetadataListOwner implementations

  @Nullable
  @Override
  public HaxeMetadataList getMetadataList(@Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.getMetadataList(this, metadataType);
  }

  @Override
  public boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.hasMeta(this, metadataType, name);
  }

  @Override
  public PsiElement @NotNull [] getChildren() {
    PsiElement psiChild = getFirstChild();
    if (psiChild == null) return PsiElement.EMPTY_ARRAY;

    List<PsiElement> result = null;
    while (psiChild != null) {
      // we want to include comments when listing children as its usefull in a lot of places
      // we could get children with comments by using custom code looping getNextSibling manually, but its more convenient
      // to just override this method and solve the need everywhere, and if we dont want Comments we can always filter the results later.
      // (including Comments here will for instance allow us to use Comments in Pattern matching for autocompletion)
      if (psiChild.getNode() instanceof CompositeElement || psiChild.getNode() instanceof PsiComment) {
        if (result == null) {
          result = new ArrayList<>();
        }
        result.add(psiChild);
      }
      psiChild = psiChild.getNextSibling();
    }
    return result == null ? PsiElement.EMPTY_ARRAY : PsiUtilCore.toPsiElementArray(result);
  }
}
