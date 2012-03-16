package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.ide.index.HaxeClassInfo;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassNameCompletionContributor extends CompletionContributor {
  public HaxeClassNameCompletionContributor() {
    extend(CompletionType.BASIC, psiElement().inside(HaxeType.class), new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        addVariantsFromIndex(result, parameters.getOriginalFile(), CLASS_INSERT_HANDLER);
      }
    });
  }

  private static void addVariantsFromIndex(final CompletionResultSet resultSet,
                                           final PsiFile targetFile,
                                           final InsertHandler<LookupElement> insertHandler) {
    final Project project = targetFile.getProject();
    HaxeComponentIndex.processAll(project, new MyProcessor(resultSet, insertHandler));
  }

  private static final InsertHandler<LookupElement> CLASS_INSERT_HANDLER = new InsertHandler<LookupElement>() {
    public void handleInsert(final InsertionContext context, final LookupElement item) {
      addImportForLookupElement(context, item, context.getTailOffset() - 1);
    }
  };

  private static void addImportForLookupElement(final InsertionContext context, final LookupElement item, final int tailOffset) {
    final PsiReference ref = context.getFile().findReferenceAt(tailOffset);
    if (ref == null || ref.resolve() != null) {
      // no import statement needed
      return;
    }
    new WriteCommandAction(context.getProject(), context.getFile()) {
      @Override
      protected void run(Result result) throws Throwable {
        final HaxeClassInfo info = (HaxeClassInfo)item.getObject();
        if (info.getPackageName().isEmpty()) {
          return;
        }
        final String importPath = info.getPackageName() + "." + item.getLookupString();
        HaxeAddImportHelper.addImport(importPath, context.getFile());
      }
    }.execute();
  }

  private static class MyProcessor implements Processor<Pair<String, HaxeClassInfo>> {
    private final CompletionResultSet myResultSet;
    private final InsertHandler<LookupElement> myInsertHandler;

    private MyProcessor(CompletionResultSet resultSet, InsertHandler<LookupElement> insertHandler) {
      myResultSet = resultSet;
      myInsertHandler = insertHandler;
    }

    @Override
    public boolean process(Pair<String, HaxeClassInfo> pair) {
      add(pair.getFirst(), pair.getSecond());
      return true;
    }

    private void add(String name, HaxeClassInfo info) {
      myResultSet.addElement(LookupElementBuilder.create(info, name)
                               .setIcon(info.getIcon())
                               .setTailText(" " + info.getPackageName(), true)
                               .setInsertHandler(myInsertHandler));
    }
  }
}
