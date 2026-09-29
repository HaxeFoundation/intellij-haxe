package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.documentation.HaxeDocTags;
import com.intellij.plugins.haxe.ide.documentation.settings.HaxeDocSettings;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Haxedoc tag completion: typing {@code @} at a doc comment line's content
 * start offers the known tags. Fences never reach this - completion inside
 * injected fragments is blocked, and their files carry no doc comment parent.
 */
public class HaxeDocTagCompletionContributor extends CompletionContributor implements DumbAware {

  // a tag position: line indentation, an optional leading asterisk, then the partial tag
  private static final Pattern TAG_POSITION = Pattern.compile("\\s*(\\*\\s*)?(@\\w*)");

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    if (!HaxeDocSettings.getInstance().getState().completeDocTags) return;
    if (PsiTreeUtil.getParentOfType(parameters.getPosition(), HaxePsiDocCommentImpl.class, false) == null) return;

    Document document = parameters.getEditor().getDocument();
    int offset = parameters.getOffset();
    int lineStart = document.getLineStartOffset(document.getLineNumber(offset));
    String linePrefix = document.getText(TextRange.create(lineStart, offset));
    Matcher tagPosition = TAG_POSITION.matcher(linePrefix);
    if (!tagPosition.matches()) return;

    CompletionResultSet tagResult = result.withPrefixMatcher(tagPosition.group(2));
    for (String tag : HaxeDocTags.ALL) {
      tagResult.addElement(LookupElementBuilder.create(tag).bold());
    }
    tagResult.stopHere();
  }
}
