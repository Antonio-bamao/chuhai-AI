uniform sampler2D textureY;
uniform sampler2D textureUV;
uniform sampler2D textureRGBA;
uniform sampler2D textureBlendY;
uniform sampler2D textureBlendUV;
uniform sampler2D textureBlendRGBA;
uniform sampler2D texRGBALookup;
uniform sampler2D textOverLay;
uniform float texelWidthOffset;
uniform float texelHeightOffset;
varying vec2 textureCoordinate;
uniform int pixFormat;
uniform int blendPixFormat;
uniform float rgbAlpha;
uniform int beautyLevel;
uniform int dark;
uniform int blendType;
uniform float bright;			  //亮度 -1.0~1.0  (0.0为正常图像)				
uniform float contrast;			  //对比度 0.0~4.0  (1.0为正常图像)	--
uniform float saturation;		  //饱和度 0.0~2.0  (1.0为正常图像)
uniform float vignette;           //0.0 ~ 1.0  (0.0为正常图像)
uniform float sharpness;          //-4.0~4.0   (0.0为正常图像)
uniform float whiteBalance;       //-1~1.0    （0.0为正常图像）
uniform float exposure; 	      //-1~1.0    （0.0为正常图像）
uniform float progress;	
uniform int useGray;		
uniform int lookup;	
uniform float lookupIntensity;
uniform int flip;		

#define RECT_QUAD 0 //任意四边形

#if RECT_QUAD

 uniform highp vec2  SrcQuadrilateral[4];  //源四边形的4个顶点在源纹理上的坐标-纹理的裁剪，顺时针0-1，左上角为0.0
 uniform highp vec2  DstQuadrilateral[4];  //目标四边形的4个顶点在渲染结果(纹理)中的坐标-纹理的显示位置，顺时针0-1，左上角为0.0
 
 varying vec2 quadDir[7];
 varying float recess;
 varying float invmap;
 uniform vec2  DstSinglePixelSize; //单个像素在目标纹理中的大小。例如目标纹理将被渲染为 800*600 的矩形，那么单个像素就是 1/800, 1/600
 
#endif
 


float brightness = 0.45;

 // Values from "Graphics Shaders: Theory and Practice" by Bailey and Cunningham
 const vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);

vec4 SetBright(vec4 inTextColor,float fBright)
{
	return vec4((inTextColor.rgb + vec3(fBright)), inTextColor.w);
	//return vec4((inTextColor.rgb * vec3(fBright)), inTextColor.w);
}
vec4 SetContrast(vec4 inTextColor,float fContrast)
{
	return vec4(((inTextColor.rgb - vec3(0.5)) * fContrast + vec3(0.5)), inTextColor.w);
}
vec4 SetSaturation(vec4 inTextColor,float fSaturation)
{
     float luminance = dot(inTextColor.rgb, luminanceWeighting);
     vec3 greyScaleColor = vec3(luminance);
     
	 return vec4(mix(greyScaleColor, inTextColor.rgb, fSaturation), inTextColor.w);
}

vec4 RGBADark(vec2 inCoordinate)
{
	int multiplier = 0;
	int GAUSSIAN_SAMPLES = 9;	
 	vec2 blurStep;
 	vec2 blurCoordinates[9];
	vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);
     
 	for (int i = 0; i < GAUSSIAN_SAMPLES; i++) {
 		multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));
		// Blur in x (horizontal)
		blurStep = float(multiplier) * singleStepOffset;
		blurCoordinates[i] = inCoordinate.xy + blurStep;
	}
 	vec4 sum = vec4(0.0);
       
	sum += texture2D(textureRGBA, blurCoordinates[0]) * 0.05;
	sum += texture2D(textureRGBA, blurCoordinates[1]) * 0.09;
	sum += texture2D(textureRGBA, blurCoordinates[2]) * 0.12;
	sum += texture2D(textureRGBA, blurCoordinates[3]) * 0.15;
	sum += texture2D(textureRGBA, blurCoordinates[4]) * 0.18;
	sum += texture2D(textureRGBA, blurCoordinates[5]) * 0.15;
	sum += texture2D(textureRGBA, blurCoordinates[6]) * 0.12;
	sum += texture2D(textureRGBA, blurCoordinates[7]) * 0.09;
	sum += texture2D(textureRGBA, blurCoordinates[8]) * 0.05;
	return  vec4((sum.rgb * vec3(brightness)), sum.a);
}

