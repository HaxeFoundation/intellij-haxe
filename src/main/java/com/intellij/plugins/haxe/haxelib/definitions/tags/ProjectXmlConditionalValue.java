package com.intellij.plugins.haxe.haxelib.definitions.tags;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProjectXmlConditionalValue {


  List<String> ifConditions = new ArrayList<>();
  List<String> unlessConditions = new ArrayList<>();

  public ProjectXmlConditionalValue(@Nullable String ifCondition, @Nullable String unlessCondition) {
    // TODO support OR evaluation (currently everything is read as AND)
    // NOTE that tags can also be wrapped in <Section if="", unless=""> and these needs to be evaluated as well
    // openfl has a include.xml with more define
    if (ifCondition != null) {
      String[] conditions = ifCondition.split("(\\|\\|)|(&&)");
      this.ifConditions = Arrays.stream(conditions).map(String::trim).toList();
    }
    if (unlessCondition != null) {
      String[] conditions = unlessCondition.split("(\\|\\|)|(&&)");
      this.unlessConditions = Arrays.stream(conditions).map(String::trim).toList();
    }
  }

  public boolean getEnabled(Map<String, String> otherDefinitions) {
    if (ifConditions.isEmpty() && unlessConditions.isEmpty()) {
      return true;
    }
    else if (!ifConditions.isEmpty()) {
      return ifConditions.stream().anyMatch(ifCon -> !isDefined(ifCon, otherDefinitions));
    }
    else {
      return unlessConditions.stream().anyMatch(ifCon -> isDefined(ifCon, otherDefinitions));
    }
  }

  private boolean isDefined(String con, Map<String, String> definitions) {
    return definitions.getOrDefault(con, "false").equalsIgnoreCase("true");
  }
}
