package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.IN_KEYWORD;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.IS_KEYWORD;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeKeywordCompletionUtil {

  private static final String CARET = "<CARET>";

  public static final Set<IElementType> PACKAGE_KEYWORD = Set.of(KPACKAGE);
  public static final Set<IElementType> TOP_LEVEL_KEYWORDS = Set.of(KIMPORT, KUSING);
  public static final Set<IElementType> VISIBILITY_KEYWORDS = Set.of(KPRIVATE, KPUBLIC);
  public static final Set<IElementType> ACCESSIBILITY_KEYWORDS = Set.of(KFINAL, KABSTRACT, KINLINE, KEXTERN, KDYNAMIC);
  public static final Set<IElementType> MODULE_STRUCTURES_KEYWORDS = Set.of(KCLASS, KABSTRACT, KINTERFACE, KENUM, KEXTERN, KTYPEDEF, KVAR);

  public static final Set<IElementType> CLASS_DEFINITION_KEYWORDS = Set.of(KEXTENDS, KIMPLEMENTS);
  public static final Set<IElementType> INTERFACE_DEFINITION_KEYWORDS = Set.of(KEXTENDS);
  public static final Set<IElementType> INTERFACE_BODY_KEYWORDS = Set.of(KFUNCTION, KVAR);
  public static final Set<IElementType> ABSTRACT_DEFINITION_KEYWORDS = Set.of(KTO, KFROM);
  public static final Set<IElementType> CLASS_BODY_KEYWORDS = Set.of(KVAR, KFUNCTION, KOVERLOAD, KOVERRIDE);
  public static final Set<IElementType> METHOD_BODY_KEYWORDS =
    Set.of(IS_KEYWORD, ONEW, KIF, KVAR, KFUNCTION, KINLINE, KFINAL, KSWITCH, KTHROW, KTRY, KCATCH, KTHIS, KSUPER, KFOR, KWHILE, KDO, KRETURN, KSTATIC, KCAST);

  public static final Set<IElementType> SWITCH_BODY_KEYWORDS = Set.of(KCASE, KDEFAULT);
  public static final Set<IElementType> LOOP_BODY_KEYWORDS = Set.of(KBREAK, KCONTINUE);
  public static final Set<IElementType> LOOP_ITERATOR_KEYWORDS = Set.of(IN_KEYWORD);

  public static final Set<IElementType> PROPERTY_KEYWORDS = Set.of(KDEFAULT, KNULL, KNEVER, KDYNAMIC);

  public static final Set<IElementType> PP_KEYWORDS = Set.of(PPIF, PPELSE, PPELSEIF, PPEND, PPERROR);

  public static final List<String> insertParentheses = List.of(KIF.toString(), KCATCH.toString(), KFOR.toString(), KWHILE.toString());
  public static final List<String> insertbrackets = List.of(KTRY.toString(), KDO.toString());

  public static void addKeywords(List<LookupElement> result, Set<IElementType> keywords) {
    for (IElementType keyword : keywords) {
      result.add(keyword(keyword,true, false));
    }
  }
  public static void addKeywords(List<LookupElement> result, Set<IElementType> keywords, float priority) {
    for (IElementType keyword : keywords) {
      result.add(keyword(keyword, priority,true, false));
    }
  }
  public static void addKeywords(List<LookupElement> result, Set<IElementType> keywords, float priority,  boolean bold, boolean italic) {
    for (IElementType keyword : keywords) {
      result.add(keyword(keyword, priority,bold, italic));
    }
  }

  public static @NotNull LookupElement keyword(IElementType keywordElement, float priority, boolean bold, boolean italic) {
    return PrioritizedLookupElement.withPriority(keyword(keywordElement, bold, italic), priority);
  }


  public static @NotNull LookupElement keyword(IElementType keywordElement,  boolean bold, boolean italic) {

    String elementString = keywordElement.toString();
    boolean addPar = insertParentheses.contains(elementString);
    boolean addBracket = insertbrackets.contains(elementString);

    StringBuilder stringBuilder = new StringBuilder().append(keywordElement).append(" ");
    if (addPar) stringBuilder.append("(" + CARET + ")");
    if (addBracket) stringBuilder.append("{\n" + CARET + "\n}");

    LookupElementBuilder builder = LookupElementBuilder.create(keywordElement, stringBuilder.toString())
      .withBoldness(bold)
      .withItemTextItalic(italic)
      .withPresentableText(elementString);

    builder = builder.withInsertHandler((context, item) -> {
      Editor editor = context.getEditor();
      Project project = editor.getProject();

      String template = item.getLookupString();
      int caretOffset = template.lastIndexOf(CARET);
      String content = template.replaceAll(CARET, "");

      Document document = editor.getDocument();
      int startOffset = context.getStartOffset();
      document.replaceString(startOffset, context.getSelectionEndOffset(), content);

      if (caretOffset != -1) {
        TextRange range = new TextRange(startOffset, startOffset + content.length());
        int caret = startOffset + caretOffset;
        editor.getCaretModel().moveToOffset(caret);

        flushChanges(project, document);
        reformatAndAdjustIndent(context, range);
      }
    });


    return builder;
  }

  private static void flushChanges(Project project, Document document) {
    PsiDocumentManager instance = PsiDocumentManager.getInstance(project);
    instance.doPostponedOperationsAndUnblockDocument(document);
    instance.commitDocument(document);
  }

  private static void reformatAndAdjustIndent(InsertionContext context, TextRange range) {
    Editor editor = context.getEditor();
    Project project = editor.getProject();
    PsiFile file = context.getFile();

    CodeStyleManager styleManager = CodeStyleManager.getInstance(project);
    styleManager.reformatRange(file, range.getStartOffset(), range.getEndOffset());
    styleManager.adjustLineIndent(file, editor.getCaretModel().getOffset());
  }


}
