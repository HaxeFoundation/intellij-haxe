package com.intellij.plugins.haxe.config;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;

/**
 * @author: Fedor.Korotkov
 */
@State(
  name = "HaxeProjectSettings",
  storages = {
    @Storage( file = StoragePathMacros.PROJECT_FILE),
    @Storage( file = StoragePathMacros.PROJECT_CONFIG_DIR + "/haxe.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class HaxeProjectSettings implements PersistentStateComponent<Element> {
  public static final String HAXE_SETTINGS = "HaxeProjectSettings";
  public static final String DEFINES = "defines";
  private String userCompilerDefinitions = "";

  public static HaxeProjectSettings getInstance(Project project) {
    return ServiceManager.getService(project, HaxeProjectSettings.class);
  }

  public String[] getUserCompilerDefinitions() {
    return userCompilerDefinitions.split(",");
  }

  public void setUserCompilerDefinitions(String[] userCompilerDefinitions) {
    this.userCompilerDefinitions = StringUtil.join(userCompilerDefinitions, ",");
  }

  @Override
  public void loadState(Element state) {
    userCompilerDefinitions = state.getAttributeValue(DEFINES, "");
  }

  @Override
  public Element getState() {
    final Element element = new Element(HAXE_SETTINGS);
    element.setAttribute(DEFINES, userCompilerDefinitions);
    return element;
  }
}
