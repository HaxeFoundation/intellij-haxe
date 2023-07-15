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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.documentation.DocumentationManagerUtil;
import com.intellij.icons.AllIcons;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

import static com.intellij.plugins.haxe.ide.HaxeDocumentationSignatureUtil.*;
import static com.intellij.util.ui.UIUtil.colorToHex;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDocumentationProvider implements DocumentationProvider {

  /*
        provides ctrl+hover info
       */
  @Override
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    return null;
  }

  @Override
  public String generateDoc(PsiElement element, PsiElement originalElement) {
    HtmlBuilder mainBuilder = new HtmlBuilder();

    if (element instanceof HaxeTypeListPart part) {
      if (part.getTypeOrAnonymous() != null) {
        HaxeType type = part.getTypeOrAnonymous().getType();
        if (type != null) {
          PsiElement resolve = type.getReferenceExpression().resolve();
          if (resolve != null) {
            element = resolve;
          }
        }
      }
    }

    HaxeNamedComponent namedComponent = getNamedComponent(element);
    if (namedComponent == null) {

      if (element instanceof HaxeLiteralExpression) {
        return null; // no need to  show docs for literal expressions
      }else {
        HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(element);
        HaxeExpressionEvaluator.evaluate(element, context, null);
        makeHeader(mainBuilder, context.result);
      }
      return mainBuilder.toString();
    }

    HaxeDocumentationRenderer renderer = element.getProject().getService(HaxeDocumentationRenderer.class);
    final HaxeComponentType type = HaxeComponentType.typeOf(namedComponent);
    HtmlBuilder definitionBuilder = new HtmlBuilder();
    switch (type) {
      case CLASS, INTERFACE, TYPEDEF, ENUM -> processType(definitionBuilder, namedComponent, renderer);
      case METHOD, FUNCTION -> processMethod(definitionBuilder, namedComponent, renderer);
      case FIELD -> processField(definitionBuilder, namedComponent, renderer);
      case VARIABLE -> processVariable(definitionBuilder, namedComponent, renderer);
      case PARAMETER -> processParameter(definitionBuilder, namedComponent, renderer);
      case TYPE_PARAMETER -> processTypeParameter(definitionBuilder, namedComponent, renderer);
    }

    HtmlChunk.Element content = definitionBuilder.wrapWith(HtmlChunk.div().attr("class", "definition"));
    mainBuilder.append(content);
    appendDocumentation(namedComponent, renderer, mainBuilder);
    mainBuilder.br();
    return mainBuilder.toString();
  }

  private static HaxeNamedComponent getNamedComponent(PsiElement element) {
    if (element instanceof HaxeNamedComponent namedComponent) {
      return namedComponent;
    }
    else if (element.getParent() instanceof HaxeNamedComponent namedComponent) {
      return namedComponent;
    }
    return null;
  }

  private void processTypeParameter(HtmlBuilder builder, HaxeNamedComponent component, HaxeDocumentationRenderer renderer) {
    if (component instanceof HaxeGenericListPart genericListPart) {
      String signature = genericListPart.getText();
      builder.br()
        .appendRaw(renderer.languageHighlighting(signature))
        .append(HtmlChunk.Element.tag("i"))
        .append(" (Type parameter)")
        .append(HtmlChunk.Element.tag("/i"))
        .br();
      HaxeGenericParam paramList = (HaxeGenericParam)genericListPart.getParent();
      PsiElement parent = paramList.getParent();
      if (parent instanceof HaxeMethodDeclaration methodDeclaration) {

        HaxeMethodModel methodModel = methodDeclaration.getModel();
        if (methodModel != null) {
          builder.br().append("Defined in:").br();
          appendMethodInfo(builder, renderer, methodModel);
        }
      }
      if (parent instanceof HaxeClass haxeClass) {
        builder.br().append("Defined in:").br();
        addTypeSignature(builder, haxeClass, renderer);
      }
    }
  }

  private void processType(HtmlBuilder builder, HaxeNamedComponent component, HaxeDocumentationRenderer renderer) {
    String packageString = HaxeResolveUtil.getPackageName(component.getContainingFile());

    StringBuilder stringBuilder = new StringBuilder();
    DocumentationManagerUtil.createHyperlink(stringBuilder, packageString, packageString, false, true);
    builder.append(HtmlChunk.icon("AllIcons.Nodes.Package", AllIcons.Nodes.Package)).nbsp(1);
    builder.appendRaw(stringBuilder.toString()).br();


    addTypeSignature(builder, component, renderer);
  }

  private void addTypeSignature(HtmlBuilder builder, HaxeNamedComponent component, HaxeDocumentationRenderer renderer) {
    if (component instanceof HaxeClassDeclaration declaration) {
      String signature = getClassSignature(declaration);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
    if (component instanceof HaxeExternClassDeclaration declaration) {
      String signature = getExternClassSignature(declaration);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
    if (component instanceof HaxeAbstractClassDeclaration declaration) {
      String signature = getAbstractSignature(declaration);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
    else if (component instanceof HaxeInterfaceDeclaration interfaceDeclaration) {
      String signature = getInterfaceSignature(interfaceDeclaration);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
    else if (component instanceof HaxeExternInterfaceDeclaration interfaceDeclaration) {
      String signature = getExternInterfaceSignature(interfaceDeclaration);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
    else if (component instanceof HaxeEnumDeclaration enumDeclaration) {
      String signature = getEnumSignature(enumDeclaration);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
    else if (component instanceof HaxeTypedefDeclaration typeDeclaration) {
      String signature = getTypeDefSignature(typeDeclaration);
      String type = "(" + getTypedefType(typeDeclaration) + ")";
      builder.br().appendRaw(renderer.languageHighlighting(signature)).append(type);
    }
  }

  private Object getTypedefType(HaxeTypedefDeclaration declaration) {
    HaxeFunctionType functionType = declaration.getFunctionType();
    if (functionType != null) {
      return functionType.getText();
    }
    else if (declaration.getTypeOrAnonymous() != null) {
      HaxeType type = declaration.getTypeOrAnonymous().getType();
      if (type != null) {
        return type.getText();
      }
      else {
        return "*Anonymous type*";
      }
    }
    return null;
  }


  private static void appendDocumentation(HaxeNamedComponent namedComponent, HaxeDocumentationRenderer service, HtmlBuilder htmlBuilder) {
    final PsiComment comment = HaxeResolveUtil.findDocumentation(namedComponent);
    if (comment != null) {
      HtmlBuilder tmpBuilder = new HtmlBuilder();
      String docs = HaxePresentableUtil.unwrapCommentDelimiters(comment.getText());
      String rendered = service.renderDocs(docs);
      HtmlChunk.Element content = tmpBuilder.appendRaw(rendered).wrapWith(HtmlChunk.Element.div().attr("class", "content"));
      htmlBuilder.append(content);
    }
  }


  private void processMethod(HtmlBuilder builder, HaxeNamedComponent component, HaxeDocumentationRenderer renderer) {


    if (component instanceof HaxeMethodDeclaration methodDeclaration) {
      appendClassOrModuleReference(builder, methodDeclaration);

      HaxeMethodModel methodModel = methodDeclaration.getModel();
      if (methodModel != null) {
        appendMethodInfo(builder, renderer, methodModel);
      }
      else {
        HaxeComponentName componentName = methodDeclaration.getComponentName();
      }
    }
  }

  private static void appendMethodInfo(HtmlBuilder builder,
                                       HaxeDocumentationRenderer renderer,
                                       HaxeMethodModel methodModel) {
    StringBuilder stringBuilder = new StringBuilder();

    @NotNull PsiElement[] children = methodModel.getBasePsi().getChildren();

    boolean gotParameters = false;
    for (PsiElement child : children) {
      if (child instanceof HaxeComponentName) {
        stringBuilder.append("function ").append(child.getText());
      }
      else if (child instanceof HaxeParameterList parameterList) {
        stringBuilder.append("(");
        @NotNull List<HaxeParameter> list = parameterList.getParameterList();
        for (int i = 0; i < list.size(); i++) {
          HaxeParameter parameter = list.get(i);
          gotParameters = true;
          stringBuilder.append("\n").append("\t").append(parameter.getText());
          if (i < list.size() - 1) stringBuilder.append(",");
        }
        if (gotParameters) stringBuilder.append("\n");
        stringBuilder.append(")");
      }
      else if (child instanceof HaxeMethodModifier) {
        stringBuilder.append(child.getText()).append(" ");
      }
      else if (child instanceof HaxeGenericParam) {
        stringBuilder.append(child.getText());
      }
      else if (child instanceof HaxeTypeTag) {
        if (gotParameters) stringBuilder.append("\n");
        stringBuilder.append(child.getText());
      }
    }


    String highlighting = renderer.languageHighlighting(stringBuilder.toString());
    builder.appendRaw(highlighting);
  }

  private static void appendClassOrModuleReference(HtmlBuilder builder, PsiMember methodDeclaration) {
    PsiClass containingClass = methodDeclaration.getContainingClass();
    if (containingClass != null) {
      StringBuilder stringBuilder = new StringBuilder();
      String qualifiedName = containingClass.getQualifiedName();
      DocumentationManagerUtil.createHyperlink(stringBuilder, qualifiedName, qualifiedName, false, true);
      // TODO haxe icons
      builder.append(HtmlChunk.icon("AllIcons.Nodes.Class", AllIcons.Nodes.Class)).nbsp(1);
      builder.appendRaw(stringBuilder.toString()).br().br();
    }
    else {
      PsiFile containingFile = methodDeclaration.getContainingFile();
      //TODO make link to module
    }
  }

  private void processField(HtmlBuilder builder, HaxeNamedComponent component, HaxeDocumentationRenderer renderer) {
    if (component instanceof HaxeFieldDeclaration fieldDeclaration) {
      appendClassOrModuleReference(builder, fieldDeclaration);
      resolveTypeAndMakeHeader(builder, component);
      String signature = getFieldSignature(fieldDeclaration);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
    else if (component instanceof HaxeEnumValueDeclaration enumValueDeclaration) {
      appendClassOrModuleReference(builder, enumValueDeclaration);
      resolveTypeAndMakeHeader(builder, component);
      String signature = getEnumValueSignature(enumValueDeclaration);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
    else if (component instanceof HaxeAnonymousTypeField anonymousTypeField) {
      appendClassOrModuleReference(builder, anonymousTypeField);
      resolveTypeAndMakeHeader(builder, component);
      String signature = getAnonymousTypeFieldSignature(anonymousTypeField);
      builder.br().appendRaw(renderer.languageHighlighting(signature));
    }
  }

  private String getAnonymousTypeFieldSignature(HaxeAnonymousTypeField field) {
    return field.getText();
  }

  private String getEnumValueSignature(HaxeEnumValueDeclaration declaration) {
    return declaration.getText();
  }

  private String getFieldSignature(HaxeFieldDeclaration declaration) {
    return declaration.getText();
  }

  private void processParameter(HtmlBuilder builder, HaxeNamedComponent component, HaxeDocumentationRenderer renderer) {
    resolveTypeAndMakeHeader(builder, component);
    if (component instanceof HaxeParameter parameter) {
      builder.br()
        .appendRaw(renderer.languageHighlighting(parameter.getText()))
        .append(HtmlChunk.Element.tag("i"))
        .append(" (Parameter)")
        .append(HtmlChunk.Element.tag("/i"));

      PsiElement parent = parameter.getParent().getParent();
      if (parent instanceof HaxeMethodDeclaration methodDeclaration) {
        HaxeMethodModel methodModel = methodDeclaration.getModel();
        if (methodModel != null) {
          builder.br().br().append("Defined in:").br();
          appendMethodInfo(builder, renderer, methodModel);
        }
      }
    }
  }

  private void processVariable(HtmlBuilder builder, HaxeNamedComponent component, HaxeDocumentationRenderer renderer) {
    if (component instanceof HaxeSwitchCaseCaptureVar captureVar) {
      resolveTypeAndMakeHeader(builder, captureVar);
      builder.br();
    }
    else if (component instanceof HaxeLocalVarDeclaration varDeclaration) {
      resolveTypeAndMakeHeader(builder, varDeclaration);
      builder.br();

      String modifier = ((HaxeLocalVarDeclarationList)component.getParent()).getMutabilityModifier().getText();
      String signature = modifier + " " + varDeclaration.getText();

      String highlighting = renderer.languageHighlighting(signature);
      builder.appendRaw(highlighting);
    }
    else if (component instanceof HaxeForStatement forStatement) {
      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(forStatement);
      HaxeExpressionEvaluator.evaluate(forStatement, context, null);

      makeHeader(builder, context.result);
    }
    else if (component instanceof HaxeEnumExtractedValue extractedValue) {
      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(extractedValue);
      HaxeExpressionEvaluator.evaluate(extractedValue, context, null);

      makeHeader(builder, context.result);
    }
  }

  private static void resolveTypeAndMakeHeader(HtmlBuilder builder, HaxeNamedComponent component) {
    HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(component);
    HaxeExpressionEvaluator.evaluate(component, context, null);

    makeHeader(builder, context.result);
  }

  private static void makeHeader(HtmlBuilder builder, ResultHolder result) {
    if (result != null && !result.isUnknown()) {
      Color color = DefaultLanguageHighlighterColors.CONSTANT.getDefaultAttributes().getForegroundColor();
      HtmlChunk.Element element = new HtmlBuilder().append("(Type: " + result.getType().withoutConstantValue() + ")")
        .wrapWith(HtmlChunk.Element.tag("code").attr("color", "#" + colorToHex(color))).wrapWith(HtmlChunk.Element.tag("i"));

      builder.append(element).br();
    }
  }

  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    return null;
  }

  @Override
  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    return null;
  }

  @Override
  public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {


    final FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(link);
    List<HaxeModel> result = HaxeProjectModel.fromElement(context).resolve(qualifiedInfo, context.getResolveScope());
    if (result != null && !result.isEmpty()) {
      HaxeModel item = result.get(0);
      if (item instanceof HaxeFileModel) {
        HaxeClassModel mainClass = ((HaxeFileModel)item).getMainClassModel();
        if (mainClass != null) {
          return mainClass.getBasePsi();
        }
      }
      return item.getBasePsi();
    }
    return null;
  }
}
