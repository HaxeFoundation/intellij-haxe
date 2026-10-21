package com.intellij.plugins.haxe.ide.references;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.fqn.HaxeFullyQualifiedClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Qualified-name completion inside string literals: segment-wise, like the
 * file-path completion beside it. Packages and types come from the FQN
 * index's key set (all three legs, generated types included); members appear
 * once the prefix resolves to a class. Explicit invocation works from the
 * first word; AUTO-popup is governed by the shared confidence — it needs two
 * dots and a known prefix (see {@code HaxeStringLinkCompletionConfidence}).
 */
public class HaxeStringQnameCompletionContributor extends CompletionContributor {

  public HaxeStringQnameCompletionContributor() {
    extend(CompletionType.BASIC,
           PlatformPatterns.psiElement().inside(HaxeStringLiteralExpression.class),
           new QnameProvider());
  }

  private static final class QnameProvider extends CompletionProvider<CompletionParameters> {

    // a qualified name in progress: word segments separated by dots, the
    // last one possibly empty (caret right after a dot)
    private static final Pattern QNAME_IN_PROGRESS = Pattern.compile("[A-Za-z_]\\w*(?:\\.\\w*)*");

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
      PsiFile file = parameters.getOriginalFile();
      int offset = parameters.getOffset();
      HaxeStringLiteralExpression literal = HaxeStringLiterals.literalAtCaret(file, offset);
      if (literal == null) return;

      String beforeCaret = HaxeStringLiterals.contentBeforeCaret(literal, file, offset);
      if (beforeCaret == null || beforeCaret.isEmpty() || !QNAME_IN_PROGRESS.matcher(beforeCaret).matches()) return;

      int lastDot = beforeCaret.lastIndexOf('.');
      String parent = lastDot < 0 ? "" : beforeCaret.substring(0, lastDot);
      String partial = beforeCaret.substring(lastDot + 1);
      CompletionResultSet matched = result.withPrefixMatcher(partial);

      Project project = file.getProject();
      Collection<String> fqns = HaxeFullyQualifiedClassNameUnifiedIndex.getAllKeys(project);
      Set<String> seen = new HashSet<>();

      if (parent.isEmpty()) {
        for (String fqn : fqns) {
          int dot = fqn.indexOf('.');
          String first = dot < 0 ? fqn : fqn.substring(0, dot);
          if (seen.add(first)) {
            matched.addElement(segmentElement(first, dot < 0));
          }
        }
        return;
      }

      String packagePrefix = parent + ".";
      for (String fqn : fqns) {
        if (!fqn.startsWith(packagePrefix)) continue;
        String rest = fqn.substring(packagePrefix.length());
        int dot = rest.indexOf('.');
        String next = dot < 0 ? rest : rest.substring(0, dot);
        if (!next.isEmpty() && seen.add(next)) {
          matched.addElement(segmentElement(next, dot < 0));
        }
      }

      if (HaxeQnameResolveUtil.findClassOrMember(parent, project) instanceof HaxeClass haxeClass) {
        for (HaxeBaseMemberModel member : haxeClass.getModel().getMembers(null)) {
          String name = member.getName();
          if (seen.add(name)) {
            boolean method = member instanceof HaxeMethodModel;
            matched.addElement(LookupElementBuilder.create(name).withIcon(method ? AllIcons.Nodes.Method : AllIcons.Nodes.Field));
          }
        }
      }
    }

    /** A package or class segment of a qualified name: the last segment of an FQN is its class. */
    @NotNull
    private static LookupElementBuilder segmentElement(@NotNull String segment, boolean classSegment) {
      return LookupElementBuilder.create(segment).withIcon(classSegment ? AllIcons.Nodes.Class : AllIcons.Nodes.Package);
    }
  }
}
