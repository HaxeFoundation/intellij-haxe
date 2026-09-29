package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Renders a `-D dump=pretty` module dump as a read-only Haxe preview. The
/// post-macro typed AST is close enough to Haxe that the normal parser,
/// highlighting and folding apply once the declaration headers are rewritten
/// ([#sanitize]). Preview files carry [#PREVIEW_KEY]. The semantic
/// annotators and the per-file highlighting level keep error analysis off
/// them, while the color annotators still paint.
///
/// The work is split by thread: [#prepare] does the file IO, parsing and
/// offset lookup on a BACKGROUND thread, and [#openPrepared] only opens the
/// editor, on the EDT.
@CustomLog
public final class HaxeGeneratedCodePreview {

  /** Marks preview virtual files; the value is the previewed module's dot path. */
  public static final Key<String> PREVIEW_KEY = Key.create("haxe.generated.preview.dotpath");

  /** A ready-to-open preview: the rendered file and the caret offset of the requested member. */
  public record PreparedPreview(@NotNull LightVirtualFile file, int offset) {
  }

  // a dumped declaration header carries the module-qualified name
  // ("class haxe.iterators.ArrayIterator<T>"). Group 1 is the declaration
  // keyword, group 2 the package prefix (lowercase first segment,
  // dot-separated), group 3 the type name. Dots are illegal in declared
  // names, so the prefix moves into a package statement.
  private static final Pattern QUALIFIED_DECLARATION =
    Pattern.compile("\\b(class|interface|enum|abstract|typedef)\\s+([a-z][\\w]*(?:\\.[\\w]+)*)\\.([A-Z]\\w*)");

  private record CachedPreview(long fileStamp, @NotNull LightVirtualFile file) {
  }

  private static final Map<Path, CachedPreview> previews = new ConcurrentHashMap<>();

  private HaxeGeneratedCodePreview() {
  }

  /** Drops every cached preview file. Called from {@link HaxeCompilerCaches#clearAndRehighlight}. */
  public static void clearCaches() {
    previews.clear();
  }

  /**
   * Renders the module dump and locates the named member of the named type
   * (falling back to the type declaration, then the top of the file).
   * Involves file IO and a PSI parse, so background threads only.
   */
  @Nullable
  public static PreparedPreview prepare(@NotNull Project project,
                                        @NotNull Path dumpFile,
                                        @NotNull String typeDotPath,
                                        @Nullable String memberName) {
    LightVirtualFile file = previewFile(dumpFile, typeDotPath);
    if (file == null) return null;
    String typeName = StringUtil.getShortName(typeDotPath);
    int offset = ReadAction.nonBlocking(() -> memberOffset(project, file, typeName, memberName)).executeSynchronously();
    return new PreparedPreview(file, Math.max(offset, 0));
  }

  /** Opens a prepared preview in the editor. EDT only. */
  public static void openPrepared(@NotNull Project project, @NotNull PreparedPreview prepared) {
    new OpenFileDescriptor(project, prepared.file(), prepared.offset()).navigate(true);
  }

  @Nullable
  private static LightVirtualFile previewFile(@NotNull Path dumpFile, @NotNull String typeDotPath) {
    long stamp = dumpFile.toFile().lastModified();
    CachedPreview cached = previews.get(dumpFile);
    if (cached != null && cached.fileStamp() == stamp) return cached.file();

    String dumpText;
    try {
      dumpText = Files.readString(dumpFile);
    } catch (IOException e) {
      log.info("cannot read dump file " + dumpFile + ": " + e.getMessage());
      return null;
    }

    String moduleName = dumpFile.getFileName().toString().replace(".dump", "");
    LightVirtualFile file = new LightVirtualFile(moduleName + ".generated.hx", HaxeLanguage.INSTANCE, sanitize(dumpText));
    file.setWritable(false);
    file.putUserData(PREVIEW_KEY, typeDotPath);
    previews.put(dumpFile, new CachedPreview(stamp, file));
    return file;
  }

  /**
   * Rewrites qualified declaration headers into a package statement plus
   * simple names, and prepends a banner saying what the reader is looking at.
   * Backtick-marked unbound identifiers need no rewriting: the grammar parses
   * them.
   */
  @NotNull
  static String sanitize(@NotNull String dumpText) {
    String packageName = null;
    Matcher matcher = QUALIFIED_DECLARATION.matcher(dumpText);
    StringBuilder rewritten = new StringBuilder(dumpText.length());
    while (matcher.find()) {
      if (packageName == null) packageName = matcher.group(2);
      matcher.appendReplacement(rewritten, matcher.group(1) + " " + matcher.group(3));
    }
    matcher.appendTail(rewritten);

    String banner = """
      // Compiler-generated preview (post-macro typed AST, -D dump=pretty).
      // Read-only; regenerated on the next code change.
      """;
    String packageStatement = packageName != null ? "package " + packageName + ";\n" : "";
    return banner + packageStatement + "\n" + rewritten;
  }

  /**
   * The offset of the member's NAME, else of the type, else 0. Walks plain
   * PSI on purpose: the model's member lookup would pull type resolution over
   * the whole dump, which can be library-sized.
   */
  private static int memberOffset(@NotNull Project project,
                                  @NotNull LightVirtualFile file,
                                  @NotNull String typeName,
                                  @Nullable String memberName) {
    if (!(PsiManager.getInstance(project).findFile(file) instanceof HaxeFile haxeFile)) return 0;
    for (HaxeClass haxeClass : haxeFile.getClassList()) {
      if (!typeName.equals(haxeClass.getName())) continue;
      if (memberName == null) return haxeClass.getTextOffset();
      for (HaxeComponentName componentName : PsiTreeUtil.findChildrenOfType(haxeClass, HaxeComponentName.class)) {
        if (memberName.equals(componentName.getText()) && isMemberDeclaration(componentName)) {
          return componentName.getTextOffset();
        }
      }
      return haxeClass.getTextOffset();
    }
    return 0;
  }

  /** A field or method name, as opposed to a local variable or parameter sharing the name. */
  private static boolean isMemberDeclaration(@NotNull HaxeComponentName componentName) {
    PsiElement declaration = componentName.getParent();
    return declaration instanceof HaxeFieldDeclaration || declaration instanceof HaxeMethod;
  }
}
