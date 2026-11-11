package com.intellij.plugins.haxe.ide.references;

import com.intellij.patterns.XmlPatterns;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeProjectXmlFiles;
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

/// Makes the library `<haxelib name>` names in a Lime, OpenFL or NME project
/// file, or in a library's include file, navigable: Ctrl+click selects the
/// library under External Libraries, as its row in the Haxe tool window does
/// ([HaxeProjectXmlLibraryReference]). A name Lime expands (`${define}`)
/// gets no reference.
public class HaxeProjectXmlLibraryReferenceContributor extends PsiReferenceContributor {

  /** The characters haxelib allows in a library name. */
  private static final Pattern LIBRARY_NAME = Pattern.compile("[\\w.-]+");

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue(ProjectXmlVocabulary.NAME_ATTRIBUTE), new LibraryReferenceProvider());
  }

  private static boolean isLibraryAttribute(@NotNull XmlAttributeValue value) {
    XmlTag tag = PsiTreeUtil.getParentOfType(value, XmlTag.class);
    return tag != null && ProjectXmlVocabulary.LIBRARY_ELEMENT.equalsIgnoreCase(tag.getLocalName());
  }

  private static final class LibraryReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      XmlAttributeValue value = (XmlAttributeValue)element;
      if (!isLibraryAttribute(value) || !HaxeProjectXmlFiles.isProjectOrLibraryIncludeXml(value.getContainingFile())) return PsiReference.EMPTY_ARRAY;
      String name = value.getValue();
      if (!LIBRARY_NAME.matcher(name).matches()) return PsiReference.EMPTY_ARRAY;
      return new PsiReference[]{new HaxeProjectXmlLibraryReference(value, name)};
    }
  }
}
