
varying  vec2 textureCoordinate;
 
 uniform sampler2D inputImageTexture;
 
uniform float graininessFactor;
 
 
 void main()
 {
	vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
	if(abs(graininessFactor) > 0.0)
    {
        //阴影
		 float strengthPower = graininessFactor*2.0;
        float strength = 16.0 * strengthPower;
        float x = (textureCoordinate.x + 4.0) * (textureCoordinate.y + 4.0) * ((mod(1.0,100.0)+3.0)* 10.0);
        vec4 grain =vec4(mod((mod(x, 13.0) + 1.0) * (mod(x, 123.0) + 1.0), 0.01) - 0.005) *strength;
        gl_FragColor = textureColor + grain;
	}
else
    gl_FragColor = textureColor;
	
	
	
}