package com.intellij.plugins.haxe.profiler.tracy.wire;

/**
 * Tracy's queue-item vocabulary across the supported protocol versions,
 * named as in the client's {@code common/TracyQueue.hpp}. The ORDER here is
 * protocol v82's (Tracy 0.14) and carries no wire meaning: each protocol
 * version numbers its items differently and a {@link TracyQueueTable}
 * maps that version's wire ordinals (and item sizes) onto these constants;
 * an older version simply never emits the constants added after it.
 * Payload types carry extra inline data right after the fixed item: a u8,
 * u16 or u32 length plus that many bytes, decoded by the version's
 * {@link TracyWireFormat} (v82 stores some u16 lengths with an offset).
 */
public enum TracyQueueType {
  ZoneText,
  ZoneName,
  Message,
  MessageColor,
  MessageCallstack,
  MessageColorCallstack,
  MessageAppInfo,
  ZoneBeginAllocSrcLoc,
  ZoneBeginAllocSrcLocCallstack,
  CallstackSerial,
  Callstack,
  CallstackAlloc,
  CallstackSample,
  CallstackSample32,
  CallstackSample16,
  CallstackSampleContextSwitch,
  CallstackSampleContextSwitch32,
  CallstackSampleContextSwitch16,
  FrameImage,
  ZoneBegin,
  ZoneBegin32,
  ZoneBegin16,
  ZoneBeginCallstack,
  ZoneBeginCallstack32,
  ZoneBeginCallstack16,
  ZoneEnd,
  ZoneEnd32,
  ZoneEnd16,
  LockWait,
  LockObtain,
  LockRelease,
  LockSharedWait,
  LockSharedObtain,
  LockSharedRelease,
  LockName,
  MemAlloc,
  MemAllocNamed,
  MemFree,
  MemFreeNamed,
  MemAllocCallstack,
  MemAllocCallstackNamed,
  MemFreeCallstack,
  MemFreeCallstackNamed,
  MemDiscard,
  MemDiscardCallstack,
  GpuZoneBegin,
  GpuZoneBeginCallstack,
  GpuZoneBeginAllocSrcLoc,
  GpuZoneBeginAllocSrcLocCallstack,
  GpuZoneEnd,
  GpuZoneBeginSerial,
  GpuZoneBeginCallstackSerial,
  GpuZoneBeginAllocSrcLocSerial,
  GpuZoneBeginAllocSrcLocCallstackSerial,
  GpuZoneEndSerial,
  PlotDataInt,
  PlotDataFloat,
  PlotDataDouble,
  ContextSwitch,
  ThreadWakeup,
  GpuTime,
  GpuContextName,
  GpuAnnotationName,
  CallstackFrameSize,
  SymbolInformation,
  ExternalNameMetadata,
  SymbolCodeMetadata,
  SourceCodeMetadata,
  FiberEnter,
  FiberLeave,
  SectionEnter,
  SectionLeave,
  SectionSetup,
  Terminate,
  KeepAlive,
  ThreadContext,
  GpuCalibration,
  GpuTimeSync,
  Crash,
  CrashReport,
  ZoneValidation,
  ZoneColor,
  ZoneValue,
  FrameMarkMsg,
  FrameMarkMsgStart,
  FrameMarkMsgEnd,
  FrameVsync,
  SourceLocation,
  LockAnnounce,
  LockTerminate,
  LockMark,
  MessageLiteral,
  MessageLiteralColor,
  MessageLiteralCallstack,
  MessageLiteralColorCallstack,
  GpuNewContext,
  CallstackFrame,
  SysTimeReport,
  SysPowerReport,
  TidToPid,
  HwSampleCpuCycle,
  HwSampleInstructionRetired,
  HwSampleCacheReference,
  HwSampleCacheMiss,
  HwSampleBranchRetired,
  HwSampleBranchMiss,
  PlotConfig,
  ParamSetup,
  AckServerQueryNoop,
  AckSourceCodeNotAvailable,
  AckSymbolCodeNotAvailable,
  CpuTopology,
  SingleStringData(Payload.U16),
  SecondStringData(Payload.U16),
  SingleStringData8(Payload.U8),
  SecondStringData8(Payload.U8),
  MemNamePayload,
  ThreadGroupHint,
  GpuZoneAnnotation,
  StringData(Payload.U16),
  ThreadName(Payload.U16),
  PlotName(Payload.U16),
  SourceLocationPayload(Payload.U16),
  CallstackPayload(Payload.U16),
  CallstackAllocPayload(Payload.U16),
  FrameName(Payload.U16),
  FrameImageData(Payload.U32),
  ExternalName(Payload.U16),
  ExternalThreadName(Payload.U16),
  SymbolCode(Payload.U32),
  SourceCode(Payload.U32),
  FiberName(Payload.U16);

  /** The inline data following the fixed item, when any. */
  public enum Payload {
    NONE,
    U8,
    U16,
    U32
  }

  private final Payload payload;

  TracyQueueType() {
    this(Payload.NONE);
  }

  TracyQueueType(Payload payload) {
    this.payload = payload;
  }

  public Payload payload() {
    return payload;
  }
}
