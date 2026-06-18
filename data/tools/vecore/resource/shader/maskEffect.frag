uniform sampler2D texture1;
uniform sampler2D texture2;
uniform int blendType;
varying vec2 textureCoordinate;

int MEDIA_EFFECT_MASK = 1;
int MEDIA_EFFECT_GRAY = 2;		//not support
int MEDIA_EFFECT_GREEN = 3;		//not support
int MEDIA_EFFECT_CHROME = 4;	//not support


void main()
{
	if (blendType == MEDIA_EFFECT_MASK)
	{
		vec4 textureColor1 = texture2D(texture1, textureCoordinate);
		vec4 textureColor2 = texture2D(texture2, textureCoordinate);
		
		vec2 newCoordinate1 = vec2(textureCoordinate.x/2.0,textureCoordinate.y);
		vec4 t1 = texture2D(texture2,newCoordinate1); // view
		vec2 newCoordinate2 = newCoordinate1 + vec2(0.5,0.0);
		vec4 t2 = texture2D(texture2, newCoordinate2);//alpha
		vec4 t3 = texture2D(texture1, textureCoordinate);//source
		float newAlpha = dot(t2.rgb, vec3(0.33333334)) * t2.a;
		vec4 t = vec4(t1.rgb,newAlpha); //compositor 
		//mix(a,b,v) = (1-v)a + v(b)
		gl_FragColor = vec4(mix(t3.rgb,t.rgb,t.a),t3.a);
		
	}
	else if (blendType == MEDIA_EFFECT_GRAY)
	{

	}
	else if (blendType == MEDIA_EFFECT_GREEN)
	{

	}
	else if (blendType == MEDIA_EFFECT_CHROME)
	{

	}
	else
		gl_FragColor = texture2D(texture1, textureCoordinate);
}