/*
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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeObjectLiteral;
import com.intellij.plugins.haxe.lang.psi.HaxeObjectLiteralComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeObjectLiteralElement;
import com.intellij.plugins.haxe.lang.psi.HaxeResolver;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Lets you Ctrl/Cmd+Click the <em>key</em> of an object literal and jump to the field it fills in
 * on the structure the literal is typed against.
 *
 * <p>For {@code var c:Config = { name: "x" }} clicking {@code name} navigates to the {@code name}
 * field declared in {@code Config}. The field is looked up through the structure's full member set,
 * so it also works when {@code Config} extends (or is built from) other typedefs.</p>
 *
 * <p>The object literal key is otherwise a field <em>declaration</em> on the literal's own anonymous
 * type (it has no reference), so plain reference resolution never produces a navigation target here.
 * This handler adds that target without touching find-usages, rename or highlighting.</p>
 */
public class HaxeGotoDeclarationHandler implements GotoDeclarationHandler {

  @Override
  public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
    if (sourceElement == null) return null;

    HaxeObjectLiteralElement literalElement = PsiTreeUtil.getParentOfType(sourceElement, HaxeObjectLiteralElement.class);
    if (literalElement == null) return null;

    HaxeObjectLiteralComponentName key = literalElement.getComponentName();
    if (key == null) return null;

    // Only react when the click is on the key, not on the value expression that follows the colon.
    if (!PsiTreeUtil.isAncestor(key, sourceElement, false)) return null;

    HaxeObjectLiteral objectLiteral = PsiTreeUtil.getParentOfType(literalElement, HaxeObjectLiteral.class);
    if (objectLiteral == null) return null;

    HaxeGenericResolver resolver = new HaxeGenericResolver();
    ResultHolder expectedType =
      HaxeExpressionEvaluator.findObjectLiteralType(new HaxeExpressionEvaluatorContext(objectLiteral), resolver, objectLiteral);
    if (expectedType == null || expectedType.isUnknown()) {
      // findObjectLiteralType only understands a few direct contexts (assignment, return, var init,
      // plain call). Fall back to the resolver's full finder, which also handles constructor
      // arguments and literals nested inside arrays or other object literals.
      expectedType = HaxeResolver.getInstance(objectLiteral.getProject()).findExpectedType(objectLiteral);
    }
    if (expectedType == null || expectedType.isUnknown()) return null;

    SpecificHaxeClassReference classType = expectedType.getClassType();
    if (classType == null) return null;

    HaxeClassModel classModel = classType.getHaxeClassModel();
    if (classModel == null) return null;

    HaxeBaseMemberModel member = classModel.getMember(literalElement.getName(), classType.getGenericResolver());
    if (member == null) return null;

    PsiElement declaration = member.getNameOrBasePsi();
    // Don't offer a target that points back into the literal we started from.
    if (declaration == null || PsiTreeUtil.isAncestor(objectLiteral, declaration, false)) return null;

    return new PsiElement[]{declaration};
  }
}
