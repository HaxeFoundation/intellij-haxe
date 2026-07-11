package adapter;

import debug.session.DebugEvent;


/**
 * Something for the worker (dispatcher) thread to process. Client frames and
 * session events funnel through one queue so the dispatcher stays single-
 * threaded and message ordering is total.
 */
enum WorkerMessage {
	ClientPayload(payload:String);
	FromSession(event:DebugEvent);
	ClientEof;
}
