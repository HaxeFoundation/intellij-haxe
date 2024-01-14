package com.intellij.plugins.haxe.haxelib.definitions;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.plugins.haxe.buildsystem.hxml.psi.HXMLFile;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.haxelib.*;
import com.intellij.plugins.haxe.haxelib.definitions.tags.ProjectXmlDefineValue;
import com.intellij.plugins.haxe.haxelib.definitions.tags.ProjectXmlHaxedefValue;
import com.intellij.plugins.haxe.haxelib.definitions.tags.ProjectXmlHaxelibValue;
import com.intellij.plugins.haxe.haxelib.definitions.tags.ProjectXmlUndefineValue;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.psi.xml.XmlFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.plugins.haxe.haxelib.definitions.HxmlDefinitionsUtil.findHxml;
import static com.intellij.plugins.haxe.haxelib.definitions.HxmlDefinitionsUtil.processHxml;
import static com.intellij.plugins.haxe.haxelib.definitions.ProjectXmlDefinitionsUtil.*;
import static java.util.function.Predicate.not;


@CustomLog
public class HaxeDefineDetectionManager implements Disposable {

  static {
    log.setLevel(LogLevel.DEBUG);
  }


  public static final Map<Module, Map<String, String>> moduleDefinitionsMap = new HashMap<>();

  public static HaxeDefineDetectionManager getInstance(Project project) {
    return project.getService(HaxeDefineDetectionManager.class);
  }

  private  Project myProject;
  public HaxeDefineDetectionManager(Project project) {
    myProject = project;
  }

  private static boolean isUnsetFlag(Map.Entry<String, String> entry) {
    return "*UNSET*".equalsIgnoreCase(entry.getValue());
  }

  public void removeDetectedDefinitions(@NotNull Module module) {
    moduleDefinitionsMap.remove(module);
  }

  private void setDetectedDefinitions(@NotNull Module module, @NotNull Map<String, String> definitions) {
    moduleDefinitionsMap.put(module, definitions);
  }


  public Map<String, String> getAllDefinitions() {
    HashMap<String, String> map = new HashMap<>();
    HaxeProjectSettings instance = HaxeProjectSettings.getInstance(myProject);
    Map<String, String> projectUserDefineMap = instance.getUserCompilerDefinitionMap();

    // add user values
    projectUserDefineMap.entrySet()
      .stream()
      .filter(not(HaxeDefineDetectionManager::isUnsetFlag))
      .forEach( e -> map.put(e.getKey(), e.getValue()));

    if (instance.getAutoDetectDefinitions()) {
      for (Module module : moduleDefinitionsMap.keySet()) {
        // add  all auto detected values
        moduleDefinitionsMap.values().stream()
          .flatMap(map1 -> map1.entrySet().stream())
          .filter(not(HaxeDefineDetectionManager::isUnsetFlag))
          .forEach( e -> map.put(e.getKey(), e.getValue()));

        //TODO move project defines to module level settings
        //HaxeModuleSettings moduleSettings = HaxeModuleSettings.getInstance(module);
        //HaxeTarget target = moduleSettings.getHaxeTarget();
      }
      // remove any value marked with unset flag (*UNSET*)
      projectUserDefineMap.entrySet().stream()
        .filter(HaxeDefineDetectionManager::isUnsetFlag)
        .forEach( e -> map.remove(e.getKey()));

    }

    return map;
  }



  public void recalculateDefinitions(Project project) {
    Collection<Module> modules = ModuleUtil.getModulesOfType(project, HaxeModuleType.getInstance());

    ApplicationManager.getApplication().runReadAction(() -> {
      for (Module module : modules) {
        Map<String, String> moduleDefines = recalculateDefinitionsForModule(module);
        setDetectedDefinitions(module, moduleDefines);
      }
    });
  }

  private static Map<String, String> recalculateDefinitionsForModule(Module module) {
    Sdk sdk = ModuleRootManager.getInstance(module).getModifiableModel().getSdk();

    // if we cant detect the SDK  we wont bother to do the rest as there might be SDK dependencies and dont want to do a bunch of null checks.
    if (sdk == null) return Map.of();

    Map<String, String> detectedDefines = new HashMap<>();
    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);

