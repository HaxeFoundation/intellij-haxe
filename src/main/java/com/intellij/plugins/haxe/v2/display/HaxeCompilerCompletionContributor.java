package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import com.intellij.plugins.haxe.display.protocol.CompletionList;
import com.intellij.plugins.haxe.display.protocol.ImportStatus;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.ide.completion.HaxeLambdaLookups;
import com.intellij.plugins.haxe.ide.completion.HaxeLambdaShape;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * Completion in the compiler-only mode. It offers the compilation server's
 * items for the caret and stops the contributor chain, so the IDE's own
 * contributors add nothing, whether the server answered or not. The request
 * carries the editor's real document and caret offset, never the completion
 * copy with its dummy identifier. The prefix is the text the compiler's
 * replace range covers, or the platform's prefix when there is no range.
 * After a dotted path ({@code go.math.Ma}, also in a type hint, {@code new}
 * or an import) the range covers the whole path and the compiler completes
 * whole paths, so types, packages and modules are offered by their dot path.
 * Elsewhere a type follows its import status: an unimported type is inserted
 * with an import, a shadowed one by its qualified path.
 */
public class HaxeCompilerCompletionContributor extends CompletionContributor {

  /** How a type item goes into the text. */
  enum TypeInsertion { NAME, NAME_WITH_IMPORT, QUALIFIED_PATH }


  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    Project project = parameters.getPosition().getProject();
    if (HaxeCompilerSettings.getInstance(project).getEffectiveCompletionMode() != HaxeCompletionMode.COMPILER_ONLY) return;
    result.stopHere();
    VirtualFile file = parameters.getOriginalFile().getVirtualFile();
    if (file == null) return;
    // without a build context or a server there is no request at all, only
    // the one-time notification
    HaxeCompilerCompletionService service = HaxeCompilerCompletionService.getInstance(project);
    if (!service.ensureAvailable(file)) return;

    Document document = parameters.getEditor().getDocument();
    int offset = parameters.getOffset();
    CompletionList completion = service.complete(file, offset, parameters.isAutoPopup());
    if (completion == null) return;

    String replaced = replacedText(document.getCharsSequence(), completion.replaceRange(), offset);
    String prefix = replaced != null ? replaced : result.getPrefixMatcher().getPrefix();
    CompletionResultSet target = result.withPrefixMatcher(prefix);
    boolean dottedPrefix = prefix.indexOf('.') >= 0;
    boolean qualifiedPosition = dottedPrefix || completion.qualifiesInsertedTypes();
    for (CompletionItem item : completion.items()) {
      target.addElement(lookupElement(item, dottedPrefix, qualifiedPosition));
    }
    // the lambda lookups are the IDE's own, shaped by the compiler's expected type
    HaxeLambdaShape lambda = HaxeLambdaShape.fromCompiler(completion.expectedType());
    if (lambda != null) HaxeLambdaLookups.addTo(target, lambda, parameters);
    if (completion.incomplete()) {
      target.restartCompletionOnAnyPrefixChange();
    }
  }

  /**
   * The text from the compiler's replace range start to the caret; null when
   * there is no range, or when it does not start on the caret's line before
   * the caret. The Haxe 5 preview reports a module path's range at the
   * module's import instead of at the caret.
   */
  @Nullable
  static String replacedText(@NotNull CharSequence text, @Nullable Range replaceRange, int offset) {
    if (replaceRange == null) return null;
    Position start = replaceRange.start();
    if (start.line() < 0 || start.character() < 0) return null;
    int startOffset = StringUtil.lineColToOffset(text, start.line(), start.character());
    if (startOffset < 0 || startOffset > offset) return null;
    String replaced = text.subSequence(startOffset, offset).toString();
    return StringUtil.containsLineBreak(replaced) ? null : replaced;
  }

  /** What the item inserts and is matched by: its dot path after a dotted prefix, when it has one; its name otherwise. */
  @NotNull
  static String lookupString(@NotNull CompletionItem item, boolean dottedPrefix) {
    String dotPath = dottedPrefix ? item.dotPath() : null;
    return dotPath == null || dotPath.isEmpty() ? item.name() : dotPath;
  }

  /**
   * How a type item is inserted: by name where the position already
   * qualifies it, otherwise as its import status requires. Every other item
   * is inserted by name.
   */
  @NotNull
  static TypeInsertion typeInsertion(@NotNull CompletionItem item, boolean qualifiedPosition) {
    ImportStatus importStatus = item.importStatus();
    if (!item.isType() || qualifiedPosition || importStatus == null) return TypeInsertion.NAME;
    return switch (importStatus) {
      case IMPORTED -> TypeInsertion.NAME;
      case UNIMPORTED -> TypeInsertion.NAME_WITH_IMPORT;
      case SHADOWED -> TypeInsertion.QUALIFIED_PATH;
    };
  }

  /** The item becomes the lookup element's object, where the documentation provider reads its doc and type. */
  private static LookupElementBuilder lookupElement(CompletionItem item, boolean dottedPrefix, boolean qualifiedPosition) {
    String lookupString = lookupString(item, dottedPrefix);
    LookupElementBuilder element = LookupElementBuilder.create(item, lookupString).withBoldness(item.isKeywordOrLiteral());
    element = switch (typeInsertion(item, qualifiedPosition)) {
      case NAME -> element;
      case NAME_WITH_IMPORT -> element.withInsertHandler((context, lookup) -> addImport(context, item));
      case QUALIFIED_PATH -> element.withInsertHandler((context, lookup) -> insertQualifiedPath(context, item));
    };
    Icon icon = iconOf(item);
    if (icon != null) element = element.withIcon(icon);
    if (item.type() != null) element = element.withTypeText(item.type().presentable());
    if (item.detail() != null && !item.detail().isEmpty() && !item.detail().equals(lookupString)) {
      element = element.withTailText(" " + item.detail(), true);
    }
    return element;
  }

  /** Imports the inserted type by its qualified path, as the IDE's own completion does. */
  private static void addImport(@NotNull InsertionContext context, @NotNull CompletionItem item) {
    if (item.detail() == null || item.detail().isEmpty()) return;
    context.commitDocument();
    HaxeAddImportHelper.addImport(item.detail(), context.getFile());
  }

  /** Replaces the inserted name with the type's qualified path, the only name that reaches a shadowed type. */
  private static void insertQualifiedPath(@NotNull InsertionContext context, @NotNull CompletionItem item) {
    if (item.detail() == null || item.detail().isEmpty()) return;
    context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), item.detail());
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
