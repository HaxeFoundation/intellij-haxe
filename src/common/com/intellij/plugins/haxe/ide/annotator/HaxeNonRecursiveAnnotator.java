/*
 * Copyright 2018-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

public class HaxeNonRecursiveAnnotator extends HaxeVisitor implements Annotator {
  protected AnnotationHolder holder;

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    this.holder = holder;
    visitElement(element);
  }

  @Override
  public void visitElement(PsiElement element) {
    super.visitElement(element);

    if (element instanceof HaxePsiToken ||
        element instanceof PsiWhiteSpace ||
        element instanceof HaxeIdentifier ||
        element instanceof GeneratedParserUtilBase.DummyBlock) {
      return;
    }

    element.accept(this);
  }

  @Override
  public void visitFile(PsiFile file) {
  }

  @Override
  public void visitComment(PsiComment comment) {
  }

  @Override
  public void visitMethodDeclaration(@NotNull HaxeMethodDeclaration o) {
  }

  @Override
  public void visitAbstractBody(@NotNull HaxeAbstractBody o) {
  }

  @Override
  public void visitAbstractClassDeclaration(@NotNull HaxeAbstractClassDeclaration o) {
  }

  @Override
  public void visitAdditiveExpression(@NotNull HaxeAdditiveExpression o) {
  }

  @Override
  public void visitAnonymousType(@NotNull HaxeAnonymousType o) {
  }

  @Override
  public void visitAnonymousTypeBody(@NotNull HaxeAnonymousTypeBody o) {
  }

  @Override
  public void visitAnonymousTypeField(@NotNull HaxeAnonymousTypeField o) {
  }

  @Override
  public void visitAnonymousTypeFieldList(@NotNull HaxeAnonymousTypeFieldList o) {
  }

  @Override
  public void visitArrayAccessExpression(@NotNull HaxeArrayAccessExpression o) {
  }

  @Override
  public void visitArrayAccessMeta(@NotNull HaxeArrayAccessMeta o) {
  }

  @Override
  public void visitArrayLiteral(@NotNull HaxeArrayLiteral o) {
  }

  @Override
  public void visitAssignExpression(@NotNull HaxeAssignExpression o) {
  }

  @Override
  public void visitAssignOperation(@NotNull HaxeAssignOperation o) {
  }

  @Override
  public void visitAutoBuildMacro(@NotNull HaxeAutoBuildMacro o) {
  }

  @Override
  public void visitBindMeta(@NotNull HaxeBindMeta o) {
  }

  @Override
  public void visitBitOperation(@NotNull HaxeBitOperation o) {
  }

  @Override
  public void visitBitmapMeta(@NotNull HaxeBitmapMeta o) {
  }

  @Override
  public void visitBitwiseExpression(@NotNull HaxeBitwiseExpression o) {
  }

  @Override
  public void visitBlockStatement(@NotNull HaxeBlockStatement o) {
  }

  @Override
  public void visitBreakStatement(@NotNull HaxeBreakStatement o) {
  }

  @Override
  public void visitBuildMacro(@NotNull HaxeBuildMacro o) {
  }

  @Override
  public void visitCallExpression(@NotNull HaxeCallExpression o) {
  }

  @Override
  public void visitCastExpression(@NotNull HaxeCastExpression o) {
  }

  @Override
  public void visitCatchStatement(@NotNull HaxeCatchStatement o) {
  }

  @Override
  public void visitClassBody(@NotNull HaxeClassBody o) {
  }

  @Override
  public void visitClassDeclaration(@NotNull HaxeClassDeclaration o) {
  }

  @Override
  public void visitCompareExpression(@NotNull HaxeCompareExpression o) {
  }

  @Override
  public void visitCompareOperation(@NotNull HaxeCompareOperation o) {
  }

  @Override
  public void visitComponentName(@NotNull HaxeComponentName o) {
  }

  @Override
  public void visitConstantExpression(@NotNull HaxeConstantExpression o) {
  }

  @Override
  public void visitContinueStatement(@NotNull HaxeContinueStatement o) {
  }

  @Override
  public void visitCoreApiMeta(@NotNull HaxeCoreApiMeta o) {
  }

  @Override
  public void visitCustomMeta(@NotNull HaxeCustomMeta o) {
  }

  @Override
  public void visitDebugMeta(@NotNull HaxeDebugMeta o) {
  }

  @Override
  public void visitDefaultCase(@NotNull HaxeDefaultCase o) {
  }

  @Override
  public void visitDoWhileStatement(@NotNull HaxeDoWhileStatement o) {
  }

  @Override
  public void visitEnumBody(@NotNull HaxeEnumBody o) {
  }

  @Override
  public void visitEnumDeclaration(@NotNull HaxeEnumDeclaration o) {
  }

  @Override
  public void visitEnumValueDeclaration(@NotNull HaxeEnumValueDeclaration o) {
  }

  @Override
  public void visitExpression(@NotNull HaxeExpression o) {
  }

  @Override
  public void visitExpressionList(@NotNull HaxeExpressionList o) {
  }

  @Override
  public void visitExtendsDeclaration(@NotNull HaxeExtendsDeclaration o) {
  }

  @Override
  public void visitExternClassDeclaration(@NotNull HaxeExternClassDeclaration o) {
  }

  @Override
  public void visitExternClassDeclarationBody(@NotNull HaxeExternClassDeclarationBody o) {
  }

  @Override
  public void visitExternInterfaceDeclaration(@NotNull HaxeExternInterfaceDeclaration o) {
  }

  @Override
  public void visitExternKeyWord(@NotNull HaxeExternKeyWord o) {
  }

  @Override
  public void visitFakeEnumMeta(@NotNull HaxeFakeEnumMeta o) {
  }

  @Override
  public void visitFatArrowExpression(@NotNull HaxeFatArrowExpression o) {
  }

  @Override
  public void visitFinalMeta(@NotNull HaxeFinalMeta o) {
  }

  @Override
  public void visitForStatement(@NotNull HaxeForStatement o) {
  }

  @Override
  public void visitFunctionLiteral(@NotNull HaxeFunctionLiteral o) {
  }

  @Override
  public void visitFunctionType(@NotNull HaxeFunctionType o) {
  }

  @Override
  public void visitGenericListPart(@NotNull HaxeGenericListPart o) {
  }

  @Override
  public void visitGenericParam(@NotNull HaxeGenericParam o) {
  }

  @Override
  public void visitGetterMeta(@NotNull HaxeGetterMeta o) {
  }

  @Override
  public void visitHackMeta(@NotNull HaxeHackMeta o) {
  }

  @Override
  public void visitIdentifier(@NotNull HaxeIdentifier o) {
  }

  @Override
  public void visitIfStatement(@NotNull HaxeIfStatement o) {
  }

  @Override
  public void visitImplementsDeclaration(@NotNull HaxeImplementsDeclaration o) {
  }

  @Override
  public void visitImportStatement(@NotNull HaxeImportStatement o) {
  }

  @Override
  public void visitImportWildcard(@NotNull HaxeImportWildcard o) {
  }

  @Override
  public void visitInheritList(@NotNull HaxeInheritList o) {
  }

  @Override
  public void visitInterfaceBody(@NotNull HaxeInterfaceBody o) {
  }

  @Override
  public void visitInterfaceDeclaration(@NotNull HaxeInterfaceDeclaration o) {
  }

  @Override
  public void visitIterable(@NotNull HaxeIterable o) {
  }

  @Override
  public void visitIteratorExpression(@NotNull HaxeIteratorExpression o) {
  }

  @Override
  public void visitJsRequireMeta(@NotNull HaxeJsRequireMeta o) {
  }

  @Override
  public void visitKeepMeta(@NotNull HaxeKeepMeta o) {
  }

  @Override
  public void visitLiteralExpression(@NotNull HaxeLiteralExpression o) {
  }

  @Override
  public void visitLocalFunctionDeclaration(@NotNull HaxeLocalFunctionDeclaration o) {
  }

  @Override
  public void visitLocalVarDeclaration(@NotNull HaxeLocalVarDeclaration o) {
  }

  @Override
  public void visitLocalVarDeclarationList(@NotNull HaxeLocalVarDeclarationList o) {
  }

  @Override
  public void visitLogicAndExpression(@NotNull HaxeLogicAndExpression o) {
  }

  @Override
  public void visitLogicOrExpression(@NotNull HaxeLogicOrExpression o) {
  }

  @Override
  public void visitLongTemplateEntry(@NotNull HaxeLongTemplateEntry o) {
  }

  @Override
  public void visitMacroClass(@NotNull HaxeMacroClass o) {
  }

  @Override
  public void visitMacroClassList(@NotNull HaxeMacroClassList o) {
  }

  @Override
  public void visitMapInitializerExpression(@NotNull HaxeMapInitializerExpression o) {
  }

  @Override
  public void visitMapInitializerExpressionList(@NotNull HaxeMapInitializerExpressionList o) {
  }

  @Override
  public void visitMapInitializerForStatement(@NotNull HaxeMapInitializerForStatement o) {
  }

  @Override
  public void visitMapInitializerWhileStatement(@NotNull HaxeMapInitializerWhileStatement o) {
  }

  @Override
  public void visitMapLiteral(@NotNull HaxeMapLiteral o) {
  }

  @Override
  public void visitMetaKeyValue(@NotNull HaxeMetaKeyValue o) {
  }

  @Override
  public void visitMetaMeta(@NotNull HaxeMetaMeta o) {
  }

  @Override
  public void visitMultiplicativeExpression(@NotNull HaxeMultiplicativeExpression o) {
  }

  @Override
  public void visitNativeMeta(@NotNull HaxeNativeMeta o) {
  }

  @Override
  public void visitNewExpression(@NotNull HaxeNewExpression o) {
  }

  @Override
  public void visitNoDebugMeta(@NotNull HaxeNoDebugMeta o) {
  }

  @Override
  public void visitNsMeta(@NotNull HaxeNsMeta o) {
  }

  @Override
  public void visitObjectLiteral(@NotNull HaxeObjectLiteral o) {
  }

  @Override
  public void visitObjectLiteralElement(@NotNull HaxeObjectLiteralElement o) {
  }

  @Override
  public void visitOpenParameterList(@NotNull HaxeOpenParameterList o) {
  }

  @Override
  public void visitOverloadMeta(@NotNull HaxeOverloadMeta o) {
  }

  @Override
  public void visitPackageStatement(@NotNull HaxePackageStatement o) {
  }

  @Override
  public void visitParameter(@NotNull HaxeParameter o) {
  }

  @Override
  public void visitParameterList(@NotNull HaxeParameterList o) {
  }

  @Override
  public void visitParenthesizedExpression(@NotNull HaxeParenthesizedExpression o) {
  }

  @Override
  public void visitPrefixExpression(@NotNull HaxePrefixExpression o) {
  }

  @Override
  public void visitPrivateKeyWord(@NotNull HaxePrivateKeyWord o) {
  }

  @Override
  public void visitPropertyAccessor(@NotNull HaxePropertyAccessor o) {
  }

  @Override
  public void visitPropertyDeclaration(@NotNull HaxePropertyDeclaration o) {
  }

  @Override
  public void visitProtectedMeta(@NotNull HaxeProtectedMeta o) {
  }

  @Override
  public void visitReferenceExpression(@NotNull HaxeReferenceExpression o) {
  }

  @Override
  public void visitRegularExpressionLiteral(@NotNull HaxeRegularExpressionLiteral o) {
  }

  @Override
  public void visitRequireMeta(@NotNull HaxeRequireMeta o) {
  }

  @Override
  public void visitReturnStatement(@NotNull HaxeReturnStatement o) {
  }

  @Override
  public void visitSetterMeta(@NotNull HaxeSetterMeta o) {
  }

  @Override
  public void visitShiftAssignOperator(@NotNull HaxeShiftAssignOperator o) {
  }

  @Override
  public void visitShiftExpression(@NotNull HaxeShiftExpression o) {
  }

  @Override
  public void visitShiftLeftAssignOperator(@NotNull HaxeShiftLeftAssignOperator o) {
  }

  @Override
  public void visitShiftLeftOperator(@NotNull HaxeShiftLeftOperator o) {
  }

  @Override
  public void visitShiftOperator(@NotNull HaxeShiftOperator o) {
  }

  @Override
  public void visitShiftRightAssignOperator(@NotNull HaxeShiftRightAssignOperator o) {
  }

  @Override
  public void visitShiftRightOperator(@NotNull HaxeShiftRightOperator o) {
  }

  @Override
  public void visitShortTemplateEntry(@NotNull HaxeShortTemplateEntry o) {
  }

  @Override
  public void visitSimpleMeta(@NotNull HaxeSimpleMeta o) {
  }

  @Override
  public void visitStringLiteralExpression(@NotNull HaxeStringLiteralExpression o) {
  }

  @Override
  public void visitSuffixExpression(@NotNull HaxeSuffixExpression o) {
  }

  @Override
  public void visitSuperExpression(@NotNull HaxeSuperExpression o) {
  }

  @Override
  public void visitSwitchBlock(@NotNull HaxeSwitchBlock o) {
  }

  @Override
  public void visitSwitchCase(@NotNull HaxeSwitchCase o) {
  }

  @Override
  public void visitSwitchCaseBlock(@NotNull HaxeSwitchCaseBlock o) {
  }

  @Override
  public void visitSwitchCaseExpression(@NotNull HaxeSwitchCaseExpression o) {
  }

  @Override
  public void visitSwitchStatement(@NotNull HaxeSwitchStatement o) {
  }

  @Override
  public void visitTernaryExpression(@NotNull HaxeTernaryExpression o) {
  }

  @Override
  public void visitThisExpression(@NotNull HaxeThisExpression o) {
  }

  @Override
  public void visitThrowStatement(@NotNull HaxeThrowStatement o) {
  }

  @Override
  public void visitTryStatement(@NotNull HaxeTryStatement o) {
  }

  @Override
  public void visitType(@NotNull HaxeType o) {
  }

  @Override
  public void visitTypeCheckExpr(@NotNull HaxeTypeCheckExpr o) {
  }

  @Override
  public void visitTypeExtendsList(@NotNull HaxeTypeExtendsList o) {
  }

  @Override
  public void visitTypeList(@NotNull HaxeTypeList o) {
  }

  @Override
  public void visitTypeListPart(@NotNull HaxeTypeListPart o) {
  }

  @Override
  public void visitTypeOrAnonymous(@NotNull HaxeTypeOrAnonymous o) {
  }

  @Override
  public void visitTypeParam(@NotNull HaxeTypeParam o) {
  }

  @Override
  public void visitTypeTag(@NotNull HaxeTypeTag o) {
  }

  @Override
  public void visitTypedefDeclaration(@NotNull HaxeTypedefDeclaration o) {
  }

  @Override
  public void visitUnderlyingType(@NotNull HaxeUnderlyingType o) {
  }

  @Override
  public void visitUnreflectiveMeta(@NotNull HaxeUnreflectiveMeta o) {
  }

  @Override
  public void visitUnsignedShiftRightAssignOperator(@NotNull HaxeUnsignedShiftRightAssignOperator o) {
  }

  @Override
  public void visitUnsignedShiftRightOperator(@NotNull HaxeUnsignedShiftRightOperator o) {
  }

  @Override
  public void visitUsingStatement(@NotNull HaxeUsingStatement o) {
  }

  @Override
  public void visitVarInit(@NotNull HaxeVarInit o) {
  }

  @Override
  public void visitWhileStatement(@NotNull HaxeWhileStatement o) {
  }

  @Override
  public void visitBlockStatementPsiMixin(@NotNull HaxeBlockStatementPsiMixin o) {
  }

  @Override
  public void visitClass(@NotNull HaxeClass o) {
  }

  @Override
  public void visitComponent(@NotNull HaxeComponent o) {
  }

  @Override
  public void visitForStatementPsiMixin(@NotNull HaxeForStatementPsiMixin o) {
  }

  @Override
  public void visitIdentifierPsiMixin(@NotNull HaxeIdentifierPsiMixin o) {
  }

  @Override
  public void visitImportStatementPsiMixin(@NotNull HaxeImportStatementPsiMixin o) {
  }

  @Override
  public void visitInherit(@NotNull HaxeInherit o) {
  }

  @Override
  public void visitMethod(@NotNull HaxeMethod o) {
  }

  @Override
  public void visitModifierList(@NotNull HaxeModifierList o) {
  }

  @Override
  public void visitNamedElement(@NotNull HaxeNamedElement o) {
  }

  @Override
  public void visitPackageStatementPsiMixin(@NotNull HaxePackageStatementPsiMixin o) {
  }

  @Override
  public void visitParameterListPsiMixin(@NotNull HaxeParameterListPsiMixin o) {
  }

  @Override
  public void visitParameterPsiMixin(@NotNull HaxeParameterPsiMixin o) {
  }

  @Override
  public void visitPsiField(@NotNull HaxePsiField o) {
  }

  @Override
  public void visitReference(@NotNull HaxeReference o) {
  }

  @Override
  public void visitStatementPsiMixin(@NotNull HaxeStatementPsiMixin o) {
  }

  @Override
  public void visitTypeListPartPsiMixin(@NotNull HaxeTypeListPartPsiMixin o) {
  }

  @Override
  public void visitTypeParamPsiMixin(@NotNull HaxeTypeParamPsiMixin o) {
  }

  @Override
  public void visitTypePsiMixin(@NotNull HaxeTypePsiMixin o) {
  }

  @Override
  public void visitFieldDeclaration(@NotNull HaxeFieldDeclaration o) {
  }

  @Override
  public void visitFieldModifier(@NotNull HaxeFieldModifier o) {
  }

  @Override
  public void visitFunctionArgument(@NotNull HaxeFunctionArgument o) {
  }

  @Override
  public void visitFunctionReturnType(@NotNull HaxeFunctionReturnType o) {
  }

  @Override
  public void visitMethodModifier(@NotNull HaxeMethodModifier o) {
  }

  @Override
  public void visitMutabilityModifier(@NotNull HaxeMutabilityModifier o) {
  }

  @Override
  public void visitOptionalMark(@NotNull HaxeOptionalMark o) {
  }

  @Override
  public void visitReturnStatementWithoutSemicolon(@NotNull HaxeReturnStatementWithoutSemicolon o) {
  }

  @Override
  public void visitPsiModifier(@NotNull HaxePsiModifier o) {
  }

  @Override
  public void visitUsingStatementPsiMixin(@NotNull HaxeUsingStatementPsiMixin o) {
  }

  @Override
  public void visitPsiCompositeElement(@NotNull HaxePsiCompositeElement o) {
  }
}
