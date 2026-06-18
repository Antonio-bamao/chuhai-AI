uniform sampler2D texture1;
uniform sampler2D texture2;
uniform int blendType;
varying vec2 textureCoordinate;
#if 0
int MEDIA_BLEND_DARKEN = 1;
int MEDIA_BLEND_MULTIPLY = 2;
int MEDIA_BLEND_COLOR_BURN = 3;
int MEDIA_BLEND_LINER_BURN = 4;
int MEDIA_BLEND_DARKER_COLOR = 5;
int MEDIA_BLEND_LIGHTEN = 6;
int MEDIA_BLEND_SCREEN = 7;
int MEDIA_BLEND_COLOR_DODGE = 8;
int MEDIA_BLEND_LINER_DODGE = 9;
int MEDIA_BLEND_LIGHTER_COLOR = 10;
int MEDIA_BLEND_OERLAY = 11;
int MEDIA_BLEND_SOFT_LIGHT = 12;
int MEDIA_BLEND_HARD_LIGHT = 13;
int MEDIA_BLEND_VIVID_LIGHT = 14;
int MEDIA_BLEND_LINER_LIGHT = 15;
int MEDIA_BLEND_PIN_LIGHT = 16;
int MEDIA_BLEND_HARD_MIX = 17;
int MEDIA_BLEND_DIFF = 18;
int MEDIA_BLEND_EXCLUSION = 19;
int MEDIA_BLEND_SUBTRACT = 20;
int MEDIA_BLEND_PIDE = 21;
int MEDIA_BLEND_NUM = 22;
#else
int MEDIA_BLEND_DARKEN = 3;
int MEDIA_BLEND_SCREEN = 4;
int MEDIA_BLEND_OERLAY = 5;
int MEDIA_BLEND_MULTIPLY = 6;
int MEDIA_BLEND_LIGHTEN = 7;
int MEDIA_BLEND_HARD_LIGHT = 8;
int MEDIA_BLEND_SOFT_LIGHT = 9;
int MEDIA_BLEND_LINER_BURN = 10;
int MEDIA_BLEND_COLOR_BURN = 11;
int MEDIA_BLEND_COLOR_DODGE = 12;
int MEDIA_BLEND_MASK = 13;		//not support
int MEDIA_BLEND_LINER_DODGE = 14;		//not support
int MEDIA_BLEND_LIGHTER_COLOR = 15;		//not support
int MEDIA_BLEND_VIVID_LIGHT = 16;		//not support
int MEDIA_BLEND_LINER_LIGHT = 17;		//not support
int MEDIA_BLEND_PIN_LIGHT = 18;			//not support
int MEDIA_BLEND_HARD_MIX = 19;			//not support
int MEDIA_BLEND_DIFF = 20;				//not support
int MEDIA_BLEND_EXCLUSION = 21;			//not support
int MEDIA_BLEND_SUBTRACT = 22;			//not support
int MEDIA_BLEND_PIDE = 23;				//not support
int MEDIA_BLEND_NUM = 24;				//not support
#endif

