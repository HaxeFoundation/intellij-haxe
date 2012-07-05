package com.intellij.plugins.haxe.config;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jdom.Element;

import java.util.Arrays;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
@State(
  name = "HaxeProjectSettings",
  storages = {
    @Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/haxe.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class HaxeProjectSettings implements PersistentStateComponent<Element> {
  public static final String HAXE_SETTINGS = "HaxeProjectSettings";
  public static final String DEFINES = "defines";
  private String userCompilerDefinitions = "";

  public Set<String> getUserCompilerDefinitionsAsSet() {
    return new THashSet<String>(Arrays.asList(getUserCompilerDefinitions()));
  }

  public static HaxeProjectSettings getInstance(Project project) {
    return ServiceManager.getService(project, HaxeProjectSettings.class);
  }

  public String[] getUserCompilerDefinitions() {
    return userCompilerDefinitions.split(",");
  }

  public void setUserCompilerDefinitions(String[] userCompilerDefinitions) {
    this.userCompilerDefinitions = StringUtil.join(ContainerUtil.filter(userCompilerDefinitions, new Condition<String>() {
      @Override
      public boolean value(String s) {
        return s == null || s.isEmpty();
      }
    }), ",");
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
