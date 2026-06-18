precision highp float;

 varying vec2 textureCoordinate;
 uniform sampler2D inputImageTexture;


 uniform mediump vec4 Hue_Red;
 uniform mediump vec4 Hue_Orange;
 uniform mediump vec4 Hue_Yellow;
 uniform mediump vec4 Hue_Green;
 //uniform mediump vec4 Hue_Cyan;
 uniform mediump vec4 Hue_Blue;
 uniform mediump vec4 Hue_Purple;
 uniform mediump vec4 Hue_Magenta;

 const lowp vec3 redShift = vec3(0.0, 1.0, 1.0);
 const lowp vec3 orangeShift = vec3(0.0, 1.0, 1.0);
 const lowp vec3 yellowShift = vec3(0.0, 1.0, 1.0);
 const lowp vec3 greenShift = vec3(0.0, 1.0, 1.0);
 const lowp vec3 aquaShift = vec3(0.0, 1.0, 1.0);
 const lowp vec3 purpleShift = vec3(0.0, 1.0, 1.0);
 const lowp vec3 magentaShift = vec3(0.0, 1.0, 1.0);

 const lowp float redOrange = 22.0 / 360.0;
 const lowp float orangeYellow = 38.0 / 360.0;
 const lowp float yellowGreen = 56.0 / 360.0;
 const lowp float greenAqua = 161.0 / 360.0;
 const lowp float aquaBlue = 174.0 / 360.0;
 const lowp float bluePurple = 285.0 / 360.0;
 const lowp float purlpeMagenta = 297.0 / 360.0;
 const lowp float magentaRed = 328.0 / 360.0;
 const highp vec3 labWhiteColor = vec3(100.0, 0.0052604999583039, -0.0104081845252679);
 const mediump vec3 lumCoeff=vec3(0.2125, 0.7154, 0.0721);

 highp vec3 HUEtoRGB(highp float H)
 {
     highp float R = abs(H * 6.0 - 3.0) - 1.0;
     highp float G = 2.0 - abs(H * 6.0 - 2.0);
     highp float B = 2.0 - abs(H * 6.0 - 4.0);
     return clamp(vec3(R, G, B), 0.0, 1.0);
 }

 highp float Epsilon = 1e-10;

 highp vec3 RGBtoHCV(highp vec3 RGB)
 {
     highp vec4 P = (RGB.g < RGB.b) ? vec4(RGB.bg, -1.0, 2.0/3.0) : vec4(RGB.gb, 0.0, -1.0/3.0);
     highp vec4 Q = (RGB.r < P.x) ? vec4(P.xyw, RGB.r) : vec4(RGB.r, P.yzx);
     highp float C = Q.x - min(Q.w, Q.y);
     highp float H = abs((Q.w - Q.y) / (6.0 * C + Epsilon) + Q.z);
     return vec3(H, C, Q.x);
 }

 highp vec3 RGBtoHSL(highp vec3 RGB)
 {
     highp vec3 HCV = RGBtoHCV(RGB);
     highp float L = HCV.z - HCV.y * 0.5;
     highp float S = HCV.y / (1.0 - abs(L * 2.0 - 1.0) + Epsilon);
     return vec3(HCV.x, S, L);
 }

 highp vec3 HSLtoRGB(highp vec3 HSL)
 {
     highp vec3 RGB = HUEtoRGB(HSL.x);
     highp float C = (1.0 - abs(2.0 * HSL.z - 1.0)) * HSL.y;
     return (RGB - 0.5) * C + HSL.z;
 }

 highp vec3 hsl2rgb(highp vec3 c)
 {
     c = vec3(fract(c.x), clamp(c.yz, 0.0, 1.0));
     highp vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
     return c.z + c.y * (rgb - 0.5) * (1.0 - abs(2.0 * c.z - 1.0));
 }

 highp vec3 RGB2Lab(highp vec3 rgb)
 {
     highp float R = rgb.x;
     highp float G = rgb.y;
     highp float B = rgb.z;
     highp float T = 0.008856;
     highp float X = R * 0.412453 + G * 0.357580 + B * 0.180423;
     highp float Y = R * 0.212671 + G * 0.715160 + B * 0.072169;
     highp float Z = R * 0.019334 + G * 0.119193 + B * 0.950227;
     X = X / 0.950456;
     Y = Y;
     Z = Z / 1.088754;
     int XT = 0;
     int YT = 0;
     int ZT = 0;
     if (X > T){
         XT = 1;
     }
     if (Y > T){
         YT = 1;
     }
     if (Z > T){
         ZT = 1;
     }
     highp float Y3 = pow(Y, 1.0/3.0);
     highp float fX;
     highp float fY;
     highp float fZ;
     if (XT!=0){
         fX = pow(X, 1.0/3.0);
     } else {
         fX = 7.787 * X + 16.0/116.0;
     }
     if (YT!=0){
         fY = Y3;
     } else {
         fY = 7.787 * Y + 16.0/116.0;
     }
     if (ZT!=0){
         fZ = pow(Z, 1.0/3.0);
     } else {
         fZ = 7.787 * Z + 16.0/116.0;
     }
     highp float L;
     if (YT!=0){
         L = (116.0 * Y3) - 16.0;
     } else {
         L = 903.3 * Y;
     }
     highp float a = 500.0 * (fX - fY);
     highp float b = 200.0 * (fY - fZ);
     return vec3(L, a, b);
 }

 highp vec3 smoothTreatment(highp vec3 hsv, highp float hueEdge0, highp float hueEdge1, highp vec3 shiftEdge0, highp vec3 shiftEdge1)
 {
     highp float smoothedHue = smoothstep(hueEdge0, hueEdge1, hsv.x);
     highp float hue = hsv.x + (shiftEdge0.x + ((shiftEdge1.x - shiftEdge0.x) * smoothedHue));
     highp float sat = hsv.y * (shiftEdge0.y + ((shiftEdge1.y - shiftEdge0.y) * smoothedHue));
     highp float lum = hsv.z * (shiftEdge0.z + ((shiftEdge1.z - shiftEdge0.z) * smoothedHue));
     return vec3(hue, sat, lum);
 }

 highp vec3 shift1(highp vec3 hsl, lowp float pHue, lowp float nHue, highp vec3 pShift, highp vec3 shift, highp vec3 nShift, highp float whiteDistance)
 {
     lowp float gradientOffset = 0.03;
     lowp float hue = mix(pHue, nHue, 0.5);
     lowp float pDistance = 1.0 - smoothstep(pHue - gradientOffset, pHue + gradientOffset, hsl.x);
     lowp float nDistance = 1.0 - smoothstep(nHue + gradientOffset, nHue - gradientOffset, hsl.x);
     highp float h = 0.0;
     highp float s = 0.0;
     highp float l = 0.0;
     highp float bright = 1.0 - whiteDistance;
     highp float lShift = (1.0 - shift.z) * - (1.0-bright) + 1.0;
     highp float lpShift = (1.0 - pShift.z) * - (1.0-bright) + 1.0;
     highp float lnShift = (1.0 - nShift.z) * - (1.0-bright) + 1.0;
     if (hsl.x <= hue)
     {
         h = hsl.x + (shift.x + ((pShift.x - shift.x) * pDistance));
         s = hsl.y * (shift.y + ((pShift.y - shift.y) * pDistance));
         l = hsl.z * (lShift + ((lpShift - lShift) * pDistance));
     }
     else
     {
         h = hsl.x + (shift.x + ((nShift.x - shift.x) * nDistance));
         s = hsl.y * (shift.y + ((nShift.y - shift.y) * nDistance));
         l = hsl.z * (lShift + ((lnShift - lShift) * nDistance));
     }
     return vec3(h, s, l);
 }

 vec3 BlueAdjust(vec3 color)
 {
     float delta = 0.0001;
     if ((abs(Hue_Blue.x) < delta) && (abs(Hue_Blue.y) < delta) && (abs(Hue_Blue.z) < delta)){
         return color;
     }

     highp vec3 hsl = RGBtoHSL(color);
     highp vec3 lab = RGB2Lab(color);
     highp float whiteDistance = distance(lab, labWhiteColor) / 100.0;
     if (hsl.x < redOrange){
         hsl = shift1(hsl, 0.0, redOrange, magentaShift, redShift, orangeShift, whiteDistance);
     }
     else if (hsl.x >= redOrange && hsl.x < orangeYellow) {
         hsl = shift1(hsl, redOrange, orangeYellow, redShift, orangeShift, yellowShift, whiteDistance);
     }
     else if (hsl.x >= orangeYellow && hsl.x < yellowGreen) {
         hsl = shift1(hsl, orangeYellow, yellowGreen, orangeShift, yellowShift, greenShift, whiteDistance);
     }
     else if (hsl.x >= yellowGreen && hsl.x < greenAqua) {
         hsl = shift1(hsl, yellowGreen, greenAqua, yellowShift, greenShift, aquaShift, whiteDistance);
     }
     else if (hsl.x >= greenAqua && hsl.x < aquaBlue) {
         hsl = shift1(hsl, greenAqua, aquaBlue, greenShift, aquaShift, Hue_Blue.rgb, whiteDistance);
     }
     else if (hsl.x >= aquaBlue && hsl.x < bluePurple) {
         hsl = shift1(hsl, aquaBlue, bluePurple, aquaShift, Hue_Blue.rgb, purpleShift, whiteDistance);
     }
     else if (hsl.x >= bluePurple && hsl.x < purlpeMagenta) {
         hsl = shift1(hsl, bluePurple, purlpeMagenta, Hue_Blue.rgb, purpleShift, magentaShift, whiteDistance);
     }
     else if (hsl.x >= purlpeMagenta && hsl.x < magentaRed) {
         hsl = shift1(hsl, purlpeMagenta, magentaRed, purpleShift, magentaShift, redShift, whiteDistance);
     }
     else if (hsl.x >= magentaRed){
         hsl = shift1(hsl, magentaRed, 1.0, magentaShift, redShift, orangeShift, whiteDistance);
     }

     return hsl2rgb(hsl);
 }

 vec3 rgb2hsv(vec3 rgb)
 {
     float rc = rgb.r;
     float gc = rgb.g;
     float bc = rgb.b;

     float h = 0.0;
     float s = 0.0;
     float v = 0.0;

     float max_v = max(rc, max(gc, bc));
     float min_v = min(rc, min(gc, bc));
     float delta = max_v - min_v;

     v = max_v;

     if (max_v != 0.0) {
         s = delta / max_v;
     } else {
         s = 0.0;
     }

     if (s == 0.0) {
         h = 0.0;
     } else {
         if (rc == max_v) {
             h = (gc - bc) / delta;
         } else if (gc == max_v) {
             h = 2.0 + (bc - rc) / delta;
         } else if (bc == max_v) {
             h = 4.0 + (rc - gc) / delta;
         }

         h *= 60.0;
         if (h < 0.0) {
             h += 360.0;
         }
     }
     return vec3(h, s, v);
 }

 vec3 hsv2rgb(vec3 rgb)
 {
     float h;
     float s;
     float v;
     float r;
     float g;
     float b;

     h=rgb.r;
     s=rgb.g;
     v=rgb.b;
     int i = 0;
     float f;
     float p;
     float q;
     float t;
     if (s == 0.0) {
         // achromatic (grey)
         r = g = b = v;
     } else {
         h /= 60.0;// sector 0 to 5
         i = int(floor(h));
         f = h - float(i);// factorial part of h
         p = v * (1.0 - s);
         q = v * (1.0 - s * f);
         t = v * (1.0 - s * (1.0 - f));

         if (i == 0) {
             r = v;
             g = t;
             b = p;
         } else if (i == 1) {
             r = q;
             g = v;
             b = p;
         } else if (i == 2) {
             r = p;
             g = v;
             b = t;
         } else if (i == 3) {
             r = p;
             g = q;
             b = v;
         } else if (i == 4) {
             r = t;
             g = p;
             b = v;

         } else {
             r = v;
             g = p;
             b = q;
         }
     }
     return vec3(r, g, b);
 }

 vec3 hueAdjust(vec3 hsb, vec3 maskHsb, float standHue, float deltaRange, vec3 hueParam, float offset, int ColorType)
 {
     float delta = 0.0001;
     if ((abs(hueParam.x) < delta) && (abs(hueParam.y) < delta) && (abs(hueParam.z) < delta)){
         return hsb;
     }

     //hsb adjust
     vec3 tmpHSB;
     float fAlpha;
     float standHue0;
     float minHue;
     float maxHue;
     float currHue;
     //Red
     if (ColorType == 3)
     hueParam.x=hueParam.x * 0.7 + 10.0;
     else
     hueParam.x=hueParam.x * 0.3;
     //    hueParam.x=hueParam.x*1.2;
     hueParam.y=hueParam.y*0.8/100.0;
     if (ColorType == 1)
     hueParam.z=hueParam.z/200.0;
     else
     hueParam.z=hueParam.z/100.0;

     //init data
     standHue0=standHue+offset;
     minHue=standHue0-deltaRange;
     maxHue=standHue0+deltaRange;
     tmpHSB=hsb;
     //check range

     if (offset>0.0)
     {
         if (maskHsb.r>180.0)
         currHue=maskHsb.r-360.0+offset;
         else
         currHue=maskHsb.r+offset;
     }
     else
     currHue=maskHsb.r;
     if ((currHue>=minHue) && (currHue <= maxHue))
     {
         //get alpha
         fAlpha=abs(currHue-standHue0);
         //fAlpha=1.0-fAlpha/deltaRange;
         fAlpha=1.0-fAlpha/deltaRange;

         //hue
         tmpHSB.x=hsb.r+hueParam.x*fAlpha;
         if (tmpHSB.x>=360.0)
         tmpHSB.x=tmpHSB.x-360.0;
         //saturation
         tmpHSB.y=hsb.y+hsb.y*hueParam.y*fAlpha;
         tmpHSB.y=clamp(tmpHSB.y, 0.0, 1.0);
         //bright
         tmpHSB.z=hsb.z+hsb.z*hsb.y*hueParam.z*fAlpha;
         //tmpHSB.z=hsb.z+hsb.z*hsb.y*hueParam.z*0.5; //
         tmpHSB.z=clamp(tmpHSB.z, 0.0, 1.0);
     }

     return tmpHSB;
 }

 void main()
 {
     vec4 srcColor = texture2D(inputImageTexture, textureCoordinate);
     vec3 blendColor = vec3(0.0);
     vec3 ansColor = srcColor.rgb;

     mediump vec3 clA;
     mediump vec3 clO;

     clA = clO= ansColor;

     mediump float fTmpGamma;
     mediump float lumBlur=dot(clO, lumCoeff);
     lumBlur=lumBlur*2.0-1.0;

     if (lumBlur < 0.0){
         fTmpGamma = pow(10.0, 0.0);
         clA=vec3(1.0 - pow(vec3(1.0 - clO), vec3(fTmpGamma)));
     }
     else {
         fTmpGamma=pow(10.0, 0.0);
         clA=pow(clO, vec3(fTmpGamma));
     }

     vec3 hsb=rgb2hsv(clA);
     vec3 hsbGuass=hsb;

     //red
     hsb=hueAdjust(hsb, hsbGuass, 0.0, 30.0, Hue_Red.rgb, 360.0, 0);
     //Hue_Orange
     hsb=hueAdjust(hsb, hsbGuass, 30.0, 20.0, Hue_Orange.rgb, 360.0, 1);
     //Hue_Yellow
     hsb=hueAdjust(hsb, hsbGuass, 50.0, 45.0, Hue_Yellow.rgb, 0.0, 2);
     //Hue_Green
     hsb=hueAdjust(hsb, hsbGuass, 120.0, 50.0, Hue_Green.rgb, 0.0, 3);// before standH=120.0
     //Hue_Cyan
     // hsb=hueAdjust(hsb,hsbGuass,180.0,20.0,Hue_Cyan.rgb,0.0, 4);
     //Hue_Blue
     hsb=hueAdjust(hsb, hsbGuass, 240.0, 60.0, vec3(0.0, 0.0, 0.0), 0.0, 5);
     //Hue_Purple
     hsb=hueAdjust(hsb, hsbGuass, 300.0, 40.0, Hue_Purple.rgb, 0.0, 6);
     //Hue_Magenta
     hsb=hueAdjust(hsb, hsbGuass, 330.0, 20.0, Hue_Magenta.rgb, 0.0, 7);

     ansColor=hsv2rgb(hsb);
     // Hue_Blue
     ansColor=BlueAdjust(ansColor);

     gl_FragColor = vec4(ansColor, srcColor.a);
 }