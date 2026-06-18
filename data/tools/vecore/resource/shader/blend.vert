uniform mat4 matViewProjection;
attribute vec2 vertexPosition;
attribute vec2 inputTextureCoordinate;

varying vec2 textureCoordinate;


void main()
{
	gl_Position = matViewProjection*vec4(vertexPosition,0.0,1.0);
	textureCoordinate = inputTextureCoordinate;	
}
 