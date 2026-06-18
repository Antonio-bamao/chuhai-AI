
 varying  vec2 textureCoordinate;
 
 uniform sampler2D inputImageTexture;
 uniform sampler2D inputHighlightMin; 	//高光
 uniform sampler2D inputHighlightMax; 	//高光
 uniform float highlightIntensity; 		//-1.0 ~ 1.0
 
 

 vec4 baseFun(vec4 baseColor, sampler2D minTex, sampler2D maxTex, float intensity)
 {
     float slider_progress = abs(intensity)*0.7;

     // vec2 uv = vTextureCoord;
     // vec4 curColor = texture2D(inputImageTexture, uv);
     vec4 curColor = baseColor;
     vec4 textureColor = curColor;
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

     vec4 newColor = vec4(0.0);
     if (intensity < 0.0){
         vec4 newColor1 = texture2D(minTex, texPos1);
         vec4 newColor2 = texture2D(minTex, texPos2);
         newColor = mix(newColor1, newColor2, alpha);
     } else {
         vec4 newColor1 = texture2D(maxTex, texPos1);
         vec4 newColor2 = texture2D(maxTex, texPos2);
         newColor = mix(newColor1, newColor2, alpha);
     }
     newColor = mix(curColor,newColor,slider_progress);
     newColor.a = curColor.a;
     return newColor;
 }

 void main()
 {
	vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
	if(abs(highlightIntensity) > 0.0)
    {
        //高光
        textureColor = baseFun(textureColor, inputHighlightMin, inputHighlightMax, highlightIntensity);
		gl_FragColor = textureColor;
	}
	else
		gl_FragColor = textureColor;
	
}