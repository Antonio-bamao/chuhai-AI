
 #define AE_FRAMEBUFFER_FETCH 1
 precision highp float;
 vec2 Flip_v(float flip, vec2 uv);
 vec3 Flip_v(float flip, vec3 uv);
 vec4 Flip_v(float flip, vec4 uv);

 varying vec2 textureCoordinate;
 uniform sampler2D inputImageTexture;

 uniform lowp vec4 u_is_texture_1_flip_;
 uniform lowp vec4 u_is_texture_0_flip_;
 uniform sampler2D lutY;
 uniform sampler2D lutR;
 uniform sampler2D lutG;
 uniform sampler2D lutB;

 const float intensityY=1.0;
 const float intensityR=1.0;
 const float intensityG=1.0;
 const float intensityB=1.0;

 float vec2ToFloat(vec2 val){
     float res = val.x + val.y/255.0;
     return res;
 }
 float vec4ToFloat(vec4 val){
     float res = val.x * 255.0 + val.y + val.z/255.0;
     res = val.w < 0.5 ? res : -res;
     return res;
 }

 vec4 processY(vec4 srcColor, vec4 lastColor)
 {
     vec4 resColor = lastColor;
     float ySrc = 0.2126 * srcColor.r + 0.7152 * srcColor.g + 0.0722 * srcColor.b;
     float yLast = 0.2126 * lastColor.r + 0.7152 * lastColor.g + 0.0722 * lastColor.b;

     float yNew = vec4ToFloat(texture2D(lutY, Flip_v(u_is_texture_0_flip_[1], vec2(ySrc, 0.5))));

     //if(yNew > 255000000.0)
     //    yNew = yLast;

     float yIncr = (yNew - ySrc) * 1.0 - (yLast - ySrc);

     resColor.rgb += yIncr;
     return resColor;
 }

 vec4 processR(vec4 srcColor, vec4 lastColor)
 {
     vec4 resColor = lastColor;

     resColor.r = vec4ToFloat(texture2D(lutR, Flip_v(u_is_texture_0_flip_[2], vec2(srcColor.r, 0.5))));
     float rIncr = (resColor.r - srcColor.r) * intensityG;
     resColor.r = lastColor.r + rIncr;

     return resColor;
 }

 vec4 processG(vec4 srcColor, vec4 lastColor)
 {
     vec4 resColor = lastColor;

     resColor.g = vec4ToFloat(texture2D(lutG, Flip_v(u_is_texture_0_flip_[3], vec2(srcColor.g, 0.5))));
     float gIncr = (resColor.g - srcColor.g) * intensityG;
     resColor.g = lastColor.g + gIncr;

     return resColor;
 }

 vec4 processB(vec4 srcColor, vec4 lastColor)
 {
     vec4 resColor = lastColor;

     resColor.b = vec4ToFloat(texture2D(lutB, Flip_v(u_is_texture_1_flip_[0], vec2(srcColor.b, 0.5))));
     float bIncr = (resColor.b - srcColor.b) * intensityB;
     resColor.b = lastColor.b + bIncr;

     return resColor;
 }

 vec2 Flip_v(float flip, vec2 uv) {
     if (flip > 0.5){
         uv.y = 1.0 - uv.y;
     }
     return uv;
 }
 vec3 Flip_v(float flip, vec3 uv) {
     if (flip > 0.5){
         uv.y = 1.0 - uv.y;
     }
     return uv;
 }
 vec4 Flip_v(float flip, vec4 uv) {
     if (flip > 0.5) {
         uv.y = 1.0 - uv.y;
     }
     return uv;
 }
 void main() {
     vec2 uv0= textureCoordinate.xy;
     //    vec4 srcColor = texture2D(inputImageTexture, textureCoordinate);
     vec4 srcColor = texture2D(inputImageTexture, Flip_v(u_is_texture_0_flip_[0], uv0));


     //    vec4 red = texture2D(lutR, uv0);
     vec4 green = texture2D(lutG, uv0);
     vec4 blue = texture2D(lutB, uv0);

     vec4 resColor = srcColor;

     resColor = processR(srcColor, resColor);
     resColor = processG(srcColor, resColor);
     resColor = processB(srcColor, resColor);
     resColor = processY(srcColor, resColor);

     //同时启用4个异常
     resColor = clamp(resColor, 0.0, 1.0);

     gl_FragColor = resColor;
     gl_FragColor.a = 1.0;

 }