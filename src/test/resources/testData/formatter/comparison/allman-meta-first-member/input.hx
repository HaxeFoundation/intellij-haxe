package app.render;

#if !flash
import app.render.Surface;
import app.render.BaseShader;

#if !app_debug
@:fileXml('tags="haxe,release"')
@:noDebug
#end
@:access(app.render.Surface)
@SuppressWarnings("checkstyle:FieldDocComment")
class TintShader extends BaseShader {

	@:glFragmentSource("varying vec2 uv;
		uniform sampler2D source;
		void main(void) {
			gl_FragColor = texture2D(source, uv);
		}")
	public function new() {
		super();
	}

}
#end

class Plain {

	@:keep
	public var value = 1;

}
