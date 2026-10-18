package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
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
/// them, while the color annotators still paint. Parse errors are hidden too
/// ([HaxeGeneratedPreviewErrorFilter]): the dump printer writes a few
/// constructs no Haxe parser accepts.
///
/// The work is split by thread: [#prepare] does the file IO, parsing and
/// offset lookup on a BACKGROUND thread, and [#openPrepared] only opens the
/// editor, on the EDT.
@CustomLog
public final class HaxeGeneratedCodePreview {

  /** Marks preview virtual files; the value is the previewed module's dot path. */
  public static final Key<String> PREVIEW_KEY = Key.create("haxe.generated.preview.dotpath");
  /** Where a preview file's dump came from; see {@link PreviewOrigin}. */
  private static final Key<PreviewOrigin> PREVIEW_ORIGIN_KEY = Key.create("haxe.generated.preview.origin");

  /** Whether the file is a generated-code preview. */
  public static boolean isPreview(@Nullable VirtualFile file) {
    return file != null && file.getUserData(PREVIEW_KEY) != null;
  }

  /** Whether the preview's dump is older than the Haxe sources on disk. */
  public static boolean isStale(@NotNull Project project, @Nullable VirtualFile file) {
    PreviewOrigin origin = originOf(file);
    return origin != null && origin.sourcesStamp() != HaxeSavedSourcesStamp.getInstance(project).get();
  }

  /** The origin of a preview file; null for any other file. */
  @Nullable
  public static PreviewOrigin originOf(@Nullable VirtualFile file) {
    return file == null ? null : file.getUserData(PREVIEW_ORIGIN_KEY);
  }

  /**
   * Where a preview's dump came from: the build context, the module it
   * renders, and the {@link HaxeSavedSourcesStamp} value its dump build
   * started from.
   */
  public record PreviewOrigin(@NotNull DisplayContext context, @NotNull String moduleDotPath, long sourcesStamp) {

    /** Whether both previews render the same module of the same build. */
    boolean sameModuleAs(@NotNull PreviewOrigin other) {
      String contextKey = HaxeCompilerDisplayService.contextKey(context);
      return moduleDotPath.equals(other.moduleDotPath) && contextKey.equals(HaxeCompilerDisplayService.contextKey(other.context));
    }
  }

  /** A ready-to-open preview: the rendered file and the caret offset of the requested member. */
  public record PreparedPreview(@NotNull LightVirtualFile file, int offset) {

    /** The same preview opening at the start of the given line, or of its last line when it has fewer. */
    @NotNull
    PreparedPreview atLine(int line) {
      CharSequence text = file.getContent();
      int lastLine = StringUtil.countNewLines(text);
      return new PreparedPreview(file, StringUtil.lineColToOffset(text, Math.min(line, lastLine), 0));
    }
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
   * Renders the module dump and locates the spot's local inside its member,
   * else the member of its type (falling back to the type declaration, then
   * the top of the file). Involves file IO and a PSI parse, so background
   * threads only.
   */
  @Nullable
  public static PreparedPreview prepare(@NotNull Project project,
                                        @NotNull Path dumpFile,
                                        @NotNull HaxeGeneratedPreviewSpot spot,
                                        @NotNull PreviewOrigin origin) {
    LightVirtualFile file = previewFile(dumpFile, spot.typeDotPath(), origin);
    if (file == null) return null;
    int offset = ReadAction.nonBlocking(() -> spotOffset(project, file, spot)).executeSynchronously();
    return new PreparedPreview(file, Math.max(offset, 0));
  }

  /** Opens a prepared preview in the editor and closes the older previews it replaces. EDT only. */
  public static void openPrepared(@NotNull Project project, @NotNull PreparedPreview prepared) {
    new OpenFileDescriptor(project, prepared.file(), prepared.offset()).navigate(true);
    closeReplacedPreviews(project, prepared.file());
  }

  /** Closes the open previews of the same module and build as the given one, which shows a newer dump. */
  private static void closeReplacedPreviews(@NotNull Project project, @NotNull VirtualFile current) {
    PreviewOrigin origin = originOf(current);
    if (origin == null) return;
    FileEditorManager editors = FileEditorManager.getInstance(project);
    for (VirtualFile open : editors.getOpenFiles()) {
      PreviewOrigin openOrigin = originOf(open);
      if (open != current && openOrigin != null && openOrigin.sameModuleAs(origin)) {
        editors.closeFile(open);
      }
    }
  }

  @Nullable
  private static LightVirtualFile previewFile(@NotNull Path dumpFile, @NotNull String typeDotPath, @NotNull PreviewOrigin origin) {
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
    file.putUserData(PREVIEW_ORIGIN_KEY, origin);
    previews.put(dumpFile, new CachedPreview(stamp, file));
    return file;
  }

  /**
   * Rewrites qualified declaration headers into a package statement plus
   * simple names, and prepends a banner saying what the reader is looking at.
   * Backtick names (unbound identifiers, the compiler's temporaries) need no
   * rewriting: the grammar parses them.
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
      // %s
      // %s
      """.formatted(HaxeBundle.message("haxe.generated.preview.header.content"),
                    HaxeBundle.message("haxe.generated.preview.header.snapshot"));
    String packageStatement = packageName != null ? "package " + packageName + ";\n" : "";
    return banner + packageStatement + "\n" + rewritten;
  }

  /**
   * The offset of the local's NAME inside the member, else of the member's
   * name, else of the type, else 0. Walks plain PSI on purpose: the model's
   * member lookup would pull type resolution over the whole dump, which can
   * be library-sized.
   */
  static int spotOffset(@NotNull Project project, @NotNull LightVirtualFile file, @NotNull HaxeGeneratedPreviewSpot spot) {
    if (!(PsiManager.getInstance(project).findFile(file) instanceof HaxeFile haxeFile)) return 0;
    String typeName = StringUtil.getShortName(spot.typeDotPath());
    for (HaxeClass haxeClass : haxeFile.getClassList()) {
      if (!typeName.equals(haxeClass.getName())) continue;
      if (spot.memberName() == null) return haxeClass.getTextOffset();
      for (HaxeComponentName componentName : PsiTreeUtil.findChildrenOfType(haxeClass, HaxeComponentName.class)) {
        if (spot.memberName().equals(componentName.getText()) && isMemberDeclaration(componentName)) {
          HaxeComponentName local = localIn(componentName.getParent(), spot.localName());
          return local != null ? local.getTextOffset() : componentName.getTextOffset();
        }
      }
      return haxeClass.getTextOffset();
    }
    return 0;
  }

  /** The first declaration of the named local (parameter or variable) inside the member, else null. */
  @Nullable
  private static HaxeComponentName localIn(@NotNull PsiElement member, @Nullable String localName) {
    if (localName == null) return null;
    for (HaxeComponentName componentName : PsiTreeUtil.findChildrenOfType(member, HaxeComponentName.class)) {
      if (localName.equals(componentName.getText()) && !isMemberDeclaration(componentName)) return componentName;
    }
    return null;
  }

  /** A field or method name, as opposed to a local variable or parameter sharing the name. */
  private static boolean isMemberDeclaration(@NotNull HaxeComponentName componentName) {
    PsiElement declaration = componentName.getParent();
    return declaration instanceof HaxeFieldDeclaration || declaration instanceof HaxeMethod;
  }
}
