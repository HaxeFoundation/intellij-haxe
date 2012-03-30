// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;

public class HaxeVisitor extends PsiElementVisitor {

  public void visitAccess(@NotNull HaxeAccess o) {
    visitPsiCompositeElement(o);
  }

  public void visitAdditiveExpression(@NotNull HaxeAdditiveExpression o) {
    visitExpression(o);
  }

  public void visitAnonymousType(@NotNull HaxeAnonymousType o) {
    visitPsiCompositeElement(o);
  }

  public void visitAnonymousTypeBody(@NotNull HaxeAnonymousTypeBody o) {
    visitPsiCompositeElement(o);
  }

  public void visitAnonymousTypeField(@NotNull HaxeAnonymousTypeField o) {
    visitPsiCompositeElement(o);
  }

  public void visitAnonymousTypeFieldList(@NotNull HaxeAnonymousTypeFieldList o) {
    visitPsiCompositeElement(o);
  }

  public void visitArrayAccessExpression(@NotNull HaxeArrayAccessExpression o) {
    visitExpression(o);
  }

  public void visitArrayLiteral(@NotNull HaxeArrayLiteral o) {
    visitReference(o);
    // visitExpression(o);
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
    visitPsiCompositeElement(o);
  }

  public void visitBreakStatement(@NotNull HaxeBreakStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitBuildMacro(@NotNull HaxeBuildMacro o) {
    visitPsiCompositeElement(o);
  }

  public void visitCallExpression(@NotNull HaxeCallExpression o) {
    visitReference(o);
    // visitExpression(o);
  }

  public void visitCaseStatement(@NotNull HaxeCaseStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitCastExpression(@NotNull HaxeCastExpression o) {
    visitExpression(o);
  }

  public void visitCatchExpression(@NotNull HaxeCatchExpression o) {
    visitExpression(o);
  }

  public void visitCatchStatement(@NotNull HaxeCatchStatement o) {
    visitPsiCompositeElement(o);
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
    visitPsiCompositeElement(o);
  }

  public void visitCustomMeta(@NotNull HaxeCustomMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitDeclarationAttribute(@NotNull HaxeDeclarationAttribute o) {
    visitPsiCompositeElement(o);
  }

  public void visitDeclarationAttributeList(@NotNull HaxeDeclarationAttributeList o) {
    visitPsiCompositeElement(o);
  }

  public void visitDefaultStatement(@NotNull HaxeDefaultStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitDoWhileStatement(@NotNull HaxeDoWhileStatement o) {
    visitPsiCompositeElement(o);
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
    visitComponent(o);
  }

  public void visitExpression(@NotNull HaxeExpression o) {
    visitPsiCompositeElement(o);
  }

  public void visitExpressionList(@NotNull HaxeExpressionList o) {
    visitPsiCompositeElement(o);
  }

  public void visitExternClassDeclaration(@NotNull HaxeExternClassDeclaration o) {
    visitClass(o);
  }

  public void visitExternClassDeclarationBody(@NotNull HaxeExternClassDeclarationBody o) {
    visitPsiCompositeElement(o);
  }

  public void visitExternFunctionDeclaration(@NotNull HaxeExternFunctionDeclaration o) {
    visitComponentWithDeclarationList(o);
  }

  public void visitExternOrPrivate(@NotNull HaxeExternOrPrivate o) {
    visitPsiCompositeElement(o);
  }

  public void visitFakeEnumMeta(@NotNull HaxeFakeEnumMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitForStatement(@NotNull HaxeForStatement o) {
    visitComponent(o);
  }

  public void visitFunctionDeclarationWithAttributes(@NotNull HaxeFunctionDeclarationWithAttributes o) {
    visitComponentWithDeclarationList(o);
  }

  public void visitFunctionLiteral(@NotNull HaxeFunctionLiteral o) {
    visitExpression(o);
  }

  public void visitFunctionPrototypeDeclarationWithAttributes(@NotNull HaxeFunctionPrototypeDeclarationWithAttributes o) {
    visitComponentWithDeclarationList(o);
  }

  public void visitFunctionType(@NotNull HaxeFunctionType o) {
    visitPsiCompositeElement(o);
  }

  public void visitGetterMeta(@NotNull HaxeGetterMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitIdentifier(@NotNull HaxeIdentifier o) {
    visitPsiCompositeElement(o);
  }

  public void visitIfExpression(@NotNull HaxeIfExpression o) {
    visitExpression(o);
  }

  public void visitIfStatement(@NotNull HaxeIfStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitImportStatement(@NotNull HaxeImportStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitInherit(@NotNull HaxeInherit o) {
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

  public void visitIteratorExpression(@NotNull HaxeIteratorExpression o) {
    visitExpression(o);
  }

  public void visitLiteralExpression(@NotNull HaxeLiteralExpression o) {
    visitReference(o);
    // visitExpression(o);
  }

  public void visitLocalFunctionDeclaration(@NotNull HaxeLocalFunctionDeclaration o) {
    visitComponent(o);
  }

  public void visitLocalVarDeclaration(@NotNull HaxeLocalVarDeclaration o) {
    visitPsiCompositeElement(o);
  }

  public void visitLocalVarDeclarationPart(@NotNull HaxeLocalVarDeclarationPart o) {
    visitComponent(o);
  }

  public void visitLogicAndExpression(@NotNull HaxeLogicAndExpression o) {
    visitExpression(o);
  }

  public void visitLogicOrExpression(@NotNull HaxeLogicOrExpression o) {
    visitExpression(o);
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
    visitReference(o);
    // visitExpression(o);
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

  public void visitPackageStatement(@NotNull HaxePackageStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitParameter(@NotNull HaxeParameter o) {
    visitComponent(o);
  }

  public void visitParameterList(@NotNull HaxeParameterList o) {
    visitPsiCompositeElement(o);
  }

  public void visitParenthesizedExpression(@NotNull HaxeParenthesizedExpression o) {
    visitExpression(o);
  }

  public void visitPp(@NotNull HaxePp o) {
    visitPsiCompositeElement(o);
  }

  public void visitPpElse(@NotNull HaxePpElse o) {
    visitPsiCompositeElement(o);
  }

  public void visitPpElseIf(@NotNull HaxePpElseIf o) {
    visitPsiCompositeElement(o);
  }

  public void visitPpEnd(@NotNull HaxePpEnd o) {
    visitPsiCompositeElement(o);
  }

  public void visitPpError(@NotNull HaxePpError o) {
    visitPsiCompositeElement(o);
  }

  public void visitPpIf(@NotNull HaxePpIf o) {
    visitPsiCompositeElement(o);
  }

  public void visitPpIfValue(@NotNull HaxePpIfValue o) {
    visitPsiCompositeElement(o);
  }

  public void visitPrefixExpression(@NotNull HaxePrefixExpression o) {
    visitExpression(o);
  }

  public void visitPropertyAccessor(@NotNull HaxePropertyAccessor o) {
    visitPsiCompositeElement(o);
  }

  public void visitPropertyDeclaration(@NotNull HaxePropertyDeclaration o) {
    visitPsiCompositeElement(o);
  }

  public void visitReferenceExpression(@NotNull HaxeReferenceExpression o) {
    visitReference(o);
    // visitExpression(o);
  }

  public void visitRequireMeta(@NotNull HaxeRequireMeta o) {
    visitPsiCompositeElement(o);
  }

  public void visitReturnStatement(@NotNull HaxeReturnStatement o) {
    visitPsiCompositeElement(o);
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

  public void visitSuffixExpression(@NotNull HaxeSuffixExpression o) {
    visitExpression(o);
  }

  public void visitSwitchExpression(@NotNull HaxeSwitchExpression o) {
    visitExpression(o);
  }

  public void visitSwitchStatement(@NotNull HaxeSwitchStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitTernaryExpression(@NotNull HaxeTernaryExpression o) {
    visitExpression(o);
  }

  public void visitThisExpression(@NotNull HaxeThisExpression o) {
    visitReference(o);
    // visitExpression(o);
  }

  public void visitThrowStatement(@NotNull HaxeThrowStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitTryExpression(@NotNull HaxeTryExpression o) {
    visitExpression(o);
  }

  public void visitTryStatement(@NotNull HaxeTryStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitType(@NotNull HaxeType o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeConstraint(@NotNull HaxeTypeConstraint o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeExtends(@NotNull HaxeTypeExtends o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeList(@NotNull HaxeTypeList o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeParam(@NotNull HaxeTypeParam o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypeTag(@NotNull HaxeTypeTag o) {
    visitPsiCompositeElement(o);
  }

  public void visitTypedefDeclaration(@NotNull HaxeTypedefDeclaration o) {
    visitClass(o);
  }

  public void visitUnsignedShiftRightOperator(@NotNull HaxeUnsignedShiftRightOperator o) {
    visitPsiCompositeElement(o);
  }

  public void visitUsingStatement(@NotNull HaxeUsingStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitVarDeclaration(@NotNull HaxeVarDeclaration o) {
    visitPsiCompositeElement(o);
  }

  public void visitVarDeclarationPart(@NotNull HaxeVarDeclarationPart o) {
    visitComponent(o);
  }

  public void visitVarInit(@NotNull HaxeVarInit o) {
    visitPsiCompositeElement(o);
  }

  public void visitWhileStatement(@NotNull HaxeWhileStatement o) {
    visitPsiCompositeElement(o);
  }

  public void visitClass(@NotNull HaxeClass o) {
    visitPsiCompositeElement(o);
  }

  public void visitComponent(@NotNull HaxeComponent o) {
    visitPsiCompositeElement(o);
  }

  public void visitComponentWithDeclarationList(@NotNull HaxeComponentWithDeclarationList o) {
    visitPsiCompositeElement(o);
  }

  public void visitNamedElement(@NotNull HaxeNamedElement o) {
    visitPsiCompositeElement(o);
  }

  public void visitReference(@NotNull HaxeReference o) {
    visitPsiCompositeElement(o);
  }

  public void visitPsiCompositeElement(@NotNull HaxePsiCompositeElement o) {
    visitElement(o);
  }

}
