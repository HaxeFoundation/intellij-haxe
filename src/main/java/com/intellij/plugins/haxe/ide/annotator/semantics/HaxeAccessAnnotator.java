package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceExpressionImpl;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataContent;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.*;

public class HaxeAccessAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeReferenceExpression referenceExpression) {
      // we want to ignore references used in package, type or metas
      if (checkIfShouldBeIgnored(referenceExpression)) return;
      checkAccessForReference(referenceExpression, holder);
    }

   else if (element instanceof HaxeCompiletimeMetaArg compileTimeMeta) {
      HaxeMeta haxeMeta = PsiTreeUtil.getParentOfType(compileTimeMeta, HaxeMeta.class);
      if (haxeMeta != null) {
        if (haxeMeta.isType(ALLOW) || haxeMeta.isType(ACCESS)) {
          checkIfFullyQualified(haxeMeta, holder);
        }
      }
    }
  }


  private static boolean checkIfShouldBeIgnored(HaxeReferenceExpression referenceExpression) {
    PsiElement expressionParent = referenceExpression.getParent();
    if(expressionParent instanceof  HaxePackageStatement) return true;
    if(expressionParent instanceof  HaxeType) return true;

    HaxeMeta haxeMeta = PsiTreeUtil.getParentOfType(expressionParent, HaxeMeta.class);
    if(haxeMeta instanceof  HaxeCompiletimeMetaArg) return true;
    return false;
  }

  private void checkAccessForReference(@NotNull HaxeReferenceExpression referenceExpression, @NotNull AnnotationHolder holder) {


    HaxeClass memberClass = null;
    HaxeMemberModel memberModel = null;
    String memberName = null;

    PsiElement resolve = referenceExpression.resolve();

    if (resolve instanceof HaxeFieldDeclaration fieldDeclaration) {
      HaxeModel model = fieldDeclaration.getModel();
      if (model instanceof HaxeMemberModel haxeMemberModel) {

        memberModel = haxeMemberModel;
        memberName = haxeMemberModel.getName();
        memberClass = getMemberHaxeClass(haxeMemberModel);
      }
    }
    if (resolve instanceof HaxeMethodDeclaration methodDeclaration) {
      HaxeMethodModel model = methodDeclaration.getModel();
      if (model instanceof HaxeMemberModel haxeMemberModel) {
        memberModel = haxeMemberModel;
        memberName = haxeMemberModel.getName();
        memberClass = getMemberHaxeClass(haxeMemberModel);
      }
    }

    // ignore if we cant find member (probably a reference to a type)
    if (memberModel != null) {
      checkStaticAccess(holder, referenceExpression, memberModel);
      if (!memberModel.isPublic()) {
        checkPrivateAccess(holder, referenceExpression, memberModel, memberClass, memberName);
      }
    }

  }

  private void checkStaticAccess(@NotNull AnnotationHolder holder, @NotNull HaxeReferenceExpression referenceExpression, @NotNull HaxeMemberModel memberModel) {
    // ignore anything inside metas (ex. @:build @:autoBuild etc)
    if (PsiTreeUtil.getParentOfType(referenceExpression, HaxeMeta.class)!= null) return;
    // ignore non chained references (usually local access in same class)
    if (HaxeResolveUtil.getLeftReference(referenceExpression) == null) return;
    if (isStaticExtensionReferences(referenceExpression)) return;

    boolean isStaticAccess = isStaticAccess(referenceExpression);
    boolean isMemberStatic = memberModel.isStatic();
    boolean isMemberInline = memberModel.isInline();
    boolean isConstructor = (memberModel instanceof HaxeMethodModel model) && model.isConstructor();
    boolean isMethodBind = (memberModel instanceof HaxeMethodModel) && referenceExpression.getLastChild().textMatches("bind");
    if(isMethodBind)  return;
    if (isStaticAccess && !isMemberStatic && !isConstructor) {
      // TODO bundle
      holder.newAnnotation(HighlightSeverity.ERROR, "Static access to instance field " + memberModel.getName() + " is not allowed ")
              .range(referenceExpression.getLastChild())
              .create();
    } else if (!isStaticAccess && isMemberStatic) {
      if (isMemberInline) return;// allow static access when inlining
      // TODO bundle
      holder.newAnnotation(HighlightSeverity.ERROR, "Cannot access static field " + memberModel.getName() + " from a class instance")
              .range(referenceExpression.getLastChild())
              .create();

    }
  }

  private static boolean isStaticExtensionReferences(@NotNull HaxeReferenceExpression referenceExpression) {
    if(referenceExpression.getParent() instanceof HaxeCallExpression callExpression) {
      return callExpression.resolveIsStaticExtension();
    }
    return false;
  }

  private  boolean isStaticAccess(HaxeReferenceExpression referenceExpression) {
    final HaxeReference leftReference = HaxeResolveUtil.getLeftReference(referenceExpression);
    if (leftReference instanceof HaxeReferenceExpressionImpl callie) {
      PsiElement callieResolved = callie.resolve();

      if (callieResolved instanceof HaxeImportAlias alias) {
        HaxeIdentifier identifier = alias.getIdentifier();
        return callie.getLastChild().textMatches(identifier);
      }
      if (callieResolved instanceof HaxeClass haxeClass) {
        String name = haxeClass.getName();
        if (name != null) {
          return callie.getLastChild().textMatches(name);
        }
      }
    }
      return false;
  }

  private void checkPrivateAccess(@NotNull AnnotationHolder holder, @NotNull HaxeReferenceExpression referenceExpression, @NotNull HaxeMemberModel memberModel, HaxeClass memberClass, String memberName) {
    // ignore anything inside metas (ex. @:build @:autoBuild etc)
    if (PsiTreeUtil.getParentOfType(referenceExpression, HaxeMeta.class)!= null) return;

    HaxeClass currentClass = PsiTreeUtil.getParentOfType(referenceExpression, HaxeClass.class);

    // ignoring anonymous types for now
    if (memberClass != null) {
      if (memberClass.isAnonymousType() || memberClass.isObjectLiteralType()) return;
    }
    if (currentClass != null) {
      if (currentClass.isAnonymousType() || currentClass.isObjectLiteralType()) return;
    }

    if (memberClass == currentClass) {
      // if same class then private access allowed
      return;
    }

    if (inheritsFrom(currentClass, memberClass)) {
      // if inherited member then private access allowed
      return;
    }

    if (overridesMemberInCommonClass(memberModel, currentClass)) {
      // if inherited member then private access allowed
      return;
    }

    HaxeMemberModel referenceParentModel = getExpressionsParentsModel(referenceExpression);

    if (expressionHasPrivateAccessMeta(referenceExpression)) {
      // @:privateAccess should allow access to normal private members
      return;
    }
    if (hasAllowMetaFor(currentClass, memberClass, memberModel, referenceParentModel)) {
      return;
    }
    if (hasAccessMetaFor(currentClass, memberClass, memberModel, referenceParentModel)) {
      return;
    }

    // TODO bundle
    holder.newAnnotation(HighlightSeverity.ERROR, "Cannot access private field " + memberName)
            .range(referenceExpression.getLastChild())
            .create();
  }

  private boolean overridesMemberInCommonClass(HaxeMemberModel memberModel, HaxeClass currentClass) {
    if(memberModel instanceof  HaxeMethodModel memberMethod) {
      HaxeMethodModel method = memberMethod;
      while(method.isOverride()) {
        method = memberMethod.getAncestorMethod(null);
        if(method == null) {
          break;
        }else {
          HaxeClassModel declaringClass = method.getDeclaringClass();
          if(declaringClass != null){
            if (inheritsFrom(currentClass, declaringClass.haxeClass)) return true;
          }
        }
      }
    }
    return false;
  }

  private void checkIfFullyQualified(HaxeMeta meta, @NotNull AnnotationHolder holder) {
    HaxeReferenceExpression metaReference = getAccessMetaReference(meta);
    if (metaReference != null) {
      PsiElement target = metaReference.resolve();
      String metaReferenceText = metaReference.getQualifiedName();
      String qualifiedName = null;
      if (target instanceof PsiPackage aPackage) {
        qualifiedName = aPackage.getQualifiedName();
      } else if (target instanceof HaxeClass aClass) {
        qualifiedName = aClass.getQualifiedName();
      } else if (target instanceof HaxeMethod method) {
        FullyQualifiedInfo qualifiedInfo = method.getModel().getQualifiedInfo();
        if(qualifiedInfo != null) {
          qualifiedName = qualifiedInfo.toShortendImportReferenceString();
        }
      }

      if(qualifiedName != null) {
        if (!qualifiedName.equals(metaReferenceText)) {
          String finalQualifiedName = qualifiedName;
          holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Reference is not fully qualified")
                  .withFix(new HaxeFixer("Replace with fully qualified path") {
                    @Override
                    public void run() {
                      HaxeDocumentModel.fromElement(metaReference).replaceElementText(metaReference, finalQualifiedName);
                    }
                  })
                  .create();
        }
      }
    }
  }

  private boolean expressionHasPrivateAccessMeta(HaxeReferenceExpression referenceExpression) {
    HaxeReferenceExpression refExpression = referenceExpression;
    while (refExpression.getParent() instanceof HaxeReferenceExpression parent) refExpression = parent;

    PsiElement expression = referenceExpression;
    while (true) {
      if(expression == null) break;
      if (HaxeMetadataUtils.hasMeta(expression, HaxeMetadataCompileTimeMeta.class, PRIVATE_ACCESS)) {
        return true;
      } else {
        expression = expression.getParent();
        if (expression instanceof HaxeMethodDeclaration || expression instanceof HaxeClass || expression instanceof  HaxeModule) {
          break;// stop search if we have left any "reasonable" scope
        }
      }
    }
    return false;
  }

  private boolean hasAccessMetaFor(@Nullable HaxeClass currentClass, @Nullable HaxeClass memberClass, HaxeMemberModel memberModel, HaxeMemberModel referenceParentModel) {
    HaxeMetadataList metadataList = collectMetadata(currentClass, referenceParentModel, ACCESS);
    for (HaxeMeta metadata : metadataList) {
      List<PsiElement> accessMetaTarget = getAccessMetaTarget(metadata);
      for (PsiElement target : accessMetaTarget) {

        if (target instanceof PsiPackage aPackage) {
          if (memberModel.getPackage() == aPackage) {
            return true;
          }
        } else if (target instanceof HaxeModule module) {
          if (memberModel.getModule() == module) {
            return true;
          }
        } else if (target instanceof HaxeClass aClass) {
          if (memberClass == aClass) {
            return true;
          }
          if (inheritsFrom(memberClass, aClass)) {
            return true;
          }
          //we allow  both directions (tests  )
          if (inheritsFrom(aClass, memberClass)) {
            return true;
          }
        } else if (target instanceof HaxeMethod method) {
          if (memberModel.getMemberPsi() == method) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean hasAllowMetaFor(@Nullable HaxeClass currentClass, @Nullable HaxeClass memberClass, HaxeMemberModel memberModel, HaxeMemberModel referenceParentModel) {
    HaxeMetadataList metadataList = collectMetadata(memberClass, memberModel, ALLOW);

    for (HaxeMeta metadata : metadataList) {
      List<PsiElement> accessMetaTarget = getAccessMetaTarget(metadata);
      for (PsiElement target : accessMetaTarget) {

        if (target instanceof PsiPackage aPackage) {
          if (referenceParentModel.getPackage() == aPackage) {
            return true;
          }
        } else if (target instanceof HaxeModule module) {
          if (referenceParentModel.getModule() == module) {
            return true;
          }
        } else if (target instanceof HaxeClass aClass) {
          if (currentClass == aClass) {
            return true;
          }
          if (currentClass != null) {
            HaxeClassModel model = currentClass.getModel();
            if (model.inheritsFrom(aClass)) {
              return true;
            } else {
              // search module
              // by default we resolve Qname to class with same name as module, but in this case it seems
              // like the meta applies for the entire module, so we need to check all members in module
              HaxeModule module = aClass.getModule();
              if(module.getModel().getName().equals(aClass.getName())) {
                for (HaxeClass moduleClass : module.getClassDeclarationList()) {
                  if (currentClass == moduleClass) {
                    return true;
                  }
                  if (model.inheritsFrom(moduleClass)) {
                    return true;
                  }
                }
              }
            }
          }
        } else if (target instanceof HaxeMethod method) {
          if (referenceParentModel.getMemberPsi() == method) {
            return true;
          }
        }
      }
    }


    return false;
  }

  private static @Nullable HaxeReferenceExpression getAccessMetaReference(HaxeMeta metadata) {
    HaxeMetadataContent content = metadata.getContent();
    List<HaxeExpression> expressions = HaxeMetadataUtils.getCompileTimeExpressions(content);
    if(!expressions.isEmpty()) {
      if(expressions.getFirst() instanceof HaxeReferenceExpression expression ) {
        return expression;
      }
    }
    return null;
  }

  private static @NotNull List<PsiElement> getAccessMetaTarget(HaxeMeta metadata) {
    HaxeReferenceExpression reference = getAccessMetaReference(metadata);
    if(reference != null){
      PsiElement resolve = reference.resolve();
      if(resolve != null) {
        return List.of(resolve);
      }else {
        // NOTE: for some reason access metas does not allow / contain module names in Qnames
        // in normal resolve module name is a required part of Qnames unless the class and module is the same name
        // this is therefore a workaround for this behaviour since it does not align with other haxe logic (for instance imports)
        return resolveQnameWithMissingModule(metadata, reference);
      }
    }
    return List.of();
  }

  private static @NotNull List<PsiElement> resolveQnameWithMissingModule(HaxeMeta metadata, HaxeReferenceExpression reference) {
    return resolveQnameWithMissingModule(metadata, reference, false);
  }
  private static @NotNull List<PsiElement> resolveQnameWithMissingModule(HaxeMeta metadata, HaxeReferenceExpression reference, boolean secondPass) {
    PsiElement firstChild = reference.getFirstChild();
    PsiElement lastChild = reference.getLastChild();
    List<PsiElement> classesWithName = new ArrayList<>();
    if(firstChild instanceof HaxeReferenceExpression packageRef) {
      PsiElement packageResolve = packageRef.resolve();
      if(packageResolve instanceof  PsiPackage aPackage) {
        PsiFile[] packageFiles = aPackage.getFiles(GlobalSearchScope.allScope(metadata.getProject()));
        for (PsiFile packageFile : packageFiles) {
            if(packageFile instanceof  HaxeFile haxeFile) {
              HaxeFileModel model = haxeFile.getModel();
              HaxeClassModel classModel = model.getClassModel(lastChild.getText());
              if(classModel != null) {
                classesWithName.add(classModel.haxeClass);
              }
            }
        }
      }
      // might be method in class not matching module name (with reference without module)
      if(!secondPass && packageResolve == null) {
        classesWithName.addAll(resolveQnameWithMissingModule(metadata, packageRef, true));
      }
    }
    return classesWithName;
  }

  private static @NotNull HaxeMetadataList collectMetadata(HaxeClass memberClass, HaxeMemberModel memberModel, HaxeMetadataTypeName metadataTypeName) {
    HaxeMetadataList metadataFromClass = HaxeMetadataUtils.getMetadataList(memberClass, HaxeMetadataCompileTimeMeta.class, metadataTypeName);
    if(memberModel != null) {
      HaxeMetadataList metadataFromMember = HaxeMetadataUtils.getMetadataList(memberModel.getMemberPsi(), HaxeMetadataCompileTimeMeta.class, metadataTypeName);
      metadataFromClass.addAll(metadataFromMember);
    }
    return metadataFromClass;
  }

  private static @Nullable HaxeMemberModel getExpressionsParentsModel(@NotNull HaxeReferenceExpression referenceExpression) {
    HaxeMethodDeclaration methodDeclaration = PsiTreeUtil.getParentOfType(referenceExpression, HaxeMethodDeclaration.class);
    HaxeMemberModel referenceParentModel =  methodDeclaration == null ? null : methodDeclaration.getModel();
    if(referenceParentModel == null) {
      HaxeFieldDeclaration fieldDeclaration = PsiTreeUtil.getParentOfType(referenceExpression, HaxeFieldDeclaration.class);
      referenceParentModel = fieldDeclaration == null ? null : (HaxeMemberModel) fieldDeclaration.getModel();
    }
    return referenceParentModel;
  }

  private static boolean inheritsFrom(@Nullable HaxeClass currentClass, @Nullable HaxeClass memberClass) {
    if(currentClass == null  || memberClass == null) return false;
    return currentClass.getModel().inheritsFrom(memberClass);
  }

  @Nullable
  private static HaxeClass getMemberHaxeClass(HaxeMemberModel model) {
      HaxeClassModel declaringClass = model.getDeclaringClass();
      if(declaringClass != null) return declaringClass.haxeClass;
    return null;
  }


}
