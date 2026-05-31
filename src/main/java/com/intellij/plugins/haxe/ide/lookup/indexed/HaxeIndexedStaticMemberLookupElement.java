package com.intellij.plugins.haxe.ide.lookup.indexed;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.lookup.HaxeCompletionPriorityData;
import com.intellij.plugins.haxe.ide.lookup.HaxePsiLookupElement;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeClassLookupData;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeMemberLookupData;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.*;
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
public class HaxeIndexedStaticMemberLookupElement extends LookupElement implements HaxePsiLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();

  @Getter private FullyQualifiedInfo qualifiedInfo;
  private final HaxeMemberLookupData lookupData;

  @Getter private final HaxeComponentType type;

  private HaxeMemberModel memberModel;

  @Getter private String packageName;
  @Getter private String moduleName;
  @Getter private String className;
  @Getter private String memberName;

  private String _typeValue;

  private boolean presentationCalculated = false;
  private Icon icon;



  public HaxeIndexedStaticMemberLookupElement(HaxeMemberLookupData lookupData) {
    this.lookupData = lookupData;
    this.qualifiedInfo = lookupData.qualifiedInfo;
    this.type = lookupData.type;
    this.icon = lookupData.icon;

    // NOTE: using string as direct access does not have resolver
    //TODO map of parameters <String, String>
    // todo return type (String)

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
    if (qualifiedInfo != null) {
      addImportIfNecessary(context, element, qualifiedInfo.withMemberName(null).toShortendImportReferenceString());
    }
    else {
      log.error("Unable to get fullyQualifiedInfo (1)");
    }
  }


  public @Nullable HaxeBaseMemberModel getModel() {
    return lookupData.getModel();
  }

  @Override
  public @Nullable PsiElement getPsiElement() {
    HaxeBaseMemberModel model = getModel();
    return model != null ?  model.getBasePsi() : null;
  }

  private void calculatePresentation() {
    packageName = qualifiedInfo.getPackageName();
    moduleName = qualifiedInfo.getModuleName();
    className = qualifiedInfo.getClassName();
    memberName = qualifiedInfo.getMemberName();

    presentationCalculated = true;
  }


  @NotNull
  @Override
  public String deduplicateKey() {
    if (qualifiedInfo != null) {
      return qualifiedInfo.toString();
    }
    else {
      log.warn("Unable to get fullyQualifiedInfo (2)");
      return "Error";
    }
  }

  public String getTypeValue() {
    if(memberModel == null) {
      return "";
    }
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
