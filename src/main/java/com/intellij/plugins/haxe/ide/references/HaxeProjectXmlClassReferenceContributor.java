package com.intellij.plugins.haxe.ide.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.XmlPatterns;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeProjectXmlFiles;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/// Makes the class `<app main>` names in a Lime, OpenFL or NME project file,
/// or in a library's include file, navigable: Ctrl+click opens the class or
/// one of its packages, Find Usages lists the attribute, and renaming the
/// class or a package updates it ([HaxeProjectXmlClassReference]). Only a
/// plain dotted name gets references; one Lime expands (`${define}`) does not.
public class HaxeProjectXmlClassReferenceContributor extends PsiReferenceContributor {

  /** Identifiers joined by dots: {@code Main}, {@code com.example.Main}. */
  private static final Pattern DOTTED_NAME = Pattern.compile("[A-Za-z_]\\w*(?:\\.[A-Za-z_]\\w*)*");

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue(ProjectXmlVocabulary.MAIN_ATTRIBUTE), new ClassReferenceProvider());
  }

  private static boolean isAppAttribute(@NotNull XmlAttributeValue value) {
    XmlTag tag = PsiTreeUtil.getParentOfType(value, XmlTag.class);
    return tag != null && ProjectXmlVocabulary.APP_ELEMENT.equalsIgnoreCase(tag.getLocalName());
  }

  private static final class ClassReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      XmlAttributeValue value = (XmlAttributeValue)element;
      if (!isAppAttribute(value) || !HaxeProjectXmlFiles.isProjectOrLibraryIncludeXml(value.getContainingFile())) return PsiReference.EMPTY_ARRAY;
      TextRange valueRange = ElementManipulators.getValueTextRange(value);
      String name = valueRange.substring(value.getText());
      if (!DOTTED_NAME.matcher(name).matches()) return PsiReference.EMPTY_ARRAY;
      return HaxeProjectXmlClassReference.segmentsOf(value, valueRange.getStartOffset(), name).toArray(PsiReference.EMPTY_ARRAY);
    }
  }
}
