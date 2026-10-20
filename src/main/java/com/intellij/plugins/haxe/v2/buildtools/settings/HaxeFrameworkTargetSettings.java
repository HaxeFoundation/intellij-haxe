package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.FrameworkTarget;
import com.intellij.plugins.haxe.config.LimeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.config.OpenFLTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The configurable target lists of the lime/openfl/nme build systems
 * (Settings | Haxe | Frameworks). The built-in enums ({@link LimeTarget},
 * {@link OpenFLTarget}, {@link NMETarget}) seed the lists; users add, edit or
 * remove entries — e.g. NDA console targets or arch-flag variants — and the
 * tool window's target selector, the project wizard and every command using a
 * target flag consume THIS list. Application-level: targets describe the
 * machine's toolchains, not one project.
 */
@Service(Service.Level.APP)
@State(name = "HaxeFrameworkTargets", storages = @Storage("haxe-framework-targets.xml"))
public final class HaxeFrameworkTargetSettings implements PersistentStateComponent<HaxeFrameworkTargetSettings.PersistedState> {

  /** The build systems whose target list is configurable. */
  public enum Framework {LIME, OPENFL, NME}

  /// One configured target: the display name (also the id target selections
  /// store), the haxe backend it compiles through (informational; null when
  /// unknown), and the tool's CLI flags — the FIRST flag is the target word
  /// (`lime build <flag>`), the rest ride along (`-64`).
  public record TargetDefinition(@NotNull String name, @Nullable HaxeTarget target, @NotNull List<String> flags) {
    @NotNull
    public String primaryFlag() {
      return flags.isEmpty() ? "" : flags.getFirst();
    }
  }

  /** Serialized row; flags space-separated. */
  public static class TargetRow {
    public String name = "";
    public String haxeTarget = "";
    public String flags = "";
  }

  public static class PersistedState {
    public List<TargetRow> lime = new ArrayList<>();
    public List<TargetRow> openfl = new ArrayList<>();
    public List<TargetRow> nme = new ArrayList<>();
  }

  private PersistedState state = new PersistedState();

  @NotNull
  public static HaxeFrameworkTargetSettings getInstance() {
    return ApplicationManager.getApplication().getService(HaxeFrameworkTargetSettings.class);
  }

  /** The framework's configured targets; the built-in defaults while nothing is stored (or every row was removed). */
  @NotNull
  public List<TargetDefinition> getTargets(@NotNull Framework framework) {
    List<TargetRow> rows = rowsOf(framework);
    if (rows.isEmpty()) {
      return defaults(framework);
    }
    return rows.stream()
      .map(HaxeFrameworkTargetSettings::toDefinition)
      .toList();
  }

  public void setTargets(@NotNull Framework framework, @NotNull List<TargetDefinition> targets) {
    List<TargetRow> rows = targets.stream()
      .map(HaxeFrameworkTargetSettings::toRow)
      .collect(Collectors.toCollection(ArrayList::new));
    storeRows(framework, rows);
  }

  /** The stored rows as the settings table edits them (the defaults rendered as rows when nothing is stored). */
  @NotNull
  public List<TargetRow> getTargetRows(@NotNull Framework framework) {
    List<TargetRow> rows = rowsOf(framework);
    if (rows.isEmpty()) {
      return defaultRows(framework);
    }
    return rows.stream()
      .map(HaxeFrameworkTargetSettings::copyRow)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  /** The defaults rendered as editable rows — what the table's Restore Defaults loads. */
  @NotNull
  public static List<TargetRow> defaultRows(@NotNull Framework framework) {
    return defaults(framework).stream()
      .map(HaxeFrameworkTargetSettings::toRow)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  /** Stores the settings table's rows; rows with neither name nor flags are dropped. */
  public void setTargetRows(@NotNull Framework framework, @NotNull List<TargetRow> rows) {
    List<TargetRow> kept = rows.stream()
      .filter(row -> !StringUtil.isEmptyOrSpaces(row.name) || !StringUtil.isEmptyOrSpaces(row.flags))
      .map(HaxeFrameworkTargetSettings::copyRow)
      .collect(Collectors.toCollection(ArrayList::new));
    storeRows(framework, kept);
  }

  /** The name of the framework's built-in default target (each target enum declares its own {@code DEFAULT}). */
  @NotNull
  public static String defaultTargetName(@NotNull Framework framework) {
    return switch (framework) {
      case LIME -> LimeTarget.DEFAULT.toString();
      case OPENFL -> OpenFLTarget.DEFAULT.toString();
      case NME -> NMETarget.DEFAULT.toString();
    };
  }

  /** The built-in list a framework starts from — and returns to when every row is removed. */
  @NotNull
  public static List<TargetDefinition> defaults(@NotNull Framework framework) {
    FrameworkTarget[] targets = switch (framework) {
      case LIME -> LimeTarget.values();
      case OPENFL -> OpenFLTarget.values();
      case NME -> NMETarget.values();
    };
    return Arrays.stream(targets)
      .map(HaxeFrameworkTargetSettings::definitionOf)
      .toList();
  }

  @NotNull
  private static TargetDefinition definitionOf(@NotNull FrameworkTarget target) {
    return new TargetDefinition(target.toString(), target.getOutputTarget(), List.of(target.getFlags()));
  }

  @NotNull
  private List<TargetRow> rowsOf(@NotNull Framework framework) {
    return switch (framework) {
      case LIME -> state.lime;
      case OPENFL -> state.openfl;
      case NME -> state.nme;
    };
  }

  private void storeRows(@NotNull Framework framework, @NotNull List<TargetRow> rows) {
    switch (framework) {
      case LIME -> state.lime = rows;
      case OPENFL -> state.openfl = rows;
      case NME -> state.nme = rows;
    }
  }

  @NotNull
  private static TargetDefinition toDefinition(@NotNull TargetRow row) {
    // flags are stored space-separated; any run of whitespace splits them
    List<String> flags = StringUtil.isEmptyOrSpaces(row.flags) ? List.of()
                                                               : List.of(row.flags.trim().split("\\s+"));
    return new TargetDefinition(StringUtil.notNullize(row.name), parseHaxeTarget(row.haxeTarget), flags);
  }

  @NotNull
  private static TargetRow toRow(@NotNull TargetDefinition definition) {
    TargetRow row = new TargetRow();
    row.name = definition.name();
    row.haxeTarget = definition.target() != null ? definition.target().name() : "";
    row.flags = String.join(" ", definition.flags());
    return row;
  }

  @NotNull
  private static TargetRow copyRow(@NotNull TargetRow row) {
    TargetRow copy = new TargetRow();
    copy.name = StringUtil.notNullize(row.name).trim();
    copy.haxeTarget = StringUtil.notNullize(row.haxeTarget).trim();
    copy.flags = StringUtil.notNullize(row.flags).trim();
    return copy;
  }

  @Nullable
  private static HaxeTarget parseHaxeTarget(@Nullable String name) {
    if (name == null || name.isBlank()) return null;
    try {
      return HaxeTarget.valueOf(name);
    }
    catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  public @NotNull PersistedState getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull PersistedState state) {
    this.state = state;
  }
}
