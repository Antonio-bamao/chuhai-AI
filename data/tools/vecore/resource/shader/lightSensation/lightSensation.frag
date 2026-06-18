
 varying  vec2 textureCoordinate;
 
 uniform sampler2D inputImageTexture;
 uniform sampler2D inputLightSensationMax; //光感
 uniform sampler2D inputLightSensationMin; //光感
 uniform float lightSensation;     //-1.0 ~ 1.0 
 
 void main()
 {
	vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
	if(abs(lightSensation) > 0.0)
     {
        //光感
        float intensity = lightSensation;
        float slider_progress = abs(intensity);

        // vec2 uv = vTextureCoord;
        vec4 curColor = textureColor;
        float blueColor = curColor.b * (17.0 - 1.0);
        vec2 standardTableSize = vec2(289.0, 17.0);
        vec2 pixelSize = 1.0 / standardTableSize;
        vec2 quad1 = vec2(0.0);
        quad1.y = floor(floor(blueColor) / 17.0);
        quad1.x = floor(blueColor) - (quad1.y * 1.0);
        vec2 quad2;
        quad2.y = floor(ceil(blueColor) / 17.0);
        quad2.x = ceil(blueColor) - (quad2.y * 1.0);
        vec2 texPos1;
        texPos1.x = (quad1.x * 1.0 / 17.0) + 0.5 / standardTableSize.x + ((1.0 / 17.0 - 1.0 / standardTableSize.x) * textureColor.r);
        texPos1.y = (quad1.y * 1.0 / 1.0) + 0.5 / standardTableSize.y +((1.0 / 1.0 - 1.0 / standardTableSize.y) * textureColor.g);
        //                    "    texPos1.x = 1.0 - texPos1.x;
        //                    "    texPos1.y = 1.0 - texPos1.y;
        vec2 texPos2;
        texPos2.x = (quad2.x * 1.0 / 17.0) + 0.5 / standardTableSize.x + ((1.0 / 17.0 - 1.0 / standardTableSize.x) * textureColor.r);
        texPos2.y = (quad2.y * 1.0 / 1.0) + 0.5 / standardTableSize.y +((1.0 / 1.0 - 1.0 / standardTableSize.y) * textureColor.g);
        //                    "    texPos2.x = 1.0 - texPos2.x;
        //                    "    texPos2.y = 1.0 - texPos2.y;
        float alpha = fract(blueColor);

        vec4 newColor = vec4(0.0, 0.0, 0.0, 1.0);
        if (intensity <= 0.0){

            vec4 newColor1 = texture2D(inputLightSensationMin, texPos1);
            vec4 newColor2 = texture2D(inputLightSensationMin, texPos2);
            newColor = mix(newColor1, newColor2, alpha);
        } else {
            vec4 newColor1 = texture2D(inputLightSensationMax, texPos1);
            vec4 newColor2 = texture2D(inputLightSensationMax, texPos2);
            newColor = mix(newColor1, newColor2, alpha);
        }

        newColor = mix(curColor,newColor,slider_progress);
        //
        gl_FragColor = newColor;
        gl_FragColor.a = curColor.a;
     }
	else
		gl_FragColor = textureColor;
	
}