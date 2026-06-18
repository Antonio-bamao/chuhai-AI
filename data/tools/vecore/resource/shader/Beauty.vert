  attribute vec4 vertexPosition;
  attribute vec4 inputTextureCoordinate;
  uniform  mat4 matViewProjection;
  uniform  vec2 texutreSize;
  uniform float skinBeauty;
  
  varying vec2 coordOfCenter;
  varying vec2 sizeOfPixel;
  
  const   float PI = 3.14159265358979323846;
  void main(void)
  {
      gl_Position = matViewProjection* vertexPosition;
  
      vec2 pixSize = vec2(100.0, 100.0);
      if ( texutreSize.x < texutreSize.y )
      {
          pixSize.y = texutreSize.y * pixSize.x / texutreSize.x;
      }
      else
      {
          pixSize.x = texutreSize.x * pixSize.y / texutreSize.y;
      }
      sizeOfPixel = (sqrt(skinBeauty) * 0.8 + 0.5) / pixSize;
  
      coordOfCenter = inputTextureCoordinate.xy;
  }