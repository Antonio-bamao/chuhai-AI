
varying  vec2 textureCoordinate;
 
uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;
uniform float fadeFactor;
 
 
 void main()
 {
	vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
	if(fadeFactor > 0.0)
    {
		//褪色
         vec4 colorR = texture2D(inputImageTexture2, vec2(textureColor.r + 0.5/256.0 ,0.5));
         vec4 colorG = texture2D(inputImageTexture2, vec2(textureColor.g + 0.5/256.0 ,0.5));
         vec4 colorB = texture2D(inputImageTexture2, vec2(textureColor.b + 0.5/256.0 ,0.5));
         vec4 newColor = vec4(colorR.r,colorG.g,colorB.b,1.0);
         gl_FragColor = vec4(mix(textureColor.rgb, newColor.rgb, fadeFactor),1.0);
	}
	else
		gl_FragColor = textureColor;
	
}