vec4 NV12toRGBA(vec2 inCoordinate)
{
	vec3 yuv;
	vec3 rgb;
	//vec3 rgb1;
	yuv.r = texture2D(textureY, inCoordinate).r;
	yuv.g = texture2D(textureUV, inCoordinate).r - 0.5;
	yuv.b = texture2D(textureUV, inCoordinate).a - 0.5;
	
	rgb = mat3( 1.0,       1.0,         1.0,
                0.0,       -0.39465,  2.03211,
                1.13983, -0.58060,  0.0) * yuv;
				
	//rgb1 = mat3( 1.164,       1.164,         1.164,
    //            0.0,       -0.392,  2.017,
    //            1.596, -0.813,  0.0) * yuv;
				
	return vec4(rgb, 1.0);
}

vec4 BlendNV12toRGBA(vec2 inCoordinate)
{
	vec3 yuv;
	vec3 rgb;
	//vec3 rgb1;
	
	yuv.r = texture2D(textureBlendY, inCoordinate).r;
	yuv.g = texture2D(textureBlendUV, inCoordinate).r - 0.5;
	yuv.b = texture2D(textureBlendUV, inCoordinate).a - 0.5;
	
	rgb = mat3( 1.0,       1.0,         1.0,
                0.0,       -0.39465,  2.03211,
                1.13983, -0.58060,  0.0) * yuv;
				
	//rgb1 = mat3( 1.164,       1.164,         1.164,
    //            0.0,       -0.391,  2.018,
    //            1.596, -0.813,  0.0) * yuv;				
	return vec4(rgb, 1.0);
}

vec4 yuvDecode(vec2 texCoord)
{
	vec3 rgb;
	vec2 uv;
	
	float y = texture2D(textureY, texCoord).r;
	
	y -= 0.0627;
	y *= 1.164;
	rgb = vec3(y);
	
	uv = texture2D(textureUV, texCoord).ar;
	uv -= 0.5;
	rgb += vec3( 1.596 * uv.x, - 0.813 * uv.x - 0.391 * uv.y, 2.018 * uv.y);
	return vec4(rgb,1.0);	
}

vec4 blendYuvDecode(vec2 texCoord)
{
	vec3 rgb;
	vec2 uv;
	
	float y = texture2D(textureBlendY, texCoord).r;
	
	y -= 0.0627;
	y *= 1.164;	
	rgb = vec3(y);
	
	uv = texture2D(textureBlendUV, texCoord).ar;
	uv -= 0.5;
	
	rgb += vec3( 1.596 * uv.x, - 0.813 * uv.x - 0.391 * uv.y, 2.018 * uv.y);
	return vec4(rgb,1.0);	
}

vec4 YUVDark(vec2 inCoordinate)
{
	int multiplier = 0;
 	int GAUSSIAN_SAMPLES = 9;	
 	vec2 blurStep;
 	vec2 blurCoordinates[9];
	vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);
     
 	for (int i = 0; i < GAUSSIAN_SAMPLES; i++) {
 		multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));
		// Blur in x (horizontal)
		blurStep = float(multiplier) * singleStepOffset;
 		blurCoordinates[i] = inCoordinate.xy + blurStep;
 	}
 	vec4 sum = vec4(0.0);
         
	sum  = NV12toRGBA(blurCoordinates[0])*0.05;			
	sum += NV12toRGBA(blurCoordinates[1])*0.09;
	sum += NV12toRGBA(blurCoordinates[2])*0.12;
	sum += NV12toRGBA(blurCoordinates[3])*0.15;
	sum += NV12toRGBA(blurCoordinates[4])*0.18;		
	sum += NV12toRGBA(blurCoordinates[5])*0.15;
	sum += NV12toRGBA(blurCoordinates[6])*0.12;		
	sum += NV12toRGBA(blurCoordinates[7])*0.09;		
	sum += NV12toRGBA(blurCoordinates[8])*0.05;
	return vec4((sum.rgb * vec3(brightness)), sum.a);
}

