package com.intellij.plugins.haxe.ide.toolWindow.haxelib;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.haxelib.HaxelibLocalDocs;
import com.intellij.plugins.haxe.ide.documentation.HaxeDocumentationRenderer;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a library version's local doc files (readme/changelog/license)
 * into the details pane's HTML tabs — the presentation half behind
 * {@link HaxelibDetailsPane.DocTab}; {@link HaxelibLocalDocs} finds the
 * files. Call from a pooled thread: rendering reads files and takes the
 * read lock.
 */
final class HaxelibDocsLoader {

  private final HaxeDocumentationRenderer markdownRenderer;

  HaxelibDocsLoader(@NotNull Project project) {
    markdownRenderer = new HaxeDocumentationRenderer(project);
  }

  @NotNull
  List<HaxelibDetailsPane.DocTab> loadDocs(@NotNull Path repoRoot, @NotNull String name, @NotNull String version) {
    Path directory = HaxelibLocalDocs.versionDirectory(repoRoot, name, version);
    if (directory == null) return List.of();
    List<HaxelibDetailsPane.DocTab> docs = new ArrayList<>();
    URL base = directoryUrl(directory);
    for (Path file : HaxelibLocalDocs.docFiles(directory)) {
      try {
        String markdown = Files.readString(file);
        // the renderer's code-fence highlighting lexes through the
        // platform's editor machinery, which requires the read lock even
        // off the EDT
        String rendered = ReadAction.nonBlocking(() -> markdownRenderer.parseAndRender(markdown))
          .executeSynchronously();
        String html = "<html><body>" + adaptImagesForSwing(rendered) + "</body></html>";
        docs.add(new HaxelibDetailsPane.DocTab(file.getFileName().toString(), html, base));
      }
      catch (IOException ignored) {
        // an unreadable doc file just contributes no tab
      }
    }
    return docs;
  }

  /**
   * Swing's HTML viewer draws a colored border around an image inside a
   * link unless the img carries border=0, and cannot decode SVG at all —
   * the typical CI/version badges would each render as a broken-image box,
   * so those are dropped.
   */
  @NotNull
  private static String adaptImagesForSwing(@NotNull String html) {
    // an entire <img ...> tag whose src attribute points at an .svg file or
    // a shields.io badge (served as SVG regardless of extension)
    String withoutSvg = html.replaceAll("<img[^>]*src=\"[^\"]*(?:\\.svg|img\\.shields\\.io)[^\"]*\"[^>]*>", "");
    return withoutSvg.replace("<img ", "<img border=\"0\" ");
  }

  @Nullable
  private static URL directoryUrl(@NotNull Path directory) {
    try {
      return directory.toUri().toURL();
    }
    catch (MalformedURLException e) {
      return null;
    }
  }
}
