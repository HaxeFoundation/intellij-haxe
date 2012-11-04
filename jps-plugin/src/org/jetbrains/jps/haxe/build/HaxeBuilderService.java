package org.jetbrains.jps.haxe.build;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.incremental.BuilderService;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeBuilderService extends BuilderService {
  @NotNull
  @Override
  public List<? extends ModuleLevelBuilder> createModuleLevelBuilders() {
    return Arrays.asList(new HaxeModuleLevelBuilder(true), new HaxeModuleLevelBuilder(false));
  }
}
