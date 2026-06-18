precision highp float;
 
 //pi 值
 const highp float PI = 3.14159265358979323846;
 
 uniform sampler2D inputBackGroundImageTexture;
 uniform sampler2D inputColorTransparencyImageTexture; //指定颜色透明的纹理
 
 
 // 如果需要指定颜色透明的纹理有旋转缩放等操作应该将图像抠图后绘制到fbo，在与底图blend，例如画中画，此时 hasBackGroundImageTexture = 0
 // 如果需要指定颜色透明的纹理没有旋转缩放等操作直接一步到位，例如AE，此时 hasBackGroundImageTexture = 1
 uniform int hasBackGroundImageTexture;
 
 
 //双色键。如果调用者只能提供一个色键，那么 keyRGB1 和 keyRGB2 要传入相同的值。
 uniform vec3 keyRGB1;            //指定的色键1（RGB）
 uniform vec3 keyRGB2;                   //指定的色键2（RGB）
 
 //uniform vec3 lavesHighlight;
 //uniform vec3 lavesMidtones;
 //uniform vec3 lavesShadow;
 //uniform float factor; //抠图精度
 
 int debugMode = 0;
 //alphaLower 和 alphaUpper 参数很重要，直接决定了抠像的质量。
 //如果要抠取的图像中没有半透明的部分，可以只使用 alphaLower，而把 alphaUpper 设置为0即可。
 //否则就需要把 alphaUpper 设置为大于 alphaLower 的值。

 uniform float alphaLower;        //透明度低阀值，值范围 0~1，通常取值 0.1~0.2，不超过 0.5。
 uniform float alphaUpper;        //透明度高阀值，值范围 0~2，通常取值 0.5 左右，但对于全幅半透明的图片，例如火焰云彩等，值可能要设置为最大。
 //边缘处理，不能与 alphaUpper 同时生效。当 edgeSize 为 0 时 alphaUpper 生效，当 edgeSize 大于 0 时 alphaUpper 失效。
 uniform    float edgeSize;            //边缘修整，去除图像与背景结合处产生的边缘，值范围为0~1 （也可以传入更大的值）。
 uniform    vec2 textureSize;        //图像的宽高（像素）
 
 
 vec2 coordOfCenter = vec2(0.0,0.0);
 
 varying vec2 textureCoordinate;
 varying vec2 sizeOfPixel;
 varying float keyDist;
 varying vec3 keyHSV1;
 varying vec3 keyHSV2;
 
 float pointToLineSegment3(vec3 p1, vec3 p2, vec3 p)
{
    if ( p1 == p2 )
        return distance(p, p1);
    float a = distance(p1, p2);
    float b = distance(p1, p);
    float c = distance(p2, p);
    if ( c + b <= a)
        return 0.0;
    else if ( c * c >= a * a + b * b )
        return b;
    else if ( b * b >= a * a + c * c )
        return c;
    float d = ( a + b + c ) * 0.5;
    float k = d * (d - a) * (d - b) * (d - c);
    float s = sqrt(k);
    return 2.0 * s / a;
}
 
 float pointToLineSegment2(vec2 p1, vec2 p2, vec2 p)
{
    if ( p1 == p2 )
        return distance(p, p1);
    float a = distance(p1, p2);
    float b = distance(p1, p);
    float c = distance(p2, p);
    if ( c + b <= a)
        return 0.0;
    else if ( c * c >= a * a + b * b )
        return b;
    else if ( b * b >= a * a + c * c )
        return c;
    float d = ( a + b + c ) * 0.5;
    float k = d * (d - a) * (d - b) * (d - c);
    float s = sqrt(k);
    return 2.0 * s / a;
    //return abs((p2.y - p1.y) * p.x + (p1.x - p2.x) * p.y + ((p2.x * p1.y) - (p1.x * p2.y))) / sqrt(pow(p2.y - p1.y, 2.0) + pow(p1.x - p2.x, 2.0));
}
 
 vec3 rgb2hsv(vec3 rgb)
{
    //返回的 vec3 中， r 表示 h， g 表示 s, b 表示 v，均为归一化的值，包括色相 s。
    float h = 0.0;
    float s = 0.0;
    float v = 0.0;
    float maxValue = max(rgb.r, max(rgb.g, rgb.b));
    float minValue = min(rgb.r, min(rgb.g, rgb.b));
    v = maxValue - minValue;
    if ( v == 0.0 )
    {
        s = h = 0.0;
    }
    else
    {
        s = v / maxValue;
        if (maxValue == rgb.r)
            h = ((rgb.g - rgb.b) / v + (rgb.g < rgb.b ? 6.0 : 0.0)) / 6.0;
        else if (maxValue == rgb.g)
            h = ((rgb.b - rgb.r) / v + 2.0) / 6.0;
        else
            h = ((rgb.r - rgb.g) / v + 4.0) / 6.0;
    }
    return vec3(h, s, maxValue);
}
 
 float distanceOfHSV(vec3 hsv1, vec3 hsv2)
{
    float arc = abs(hsv1.r - hsv2.r);
    arc = (arc > 0.5 ? 1.0 - arc : arc) * 2.0 * PI;
    
    float r1 = hsv1.g * hsv1.b;
    float r2 = hsv2.g * hsv2.b;
    float height = distance( vec2(r1, hsv1.b), vec2(r2, hsv2.b) );
    return sqrt(r1 * arc * r2 * arc + height * height);
}
 
 float distanceOfHsvToKey(vec3 hsv)
{
    if ( keyDist == 0.0 )
        return distanceOfHSV(keyHSV1, hsv);
    
    float a = keyDist;
    float b = distanceOfHSV(keyHSV1, hsv);
    float c = distanceOfHSV(keyHSV2, hsv);
    
    if ( c + b <= a)
        return 0.0;
    else if ( c * c >= a * a + b * b )
        return b;
    else if ( b * b >= a * a + c * c )
        return c;
    float d = ( a + b + c ) * 0.5;
    float k = d * (d - a) * (d - b) * (d - c);
    float s = 2.0 * sqrt(k) / a;
    return s;
}
 
 bool dispDbgInfo()
{
    //这几行代码测试时使用，在左上角显示未处理的原始小图。
    if ( coordOfCenter.x > 0.05 && coordOfCenter.x <= 0.25 )
    {
        if ( coordOfCenter.y > 0.85 && coordOfCenter.y <= 0.95 )
        {
            if ( coordOfCenter.x <= 0.15 )
            {
                gl_FragColor.rgb = keyRGB1;
            }
            else
            {
                gl_FragColor.rgb = keyRGB2;
            }
            return true;
        }
        else if ( coordOfCenter.y > 0.65 && coordOfCenter.y <= 0.85 )
        {
            gl_FragColor = texture2D(inputColorTransparencyImageTexture, vec2((coordOfCenter.x - 0.05) / 0.2, (coordOfCenter.y - 0.65) / 0.2));
            return true;
        }
    }
    return false;
}
 
 
 vec4 baseAlpha()
{
    vec4 center = texture2D( inputColorTransparencyImageTexture, coordOfCenter );
    vec4 bgColor = vec4((keyRGB1 + keyRGB2) * 0.5, 1.0);
    float alpha;
    vec3 cenHSV = rgb2hsv(center.rgb);
    float cenDist = distanceOfHsvToKey(cenHSV);
    
    if ( alphaLower >= cenDist )
    {
        alpha = 0.0;
    }
    else if ( alphaUpper >= cenDist )
    {
        alpha = (cenDist - alphaLower) / (alphaUpper - alphaLower);
        center.rgb = clamp(( center.rgb - bgColor.rgb ) / alpha + bgColor.rgb, 0.0, 1.0);
    }
    else
    {
        alpha = 1.0;
    }
    if (0 == hasBackGroundImageTexture)
    {
        center.a *= alpha;
        return center;
    }
        
    return mix( texture2D( inputBackGroundImageTexture, coordOfCenter ), center, alpha * center.a);
}
 
 vec4 edgeAlpha()
{
    vec4 center = texture2D( inputColorTransparencyImageTexture, coordOfCenter );
    vec3 cenRGB = center.rgb;
    float alpha;
    vec2 limitA = 1.0 / textureSize;
    vec2 limitB = 1.0 - 1.0 / textureSize;
    
    float distRound[8];
    vec3 pixels[8];
    vec2 offset[8];
    offset[0] = vec2(- sizeOfPixel.x, - sizeOfPixel.y);
    offset[2] = vec2(- sizeOfPixel.x, + sizeOfPixel.y);
    offset[4] = vec2(+ sizeOfPixel.x, + sizeOfPixel.y);
    offset[6] = vec2(+ sizeOfPixel.x, - sizeOfPixel.y);
    
    pixels[0] = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[0], limitA, limitB) ).rgb;
    pixels[2] = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[2], limitA, limitB) ).rgb;
    pixels[4] = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[4], limitA, limitB) ).rgb;
    pixels[6] = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[6], limitA, limitB) ).rgb;
    
    int foPixCount = 0;
    if (distance(pixels[0], pixels[4]) < 0.1 && distance(pixels[2], pixels[6]) < 0.1 )
    {
        alpha = distanceOfHsvToKey(rgb2hsv((cenRGB + pixels[0] + pixels[2] + pixels[4] + pixels[6]) * 0.2));
        if ( alphaLower >= alpha )
        {
            alpha = 0.0;
        }
        else
        {
            alpha = 1.0;
        }
    }
    else
    {
        
        offset[1] = vec2(- 1.41 * sizeOfPixel.x, 0.0);
        offset[3] = vec2(0.0, + 1.41 * sizeOfPixel.y);
        offset[5] = vec2(+ 1.41 * sizeOfPixel.x, 0.0);
        offset[7] = vec2(0.0, - 1.41 * sizeOfPixel.y);
        pixels[1] = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[1], limitA, limitB) ).rgb;
        pixels[3] = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[3], limitA, limitB) ).rgb;
        pixels[5] = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[5], limitA, limitB) ).rgb;
        pixels[7] = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[7], limitA, limitB) ).rgb;
        
        vec3 foRGB = vec3(0.0);
        float weightFo = 0.0;
        vec3 bgRGB = vec3(0.0);
        float weightBg = 0.0;
        vec3 cenHSV = rgb2hsv(cenRGB);
        float cenDist = distanceOfHsvToKey(cenHSV.rgb);
        
        for ( int i = 0; i < 8; ++i )
        {
            vec3 hsv = rgb2hsv(pixels[i]);
            float distHsv = distanceOfHsvToKey(hsv.rgb);
            distRound[i] = distHsv;
            if ( alphaLower < distHsv )
            {
                float w =  PI - distanceOfHSV(hsv, cenHSV);
                foRGB += w * pixels[i];
                weightFo += w;
                ++foPixCount;
            }
            else
            {
                float w = ( alphaLower - distHsv );
                weightBg += w;
                bgRGB += pixels[i] * w;
            }
        }
        
        bgRGB /= weightBg;
        foRGB /= weightFo;
        
        if ( alphaLower < cenDist )
        {
            if ( foPixCount <= 4 && distance(bgRGB, foRGB) < 0.0666 )
            {
                foPixCount = 0;
            }
            else if ( foPixCount < 8 )
            {
                for ( int i = 0; i < 8; ++i )
                {
                    if ( alphaLower >= distRound[i] )
                    {
                        vec3 pix1 = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[i] + offset[i], limitA, limitB) ).rgb;
                        vec3 pix2 = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[i] + offset[(i + 2) - (i + 2) / 8 * 8], limitA, limitB) ).rgb;
                        vec3 pix3 = texture2D( inputColorTransparencyImageTexture, clamp( coordOfCenter + offset[i] + offset[(i + 6) - (i + 6) / 8 * 8], limitA, limitB) ).rgb;
                        int fo = distanceOfHsvToKey(rgb2hsv(pix1)) > alphaLower ? 1 : 0;
                        fo += distanceOfHsvToKey(rgb2hsv(pix2)) > alphaLower ? 1 : 0;
                        fo += distanceOfHsvToKey(rgb2hsv(pix3)) > alphaLower ? 1 : 0;
                        
                        if ( fo >= 2 )
                        {
                            ++foPixCount;
                        }
                    }
                }
            }
        }
        else if ( foPixCount > 0 )
        {
            if ( foPixCount <= 4 )
            {
                if (distance(bgRGB, foRGB) < 0.0666)
                    foPixCount = 0;
                else
                {
                    for ( int i = 0; i < 8; ++i )
                    {
                        if ( alphaLower < distRound[i] )
                        {
                            if (distance(bgRGB, pixels[i]) < 0.125)
                            {
                                --foPixCount;
                            }
                        }
                    }
                }
            }
            else if (foPixCount > 7)
            {
                for ( int i = 0; i < 8; ++i )
                {
                    if ( alphaLower < distRound[i] )
                    {
                        vec3 c = cenRGB - pixels[i];
                        float ma = max(c.r, max(c.g, c.b));
                        float mi = min(c.r, min(c.g, c.b));
                        if ( (ma-mi) < 0.125 )
                        {
                            cenDist = alphaLower * 2.0;
                            foPixCount = 8;
                            break;
                        }
                    }
                }
            }
        }
        
        if ( foPixCount == 0 )
        {
            alpha = 0.0;
        }
        else if ( foPixCount == 8 )
        {
            alpha = ( alphaLower < cenDist ) ? 1.0 : 0.0;
        }
        else
        {
            float gx = distRound[4] + distRound[5] * 2.0 + distRound[6] - distRound[0] - distRound[1] * 2.0 - distRound[2];
            float gy = distRound[0] + distRound[7] * 2.0 + distRound[6] - distRound[2] - distRound[3] * 2.0 - distRound[4];
            
            float arc = atan(gy, gx) + PI * 0.5;
            vec2 pixSize = 1.0 / textureSize;
            vec2 round = vec2(sin(arc), cos(arc));
            vec2 startPos = max( 0.0, float( foPixCount - 3 ) / 4.0 ) * sizeOfPixel * round;
            
            vec2 dd = round * max(pixSize, sizeOfPixel / 5.0);
            startPos = coordOfCenter - startPos;
            
            weightBg = 0.0;
            weightFo = 0.0;
            vec3 tmpRGB = vec3(0.0);
            
            for ( int i = 2; i <= 20; i+=2 )
            {
                vec3 pix = texture2D( inputColorTransparencyImageTexture, clamp(startPos - dd * float(i), limitA, limitB) ).rgb;
                vec3 hsv = rgb2hsv(pix);
                float distHsv = distanceOfHsvToKey(hsv);
                if ( alphaLower >= distHsv )
                {
                    float w = ( alphaLower - distHsv );
                    weightBg += w;
                    tmpRGB += pix * w;
                }
            }
            if ( weightBg > 0.0 )
                bgRGB = tmpRGB / weightBg;
            
            
            if (alphaLower < cenDist || foPixCount < 6 )
            {
                startPos = float( 4 - foPixCount ) / 4.0 * sizeOfPixel * round ;
                startPos = coordOfCenter + startPos;
                tmpRGB = vec3(0.0);
                
                if (cenRGB != bgRGB)
                {
                    float a = distance(cenRGB, bgRGB);
                    for ( int i = 1; i < 5; ++i )
                    {
                        vec3 pix = texture2D( inputColorTransparencyImageTexture, clamp(startPos + dd * float(i), limitA, limitB) ).rgb;
                        vec3 hsv = rgb2hsv(pix);
                        float distHsv = distanceOfHsvToKey(hsv);
                        if ( alphaLower < distHsv )
                        {
                            float b = distance(pix, bgRGB);
                            float c = distance(pix, cenRGB);
                            
                            float w =  (b - min(c,a)) / (a*a);
                            if ( w > 0.0 )
                            {
                                tmpRGB += w * pix;
                                weightFo += w;
                            }
                        }
                        dd *= 1.1;
                    }
                }
            }
            
            if ( weightFo > 0.0 )
            {
                foRGB = tmpRGB / weightFo;
            }
            
            
            
            if (debugMode == 2)
            {
                if ( arc < 0.0 ) arc += PI;
                arc = arc / (PI * 2.0) * 360.0;
                vec3 aa;
                aa.r = floor(arc / 100.0) / 255.0;
                arc = mod(arc, 100.0);
                aa.g = floor(arc / 10.0) / 255.0;
                arc = mod(arc, 10.0);
                aa.b = floor(arc) / 255.0;
                return vec4(aa ,1.0);
            }
            else if (debugMode == 3)
            {
                return vec4(foRGB,1.0);
            }
            else if (debugMode == 4)
            {
                return vec4(bgRGB,1.0);
            }
            
            if ( weightBg > 0.0 && weightFo > 0.0 )
            {
                if ( distance(foRGB, bgRGB) < 0.0625 || distance(bgRGB, cenRGB) < 0.0625 )
                {
                    alpha = 0.0;
                }
                else
                {
                    alpha = min(1.0, distanceOfHSV(cenHSV, rgb2hsv(bgRGB)) / distanceOfHSV(rgb2hsv(foRGB), rgb2hsv(bgRGB)));
                }
                
            }
            else if ( weightBg > 0.0 )
            {
                if ( distance(bgRGB, cenRGB) < 0.0625 )
                {
                    alpha = 0.0;
                }
                else
                {
                    alpha = ( alphaLower < cenDist ) ? 1.0 : 0.0;
                }
            }
            else
            {
                alpha = ( alphaLower < cenDist ) ? 1.0 : 0.0;
            }
            
            if (alphaLower < cenDist )
            {
                alpha = max( alpha, float(foPixCount- 4) / 4.0 );
            }
            else
            {
                alpha *= min(float(foPixCount) / 4.0, 1.0);
            }
            
            center.rgb = clamp(( center.rgb - bgRGB.rgb ) / alpha + bgRGB.rgb, 0.0, 1.0);
        }
    }
    if (0 == hasBackGroundImageTexture)
    {
        center.a *= alpha;
        return center;
    }
    return mix( texture2D( inputBackGroundImageTexture, coordOfCenter ), center, alpha * center.a);
}
 
 void main(void)
{
    coordOfCenter = textureCoordinate;
    
    gl_FragColor = vec4(0.0);
    
    
//    if( dispDbgInfo() )
//    {
//        gl_FragColor = vec4(1.0,0.0,0.0,1.0);
//        return ;
//    }
    
    
    
    if (edgeSize == 0.0)
    {
        gl_FragColor = baseAlpha();
    }
    else if (edgeSize == 2.0)  //gpuimage 绿幕抠图
    {
        float thresholdSensitivity = 0.4*alphaUpper;
        float smoothing = 0.10*alphaUpper; //安卓是0.13
        vec3 colorToReplace = keyRGB1;
        
        vec4 textureColor = texture2D(inputColorTransparencyImageTexture, textureCoordinate);
        
        float maskY = 0.2989 * colorToReplace.r + 0.5866 * colorToReplace.g + 0.1145 * colorToReplace.b;
        float maskCr = 0.7132 * (colorToReplace.r - maskY);
        float maskCb = 0.5647 * (colorToReplace.b - maskY);
        
        float Y = 0.2989 * textureColor.r + 0.5866 * textureColor.g + 0.1145 * textureColor.b;
        float Cr = 0.7132 * (textureColor.r - Y);
        float Cb = 0.5647 * (textureColor.b - Y);
        
        //     float blendValue = 1.0 - smoothstep(thresholdSensitivity - smoothing, thresholdSensitivity , abs(Cr - maskCr) + abs(Cb - maskCb));
        float blendValue = smoothstep(thresholdSensitivity, thresholdSensitivity + smoothing, distance(vec2(Cr, Cb), vec2(maskCr, maskCb)));
        gl_FragColor = vec4(textureColor.rgb, textureColor.a * blendValue);
    }
    else
    {
        gl_FragColor = edgeAlpha();
    }
}