vec4 Blend(vec4 inTextColor, vec4 blendTexture)
{
	if(1 == blendType)//透黑色，显示白色
	{
		inTextColor.a = max(blendTexture.b,max(blendTexture.r,blendTexture.g));
	}
	else if(2 == blendType)//透黑色 mix
	{
		vec4 whiteColor = vec4(1.0);			
		inTextColor = whiteColor - ((whiteColor - blendTexture) * (whiteColor - inTextColor));
		//inTextColor.r = blendTexture.r;
		//inTextColor.g = blendTexture.g;
		//inTextColor.b = blendTexture.b;
		
	}		
	else if(3 == blendType)//反色 mix
	{
		vec4 whiteColor = vec4(1.0);			
		inTextColor = whiteColor - (blendTexture) * (whiteColor - inTextColor);
	}			
	else if(4 == blendType)//透白色，显示黑色
	{
		inTextColor.a = inTextColor.r * 0.333334 + inTextColor.g * 0.333334 + inTextColor.b * 0.333334;
	}	
	else if(5 == blendType)//透白色，显示黑色
	{
		inTextColor.a = 1.0 - max(inTextColor.b,max(inTextColor.r,inTextColor.g));
	}	
	
	return inTextColor;
}

vec4 CaclAlphaColor(vec2 inCoordinate)
{
	vec4 AlphaColor;
	float r = 0.0;
	
	AlphaColor = vec4(texture2D(textOverLay,inCoordinate));	
	
	if(useGray == 1)
	{
		r = AlphaColor.r;
		if (AlphaColor.r < progress/100.0)
			AlphaColor.a = 1.0;
		else if (r >= progress/100.0)
			AlphaColor.a = 0.0;
		else
			AlphaColor.a =  (r - progress/100.0) / (1.0-progress/100.0);	
	}
	else
	{
	}

	return AlphaColor;
}
#if RECT_QUAD
 bool inTriangle(vec2 p0, vec2 p1, vec2 p2, vec2 p)
 {
     return ((p0.x - p1.x) * (p.y - p1.y) > (p0.y - p1.y) * (p.x - p1.x))
             && ((p1.x - p2.x) * (p.y - p2.y) > (p1.y - p2.y) * (p.x - p2.x))
             && ((p2.x - p0.x) * (p.y - p0.y) > (p2.y - p0.y) * (p.x - p0.x));
 }

 float pointToLine(vec2 p0, vec2 p1, vec2 p)
 {
     if (p0 == p1)
     {
         return distance(p0, p);
     }
     return abs((p1.y - p0.y) * p.x + (p0.x - p1.x) * p.y + ((p1.x * p0.y) - (p0.x * p1.y))) / sqrt(pow(p1.y - p0.y, 2.0) + pow(p0.x - p1.x, 2.0));
 }

 bool onLineRight(vec2 p0, vec2 p1, vec2 p)
 {
     return (p0.x - p1.x) * (p.y - p1.y) > (p0.y - p1.y) * (p.x - p1.x);
 }

 vec2 warpMap(vec2 c)
 {
     vec2 ret = vec2(-1.0, -1.0);

     if (quadDir[6].x == 0.0)
     {
         if (onLineRight(DstQuadrilateral[1], DstQuadrilateral[0], c) && onLineRight(DstQuadrilateral[3], DstQuadrilateral[2], c))
         {
             ret.y = pointToLine(DstQuadrilateral[0], DstQuadrilateral[1] + quadDir[0] + quadDir[1], c)
                     / pointToLine(DstQuadrilateral[3], DstQuadrilateral[2], DstQuadrilateral[0]);
             vec2 a = quadDir[2] * ret.y + DstQuadrilateral[0];
             vec2 b = quadDir[3] * ret.y + DstQuadrilateral[1];
             ret.x = (a.x == b.x) ? (c.y - a.y) / (b.y - a.y) : (c.x - a.x) / (b.x - a.x);
         }
     }
     else if (quadDir[6].y == 0.0)
     {
         if (onLineRight(DstQuadrilateral[0], DstQuadrilateral[3], c) && onLineRight(DstQuadrilateral[2], DstQuadrilateral[1], c))
         {
             ret.x = pointToLine(DstQuadrilateral[0], DstQuadrilateral[3] + quadDir[2] + quadDir[3], c)
                     / pointToLine(DstQuadrilateral[1], DstQuadrilateral[2], DstQuadrilateral[0]);
             vec2 a = quadDir[0] * ret.x + DstQuadrilateral[0];
             vec2 b = quadDir[1] * ret.x + DstQuadrilateral[3];
             ret.y = (a.x == b.x) ? (c.y - a.y) / (b.y - a.y) : (c.x - a.x) / (b.x - a.x);
         }
     }
     else
     {
         float mx = c.x * (quadDir[1].y - quadDir[0].y) - c.y * (quadDir[1].x - quadDir[0].x) + quadDir[4].x;
         float my = c.y * (quadDir[2].x - quadDir[3].x) - c.x * (quadDir[2].y - quadDir[3].y) + quadDir[4].y;
         float sx = mx * mx + 2.0 * quadDir[6].x * (c.y * -quadDir[2].x + quadDir[5].x + c.x * quadDir[2].y);
         float sy = my * my - 2.0 * quadDir[6].y * (c.y * -quadDir[0].x + quadDir[5].y + c.x * quadDir[0].y);
         if (sx >= 0.0 && sy >= 0.0)
         {
             sx = sqrt(sx);
             sy = sqrt(sy);
             if (invmap > 0.0)
             {
                 if ((recess == 0.0 && inTriangle(DstQuadrilateral[1], DstQuadrilateral[0], DstQuadrilateral[3], c))
                     || (recess == 1.0 && inTriangle(DstQuadrilateral[2], DstQuadrilateral[1], DstQuadrilateral[0], c))
                     || (recess == 2.0 && inTriangle(DstQuadrilateral[3], DstQuadrilateral[2], DstQuadrilateral[1], c))
                     || (recess == 3.0 && inTriangle(DstQuadrilateral[0], DstQuadrilateral[3], DstQuadrilateral[2], c)))
                 {
                     ret.x = (sx + mx) / quadDir[6].x;
                     ret.y = (sy - my) / quadDir[6].y;
                 }
                 else
                 {
                     ret.x = -(sx - mx) / quadDir[6].x;
                     ret.y = -(sy + my) / quadDir[6].y;
                 }
             }
             else
             {
                 if ((recess == 0.0 && inTriangle(DstQuadrilateral[3], DstQuadrilateral[0], DstQuadrilateral[1], c))
                     || (recess == 1.0 && inTriangle(DstQuadrilateral[0], DstQuadrilateral[1], DstQuadrilateral[2], c))
                     || (recess == 2.0 && inTriangle(DstQuadrilateral[1], DstQuadrilateral[2], DstQuadrilateral[3], c))
                     || (recess == 3.0 && inTriangle(DstQuadrilateral[2], DstQuadrilateral[3], DstQuadrilateral[0], c)))
                 {
                     ret.x = -(sx - mx) / quadDir[6].x;
                     ret.y = -(sy + my) / quadDir[6].y;
                 }
                 else
                 {
                     ret.x = (sx + mx) / quadDir[6].x;
                     ret.y = (sy - my) / quadDir[6].y;
                 }
             }
         }
     }
     return ret;
 }
 #endif
