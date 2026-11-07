package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One dotted segment of a fully-qualified name in a string literal, like
 * the debugger's value links but in source. It resolves to what the name up
 * to and including the segment denotes: the package for a package segment,
 * the class or member for the rest, segment by segment like an import.
 * Soft: dot-shaped prose is never an error, and the contributor attaches
 * these references only when the whole name resolves.
 */
public class HaxeStringQnameReference extends PsiReferenceBase<HaxeStringLiteralExpression> {

  /** The name up to and including this segment. */
  private final String qname;

  public HaxeStringQnameReference(@NotNull HaxeStringLiteralExpression literal, @NotNull TextRange range, @NotNull String qname) {
    super(literal, range, true);
    this.qname = qname;
  }

  /** One reference per dotted segment of the value; the last one names the whole value. The value starts right after the opening quote. */
  @NotNull
  public static List<HaxeStringQnameReference> segmentsOf(@NotNull HaxeStringLiteralExpression literal, @NotNull String value) {
    List<HaxeStringQnameReference> references = new ArrayList<>();
    int start = 0;
    while (true) {
      int dot = value.indexOf('.', start);
      int end = dot < 0 ? value.length() : dot;
      // + 1: the content follows the opening quote
      references.add(new HaxeStringQnameReference(literal, new TextRange(start + 1, end + 1), value.substring(0, end)));
      if (dot < 0) return references;
      start = dot + 1;
    }
  }

  @Override
  @Nullable
  public PsiElement resolve() {
    Project project = getElement().getProject();
    PsiElement element = HaxeQnameResolveUtil.findClassOrMember(qname, project);
    if (element != null) return element;
    return HaxeResolveUtil.findPackageByQName(qname, PsiManager.getInstance(project), GlobalSearchScope.allScope(project));
  }
}
