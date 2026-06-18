precision highp float;
 uniform sampler2D inputImageTexture;
 varying vec2 textureCoordinate;
 

 //varying vec2 v_TexturePosition;
 //uniform sampler2D u_Y_RGBTexture;

 uniform  vec2 resolution;

 uniform vec2 pixel;
 uniform vec4 u_color;
 uniform int strip;
 uniform int type;

 vec2 pixelSet;

 vec2 stitch(vec2 uv)
 {
     vec2 screenPos = floor(uv * resolution);
     float x = mod(screenPos.x, pixelSet.x);
     float y = mod(screenPos.y, pixelSet.y);
     if (y == x || y == pixelSet.x - x)
         return vec2(0.0);
     else
         return vec2(1.0);
 }

 vec2 hexagon(vec2 uv){
     uv *= vec2(0.577350278, 1.0); // hexagonal ratio
     float z = clamp(abs(mod(uv.x+floor(uv.y), 2.0)-1.0)*3.141592653-1.047197551, 0.0, 1.0);
     uv.y = floor(uv.y + z);
     uv.x = (floor(uv.x*0.5 + mod(uv.y, 2.0)*0.5) - mod(uv.y, 2.0)*0.5 + 0.5)*3.464101665;
     // convert back from hexagonal ratio
     return uv;
 }

 vec4 triangleColor(vec2 triang)
 {
     vec2 uv = triang;
     float size = 20.;
     float d = 0.;
     float f = 0.;
     int mirror = 0;

     //size = (resolution.y - resolution.x) / size;
     float pixelSize = resolution.y / size;
     float imagePixelX = pixelSize / resolution.x;
     float imagePixelY = pixelSize / resolution.y;

     float x = imagePixelX * floor(uv.x / imagePixelX);
     float y = imagePixelY * floor(uv.y / imagePixelY);

     float j = size * resolution.x / resolution.y;
     j = size / j * 0.5;
     float x2 = imagePixelX * floor((uv.x - j / size) / imagePixelX);
     float y2 = imagePixelY * floor(uv.y / imagePixelY);

     uv.x *= resolution.x / resolution.y;
     uv *= size;
     float tx = floor(uv.x);
     float ty = floor(uv.y);

     uv = fract(uv);

     float back = 0.;
     float front = 1.0;

     if (floor(ty / 2.) * 2. == ty)
     {
         mirror = 1;
     }
     if (1 == mirror)
     {
         d = step((0.), uv.x - uv.y * 0.5);
         f = step((1.), uv.x + uv.y * 0.5);
         back += d - f;
         front -= d - f;
     } else
     {
         d = step((0.5), uv.x - uv.y * 0.5);
         f = step((0.5), uv.x + uv.y * 0.5);
         back += f - d;
         front -= f - d;
     }

     vec4 pixel_f = texture2D(inputImageTexture, vec2(x2, y2)) * front;
     vec4 pixel_b = texture2D(inputImageTexture, vec2(x + 0.01, y + 0.01)) * back;
     pixel_f += pixel_b;
     return pixel_f;
 }

 void main()
 {
     vec2 uv = textureCoordinate.xy;
     pixelSet = vec2(floor(pixel.x), floor(pixel.y));

      if (type == 1) //triangle pixelation
     {
         gl_FragColor = u_color *  triangleColor(uv);
     }else if (type == 2)  //hexagon pixelation
     {
          vec2 uvSet = uv * resolution;
          uv = uvSet.xy / resolution.y;
         vec2 center = resolution.xy * 0.5 / resolution.y; // normalised center
         vec2 r = vec2(resolution.y / resolution.x, 1.0); // skew the square UV map to fill image sources to the screen

         float count = pixelSet.x;
         gl_FragColor = u_color * texture2D(inputImageTexture, hexagon(uv * count) / count * r);

     }else
      {
          float dx = pixelSet.x * 1.0 / resolution.x;
          float dy = pixelSet.y * 1.0 / resolution.y;
          vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));
          gl_FragColor = u_color * texture2D(inputImageTexture, coord);
          vec2 reminder = stitch(uv);
          if ((reminder.x == 0. || reminder.y == 0.) && strip == 1)
          {
              gl_FragColor = vec4(0., 0., 0., 1.);
          }
      }

 }