package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.documentation.DocumentationManagerUtil;
import com.intellij.lang.Language;
import com.intellij.lang.documentation.DocumentationSettings;
import com.intellij.lang.documentation.QuickDocHighlightingHelper;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeLanguage;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.*;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class HaxeDocumentationRenderer {


  private final Project myProject;
  private final HtmlRenderer renderer;
  private final Parser parser;

  public HaxeDocumentationRenderer(Project project) {
    List<Extension> extensions = Arrays.asList(
            AutolinkExtension.create(),
            TablesExtension.create(),
            HaxeDocumentationTagsExtension.create(),
            HaxeCodeReferenceToLinksExtension.create(project)
    );

    myProject = project;

    parser = Parser.builder()
      .extensions(extensions)
      .build();

    renderer = HtmlRenderer.builder()
      .attributeProviderFactory(new HighlighterAttributeProvider())
      .nodeRendererFactory(new languageHighlighter(this))
      .extensions(extensions).build();
  }


  public String parseAndRenderDocs(String docs) {
      Node document = parser.parse(docs);
      return renderer.render(document);
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
          return Set.of(FencedCodeBlock.class, Code.class, HtmlBlock.class, ReferenceCodeLink.class);
        }

        @Override
        public void render(Node node) {
          if (node instanceof ReferenceCodeLink link) {
            StringBuilder builder = new StringBuilder();
            DocumentationManagerUtil.createHyperlink(builder, link.getPsiReference(), link.getLinkText(), false);
            context.getWriter().tag("code");
            context.getWriter().raw(builder.toString());
            context.getWriter().tag("/code");
          }

          if (node instanceof Code code) {

            String highlighting = renderer.languageHighlighting(code.getLiteral());

            context.getWriter().tag("code");
            context.getWriter().raw(highlighting);
            context.getWriter().tag("/code");
          }
          if (node instanceof HtmlBlock htmlBlock) {
            // TODO determine type and
            String literal = htmlBlock.getLiteral();

            context.getWriter().raw(literal);
//            renderer.languageHighlighting(language, code, true);
          }
          if (node instanceof FencedCodeBlock codeBlock) {

            String languageString = codeBlock.getInfo();
            String code = codeBlock.getLiteral();


            Optional<Language> optionalLanguage = Language.getRegisteredLanguages().stream()
              .filter(l -> !l.getID().isEmpty())
              .filter(l -> l.getID().toLowerCase().equalsIgnoreCase(languageString))
              .findFirst();

            Language language = optionalLanguage.orElse(HaxeLanguage.INSTANCE);

            String highlighting = renderer.languageHighlighting(language, code, true);

            context.getWriter().raw(highlighting);
          }
        }
      };
    }
  }

  public String languageHighlighting(String code) {
   return languageHighlighting(HaxeLanguage.INSTANCE, code, false);
  }
  public String languageHighlighting(Language language, String code, boolean block) {
    StringBuilder stringBuilder = new StringBuilder();
    float saturation = DocumentationSettings.getHighlightingSaturation(true);
    if(block) {
      StringBuilder builder = QuickDocHighlightingHelper.appendStyledCodeBlock(stringBuilder, myProject, language, code);
      return builder.toString();
    }
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
