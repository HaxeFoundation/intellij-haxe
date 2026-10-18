package com.intellij.plugins.haxe.ide.references;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeProjectXmlFiles;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;


/// Makes the path attributes of a Lime, OpenFL or NME project file, or of a
/// library's include file, navigable:
/// `<include path|name>`, `<source path|name>`, `<classpath path|name>`,
/// `<assets path>`, `<icon path>` and `<template path>` resolve against the
/// project file's folder, and an `<include>` naming a folder opens the
/// include.lime / include.nmml / include.xml Lime picks inside it. The
/// references are soft: a path Lime expands (`${haxelib:x}`, environment
/// variables) gets none, and one naming nothing is never an error.
// TODO: link <haxelib name> to the library's include file; needs the haxelib repository root without running haxelib config
public class HaxeProjectXmlPathReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    ElementPattern<XmlAttributeValue> pathValues =
      XmlPatterns.xmlAttributeValue(ProjectXmlVocabulary.PATH_ATTRIBUTE, ProjectXmlVocabulary.NAME_ATTRIBUTE);
    registrar.registerReferenceProvider(pathValues, new PathReferenceProvider());
  }

  private static boolean isPathAttribute(@NotNull XmlAttributeValue value) {
    XmlAttribute attribute = PsiTreeUtil.getParentOfType(value, XmlAttribute.class);
    return attribute != null && ProjectXmlVocabulary.isPathAttribute(attribute);
  }

  private static final class PathReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      XmlAttributeValue value = (XmlAttributeValue)element;
      if (!isPathAttribute(value) || !HaxeProjectXmlFiles.isProjectOrLibraryIncludeXml(value.getContainingFile())) return PsiReference.EMPTY_ARRAY;
      String path = value.getValue().trim();
      if (path.isEmpty() || ProjectXmlVocabulary.isToolExpanded(path)) return PsiReference.EMPTY_ARRAY;
      return new PsiReference[]{new HaxeProjectXmlPathReference(value, path)};
    }
  }
}
