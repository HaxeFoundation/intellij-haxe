package com.intellij.plugins.haxe.buildsystem;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.plugins.haxe.buildsystem.lime.LimeXmlSchemaProvider;
import com.intellij.plugins.haxe.buildsystem.nmml.NMMLSchemaProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Documentation for an attribute offered by completion in a Lime, OpenFL or NME project file. The platform's XML
 * documentation takes a completion entry for an attribute only when the caret is in an attribute or right before
 * the tag's end; with whitespace after the caret, the usual place to complete a new attribute, it looks for a tag of
 * that name and shows nothing. This provider finds the attribute's schema declaration in that case, and the
 * platform's XML documentation renders it.
 */
public final class ProjectXmlAttributeLookupDocumentationProvider implements DocumentationProvider {

  private static final String ANNOTATION = "annotation";
  private static final String DOCUMENTATION = "documentation";

  @Override
  public @Nullable PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    if (!(object instanceof String name) || !(element instanceof PsiWhiteSpace)) return null;
    if (!(element.getParent() instanceof XmlTag tag) || !isInStartTag(element)) return null;
    if (!(tag.getContainingFile() instanceof XmlFile file) || !isProjectFile(file)) return null;

    XmlElementDescriptor descriptor = tag.getDescriptor();
    XmlAttributeDescriptor attribute = descriptor == null ? null : descriptor.getAttributeDescriptor(name, tag);
    PsiElement declaration = attribute == null ? null : attribute.getDeclaration();
    // without documentation of its own, the platform would show the documentation of the tag's type instead
    return isDocumented(declaration) ? declaration : null;
  }

  /** Whether the schema declaration carries an {@code xs:annotation} with an {@code xs:documentation}. */
  private static boolean isDocumented(@Nullable PsiElement declaration) {
    if (!(declaration instanceof XmlTag tag)) return false;
    for (XmlTag annotation : tag.getSubTags()) {
      if (!ANNOTATION.equals(annotation.getLocalName())) continue;
      for (XmlTag documentation : annotation.getSubTags()) {
        if (DOCUMENTATION.equals(documentation.getLocalName())) return true;
      }
    }
    return false;
  }

  /** Whether the element lies between the tag's name and the end of its start tag, where attributes go. */
  private static boolean isInStartTag(@NotNull PsiElement element) {
    for (PsiElement sibling = element.getPrevSibling(); sibling != null; sibling = sibling.getPrevSibling()) {
      IElementType type = PsiUtilCore.getElementType(sibling);
      if (type == XmlTokenType.XML_TAG_END || type == XmlTokenType.XML_EMPTY_ELEMENT_END) return false;
    }
    return true;
  }

  private static boolean isProjectFile(@NotNull XmlFile file) {
    return new LimeXmlSchemaProvider().isAvailable(file) || new NMMLSchemaProvider().isAvailable(file);
  }
}
