package ijhaxe.dap.protocol.responses;
import ijhaxe.dap.protocol.ThreadInfo;

/**
	Body of the "threads" response.
**/
typedef ThreadsResponseBody = {
	var threads:Array<ThreadInfo>;
}