    // getSDK version and add definition
    detectedDefines.put("haxe_ver", sdk.getVersionString());

    //TODO add and detect hashlink ? ("hl_ver") & neko?


    HaxeTarget target = settings.getCompilationTarget();
    // add default definitions for target
    target.getDefinitions().forEach(def -> detectedDefines.put(def, "true"));

    // buildsystems
    switch (settings.getBuildConfiguration()) {
      case OPENFL -> {
        processOpenFlModule(module, settings, detectedDefines);
      }
      case NMML -> {
        proccessNmmlModule(module, settings, detectedDefines);
      }
      case HXML -> {
        proccessHxmlModule(module, settings, detectedDefines);
      }
      case CUSTOM -> {
        // TODO  read parameters from argument string

      }
    }

    return detectedDefines;
  }

  private static void proccessHxmlModule(Module module, HaxeModuleSettings settings, Map<String, String> detectedDefines) {
    HXMLFile hxml = findHxml(module, settings.getHxmlPath());
    processHxml(module, detectedDefines, hxml);
  }




  private static void proccessNmmlModule(Module module, HaxeModuleSettings settings, Map<String, String> detectedDefines) {
    String[] flags = settings.getNmeTarget().getFlags();
    Arrays.stream(flags).forEach(def -> detectedDefines.put(def, "true"));
    XmlFile projectXml = findProjectXml(module, settings.getNmmlPath());
    processProjectXml(module, detectedDefines, projectXml);
  }

  private static void processOpenFlModule(Module module, HaxeModuleSettings settings, Map<String, String> detectedDefines) {
    String[] flags = settings.getOpenFLTarget().getFlags();
    Arrays.stream(flags).forEach(def -> detectedDefines.put(def, "true"));
    XmlFile projectXml = findProjectXml(module, settings.getOpenFLPath());
    processProjectXml(module, detectedDefines, projectXml);
  }

  private static void processProjectXml(Module module, Map<String, String> detectedDefines, XmlFile projectXml) {
    if (projectXml == null) return;

    //TODO rewrite to include <sections> and then parse its children (copy if/unless into children)

    List<ProjectXmlDefineValue> defines = getDefinesFromProjectXmlFile(projectXml);
    defines.forEach( value -> {
      if (value.getEnabled(detectedDefines)) {
        detectedDefines.put(value.getName(), "true");
      }
    });
    List<ProjectXmlHaxedefValue> haxedef = getHaxeDefFromProjectXmlFile(projectXml);
    haxedef.forEach( value -> {
      if (value.getEnabled(detectedDefines)) {
        detectedDefines.put(value.getName(), "true");
      }
    });

    List<ProjectXmlUndefineValue> undefines = getUndefinesFromProjectXmlFile(projectXml);
    undefines.forEach( value -> {
      if (value.getEnabled(detectedDefines)) {
        detectedDefines.put(value.getName(), "*UNSET*");
      }
    });

    ProjectLibraryCacheManager manager = HaxelibProjectUpdater.INSTANCE.getLibraryCacheManager(module);
    ModuleLibraryCache libraryManager = manager.getLibraryManager(module);

    List<ProjectXmlHaxelibValue> libs = getLibsFromProjectXmlFile(projectXml);
    libs.forEach( lib -> {
      if (lib.getEnabled(detectedDefines)) {
        detectedDefines.put(lib.getName(), Optional.ofNullable(lib.getVersion()).orElse(findVersion(lib.getName(), libraryManager)));
      }
    });
  }

  private static String findVersion(String name, ModuleLibraryCache manager) {
    HaxeLibrary library = manager.getLibrary(name, HaxelibSemVer.ANY_VERSION);
    if (library != null) {
      HaxelibSemVer version = library.getVersion();
      return version.toString();
    }else {
      // unknown version, setting true to make the define active (might not work well with compiler, som might have to change this)
      return "true";
    }
  }


  @Override
  public void dispose() {
    moduleDefinitionsMap.clear();
    myProject = null;
  }
}
