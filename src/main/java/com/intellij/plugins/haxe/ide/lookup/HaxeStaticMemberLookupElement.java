package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.*;
import icons.HaxeIcons;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.swing.*;
import java.util.Set;

import static com.intellij.plugins.haxe.ide.lookup.lookupItemImportUtil.*;

@CustomLog
@EqualsAndHashCode
public class HaxeStaticMemberLookupElement extends LookupElement implements HaxePsiLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();
  @Getter private final HaxeComponentType type;
  private final HaxeMemberModel memberModel;

  @Getter private String packageName;
  @Getter private String moduleName;
  @Getter private String className;
  @Getter private String memberName;

  private String _typeValue;

  private boolean presentationCalculated = false;
  private Icon icon;



  public HaxeStaticMemberLookupElement(HaxeMemberModel memberModel) {
    type = HaxeComponentType.typeOf(memberModel.getNamedComponentPsi());
    icon = type.getCompletionIcon();
    this.memberModel = memberModel;

  }

  @NotNull
  @Override
  public String getLookupString() {
    if (!presentationCalculated) {
      calculatePresentation();
    }
    if (className != null) {
      return className + "." + memberName;
    }
    else {
      return moduleName + "." + memberName;
    }
  }

  @Override
  public @Unmodifiable Set<String> getAllLookupStrings() {
    return Set.of(getLookupString(), memberName);
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    if (!presentationCalculated) {
      calculatePresentation();
    }
    presentation.setItemText(getLookupString());
    presentation.setTypeText(packageName);
    presentation.setIcon(icon);
  }


  @Override
  public void handleInsert(InsertionContext context) {
    PsiFile file = context.getFile();
    PsiElement element = file.findElementAt(context.getStartOffset());
    FullyQualifiedInfo qualifiedInfo = memberModel.getQualifiedInfo();
    if (qualifiedInfo != null) {
      addImportIfNecessary(context, element, qualifiedInfo.withMemberName(null).toShortendImportReferenceString());
    }
    else {
      log.error("Unable to get fullyQualifiedInfo (1)");
    }
  }


  @Override
  public @Nullable PsiElement getPsiElement() {
    return memberModel.getBasePsi();
  }


  private void calculatePresentation() {

    HaxeClassModel classModel = memberModel.getDeclaringClass();
    HaxeModuleModel moduleModel = memberModel.getDeclaringModule();

    FullyQualifiedInfo qualifiedInfo = memberModel.getQualifiedInfo();

    packageName = qualifiedInfo != null ? qualifiedInfo.packagePath : "";
    moduleName = moduleModel != null ? moduleModel.getName() : null;
    className = classModel != null ? classModel.getName() : null;
    memberName = memberModel.getName();


    presentationCalculated = true;
  }


  @NotNull
  @Override
  public String deduplicateKey() {
    FullyQualifiedInfo qualifiedInfo = memberModel.getQualifiedInfo();
    if (qualifiedInfo != null) {
      return qualifiedInfo.toString();
    }
    else {
      log.warn("Unable to get fullyQualifiedInfo (2)");
      return "Error";
    }
  }

  public String getTypeValue() {
    if (_typeValue == null) {
      ResultHolder resultType = memberModel.getResultType();
      if (resultType == null || resultType.isUnknown()) {
        _typeValue = "";
      } else {
        _typeValue = resultType.getType().toPresentationString();
      }
    }
    return _typeValue;
  }

}
