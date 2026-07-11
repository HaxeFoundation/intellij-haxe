package debug;

/**
 * The kind of source-level step requested by the client.
 */
enum StepMode {
	Next; // step over
	StepIn;
	StepOut;
}
