package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.parser.GeneratedParserUtilBase;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeCodeGenerateUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeKeywordCompletionContributor extends CompletionContributor {
  private static final Set<String> allowedKeywords = new THashSet<String>() {
    {
      for (IElementType elementType : HaxeTokenTypeSets.KEYWORDS.getTypes()) {
        add(elementType.toString());
      }
      add(HaxeTokenTypes.ONEW.toString());
    }
  };

  public HaxeKeywordCompletionContributor() {
    final PsiElementPattern.Capture<PsiElement> inExpression = psiElement().withParent(HaxeReference.class);
    extend(CompletionType.BASIC, psiElement().inFile(StandardPatterns.instanceOf(HaxeFile.class)).andNot(inExpression),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               final Collection<String> suggestedKeywords = suggestKeywords(parameters.getPosition());
               suggestedKeywords.retainAll(allowedKeywords);
               for (String keyword : suggestedKeywords) {
                 result.addElement(LookupElementBuilder.create(keyword));
               }
             }
           });
  }

  private static Collection<String> suggestKeywords(PsiElement position) {
    final TextRange posRange = position.getTextRange();
    final HaxeFile posFile = (HaxeFile)position.getContainingFile();

    final List<PsiElement> pathToBlockStatement = UsefulPsiTreeUtil.getPathToParentOfType(position, HaxeBlockStatement.class);

    final HaxePsiCompositeElement classInterfaceEnum =
      PsiTreeUtil.getParentOfType(position, HaxeClassBody.class, HaxeInterfaceBody.class, HaxeEnumBody.class);

    final String text;
    final int offset;
    if (pathToBlockStatement != null) {
      final Pair<String, Integer> pair = HaxeCodeGenerateUtil.wrapStatement(posRange.substring(posFile.getText()));
      text = pair.getFirst();
      offset = pair.getSecond();
    }
    else if (classInterfaceEnum != null) {
      final Pair<String, Integer> pair = HaxeCodeGenerateUtil.wrapFunction(posRange.substring(posFile.getText()));
      text = pair.getFirst();
      offset = pair.getSecond();
    }
    else {
      text = posFile.getText().substring(0, posRange.getStartOffset() + 1);
      offset = 0;
    }

    final List<String> result = new ArrayList<String>();
    if (pathToBlockStatement != null && pathToBlockStatement.size() > 1) {
      final PsiElement blockChild = pathToBlockStatement.get(pathToBlockStatement.size() - 2);
      result.addAll(suggestBySibling(UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(blockChild)));
    }

    PsiFile file = PsiFileFactory.getInstance(posFile.getProject()).createFileFromText("a.hx", HaxeLanguage.INSTANCE, text, true, false);
    GeneratedParserUtilBase.CompletionState state = new GeneratedParserUtilBase.CompletionState(text.length() - offset);
    file.putUserData(GeneratedParserUtilBase.COMPLETION_STATE_KEY, state);
    TreeUtil.ensureParsed(file.getNode());
    result.addAll(state.items);
    return result;
  }

  @NotNull
  private static Collection<? extends String> suggestBySibling(@Nullable PsiElement sibling) {
    if (HaxeIfStatement.class.isInstance(sibling)) {
      return Arrays.asList(HaxeTokenTypes.KELSE.toString());
    }
    else if (HaxeTryStatement.class.isInstance(sibling) || HaxeCatchStatement.class.isInstance(sibling)) {
      return Arrays.asList(HaxeTokenTypes.KCATCH.toString());
    }

    return Collections.emptyList();
  }
}
