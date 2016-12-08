/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;

public class HaxeVisitor extends PsiElementVisitor {

  public void visitAbstractClassDeclaration(@NotNull HaxeAbstractClassDeclaration o) {
    visitClass(o);
  }

  public void visitAccess(@NotNull HaxeAccess o) {
    visitPsiCompositeElement(o);
  }

  public void visitAdditiveExpression(@NotNull HaxeAdditiveExpression o) {
    visitExpression(o);
  }

  public void visitAnonymousFunctionDeclaration(@NotNull HaxeAnonymousFunctionDeclaration o) {
    visitPsiCompositeElement(o);
  }

  public void visitAnonymousType(@NotNull HaxeAnonymousType o) {
    visitClass(o);
  }

  public void visitAnonymousTypeBody(@NotNull HaxeAnonymousTypeBody o) {
    visitPsiCompositeElement(o);
  }

  public void visitAnonymousTypeField(@NotNull HaxeAnonymousTypeField o) {
    visitPsiField(o);
  }

  public void visitAnonymousTypeFieldList(@NotNull HaxeAnonymousTypeFieldList o) {
    visitPsiCompositeElement(o);
  }

  public void visitArrayAccessExpression(@NotNull HaxeArrayAccessExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitArrayLiteral(@NotNull HaxeArrayLiteral o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitAssignExpression(@NotNull HaxeAssignExpression o) {
    visitExpression(o);
  }

  public void visitAssignOperation(@NotNull HaxeAssignOperation o) {
    visitPsiCompositeElement(o);
  }

  public void visitAutoBuildMacro(@NotNull HaxeAutoBuildMacro o) {
    visitPsiCompositeElement(o);
  }

  public void visitBindMeta(@NotNull HaxeBindMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitBitOperation(@NotNull HaxeBitOperation o) {
    visitPsiCompositeElement(o);
  }

  public void visitBitmapMeta(@NotNull HaxeBitmapMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitBitwiseExpression(@NotNull HaxeBitwiseExpression o) {
    visitExpression(o);
  }

  public void visitBlockStatement(@NotNull HaxeBlockStatement o) {
    visitBlockStatementPsiMixin(o);
  }

  public void visitBreakStatement(@NotNull HaxeBreakStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitBuildMacro(@NotNull HaxeBuildMacro o) {
    visitPsiCompositeElement(o);
  }

  public void visitCallExpression(@NotNull HaxeCallExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitCastExpression(@NotNull HaxeCastExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitCatchStatement(@NotNull HaxeCatchStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitClassBody(@NotNull HaxeClassBody o) {
    visitPsiCompositeElement(o);
  }

  public void visitClassDeclaration(@NotNull HaxeClassDeclaration o) {
    visitClass(o);
  }

  public void visitCompareExpression(@NotNull HaxeCompareExpression o) {
    visitExpression(o);
  }

  public void visitCompareOperation(@NotNull HaxeCompareOperation o) {
    visitPsiCompositeElement(o);
  }

  public void visitComponentName(@NotNull HaxeComponentName o) {
    visitNamedElement(o);
  }

  public void visitContinueStatement(@NotNull HaxeContinueStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitCoreApiMeta(@NotNull HaxeCoreApiMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitCustomMeta(@NotNull HaxeCustomMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitDebugMeta(@NotNull HaxeDebugMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitDeclarationAttribute(@NotNull HaxeDeclarationAttribute o) {
    visitPsiCompositeElement(o);
  }

  public void visitDefaultCase(@NotNull HaxeDefaultCase o) {
    visitPsiCompositeElement(o);
  }

  public void visitDoWhileStatement(@NotNull HaxeDoWhileStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitEnumBody(@NotNull HaxeEnumBody o) {
    visitPsiCompositeElement(o);
  }

  public void visitEnumConstructorParameters(@NotNull HaxeEnumConstructorParameters o) {
    visitPsiCompositeElement(o);
  }

  public void visitEnumDeclaration(@NotNull HaxeEnumDeclaration o) {
    visitClass(o);
  }

  public void visitEnumValueDeclaration(@NotNull HaxeEnumValueDeclaration o) {
    visitPsiField(o);
  }

  public void visitExpression(@NotNull HaxeExpression o) {
    visitPsiCompositeElement(o);
  }

  public void visitExpressionList(@NotNull HaxeExpressionList o) {
    visitPsiCompositeElement(o);
  }

  public void visitExtendsDeclaration(@NotNull HaxeExtendsDeclaration o) {
    visitInherit(o);
  }

  public void visitExternClassDeclaration(@NotNull HaxeExternClassDeclaration o) {
    visitClass(o);
  }

  public void visitExternClassDeclarationBody(@NotNull HaxeExternClassDeclarationBody o) {
    visitPsiCompositeElement(o);
  }

  public void visitExternFunctionDeclaration(@NotNull HaxeExternFunctionDeclaration o) {
    visitMethod(o);
  }

  public void visitExternInterfaceDeclaration(@NotNull HaxeExternInterfaceDeclaration o) {
    visitClass(o);
  }

  public void visitExternKeyWord(@NotNull HaxeExternKeyWord o) {
    visitPsiCompositeElement(o);
  }

  public void visitFakeEnumMeta(@NotNull HaxeFakeEnumMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitFatArrowExpression(@NotNull HaxeFatArrowExpression o) {
    visitExpression(o);
  }

  public void visitFinalMeta(@NotNull HaxeFinalMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitForStatement(@NotNull HaxeForStatement o) {
    visitForStatementPsiMixin(o);
  }

  public void visitFunctionDeclarationWithAttributes(@NotNull HaxeFunctionDeclarationWithAttributes o) {
    visitMethod(o);
  }

  public void visitFunctionLiteral(@NotNull HaxeFunctionLiteral o) {
    visitExpression(o);
  }

  public void visitFunctionPrototypeDeclarationWithAttributes(@NotNull HaxeFunctionPrototypeDeclarationWithAttributes o) {
    visitMethod(o);
  }

  public void visitFunctionType(@NotNull HaxeFunctionType o) {
    visitPsiCompositeElement(o);
  }

  public void visitGenericListPart(@NotNull HaxeGenericListPart o) {
    visitComponent(o);
  }

  public void visitGenericParam(@NotNull HaxeGenericParam o) {
    visitPsiCompositeElement(o);
  }

  public void visitGetterMeta(@NotNull HaxeGetterMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitHackMeta(@NotNull HaxeHackMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitIdentifier(@NotNull HaxeIdentifier o) {
    visitIdentifierPsiMixin(o);
  }

  public void visitIfStatement(@NotNull HaxeIfStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitImplementsDeclaration(@NotNull HaxeImplementsDeclaration o) {
    visitInherit(o);
  }

  public void visitImportStatementRegular(@NotNull HaxeImportStatementRegular o) {
    visitPsiCompositeElement(o);
  }

  public void visitImportStatementWithInSupport(@NotNull HaxeImportStatementWithInSupport o) {
    visitPsiCompositeElement(o);
  }

  public void visitImportStatementWithWildcard(@NotNull HaxeImportStatementWithWildcard o) {
    visitPsiCompositeElement(o);
  }

  public void visitInheritList(@NotNull HaxeInheritList o) {
    visitPsiCompositeElement(o);
  }

  public void visitInterfaceBody(@NotNull HaxeInterfaceBody o) {
    visitPsiCompositeElement(o);
  }

  public void visitInterfaceDeclaration(@NotNull HaxeInterfaceDeclaration o) {
    visitClass(o);
  }

  public void visitIterable(@NotNull HaxeIterable o) {
    visitPsiCompositeElement(o);
  }

  public void visitIteratorExpression(@NotNull HaxeIteratorExpression o) {
    visitExpression(o);
  }

  public void visitJsRequireMeta(@NotNull HaxeJsRequireMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitKeepMeta(@NotNull HaxeKeepMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitLiteralExpression(@NotNull HaxeLiteralExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitLocalFunctionDeclaration(@NotNull HaxeLocalFunctionDeclaration o) {
    visitMethod(o);
  }

  public void visitLocalVarDeclaration(@NotNull HaxeLocalVarDeclaration o) {
    visitPsiField(o);
  }

  public void visitLocalVarDeclarationPart(@NotNull HaxeLocalVarDeclarationPart o) {
    visitPsiField(o);
  }

  public void visitLogicAndExpression(@NotNull HaxeLogicAndExpression o) {
    visitExpression(o);
  }

  public void visitLogicOrExpression(@NotNull HaxeLogicOrExpression o) {
    visitExpression(o);
  }

  public void visitLongTemplateEntry(@NotNull HaxeLongTemplateEntry o) {
    visitPsiCompositeElement(o);
  }

  public void visitMacroClass(@NotNull HaxeMacroClass o) {
    visitPsiCompositeElement(o);
  }

  public void visitMacroClassList(@NotNull HaxeMacroClassList o) {
    visitModifierList(o);
  }

  public void visitMetaKeyValue(@NotNull HaxeMetaKeyValue o) {
    visitPsiCompositeElement(o);
  }

  public void visitMetaMeta(@NotNull HaxeMetaMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitMultiplicativeExpression(@NotNull HaxeMultiplicativeExpression o) {
    visitExpression(o);
  }

  public void visitNativeMeta(@NotNull HaxeNativeMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitNewExpression(@NotNull HaxeNewExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitNoDebugMeta(@NotNull HaxeNoDebugMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitNsMeta(@NotNull HaxeNsMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitObjectLiteral(@NotNull HaxeObjectLiteral o) {
    visitExpression(o);
  }

  public void visitObjectLiteralElement(@NotNull HaxeObjectLiteralElement o) {
    visitPsiCompositeElement(o);
  }

  public void visitOverloadMeta(@NotNull HaxeOverloadMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitPackageStatement(@NotNull HaxePackageStatement o) {
    visitPackageStatementPsiMixin(o);
  }

  public void visitParameter(@NotNull HaxeParameter o) {
    visitParameterPsiMixin(o);
  }

  public void visitParameterList(@NotNull HaxeParameterList o) {
    visitParameterListPsiMixin(o);
  }

  public void visitParenthesizedExpression(@NotNull HaxeParenthesizedExpression o) {
    visitExpression(o);
  }

  public void visitPrefixExpression(@NotNull HaxePrefixExpression o) {
    visitExpression(o);
  }

  public void visitPrivateKeyWord(@NotNull HaxePrivateKeyWord o) {
    visitPsiCompositeElement(o);
  }

  public void visitPropertyAccessor(@NotNull HaxePropertyAccessor o) {
    visitPsiCompositeElement(o);
  }

  public void visitPropertyDeclaration(@NotNull HaxePropertyDeclaration o) {
    visitPsiCompositeElement(o);
  }

  public void visitProtectedMeta(@NotNull HaxeProtectedMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitReferenceExpression(@NotNull HaxeReferenceExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitRegularExpressionLiteral(@NotNull HaxeRegularExpressionLiteral o) {
    visitLiteralExpression(o);
    // visitRegularExpression(o);
  }

  public void visitRequireMeta(@NotNull HaxeRequireMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitReturnStatement(@NotNull HaxeReturnStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitReturnStatementWithoutSemicolon(@NotNull HaxeReturnStatementWithoutSemicolon o) {
    visitPsiCompositeElement(o);
  }

  public void visitSetterMeta(@NotNull HaxeSetterMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitShiftExpression(@NotNull HaxeShiftExpression o) {
    visitExpression(o);
  }

  public void visitShiftOperator(@NotNull HaxeShiftOperator o) {
    visitPsiCompositeElement(o);
  }

  public void visitShiftRightOperator(@NotNull HaxeShiftRightOperator o) {
    visitPsiCompositeElement(o);
  }

  public void visitShortTemplateEntry(@NotNull HaxeShortTemplateEntry o) {
    visitPsiCompositeElement(o);
  }

  public void visitSimpleMeta(@NotNull HaxeSimpleMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitStringLiteralExpression(@NotNull HaxeStringLiteralExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitSuffixExpression(@NotNull HaxeSuffixExpression o) {
    visitExpression(o);
  }

  public void visitSuperExpression(@NotNull HaxeSuperExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitSwitchBlock(@NotNull HaxeSwitchBlock o) {
    visitPsiCompositeElement(o);
  }

  public void visitSwitchCase(@NotNull HaxeSwitchCase o) {
    visitPsiCompositeElement(o);
  }

  public void visitSwitchCaseBlock(@NotNull HaxeSwitchCaseBlock o) {
    visitPsiCompositeElement(o);
  }

  public void visitSwitchCaseExpression(@NotNull HaxeSwitchCaseExpression o) {
    visitExpression(o);
  }

  public void visitSwitchStatement(@NotNull HaxeSwitchStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitTernaryExpression(@NotNull HaxeTernaryExpression o) {
    visitExpression(o);
  }

  public void visitThisExpression(@NotNull HaxeThisExpression o) {
    visitExpression(o);
    // visitReference(o);
  }

  public void visitThrowStatement(@NotNull HaxeThrowStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitTryStatement(@NotNull HaxeTryStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitType(@NotNull HaxeType o) {
    visitTypePsiMixin(o);
  }

  public void visitTypeCheckExpr(@NotNull HaxeTypeCheckExpr o) {
    visitReference(o);
  }

  public void visitTypeExtends(@NotNull HaxeTypeExtends o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeList(@NotNull HaxeTypeList o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeListPart(@NotNull HaxeTypeListPart o) {
    visitTypeListPartPsiMixin(o);
  }

  public void visitTypeOrAnonymous(@NotNull HaxeTypeOrAnonymous o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeParam(@NotNull HaxeTypeParam o) {
    visitTypeParamPsiMixin(o);
  }

  public void visitTypeTag(@NotNull HaxeTypeTag o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypedefDeclaration(@NotNull HaxeTypedefDeclaration o) {
    visitClass(o);
  }

  public void visitUnreflectiveMeta(@NotNull HaxeUnreflectiveMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitUnsignedShiftRightOperator(@NotNull HaxeUnsignedShiftRightOperator o) {
    visitPsiCompositeElement(o);
  }

  public void visitUsingStatement(@NotNull HaxeUsingStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitVarDeclaration(@NotNull HaxeVarDeclaration o) {
    visitPsiField(o);
  }

  public void visitVarDeclarationPart(@NotNull HaxeVarDeclarationPart o) {
    visitPsiField(o);
  }

  public void visitVarInit(@NotNull HaxeVarInit o) {
    visitPsiCompositeElement(o);
  }

  public void visitWhileStatement(@NotNull HaxeWhileStatement o) {
    visitStatementPsiMixin(o);
  }

  public void visitWildcard(@NotNull HaxeWildcard o) {
    visitPsiCompositeElement(o);
  }

  public void visitBlockStatementPsiMixin(@NotNull HaxeBlockStatementPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitClass(@NotNull HaxeClass o) {
    visitPsiCompositeElement(o);
  }

  public void visitComponent(@NotNull HaxeComponent o) {
    visitPsiCompositeElement(o);
  }

  public void visitForStatementPsiMixin(@NotNull HaxeForStatementPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitIdentifierPsiMixin(@NotNull HaxeIdentifierPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitInherit(@NotNull HaxeInherit o) {
    visitPsiCompositeElement(o);
  }

  public void visitMethod(@NotNull HaxeMethod o) {
    visitPsiCompositeElement(o);
  }

  public void visitModifierList(@NotNull HaxeModifierList o) {
    visitPsiCompositeElement(o);
  }

  public void visitNamedElement(@NotNull HaxeNamedElement o) {
    visitPsiCompositeElement(o);
  }

  public void visitPackageStatementPsiMixin(@NotNull HaxePackageStatementPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitParameterListPsiMixin(@NotNull HaxeParameterListPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitParameterPsiMixin(@NotNull HaxeParameterPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitPsiField(@NotNull HaxePsiField o) {
    visitPsiCompositeElement(o);
  }

  public void visitReference(@NotNull HaxeReference o) {
    visitPsiCompositeElement(o);
  }

  public void visitStatementPsiMixin(@NotNull HaxeStatementPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeListPartPsiMixin(@NotNull HaxeTypeListPartPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeParamPsiMixin(@NotNull HaxeTypeParamPsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypePsiMixin(@NotNull HaxeTypePsiMixin o) {
    visitPsiCompositeElement(o);
  }

  public void visitPsiCompositeElement(@NotNull HaxePsiCompositeElement o) {
    visitElement(o);
  }

}
