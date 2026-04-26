package com.intellij.plugins.haxe.ide.documentation;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.ReferenceCodeLink;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeModuleImpl;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.commonmark.node.*;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeDocumentationCodeVisitor extends AbstractVisitor {

    public static Pattern qNamePattern = Pattern.compile("(\\w+)(\\.(\\w+))*");
    private final Project project;
    private final PsiDocCommentBase context;


    public HaxeDocumentationCodeVisitor(PsiDocCommentBase element) {
        this.context = element;
        this.project = element.getProject();
    }

    @Override
    public void visit(Code code) {
        String literal = code.getLiteral();
        Matcher matcher = qNamePattern.matcher(literal);
        if (matcher.find()) {
            if (replaceFullyQualifiedClass(code, literal)) return;
            if (replaceIndexedClassName(code, literal)) return;
            if(replaceMethodParameter(code, literal)) return;
            if(replaceClassMemberReference(code, literal)) return;
            if(context != null) {
                replaceContextClassMemberReference(code, matcher.group(1), literal);
            }
        }
    }

    private boolean replaceMethodParameter(Code code, String literal) {
        PsiElement owner = context.getOwner();
        if (owner  instanceof  HaxeMethod method) {
            List<HaxeParameterModel> parameters = method.getModel().getParameters();
            for (HaxeParameterModel parameter : parameters) {
                if(literal.equals(parameter.getName())) {
                    String qualifiedName = parameter.getQualifiedName();
                    replaceCodeWithReferenceCodeLink(code, qualifiedName, literal);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean replaceContextClassMemberReference(Code code, String member, String literal) {
        HaxeClass parentOfType = PsiTreeUtil.getStubOrPsiParentOfType(context, HaxeClass.class);
        if(parentOfType != null) {
            List<HaxeNamedComponent> members = parentOfType.findHaxeMemberByName(member, null);
            if (!members.isEmpty() &&  members.getFirst()  instanceof PsiMember psiMember) {
                PsiClass containingClass = psiMember.getContainingClass();
                if(containingClass != null) {
                    replaceCodeWithReferenceCodeLink(code, containingClass.getQualifiedName() + "." + member, literal);
                    return true;
                }
            }
        }

        HaxeModuleImpl haxeModule = PsiTreeUtil.getStubOrPsiParentOfType(context, HaxeModuleImpl.class);
        if(haxeModule != null) {

            HaxeModuleModel haxeModuleModel = haxeModule.getModel();
            HaxeBaseMemberModel memberPsi = haxeModuleModel.getMember(member, null);
            FullyQualifiedInfo qualifiedInfo = haxeModuleModel.getQualifiedInfo();
            if(qualifiedInfo != null) {
                String presentableText = qualifiedInfo.getPresentableText();

                if (memberPsi != null && memberPsi.getNameOrBasePsi() instanceof PsiMember psiMember) {
                    replaceCodeWithReferenceCodeLink(code, presentableText + "." + member, literal);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean replaceClassMemberReference(Code code, String literal) {
        // check if member of type
        HaxeClass haxeClass = null;
        String member = literal;
        int lastDotIndex = literal.lastIndexOf(".");
        if (lastDotIndex > -1) {
            String classRef = literal.substring(0, lastDotIndex);
            member  = literal.substring(lastDotIndex+1).replaceAll("[(\\[].*", "");
            haxeClass =  HaxeResolveUtil.findClassByQName(classRef, context);
            if (haxeClass == null) haxeClass = findUniqueClassFromIndex(classRef);
        } else {
            PsiElement owner = context.getOwner();
            if (owner instanceof HaxeClass ownerClass) {
                haxeClass = ownerClass;
            }
        }

        if (haxeClass != null) {
            List<HaxeNamedComponent> members = haxeClass.findHaxeMemberByName(member, null);
            if (!members.isEmpty() && members.getFirst() instanceof PsiMember psiMember) {
                PsiClass containingClass = psiMember.getContainingClass();
                replaceCodeWithReferenceCodeLink(code, containingClass.getQualifiedName() + "." + member, literal);
                return true;
            }
        }
        return false;
    }

    private boolean replaceIndexedClassName(Code code, String literal) {
        HaxeClass haxeClass = findUniqueClassFromIndex(literal);
        if (haxeClass != null) {
            replaceCodeWithReferenceCodeLink(code, haxeClass.getQualifiedName(), literal);
            return true;
        }
        return false;
    }

    private HaxeClass findUniqueClassFromIndex(String literal) {
        Collection<HaxeClass> itemsByName = HaxeClassNameStubIndex.getByNameFiltered(literal, project, GlobalSearchScope.allScope(project));
        if (itemsByName.size() == 1) {
            return itemsByName.iterator().next();
        }
        return null;
    }

    private boolean replaceFullyQualifiedClass(Code code, String literal) {
        HaxeClass classByQName = HaxeResolveUtil.findClassByQName(literal, context);
        if (classByQName != null) {
            replaceCodeWithReferenceCodeLink(code, literal, literal);
            return true;
        }
        return false;
    }

    private static void replaceCodeWithReferenceCodeLink(Code code, String qname, String linkText) {
        ReferenceCodeLink link = new ReferenceCodeLink();
        link.setPsiReference(qname);
        link.setLinkText(linkText);
        code.insertBefore(link);
        code.unlink();
    }
}