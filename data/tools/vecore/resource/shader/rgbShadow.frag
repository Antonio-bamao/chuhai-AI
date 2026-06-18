precision highp float;

 varying vec2 textureCoordinate;
 uniform sampler2D inputImageTexture;

 uniform sampler2D texture_Shadow1;
 uniform sampler2D texture_Shadow2;
 uniform sampler2D texture_Shadow3;
 uniform sampler2D texture_Shadow4;
 uniform sampler2D texture_Shadow5;
 uniform sampler2D texture_Shadow6;
 
 uniform int shadowMode;
 uniform float tingeSptShadowAlpha;

 vec4 tingeSptLightShadow(vec4 textureColor, sampler2D lut, float alpha)
 {
     float blueColor = textureColor.b * 63.0;
     vec2 quad1;
     float fb = floor(blueColor);
     quad1.y = floor(fb * 0.125);
     quad1.x = fb - quad1.y * 8.0;

     vec2 quad2;
     float cb = ceil(blueColor);
     quad2.y = floor(cb * 0.125);
     quad2.x = cb - quad2.y * 8.0;

     vec2 texPos1;
     texPos1.xy = quad1.xy * 0.125 + 0.12304688 * textureColor.rg + 0.00097656;
     vec2 texPos2;
     texPos2.xy = quad2.xy * 0.125 + 0.12304688 * textureColor.rg + 0.00097656;

     vec4 newColor1 = texture2D(lut, texPos1);
     vec4 newColor2 = texture2D(lut, texPos2);

     vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
     newColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), alpha);
     return newColor;
 }

 void main()
 {
     vec4 srcColor = texture2D(inputImageTexture, textureCoordinate);

     // 色调分离阴影
     if (tingeSptShadowAlpha != 0.0)
     {
         if (shadowMode==0){
             srcColor = tingeSptLightShadow(srcColor, texture_Shadow1, tingeSptShadowAlpha);
         } else if (shadowMode==1){
             srcColor = tingeSptLightShadow(srcColor, texture_Shadow2, tingeSptShadowAlpha);
         } else if (shadowMode==2){
             srcColor = tingeSptLightShadow(srcColor, texture_Shadow3, tingeSptShadowAlpha);
         } else if (shadowMode==3){
             srcColor = tingeSptLightShadow(srcColor, texture_Shadow4, tingeSptShadowAlpha);
         } else if (shadowMode==4){
             srcColor = tingeSptLightShadow(srcColor, texture_Shadow5, tingeSptShadowAlpha);
         } else if (shadowMode==5){
             srcColor = tingeSptLightShadow(srcColor, texture_Shadow6, tingeSptShadowAlpha);
         }
     }
     gl_FragColor = srcColor;
 }