void main()
{
	if (blendType == MEDIA_BLEND_DARKEN)
	{
		vec4 base = texture2D(texture1, textureCoordinate);
		vec4 overlayer = texture2D(texture2, textureCoordinate);

		gl_FragColor = vec4(min(overlayer.rgb * base.a, base.rgb * overlayer.a) + overlayer.rgb * (1.0 - base.a) + base.rgb * (1.0 - overlayer.a), 1.0);

	}
	else if (blendType == MEDIA_BLEND_MULTIPLY)
	{
		vec4 base = texture2D(texture1, textureCoordinate);
		vec4 overlayer = texture2D(texture2, textureCoordinate);

		gl_FragColor = overlayer * base + overlayer * (1.0 - base.a) + base * (1.0 - overlayer.a);
	}
	else if (blendType == MEDIA_BLEND_COLOR_BURN)
	{
		vec4 textureColor = texture2D(texture1, textureCoordinate);
		vec4 textureColor2 = texture2D(texture2, textureCoordinate);
		vec4 whiteColor = vec4(1.0);

		if (textureColor2.a == 0.0)
			gl_FragColor = textureColor;
		else
			gl_FragColor = whiteColor - (whiteColor - textureColor) / textureColor2;
	}
	else if (blendType == MEDIA_BLEND_LINER_BURN)
	{
		vec4 textureColor = texture2D(texture1, textureCoordinate);
		vec4 textureColor2 = texture2D(texture2, textureCoordinate);
		vec3 d = max(textureColor.rgb + textureColor2.rgb - vec3(1.0), vec3(0.0, 0.0, 0.0));
		if (textureColor2.a == 0.0)
			gl_FragColor = textureColor;
		else
			gl_FragColor = vec4(clamp(textureColor.rgb + textureColor2.rgb - vec3(1.0), vec3(0.0), vec3(1.0)), textureColor.a);
	}
	else if (blendType == MEDIA_BLEND_LIGHTEN)
	{
		lowp vec4 textureColor = texture2D(texture1, textureCoordinate);
		lowp vec4 textureColor2 = texture2D(texture2, textureCoordinate);

		gl_FragColor = max(textureColor, textureColor2);

	}
	else if (blendType == MEDIA_BLEND_SCREEN)
	{
		mediump vec4 textureColor = texture2D(texture1, textureCoordinate);
		mediump vec4 textureColor2 = texture2D(texture2, textureCoordinate);
		mediump vec4 whiteColor = vec4(1.0);

		gl_FragColor = whiteColor - ((whiteColor - textureColor2) * (whiteColor - textureColor));
	}
	else if (blendType == MEDIA_BLEND_COLOR_DODGE)
	{
		vec4 base = texture2D(texture1, textureCoordinate);
		vec4 overlay = texture2D(texture2, textureCoordinate);

		vec3 baseOverlayAlphaProduct = vec3(overlay.a * base.a);
		vec3 rightHandProduct = overlay.rgb * (1.0 - base.a) + base.rgb * (1.0 - overlay.a);

		vec3 firstBlendColor = baseOverlayAlphaProduct + rightHandProduct;
		vec3 overlayRGB = clamp((overlay.rgb / clamp(overlay.a, 0.01, 1.0)) * step(0.0, overlay.a), 0.0, 0.99);

		vec3 secondBlendColor = (base.rgb * overlay.a) / (1.0 - overlayRGB) + rightHandProduct;

		vec3 colorChoice = step((overlay.rgb * base.a + base.rgb * overlay.a), baseOverlayAlphaProduct);

		gl_FragColor = vec4(mix(firstBlendColor, secondBlendColor, colorChoice), 1.0);
	}
	else if (blendType == MEDIA_BLEND_LINER_DODGE)
	{

	}
	else if (blendType == MEDIA_BLEND_LIGHTER_COLOR)
	{

	}
	else if (blendType == MEDIA_BLEND_OERLAY)
	{
		mediump vec4 base = texture2D(texture1, textureCoordinate);
		mediump vec4 overlay = texture2D(texture2, textureCoordinate);

		mediump float ra;
		if (2.0 * base.r < base.a) {
			ra = 2.0 * overlay.r * base.r + overlay.r * (1.0 - base.a) + base.r * (1.0 - overlay.a);
		}
		else {
			ra = overlay.a * base.a - 2.0 * (base.a - base.r) * (overlay.a - overlay.r) + overlay.r * (1.0 - base.a) + base.r * (1.0 - overlay.a);
		}

		mediump float ga;
		if (2.0 * base.g < base.a) {
			ga = 2.0 * overlay.g * base.g + overlay.g * (1.0 - base.a) + base.g * (1.0 - overlay.a);
		}
		else {
			ga = overlay.a * base.a - 2.0 * (base.a - base.g) * (overlay.a - overlay.g) + overlay.g * (1.0 - base.a) + base.g * (1.0 - overlay.a);
		}

		mediump float ba;
		if (2.0 * base.b < base.a) {
			ba = 2.0 * overlay.b * base.b + overlay.b * (1.0 - base.a) + base.b * (1.0 - overlay.a);
		}
		else {
			ba = overlay.a * base.a - 2.0 * (base.a - base.b) * (overlay.a - overlay.b) + overlay.b * (1.0 - base.a) + base.b * (1.0 - overlay.a);
		}

		gl_FragColor = vec4(ra, ga, ba, 1.0);
	}
	else if (blendType == MEDIA_BLEND_SOFT_LIGHT)
	{
		mediump vec4 base = texture2D(texture1, textureCoordinate);
		mediump vec4 overlay = texture2D(texture2, textureCoordinate);

		lowp float alphaDivisor = base.a + step(base.a, 0.0); // Protect against a divide-by-zero blacking out things in the output
		gl_FragColor = base * (overlay.a * (base / alphaDivisor) + (2.0 * overlay * (1.0 - (base / alphaDivisor)))) + overlay * (1.0 - base.a) + base * (1.0 - overlay.a);
	}
	else if (blendType == MEDIA_BLEND_HARD_LIGHT)
	{
		mediump vec4 base = texture2D(texture1, textureCoordinate);
		mediump vec4 overlay = texture2D(texture2, textureCoordinate);

		highp float ra;
		if (2.0 * overlay.r < overlay.a) {
			ra = 2.0 * overlay.r * base.r + overlay.r * (1.0 - base.a) + base.r * (1.0 - overlay.a);
		}
		else {
			ra = overlay.a * base.a - 2.0 * (base.a - base.r) * (overlay.a - overlay.r) + overlay.r * (1.0 - base.a) + base.r * (1.0 - overlay.a);
		}

		highp float ga;
		if (2.0 * overlay.g < overlay.a) {
			ga = 2.0 * overlay.g * base.g + overlay.g * (1.0 - base.a) + base.g * (1.0 - overlay.a);
		}
		else {
			ga = overlay.a * base.a - 2.0 * (base.a - base.g) * (overlay.a - overlay.g) + overlay.g * (1.0 - base.a) + base.g * (1.0 - overlay.a);
		}

		highp float ba;
		if (2.0 * overlay.b < overlay.a) {
			ba = 2.0 * overlay.b * base.b + overlay.b * (1.0 - base.a) + base.b * (1.0 - overlay.a);
		}
		else {
			ba = overlay.a * base.a - 2.0 * (base.a - base.b) * (overlay.a - overlay.b) + overlay.b * (1.0 - base.a) + base.b * (1.0 - overlay.a);
		}

		gl_FragColor = vec4(ra, ga, ba, 1.0);
	}
	else if (blendType == MEDIA_BLEND_VIVID_LIGHT)
	{

	}
	else if (blendType == MEDIA_BLEND_LINER_LIGHT)
	{

	}
	else if (blendType == MEDIA_BLEND_PIN_LIGHT)
	{

	}
	else if (blendType == MEDIA_BLEND_HARD_MIX)
	{

	}
	else if (blendType == MEDIA_BLEND_DIFF)
	{

	}
	else if (blendType == MEDIA_BLEND_EXCLUSION)
	{

	}
	else if (blendType == MEDIA_BLEND_SUBTRACT)
	{

	}
	else if (blendType == MEDIA_BLEND_PIDE)
	{

	}
	else if (blendType == MEDIA_BLEND_NUM)
	{


	}
	else
		gl_FragColor = texture2D(texture1, textureCoordinate);
}