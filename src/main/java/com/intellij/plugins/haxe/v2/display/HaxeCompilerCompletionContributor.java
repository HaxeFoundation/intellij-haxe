package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import com.intellij.plugins.haxe.display.protocol.CompletionList;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.ide.completion.HaxeLambdaLookups;
import com.intellij.plugins.haxe.ide.completion.HaxeLambdaShape;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Completion in the compiler-only mode. It offers the compilation server's
 * items for the caret and stops the contributor chain, so the IDE's own
 * contributors add nothing, whether the server answered or not. The request
 * carries the editor's real document and caret offset, never the completion
 * copy with its dummy identifier. The prefix is the text the compiler's
 * replace range covers, or the platform's prefix when there is no range.
 */
public class HaxeCompilerCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    Project project = parameters.getPosition().getProject();
    if (HaxeCompilerSettings.getInstance(project).getCompletionMode() != HaxeCompletionMode.COMPILER_ONLY) return;
    result.stopHere();
    VirtualFile file = parameters.getOriginalFile().getVirtualFile();
    if (file == null) return;
    // without a build context or a server there is no request at all, only
    // the one-time notification
    HaxeCompilerCompletionService service = HaxeCompilerCompletionService.getInstance(project);
    if (!service.ensureAvailable(file)) return;

    Document document = parameters.getEditor().getDocument();
    boolean unsaved = FileDocumentManager.getInstance().isDocumentUnsaved(document);
    int offset = parameters.getOffset();
    CompletionList completion = service.complete(file, document.getText(), unsaved, offset, parameters.isAutoPopup());
    if (completion == null) return;

    CompletionResultSet target = result.withPrefixMatcher(prefixOf(completion, document, offset, result));
    for (CompletionItem item : completion.items()) {
      target.addElement(lookupElement(item));
    }
    // the lambda lookups are the IDE's own, shaped by the compiler's expected type
    HaxeLambdaShape lambda = HaxeLambdaShape.fromCompiler(completion.expectedType());
    if (lambda != null) HaxeLambdaLookups.addTo(target, lambda, parameters);
    if (completion.incomplete()) {
      target.restartCompletionOnAnyPrefixChange();
    }
  }

  /** The text from the compiler's replace range start to the caret; the platform's prefix when it reports none. */
  private static String prefixOf(CompletionList completion, Document document, int offset, CompletionResultSet result) {
    if (completion.replaceRange() == null) return result.getPrefixMatcher().getPrefix();
    Position start = completion.replaceRange().start();
    if (start.line() < 0 || start.line() >= document.getLineCount()) return result.getPrefixMatcher().getPrefix();
    int startOffset = document.getLineStartOffset(start.line()) + start.character();
    if (startOffset < 0 || startOffset > offset) return result.getPrefixMatcher().getPrefix();
    return document.getText().substring(startOffset, offset);
  }

  /** The item becomes the lookup element's object, where the documentation provider reads its doc and type. */
  private static LookupElementBuilder lookupElement(CompletionItem item) {
    LookupElementBuilder element = LookupElementBuilder.create(item, item.name()).withBoldness(item.isKeywordOrLiteral());
    Icon icon = iconOf(item);
    if (icon != null) element = element.withIcon(icon);
    if (item.type() != null) element = element.withTypeText(item.type().presentable());
    if (item.detail() != null && !item.detail().isEmpty() && !item.detail().equals(item.name())) {
      element = element.withTailText(" " + item.detail(), true);
    }
    return element;
  }

  private static Icon iconOf(CompletionItem item) {
    if (item.isType()) return typeIcon(item.moduleTypeKind());
    if (item.isField()) return item.type() != null && item.type().isFunction() ? HaxeIcons.Method : HaxeIcons.Field;
    if (item.isEnumField()) return HaxeIcons.Enum;
    if (item.isLocalOrTypeParameter()) return HaxeIcons.Variable;
    if (item.isPackageOrModule()) return HaxeIcons.Module;
    return null;
  }

  private static Icon typeIcon(String moduleTypeKind) {
    return switch (moduleTypeKind == null ? "" : moduleTypeKind) {
      case "interface" -> HaxeIcons.Interface;
      case "enum" -> HaxeIcons.Enum;
      case "typedef" -> HaxeIcons.Typedef;
      case "abstract" -> HaxeIcons.Abstract;
      default -> HaxeIcons.Class;
    };
  }
}
