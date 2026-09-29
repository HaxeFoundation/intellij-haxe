package com.intellij.plugins.haxe.profiler.bridge.tracy;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationTypeBase;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The "hxcpp Tracy" entry of the IU Run-with-Profiler executor: builds the
 * program with hxcpp's Tracy client compiled in ({@code -D HXCPP_TRACY},
 * exact instrumented zones for every haxe function), captures the stream
 * with the pure-Java receiver and saves it as a session file. Needs an
 * hxcpp version that bundles Tracy (haxe 5 era).
 */
public class HaxeHxcppTracyProfilerConfigurationType
  extends HaxeProfilerConfigurationTypeBase<HaxeHxcppTracyProfilerConfigurationState> {

  public static final String ID = "HaxeHxcppTracyProfilerConfiguration";
  private static final String COMPRESSION_ATTRIBUTE = "compressionLevel";
  private static final String CAPTURE_MEMORY_ATTRIBUTE = "captureMemory";
  private static final String COLLECT_PROCESS_CPU_ATTRIBUTE = "collectProcessCpu";
  /** The pinned protocol's wire number; absent or unparsable = detect. */
  private static final String PROTOCOL_ATTRIBUTE = "protocolVersion";

  @Override
  public @NotNull String getId() {
    return ID;
  }

  @Override
  public @NotNull String getDisplayName() {
    return HaxeProfilerBundle.message("haxe.profiler.hxcpp.tracy.configuration.name");
  }

  /** The user-visible name of a protocol version ("76 (Tracy 0.13)"), for the settings dropdown and the capture notification. */
  @NotNull
  static String protocolLabel(@NotNull TracyProtocolVersion version) {
    return HaxeProfilerBundle.message("haxe.profiler.tracy.protocol.version", version.wire(), version.tracyRelease());
  }

  @Override
  protected @NotNull HaxeProfilableRunConfiguration.Lane lane() {
    return HaxeProfilableRunConfiguration.Lane.HXCPP;
  }

  @Override
  protected @NotNull String stateElementName() {
    return "haxeHxcppTracyProfiler";
  }

  @Override
  public @NotNull HaxeHxcppTracyProfilerConfigurationState getTemplateState() {
    return new HaxeHxcppTracyProfilerConfigurationState(getDisplayName());
  }

  @Override
  public @NotNull HaxeHxcppTracyProfilerConfigurationState copyState(@NotNull HaxeHxcppTracyProfilerConfigurationState state) {
    HaxeHxcppTracyProfilerConfigurationState copy = new HaxeHxcppTracyProfilerConfigurationState(state.getDisplayName());
    copy.setCompressionLevel(state.getCompressionLevel());
    copy.setCaptureMemory(state.isCaptureMemory());
    copy.setCollectProcessCpu(state.isCollectProcessCpu());
    copy.setPinnedProtocol(state.getPinnedProtocol());
    return copy;
  }

  @Override
  protected void readSettings(@NotNull HaxeHxcppTracyProfilerConfigurationState state, @NotNull Element element) {
    String level = element.getAttributeValue(COMPRESSION_ATTRIBUTE);
    state.setCompressionLevel(StringUtil.parseInt(level, state.getCompressionLevel()));
    // memory capture defaults ON, process CPU (elevated launch) OFF
    state.setCaptureMemory(!"false".equals(element.getAttributeValue(CAPTURE_MEMORY_ATTRIBUTE)));
    state.setCollectProcessCpu("true".equals(element.getAttributeValue(COLLECT_PROCESS_CPU_ATTRIBUTE)));
    state.setPinnedProtocol(pinnedProtocol(element.getAttributeValue(PROTOCOL_ATTRIBUTE)));
  }

  @Override
  protected void writeSettings(@NotNull HaxeHxcppTracyProfilerConfigurationState state, @NotNull Element element) {
    element.setAttribute(COMPRESSION_ATTRIBUTE, String.valueOf(state.getCompressionLevel()));
    element.setAttribute(CAPTURE_MEMORY_ATTRIBUTE, String.valueOf(state.isCaptureMemory()));
    element.setAttribute(COLLECT_PROCESS_CPU_ATTRIBUTE, String.valueOf(state.isCollectProcessCpu()));
    TracyProtocolVersion pinned = state.getPinnedProtocol();
    if (pinned != null) {
      element.setAttribute(PROTOCOL_ATTRIBUTE, String.valueOf(pinned.wire()));
    }
  }

  @Nullable
  private static TracyProtocolVersion pinnedProtocol(@Nullable String attribute) {
    int wire = StringUtil.parseInt(attribute, -1);
    return wire < 0 ? null : TracyProtocolVersion.of(wire);
  }

  @Override
  public @NotNull UnnamedConfigurable createConfigurable(@NotNull HaxeHxcppTracyProfilerConfigurationState state) {
    return new HaxeHxcppTracyProfilerConfigurable(state);
  }
}
