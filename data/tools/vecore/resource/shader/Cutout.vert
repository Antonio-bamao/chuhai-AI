attribute vec4 position;
  attribute vec4 inputTextureCoordinate;
  uniform vec3 keyRGB1;            //指定的色键（RGB）
  uniform vec3 keyRGB2;
  uniform    float edgeSize;
  uniform    vec2 textureSize;        //图像的宽高（像素）
  
  varying vec2 textureCoordinate;
  varying vec2 sizeOfPixel;
  varying float keyDist;
  varying vec3 keyHSV1;
  varying vec3 keyHSV2;
  
  const float PI = 3.14159265358979323846;
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
  
  void main(void)
 {
     gl_Position = position;
     
     vec2 pixSize = vec2(100.0, 100.0);
     if ( textureSize.x < textureSize.y )
     {
         pixSize.y = textureSize.y * pixSize.x / textureSize.x;
     }
     else
     {
         pixSize.x = textureSize.x * pixSize.y / textureSize.y;
     }
     sizeOfPixel = edgeSize / pixSize;
     textureCoordinate = inputTextureCoordinate.xy;
     
     keyHSV1 = rgb2hsv(keyRGB1);
     keyHSV2 = rgb2hsv(keyRGB2);
     keyDist = distanceOfHSV(keyHSV1, keyHSV2);
 }
  