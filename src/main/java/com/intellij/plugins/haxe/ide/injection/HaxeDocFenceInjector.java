package com.intellij.plugins.haxe.ide.injection;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.ide.documentation.settings.HaxeDocSettings;
import com.intellij.plugins.haxe.lang.parser.HaxeDocMarkdown;
import com.intellij.plugins.haxe.lang.parser.HaxeDocMarkdown.DocLine;
import com.intellij.plugins.haxe.lang.parser.HaxeDocMarkdown.Fence;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * KDoc-style code blocks: a doc comment's markdown fences are injected as
 * real language fragments, so they get the language's own highlighting.
 * Semantic errors inside the fragments are suppressed separately - sample
 * snippets are not expected to resolve (see AnnotatorUtil.shouldSkip and
 * {@link HaxeExemptCodeErrorFilter}).
 */
public class HaxeDocFenceInjector implements MultiHostInjector {

  private static final Set<String> HAXE_FENCE_TAGS = Set.of("", "haxe", "hx");

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    if (!(context instanceof HaxePsiDocCommentImpl docComment)) return;
    if (!HaxeDocSettings.getInstance().getState().injectCodeFences) return;
    // fences in a dead branch would render full-color injected code inside
    // otherwise dimmed content - the whole doc stays one dimmed comment
    if (AnnotatorUtil.isInInactiveBranch(docComment)) return;

    int hostStart = docComment.getTextRange().getStartOffset();
    List<Fence> fences = HaxeDocMarkdown.scan(docComment).fences();
    for (Fence fence : fences) {
      Language language = fenceLanguage(fence.tag());
      List<DocLine> lines = fence.lines();
      if (language == null || lines.isEmpty()) continue;

      registrar.startInjecting(language);
      for (int i = 0; i < lines.size(); i++) {
        DocLine line = lines.get(i);
        // gaps of blank doc lines carry no tokens - restore them as prefix newlines
        String prefix = i == 0 ? null : "\n".repeat(line.newlinesBefore());
        TextRange rangeInHost = TextRange.from(line.startOffset() - hostStart, line.text().length());
        registrar.addPlace(prefix, null, docComment, rangeInHost);
      }
      registrar.doneInjecting();
    }
  }

  @Nullable
  private static Language fenceLanguage(@NotNull String tag) {
    if (HAXE_FENCE_TAGS.contains(tag)) {
      return HaxeLanguage.INSTANCE;
    }
    return Language.getRegisteredLanguages().stream()
      .filter(language -> language.getID().equalsIgnoreCase(tag))
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return List.of(HaxePsiDocCommentImpl.class);
  }
}
