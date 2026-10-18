package com.intellij.plugins.haxe.ide.inspections.buildfiles;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.modcommand.PsiUpdateModCommandQuickFix;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.buildsystem.hxml.psi.HXMLFile;
import com.intellij.plugins.haxe.hxml.psi.HXMLDefine;
import com.intellij.plugins.haxe.hxml.psi.HXMLValue;
import com.intellij.plugins.haxe.hxml.psi.HXMLVisitor;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeProjectXmlFiles;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// A define whose name carries a dash: the compiler registers `-D my-flag` (or
/// `<haxedef name="my-flag"/>`) under `my_flag`, so only that spelling works in
/// `#if`. Covers hxml files and the XML files the plugin classifies as Lime,
/// OpenFL or NME project files. The quick fix rewrites the name to the
/// compiler's spelling; a `=value` part stays. A Lime `<define>` is also a
/// condition define, matched by its written name in `if`/`unless` attributes
/// and `${name}` substitutions, so those references in the file follow the
/// rename.
public class HaxeDashedDefineNameInspection extends LocalInspectionTool {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    PsiFile file = holder.getFile();
    if (file instanceof HXMLFile) return new HxmlDefineVisitor(holder);
    if (HaxeProjectXmlFiles.isProjectXml(file)) return new ProjectXmlDefineVisitor(holder);
    return PsiElementVisitor.EMPTY_VISITOR;
  }

  private static void checkName(@NotNull ProblemsHolder holder, @NotNull PsiElement element, @NotNull TextRange nameRange) {
    String name = nameRange.substring(element.getText());
    if (name.indexOf('-') < 0) return;
    String compilerName = HaxeDefine.compilerName(name);
    String message = HaxeBundle.message("haxe.inspections.dashed.define.message", name, compilerName);
    holder.registerProblem(element, nameRange, message, new ReplaceWithCompilerNameFix(compilerName));
  }

  /// The name's range inside a `-D` value: it ends at the `=value` part or at
  /// whitespace (the lexer keeps a trailing comment inside the value token).
  @NotNull
  private static TextRange hxmlNameRange(@NotNull String defineValue) {
    int end = StringUtil.indexOfAny(defineValue, "= \t");
    return TextRange.from(0, end < 0 ? defineValue.length() : end);
  }

  private static final class HxmlDefineVisitor extends HXMLVisitor {
    private final ProblemsHolder holder;

    private HxmlDefineVisitor(@NotNull ProblemsHolder holder) {
      this.holder = holder;
    }

    @Override
    public void visitDefine(@NotNull HXMLDefine define) {
      HXMLValue value = define.getValue();
      if (value != null) checkName(holder, value, hxmlNameRange(value.getText()));
    }
  }

  private static final class ProjectXmlDefineVisitor extends XmlElementVisitor {
    private final ProblemsHolder holder;

    private ProjectXmlDefineVisitor(@NotNull ProblemsHolder holder) {
      this.holder = holder;
    }

    @Override
    public void visitXmlTag(@NotNull XmlTag tag) {
      if (!ProjectXmlVocabulary.DEFINE_ELEMENTS.contains(tag.getLocalName().toLowerCase(Locale.ROOT))) return;
      XmlAttributeValue name = HaxeXmlAttributeValues.nameAttributeValue(tag);
      if (name == null) return;
      checkName(holder, name, HaxeXmlAttributeValues.valueRange(name));
    }
  }

  private static final class ReplaceWithCompilerNameFix extends PsiUpdateModCommandQuickFix {
    private static final String CONDITION_DEFINE_TAG = "define";
    private static final Set<String> CONDITION_ATTRIBUTES = Set.of("if", "unless");

    private final String compilerName;

    private ReplaceWithCompilerNameFix(@NotNull String compilerName) {
      this.compilerName = compilerName;
    }

    @Override
    public @NotNull String getName() {
      return HaxeBundle.message("haxe.inspections.dashed.define.fix", compilerName);
    }

    @Override
    public @NotNull String getFamilyName() {
      return HaxeBundle.message("haxe.inspections.dashed.define.fix.family");
    }

    @Override
    protected void applyFix(@NotNull Project project, @NotNull PsiElement element, @NotNull ModPsiUpdater updater) {
      switch (element) {
        case HXMLValue value -> replaceHxmlValue(value);
        case XmlAttributeValue value -> renameAttribute(value);
        default -> { }
      }
    }

    private static void renameAttribute(@NotNull XmlAttributeValue value) {
      if (!(value.getParent() instanceof XmlAttribute attribute)) return;
      String name = value.getValue();
      String compilerName = HaxeDefine.compilerName(name);
      attribute.setValue(compilerName);
      if (isConditionDefine(attribute)) renameConditionReferences(attribute.getContainingFile(), name, compilerName);
    }

    /** Only {@code <define>} feeds Lime's condition defines; a {@code <haxedef>} reaches the compiler alone. */
    private static boolean isConditionDefine(@NotNull XmlAttribute attribute) {
      XmlTag tag = attribute.getParent();
      return tag != null && CONDITION_DEFINE_TAG.equalsIgnoreCase(tag.getLocalName());
    }

    private static void renameConditionReferences(@NotNull PsiFile file, @NotNull String name, @NotNull String compilerName) {
      // the name as a whole condition token: bounded by the value's ends, whitespace or the || separator
      Pattern conditionToken = Pattern.compile("(?<![^\\s|])" + Pattern.quote(name) + "(?![^\\s|])");
      // the name directly after a ${ opener, followed by the closing }, whitespace or a comparison operator
      Pattern substitution = Pattern.compile("(?<=\\$\\{)" + Pattern.quote(name) + "(?=[\\s}=!<>])");
      String replacement = Matcher.quoteReplacement(compilerName);
      for (XmlAttribute attribute : PsiTreeUtil.findChildrenOfType(file, XmlAttribute.class)) {
        String value = attribute.getValue();
        if (value == null || !value.contains(name)) continue;
        boolean condition = CONDITION_ATTRIBUTES.contains(attribute.getLocalName().toLowerCase(Locale.ROOT));
        String renamed = condition ? conditionToken.matcher(value).replaceAll(replacement) : value;
        renamed = substitution.matcher(renamed).replaceAll(replacement);
        if (!renamed.equals(value)) attribute.setValue(renamed);
      }
    }

    private static void replaceHxmlValue(@NotNull HXMLValue value) {
      String text = value.getText();
      TextRange nameRange = hxmlNameRange(text);
      String renamed = HaxeDefine.compilerName(nameRange.substring(text)) + text.substring(nameRange.getEndOffset());
      value.replace(hxmlValue(value.getProject(), renamed));
    }

    /// A VALUE element parsed from the `-D` line of a throwaway hxml file.
    @NotNull
    private static HXMLValue hxmlValue(@NotNull Project project, @NotNull String text) {
      PsiFile file = PsiFileFactory.getInstance(project).createFileFromText("define.hxml", HXMLLanguage.INSTANCE, "-D " + text);
      return Objects.requireNonNull(PsiTreeUtil.findChildOfType(file, HXMLValue.class));
    }
  }
}