void main()
{
	vec4 textureColor;
	vec4 textureAlphaColor;
	vec4 blendTexture;	
	vec2 txtCord;
	
#if RECT_QUAD
    

    SrcQuadrilateral[0] = vec2(0.0, 0.0);
    SrcQuadrilateral[1] = vec2(1.0, 0.0);
    SrcQuadrilateral[2] = vec2(1.0, 1.0);
    SrcQuadrilateral[3] = vec2(0.0, 1.0);

    DstQuadrilateral[0] = vec2(0.0, 0.0);
    DstQuadrilateral[1] = vec2(0.5, 0.0);
    DstQuadrilateral[2] = vec2(1.0, 1.0);
    DstQuadrilateral[3] = vec2(0.0, 1.0);
#endif
	
	if(flip == 1)
		txtCord = vec2(1.0 - textureCoordinate.x,textureCoordinate.y);		
	else if(flip == 2)
		txtCord = vec2(textureCoordinate.x,1.0 - textureCoordinate.y);
	else if(flip == 3)
		txtCord = vec2(1.0 - textureCoordinate.x,1.0 - textureCoordinate.y);	
	else
		txtCord = vec2(textureCoordinate.x,textureCoordinate.y);
	
#if RECT_QUAD
	//四边形
	txtCord = textureCoordinate;
	vec2 p = warpMap(txtCord);
    if (p.x >= 0.0 && p.x <= 1.0 && p.y >= 0.0 && p.y <= 1.0)
    {
        vec2 a = (SrcQuadrilateral[1] - SrcQuadrilateral[0]) * p.x + SrcQuadrilateral[0];
        vec2 b = (SrcQuadrilateral[2] - SrcQuadrilateral[3]) * p.x + SrcQuadrilateral[3];
        p = (b - a) * p.y + a;
		txtCord = p;
		
		if(pixFormat == 1005) //BGRA
		{
			//if(dark > 0)
			//	textureColor = RGBADark(txtCord);
			//else		
				textureColor = vec4(texture2D(textureRGBA,txtCord));
		
		}
		else
		{
			//YUV
			if(1 == dark)
				textureColor = YUVDark(txtCord);
			else
			{
				if(lookup > 0)
					textureColor = NV12toRGBA(txtCord);
				else
					textureColor = yuvDecode(txtCord);
			
			}
			textureColor.a = 1.0;	
		}
        //textureColor = vec4(1.0,0.0,0.0,1.0);//vec4(texture2D(textureRGBA,txtCord));
    }
	else
    {
        textureColor = vec4(0.0);
    }
    gl_FragColor = textureColor;
	return;
#else
	if(pixFormat == 1005) //BGRA
	{
		if(dark > 0)
			textureColor = RGBADark(txtCord);
		else		
			textureColor = vec4(texture2D(textureRGBA,txtCord));
		
	}
	else
	{
		//YUV
		if(1 == dark)
			textureColor = YUVDark(txtCord);
		else
		{
			if(lookup > 0)
				textureColor = NV12toRGBA(txtCord);
			else
				textureColor = yuvDecode(txtCord);
			
		}
		textureColor.a = 1.0;	
	}
#endif
	if(useGray > 0)	
	{
		textureAlphaColor = CaclAlphaColor(txtCord);
		textureColor.a = textureAlphaColor.a;
	}		

	textureColor.a *= rgbAlpha;
	
	if(lookup == 1)
	{
		//"Precision qualifier" is not supported prior to GLSL version 1.30
		// disable highp
   		float blueColor = textureColor.b * 63.0;
 
    	vec2 quad1;
    	quad1.y = floor(floor(blueColor) / 8.0);
    	quad1.x = floor(blueColor) - (quad1.y * 8.0);
 
    	vec2 quad2;
    	quad2.y = floor(ceil(blueColor) / 8.0);
    	quad2.x = ceil(blueColor) - (quad2.y * 8.0);
 
    	vec2 texPos1;
    	texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
    	texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);
 
    	vec2 texPos2;
    	texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
    	texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);
 
    	vec4 newColor1 = texture2D(texRGBALookup, texPos1);
    	vec4 newColor2 = texture2D(texRGBALookup, texPos2);
 
    	vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
		textureColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), lookupIntensity);
	}
	
	
	
	if(vignette > 0.0)
    {
#if 0
         float vignetteValue = 1.0 - (1.0-0.75)*vignette;
         float d = distance(txtCord,vec2(0.5,0.5));
         float percent = smoothstep(0.5,vignetteValue,d);
         textureColor = vec4(mix(textureColor.rgb,vec3(0.0,0.0,0.0),percent),textureColor.a);
#else
		float vignetteStart = 0.0;
        float vignetteEnd = 0.5;
        vec2 vignetteCenter = vec2(0.5,0.5); //计算晕影中心位置
        vec3 vignetteColor = vec3(0.0);      //计算晕影颜色
        
        if(vignette > 0.0)
            vignetteEnd = 2.0 - 1.5*vignette;
        else
            vignetteEnd = 2.0 + 1.5*vignette;
        
        float d = distance(textureCoordinate, vec2(vignetteCenter.x, vignetteCenter.y));
        float percent = smoothstep(vignetteStart, vignetteEnd, d);
        if(vignette > 0.0)
            textureColor = vec4(mix(textureColor.rgb, vec3(0.0,0.0,0.0), percent), textureColor.a);
        else
            textureColor = vec4(mix(textureColor.rgb, vec3(1.0,1.0,1.0), percent), textureColor.a);
#endif
     }
	 
    if(sharpness >= -4.0 && sharpness <= 4.0)
    {       
        vec2 widthStep = vec2(texelWidthOffset, 0.0);
        vec2 heightStep = vec2(0.0, texelHeightOffset);
         
        vec2 inputTextureCoordinate = txtCord;
        vec2 leftTextureCoordinate = inputTextureCoordinate.xy - widthStep;
        vec2 rightTextureCoordinate = inputTextureCoordinate.xy + widthStep;
        vec2 topTextureCoordinate = inputTextureCoordinate.xy + heightStep;
        vec2 bottomTextureCoordinate = inputTextureCoordinate.xy - heightStep;
         
        float centerMultiplier = 1.0 + 4.0 * sharpness;
        float edgeMultiplier = sharpness;
         
        vec4 leftTextureColor;
        vec4 rightTextureColor;
        vec4 topTextureColor;
        vec4 bottomTextureColor;
		
		if(pixFormat == 1005)
		{
			leftTextureColor = texture2D(textureRGBA, leftTextureCoordinate);
			rightTextureColor = texture2D(textureRGBA, rightTextureCoordinate);
			topTextureColor = texture2D(textureRGBA, topTextureCoordinate);
			bottomTextureColor = texture2D(textureRGBA, bottomTextureCoordinate);		
		}
		else
		{
			leftTextureColor = yuvDecode(leftTextureCoordinate);
			rightTextureColor = yuvDecode(rightTextureCoordinate);
			topTextureColor = yuvDecode(topTextureCoordinate);
			bottomTextureColor = yuvDecode(bottomTextureCoordinate);			
		}
		        
        vec4 v1 = textureColor * centerMultiplier;
        vec4 v2 = leftTextureColor * edgeMultiplier;
        vec4 v3 = rightTextureColor * edgeMultiplier;
        vec4 v4 = topTextureColor * edgeMultiplier;
        vec4 v5 = bottomTextureColor * edgeMultiplier;
        textureColor = v1 - v2 - v3 - v4 -v5;      
     }
	 
     if(whiteBalance >= -1.0 && whiteBalance <= 1.0)
     {     
         float tint = 0.0;
         const vec3 warmFilter = vec3(0.93, 0.54, 0.0);
         const mat3 RGBtoYIQ = mat3(0.299, 0.587, 0.114, 0.596, -0.274, -0.322, 0.212, -0.523, 0.311);
         const mat3 YIQtoRGB = mat3(1.0, 0.956, 0.621, 1.0, -0.272, -0.647, 1.0, -1.105, 1.702);
         vec3 yiq = RGBtoYIQ * textureColor.rgb; //adjusting tint
         yiq.b = clamp(yiq.b + tint*0.5226*0.1, -0.5226, 0.5226);
         vec3 rgb = YIQtoRGB * yiq;
         
         vec3 processed = vec3(
                                    (rgb.r < 0.5 ? (2.0 * rgb.r * warmFilter.r) : (1.0 - 2.0 * (1.0 - rgb.r) * (1.0 - warmFilter.r))), //adjusting whiteBalance
                                    (rgb.g < 0.5 ? (2.0 * rgb.g * warmFilter.g) : (1.0 - 2.0 * (1.0 - rgb.g) * (1.0 - warmFilter.g))),
                                    (rgb.b < 0.5 ? (2.0 * rgb.b * warmFilter.b) : (1.0 - 2.0 * (1.0 - rgb.b) * (1.0 - warmFilter.b))));
         
         textureColor = vec4(mix(rgb, processed, whiteBalance), textureColor.a);
    }
	if(exposure >= -1.0 && exposure <= 1.0)
	{
		textureColor = vec4(textureColor.rgb * pow(2.0, exposure), textureColor.w);
	}
	
	//rang 	[-1 ~ 1]	
	if(bright != 0.0)
		textureColor = SetBright(textureColor,bright);			
	//rang [ 0 ~ 4] normal 1.0
	if(contrast != 1.0)	
		textureColor = SetContrast(textureColor,contrast);
	//rang [ 0 ~ 2] normal 1.0	
	if(saturation != 1.0)
		textureColor = SetSaturation(textureColor,saturation);
	
	if(blendType > 0)
	{
		if(blendType <= 3)
		{
			if(blendPixFormat == 1005)
				blendTexture = vec4(texture2D(textureBlendRGBA,txtCord));
			else
			{
				if(lookup > 0)
					blendTexture = BlendNV12toRGBA(txtCord);	
				else	
					blendTexture = blendYuvDecode(txtCord);				
			}
		}
		
		gl_FragColor = Blend(textureColor,blendTexture);			
	}
	else
		gl_FragColor = textureColor;	
		
	//if((textureCoordinate.x > 0.496&&textureCoordinate.x < 0.504) || (textureCoordinate.y > 0.493&&textureCoordinate.y < 0.507))
	//	gl_FragColor = vec4(1.0,1.0,0.0,1.0);
	
}
