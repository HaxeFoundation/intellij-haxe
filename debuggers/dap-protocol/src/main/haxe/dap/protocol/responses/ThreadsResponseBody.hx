package dap.protocol.responses;
import dap.protocol.ThreadInfo;

/**
	Body of the "threads" response.
**/
typedef ThreadsResponseBody = {
	var threads:Array<ThreadInfo>;
}
