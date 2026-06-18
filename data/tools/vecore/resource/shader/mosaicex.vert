 attribute vec4 vertexPosition;
 attribute vec2 inputTextureCoordinate;
 
 
 varying vec2 textureCoordinate;
 
 void main()
 {
     textureCoordinate = inputTextureCoordinate;
     gl_Position = vertexPosition;
     
 }