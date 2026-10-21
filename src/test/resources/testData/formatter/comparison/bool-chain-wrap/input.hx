class Main {
	static function main() {
		var snapping = 1;
		var placement = {
			a: 0.0,
			b: 0.0,
			c: 0.0,
			d: 0.0,
			tx: 0.0,
			ty: 0.0
		};
		if (snapping == 2
			|| (snapping == 1 && placement.b == 0 && placement.c == 0 && (placement.a < 1.001 && placement.a > 0.999) && (placement.d < 1.001 && placement.d > 0.999))) {
			placement.tx = 1;
		}
		if (snapping == 2 || (snapping == 1 && placement.b == 0 && placement.c == 0)) {
			placement.ty = 1;
		}
		if (placement.a < 1.001 && placement.a > 0.999 && placement.b == 0 && placement.c == 0 && placement.d < 1.001 && placement.tx == 0) {
			placement.ty = 2;
		}
		if (snapping == 2 && placement.b == 0) {
			placement.ty = 3;
		}
		var buffer = {width: 0, height: 0, renderContextHandle: 0};
		var renderContextHandle = 1;
		if (buffer == null || buffer.width < placement.tx || buffer.height < placement.ty || buffer.renderContextHandle != renderContextHandle) {
			placement.tx = 4;
		}
		if (buffer.width < placement.tx || buffer.renderContextHandle != renderContextHandle) {
			placement.ty = 5;
		}
		if (buffer != null) {
			if (buffer.width > 0) {
				if (buffer.height > 0) {
					var needsFill = (displayObject.opaqueBackground != null && (bitmapWidth != filterWidth || bitmapHeight != filterHeight));
					var fillColor = displayObject.opaqueBackground != null ? (0xFF << 24) | displayObject.opaqueBackground : 0;
				}
			}
		}
	}
}
