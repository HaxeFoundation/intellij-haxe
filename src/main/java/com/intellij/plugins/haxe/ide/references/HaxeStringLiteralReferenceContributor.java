package com.intellij.plugins.haxe.ide.references;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Makes string literals navigable and completable when their VALUE denotes
 * something real: a file path (absolute, project-relative or relative to
 * the containing file's directory) or a fully-qualified name. File-path
 * SEGMENT references attach to every clean constant string so explicit
 * completion works from the first character; qualified-name and
 * absolute-path references attach only when the target resolves. What
 * PAINTS as a link is gated separately on shape + resolution — prose never
 * lights up.
 */
public class HaxeStringLiteralReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(HaxeStringLiteralExpression.class),
      new StringLiteralReferenceProvider());
  }

  private static final class StringLiteralReferenceProvider extends PsiReferenceProvider {

    // longer than any plausible path or qname - everything above is prose
    private static final int MAX_CANDIDATE_LENGTH = 300;

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      HaxeStringLiteralExpression literal = (HaxeStringLiteralExpression)element;
      String value = constantValueOf(literal);
      if (value == null || value.length() > MAX_CANDIDATE_LENGTH) {
        return PsiReference.EMPTY_ARRAY;
      }

      // the whole content between the quotes is the link
      TextRange range = new TextRange(1, literal.getTextLength() - 1);
      List<PsiReference> references = new SmartList<>();

      // escapes shift value offsets away from raw-text offsets, which the
      // per-segment set cannot represent - such paths (and absolute ones)
      // keep the single navigation-only reference
      boolean segmentable = !HaxeStringFilePathReference.looksAbsolute(value) && rawContentEquals(literal, value);
      if (segmentable) {
        // attached to EVERY clean constant string, resolved or not, path-shaped
        // or not: explicit completion must work from the very first segment
        // (variants merge the children of all bases). Link painting is gated
        // separately on shape + the last segment's resolution.
        references.addAll(List.of(new HaxeStringFileReferenceSet(value, literal, 1).getAllReferences()));
      }
      else if (value.length() >= 3
               && HaxeStringFilePathReference.looksLikePath(value)
               && HaxeStringFilePathReference.resolveFile(literal, value) != null) {
        references.add(new HaxeStringFilePathReference(literal, range, value));
      }
      // require a dot: bare capitalized words ("Main", "Error") are everyday
      // prose; only a DOTTED name reads as an intentional qualified name
      boolean qnameShaped = value.length() >= 3 && value.indexOf('.') > 0 && HaxeReferenceUtil.textCanBeQname(value);
      if (qnameShaped && HaxeQnameResolveUtil.findClassOrMember(value, literal.getProject()) != null) {
        references.add(new HaxeStringQnameReference(literal, range, value));
      }
      return references.isEmpty() ? PsiReference.EMPTY_ARRAY : references.toArray(PsiReference.EMPTY_ARRAY);
    }

    /** True when the literal's raw content between the quotes IS the value (no escape sequences). */
    private static boolean rawContentEquals(@NotNull HaxeStringLiteralExpression literal, @NotNull String value) {
      String text = literal.getText();
      return text.length() == value.length() + 2 && text.regionMatches(1, value, 0, value.length());
    }

    /**
     * The literal's compile-time value, or null when it has none worth
     * linking: interpolation makes the value dynamic, and escape sequences
     * beyond quote/backslash/slash (newline, tab, unicode) never appear in
     * paths or qualified names.
     */
    @Nullable
    private static String constantValueOf(@NotNull HaxeStringLiteralExpression literal) {
      StringBuilder value = new StringBuilder(literal.getTextLength());
      for (ASTNode child = literal.getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
        IElementType type = child.getElementType();
        if (type == HaxeTokenTypes.OPEN_QUOTE || type == HaxeTokenTypes.CLOSING_QUOTE) continue;
        if (type == HaxeTokenTypes.REGULAR_STRING_PART) {
          value.append(child.getText());
          continue;
        }
        if (type == HaxeTokenTypes.ESCAPED_STRING_PART) {
          String unescaped = unescapePathChar(child.getText());
          if (unescaped == null) return null;
          value.append(unescaped);
          continue;
        }
        // template entries, invalid escapes, anything unexpected
        return null;
      }
      return value.toString();
    }

    @Nullable
    private static String unescapePathChar(@NotNull String escape) {
      return switch (escape) {
        case "\\\\" -> "\\";
        case "\\/" -> "/";
        case "\\\"" -> "\"";
        case "\\'" -> "'";
        default -> null;
      };
    }
  }
}
