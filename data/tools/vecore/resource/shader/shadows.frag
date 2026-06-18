
varying  vec2 textureCoordinate;
 
 uniform sampler2D inputImageTexture;
 
uniform float shadowFactor;
 
 
 void main()
 {
	vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
	if(abs(shadowFactor) > 0.0)
    {



        //阴影
		float shadows = shadowFactor;
        float highlights = 1.0;
        const mediump vec3 luminanceHighWeighting = vec3(0.3, 0.3, 0.3);
        float luminance = dot(textureColor.rgb, luminanceHighWeighting);
        mediump float shadow = clamp((pow(luminance, 1.0/(shadows+1.0)) + (-0.76)*pow(luminance, 2.0/(shadows+1.0))) - luminance, 0.0, 1.0);
        mediump float highlight = clamp((1.0 - (pow(1.0-luminance, 1.0/(2.0-highlights)) + (-0.8)*pow(1.0-luminance, 2.0/(2.0-highlights)))) - luminance, -1.0, 0.0);
        gl_FragColor.rgb = vec3(0.0, 0.0, 0.0) + ((luminance + shadow + highlight) - 0.0) * ((textureColor.rgb - vec3(0.0, 0.0, 0.0))/(luminance - 0.0));
		gl_FragColor.a = 1.0;
	}
else
    gl_FragColor = textureColor;
	
	
	
}