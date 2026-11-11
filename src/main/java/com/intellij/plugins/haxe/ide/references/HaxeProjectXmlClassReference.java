package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// One dotted segment of the class a project-file attribute names, like
/// `<app main="com.example.Main"/>`: the name up to and including the segment
/// resolves to its package, module or class, and the last segment to the
/// class. The project file's module is searched first, then the whole
/// project. Soft: a name that denotes nothing is never an error.
final class HaxeProjectXmlClassReference extends PsiReferenceBase<XmlAttributeValue> {

  /** The name up to and including this segment. */
  private final String qname;
  private final boolean lastSegment;

  private HaxeProjectXmlClassReference(@NotNull XmlAttributeValue value, @NotNull TextRange range, @NotNull String qname, boolean lastSegment) {
    super(value, range, true);
    this.qname = qname;
    this.lastSegment = lastSegment;
  }

  /** One reference per dotted segment of {@code name}, which starts at {@code nameStart} in the attribute value's text. */
  @NotNull
  static List<HaxeProjectXmlClassReference> segmentsOf(@NotNull XmlAttributeValue value, int nameStart, @NotNull String name) {
    List<HaxeProjectXmlClassReference> references = new ArrayList<>();
    int start = 0;
    while (true) {
      int dot = name.indexOf('.', start);
      int end = dot < 0 ? name.length() : dot;
      TextRange range = new TextRange(nameStart + start, nameStart + end);
      references.add(new HaxeProjectXmlClassReference(value, range, name.substring(0, end), dot < 0));
      if (dot < 0) return references;
      start = dot + 1;
    }
  }

  @Override
  public @Nullable PsiElement resolve() {
    PsiManager manager = getElement().getManager();
    GlobalSearchScope moduleScope = moduleScope();
    PsiElement target = moduleScope == null ? null : resolveIn(manager, moduleScope);
    return target != null ? target : resolveIn(manager, GlobalSearchScope.allScope(manager.getProject()));
  }

  @Nullable
  private PsiElement resolveIn(@NotNull PsiManager manager, @NotNull GlobalSearchScope scope) {
    if (lastSegment) return HaxeResolveUtil.findClassByQName(qname, manager, scope);
    PsiElement pack = HaxeResolveUtil.findPackageByQName(qname, manager, scope);
    return pack != null ? pack : HaxeResolveUtil.findClassByQName(qname, manager, scope);
  }

  /** The project file's module with what it depends on, whose class paths the tool compiles; null outside a module. */
  @Nullable
  private GlobalSearchScope moduleScope() {
    PsiFile file = getElement().getContainingFile().getOriginalFile();
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    return module == null ? null : module.getModuleWithDependenciesAndLibrariesScope(false);
  }
}
