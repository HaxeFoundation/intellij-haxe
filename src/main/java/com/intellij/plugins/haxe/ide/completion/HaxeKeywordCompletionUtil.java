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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.ide.completion.KeywordCompletionData.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.IN_KEYWORD;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.IS_KEYWORD;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeKeywordCompletionUtil {



  public static final Set<KeywordCompletionData> PACKAGE_KEYWORD = Set.of(keywordWithSpace(KPACKAGE));
  public static final Set<KeywordCompletionData> TOP_LEVEL_KEYWORDS = Set.of(keywordWithSpace(KIMPORT),
                                                                             keywordWithSpace((KUSING)));
  public static final Set<KeywordCompletionData> VISIBILITY_KEYWORDS = Set.of(keywordWithSpace(KPRIVATE),
                                                                              keywordWithSpace(KPUBLIC));
  public static final Set<KeywordCompletionData> ACCESSIBILITY_KEYWORDS = Set.of(keywordWithSpace(KFINAL),
                                                                                 keywordWithSpace(KABSTRACT),
                                                                                 keywordWithSpace(KINLINE),
                                                                                 keywordWithSpace(KEXTERN),
                                                                                 keywordWithSpace(KDYNAMIC));
  public static final Set<KeywordCompletionData> MODULE_STRUCTURES_KEYWORDS = Set.of(keywordWithSpace(KCLASS),
                                                                                     keywordWithSpace(KABSTRACT),
                                                                                     keywordWithSpace(KINTERFACE),
                                                                                     keywordWithSpace(KENUM),
                                                                                     keywordWithSpace(KEXTERN),
                                                                                     keywordWithSpace(KTYPEDEF),
                                                                                     keywordWithSpace(KVAR));

  public static final Set<KeywordCompletionData> CLASS_DEFINITION_KEYWORDS = Set.of(keywordWithSpace(KEXTENDS),
                                                                                    keywordWithSpace(KIMPLEMENTS));
  public static final Set<KeywordCompletionData> INTERFACE_DEFINITION_KEYWORDS = Set.of(keywordWithSpace(KEXTENDS));
  public static final Set<KeywordCompletionData> INTERFACE_BODY_KEYWORDS = Set.of(keywordWithSpace(KFUNCTION),
                                                                                  keywordWithSpace(KVAR));
  public static final Set<KeywordCompletionData> ABSTRACT_DEFINITION_KEYWORDS = Set.of(keywordWithSpace(KTO),
                                                                                       keywordWithSpace(KFROM));
  public static final Set<KeywordCompletionData> CLASS_BODY_KEYWORDS = Set.of(keywordWithSpace(KVAR),
                                                                              keywordWithSpace(KFUNCTION),
                                                                              keywordWithSpace(KOVERLOAD),
                                                                              keywordWithSpace(KOVERRIDE));
  public static final Set<KeywordCompletionData> METHOD_BODY_KEYWORDS =
    Set.of(keywordWithSpace(IS_KEYWORD),
           keywordWithSpace(ONEW),
           keywordParentheses(KIF),
           keywordWithSpace(KVAR),
           keywordWithSpace(KFUNCTION),
           keywordWithSpace(KINLINE),
           keywordWithSpace(KFINAL),
           keywordWithSpace(KSWITCH),
           keywordWithSpace(KTHROW),
           keywordCurlyBrackets(KTRY),
           keywordParentheses(KCATCH),
           keywordOnly(KTHIS),
           keywordOnly(KSUPER),
           keywordParentheses(KFOR),
           keywordParentheses(KWHILE),
           keywordCurlyBrackets(KDO),
           keywordWithSpace(KRETURN),
           keywordWithSpace(KSTATIC),
           keywordWithSpace(KCAST));
  public static final Set<KeywordCompletionData> VALUE_KEYWORDS =
    Set.of(keywordOnly(KNULL),
           keywordOnly(KTRUE),
           keywordOnly(KFALSE)
           );

  public static final Set<KeywordCompletionData> SWITCH_BODY_KEYWORDS = Set.of(keywordWithSpace(KCASE),
                                                                               keywordWithSpace(KDEFAULT));
  public static final Set<KeywordCompletionData> LOOP_BODY_KEYWORDS = Set.of(keywordTemplate(KBREAK, KBREAK + ";" + CARET),
                                                                             keywordTemplate(KCONTINUE, KCONTINUE + ";" + CARET));
  public static final Set<KeywordCompletionData> LOOP_ITERATOR_KEYWORDS = Set.of(keywordWithSpace(IN_KEYWORD));

  public static final Set<KeywordCompletionData> PROPERTY_KEYWORDS = Set.of(keywordOnly(KDEFAULT),
                                                                            keywordOnly(KNULL),
                                                                            keywordOnly(KNEVER),
                                                                            keywordOnly(KDYNAMIC));

  public static final Set<KeywordCompletionData> MISC_KEYWORDS = Set.of(keywordWithSpace(KMACRO2),
                                                                      keywordWithSpace(KUNTYPED));

  public static final Set<KeywordCompletionData> PP_KEYWORDS = Set.of(keywordWithSpace(PPIF, false, true),
                                                                      keywordWithSpace(PPELSE,false, true),
                                                                      keywordWithSpace(PPELSEIF,false, true),
                                                                      keywordWithSpace(PPEND,false, true),
                                                                      keywordWithSpace(PPERROR,false, true));


  public static void addKeywords(List<LookupElement> result, Set<KeywordCompletionData> keywords) {
    for (KeywordCompletionData keyword : keywords) {
      result.add(keyword(keyword));
    }
  }

  public static void addKeywords(List<LookupElement> result, Set<KeywordCompletionData> keywords, float priority) {
    for (KeywordCompletionData keyword : keywords) {
      result.add(keyword(keyword, priority));
    }
  }

  public static @NotNull LookupElement keyword(KeywordCompletionData dataElement, float priority) {
    return PrioritizedLookupElement.withPriority(keyword(dataElement), priority);
  }


  public static @NotNull LookupElement keyword(KeywordCompletionData data) {

    String elementName = data.getKeyword().toString();
    String templateValue = data.getTemplate();

    String template = templateValue != null ? templateValue : elementName;


    LookupElementBuilder builder = LookupElementBuilder.create(data.getKeyword(), template)
      .withBoldness(data.isBold())
      .withItemTextItalic(data.isItalic())
      .withPresentableText(elementName);

    builder = builder.withInsertHandler((context, item) -> {
      Editor editor = context.getEditor();
      Project project = editor.getProject();

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
