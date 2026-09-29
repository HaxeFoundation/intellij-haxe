class Main {
	static function main() {
		var point = {x: 1, y: 2};
		var config = {host: "localhost", port: 8080, secure: false, retries: 3, timeoutMillis: 30000, userAgent: "example-agent/1.0", followRedirects: true, keepAlive: true, maxConnections: 16, proxyHost: "proxy.internal"};
		trace(point, config);
	}
}
