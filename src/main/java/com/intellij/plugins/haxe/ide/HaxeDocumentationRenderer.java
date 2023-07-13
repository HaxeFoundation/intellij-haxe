package com.intellij.plugins.haxe.ide;

import com.intellij.lang.Language;
import com.intellij.lang.documentation.DocumentationSettings;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeLanguage;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class HaxeDocumentationRenderer {

  private List<Extension> extensions = Arrays.asList(AutolinkExtension.create(), TablesExtension.create());

  private final Project myProject;
  private final HtmlRenderer renderer;
  private final Parser parser;

  public HaxeDocumentationRenderer(Project project) {
    myProject = project;

    parser = Parser.builder()
      .extensions(extensions)
      .build();

    renderer = HtmlRenderer.builder()
      .attributeProviderFactory(new HighlighterAttributeProvider())
      .nodeRendererFactory(new languageHighlighter(this))
      .extensions(extensions).build();
  }


  public String renderDocs(String docs) {
    // docs are usually divided into multiple lines to fit the screen, but we dont want this when rendering the docs in a resizable window
    // we also dont want unnecessary many tabs in the middle of a line so we try to strip out indents
    String[] split = docs.split("\n");
    String secondLine = split[1];
    int before = secondLine.length();
    int after = secondLine.stripLeading().length();
    int estimatedIdents = before - after ;

    String trimmedDocs = Arrays.stream(docs.trim().split("\n"))
      .map(s -> trimIdents(s, estimatedIdents))
      .map(s -> s.isEmpty() ? "\n" : s)
      .collect(Collectors.joining("\n"));

    Node document = parser.parse(trimmedDocs);
    return renderer.render(document);
  }

  @NotNull
  private static String trimIdents(String s, int estimatedIdents) {
    for (int i = 0; i < estimatedIdents; i++) {
      s = s.replaceFirst("^(\s|\t)", "");
    }
    return s;
  }

  private static class languageHighlighter implements HtmlNodeRendererFactory {


    private final HaxeDocumentationRenderer renderer;

    public languageHighlighter(HaxeDocumentationRenderer renderer) {
      this.renderer = renderer;
    }

    @Override
    public NodeRenderer create(HtmlNodeRendererContext context) {
      return new NodeRenderer() {
        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
          return Set.of(FencedCodeBlock.class, Code.class);
        }

        @Override
        public void render(Node node) {
          if (node instanceof Code code) {

            String highlighting = renderer.languageHighlighting(code.getLiteral());

            context.getWriter().tag("code");
            context.getWriter().raw(highlighting);
            context.getWriter().tag("/code");
          }
          if (node instanceof FencedCodeBlock codeBlock) {

            String languageString = codeBlock.getInfo();
            String code = codeBlock.getLiteral();


            Optional<Language> optionalLanguage = Language.getRegisteredLanguages().stream()
              .filter(l -> !l.getID().isEmpty())
              .filter(l -> l.getID().toLowerCase().equalsIgnoreCase(languageString))
              .findFirst();

            Language language = optionalLanguage.orElse(HaxeLanguage.INSTANCE);

            String highlighting = renderer.languageHighlighting(language, code);

            context.getWriter().tag("br/");
            context.getWriter().tag("code");
            context.getWriter().raw(highlighting);
            context.getWriter().tag("/code");
            context.getWriter().tag("br/");

          }
        }
      };
    }
  }

  public String languageHighlighting(String code) {
   return languageHighlighting(HaxeLanguage.INSTANCE, code);
  }
  public String languageHighlighting(Language language, String code) {
    StringBuilder stringBuilder = new StringBuilder();
    float saturation = DocumentationSettings.getHighlightingSaturation(true);
    HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(stringBuilder, myProject,
                                                                           language,
                                                                           code,
                                                                           false,
                                                                           saturation);
    return stringBuilder.toString();
  }

  private static class HighlighterAttributeProvider implements AttributeProviderFactory {
    @Override
    public AttributeProvider create(AttributeProviderContext context) {
      return (node, tagName, attributes) -> {
        if (node instanceof Link) {
          Color color = DefaultLanguageHighlighterColors.DOC_COMMENT_LINK.getDefaultColor();
          if (color != null) {
            String hexColor = convertToHexColor(color);
            attributes.put("color", hexColor);
          }
        }
      };
    }

    public static String convertToHexColor(Color color) {
      return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
  }
}
