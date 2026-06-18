
varying  vec2 textureCoordinate;
 
 uniform sampler2D inputImageTexture;
 uniform  vec2  inputpixelSize; //单个像素在目标纹理中的大小。例如目标纹理将被渲染为 800*600 的矩形，那么单个像素就是 1/800, 1/600
 
 uniform vec2 pointLB;
 uniform vec2 pointLT;
 uniform vec2 pointRT;
 uniform vec2 pointRB;
 
 uniform float mosaicBlockSize;
 
 int isPointInRect(float x, float y) {

     vec2 A = pointLB*100.0;
     vec2 B = pointLT*100.0;
     vec2 C = pointRT*100.0;
     vec2 D = pointRB*100.0;
     
     float a = (B.x - A.x)*(y - A.y) - (B.y - A.y)*(x - A.x);
     float b = (C.x - B.x)*(y - B.y) - (C.y - B.y)*(x - B.x);
     float c = (D.x - C.x)*(y - C.y) - (D.y - C.y)*(x - C.x);
     float d = (A.x - D.x)*(y - D.y) - (A.y - D.y)*(x - D.x);
     if((a > 0.0 && b > 0.0 && c > 0.0 && d > 0.0) || (a < 0.0 && b < 0.0 && c < 0.0 && d < 0.0)) {
         return 1;
     }
     return 0;
 }
 
 void main()
 {
    if(1 == isPointInRect(textureCoordinate.x*100.0,textureCoordinate.y*100.0))
    {
         vec2 texSize = vec2(0.0,0.0);
         vec2 mosaicSize = vec2(mosaicBlockSize,mosaicBlockSize);//马赛克大小
         vec2 vTextueCoords = textureCoordinate;

        texSize.x = 1.0/inputpixelSize.x;
        texSize.y = 1.0/inputpixelSize.y;


         vec2 xy = vec2(vTextueCoords.x * texSize.x , vTextueCoords.y * texSize.y);

         vec2 xyMosaic = vec2(floor(xy.x / mosaicSize.x) * mosaicSize.x,
                                        floor(xy.y / mosaicSize.y) * mosaicSize.y );

             //第几块mosaic
         vec2 xyFloor = vec2(floor(mod(xy.x, mosaicSize.x)),
                                       floor(mod(xy.y, mosaicSize.y)));

         vec2 uvMosaic = vec2(xyMosaic.x / texSize.x, xyMosaic.y / texSize.y);
        gl_FragColor = vec4(texture2D( inputImageTexture, uvMosaic ).rgb,1.0);
		//gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
    }
    else
        gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
	
	
}