package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.search.GlobalSearchScope;
import org.commonmark.Extension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.parser.PostProcessor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeCodeReferenceToLinksExtension implements Parser.ParserExtension {

    private final Project project;

    private HaxeCodeReferenceToLinksExtension(Project project) {
        this.project = project;
    }

    public static Extension create(Project project) {
        return new HaxeCodeReferenceToLinksExtension(project);
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.postProcessor(new HaxeDocumentationCodeReferenceProcessor(project));
    }

}

class HaxeDocumentationCodeReferenceProcessor implements PostProcessor {

    private final Project project;

    public HaxeDocumentationCodeReferenceProcessor(Project project) {
        this.project = project;
    }

    @Override
    public Node process(Node node) {
        HaxeDocumentationCodeVisitor tagsVisitor = new HaxeDocumentationCodeVisitor(project);
        node.accept(tagsVisitor);
        return node;
    }
}

class HaxeDocumentationCodeVisitor extends AbstractVisitor {

    public static Pattern qNamePattern = Pattern.compile("(\\w+)(\\.(\\w+))*");
    private final Project project;
    private final PsiManager service;

    public HaxeDocumentationCodeVisitor(Project project) {
        this.service = project.getService(PsiManager.class);
        this.project = project;
    }

    @Override
    public void visit(Code code) {
        String literal = code.getLiteral();
        Matcher matcher = qNamePattern.matcher(literal);

        if (matcher.find()) {
            if (replaceFullyQualifiedClass(code, literal)) return;
            if (replaceIndexedClassName(code, literal)) return;
            replaceClassMemberReference(code, literal);
        }
    }

    private boolean replaceClassMemberReference(Code code, String literal) {
        // check if member of type
        int lastDotIndex = literal.lastIndexOf(".");
        if (lastDotIndex > -1) {

            String classRef = literal.substring(0, lastDotIndex);
            String member = literal.substring(lastDotIndex + 1).replaceAll("(\\w+).*", "$1");

            HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(classRef, service, GlobalSearchScope.allScope(project));
            if (haxeClass == null) haxeClass = findUniqueClassFromIndex(classRef);
            if(haxeClass != null) {
                HaxeNamedComponent haxeMemberByName = haxeClass.findHaxeMemberByName(member, null);
                if (haxeMemberByName  instanceof PsiMember psiMember) {
                    PsiClass containingClass = psiMember.getContainingClass();
                    replaceCodeWithLink(code, containingClass.getQualifiedName() + "."+ member, literal);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean replaceIndexedClassName(Code code, String literal) {
        HaxeClass haxeClass = findUniqueClassFromIndex(literal);
        if (haxeClass != null) {
            replaceCodeWithLink(code, haxeClass.getQualifiedName(), literal);
            return true;
        }
        return false;
    }

    private HaxeClass findUniqueClassFromIndex(String literal) {
        List<HaxeComponent> itemsByName = HaxeComponentIndex.getItemsByName(literal, project, GlobalSearchScope.allScope(project));
        if (itemsByName.size() == 1) {
            HaxeComponent first = itemsByName.getFirst();
            if (first instanceof HaxeClass haxeClass) {
                return haxeClass;
            }
        }
        return null;
    }

    private boolean replaceFullyQualifiedClass(Code code, String literal) {
        HaxeClass classByQName = HaxeResolveUtil.findClassByQName(literal, service, GlobalSearchScope.allScope(project));
        if (classByQName != null) {
            replaceCodeWithLink(code, literal, literal);
            return true;
        }
        return false;
    }

    private static void replaceCodeWithLink(Code code, String qname, String linkText) {
        Link link = new Link();
        link.setDestination(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL + qname);
        link.appendChild(new Text(linkText));
        code.insertBefore(link);
        code.unlink();
    }
}