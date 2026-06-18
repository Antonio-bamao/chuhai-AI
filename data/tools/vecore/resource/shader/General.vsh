uniform mat4 matViewProjection;			//操作后的矩阵(旋转/平移/缩放/透视投影)
attribute vec2 vertexPosition;
attribute vec2 inputTextureCoordinate;

#define RECT_QUAD 0 //任意四边形

#if RECT_QUAD
uniform vec2  SrcQuadrilateral[4];  //源四边形的4个顶点在源纹理上的坐标-纹理的裁剪，顺时针0-1，左上角为0.0
uniform vec2  DstQuadrilateral[4];  //目标四边形的4个顶点在渲染结果(纹理)中的坐标-纹理的显示位置，顺时针0-1，左上角为0.0

varying vec2 quadDir[7];
varying float recess;
varying float invmap;
#endif

varying vec2 textureCoordinate;





bool onLineRight(vec2 p0, vec2 p1, vec2 p)
{
    return (p0.x - p1.x) * (p.y - p1.y) > (p0.y - p1.y) * (p.x - p1.x);
}

void main()
{
#if RECT_QUAD
    vec2  SrcQuadrilateral[4];
    vec2  DstQuadrilateral[4];

    SrcQuadrilateral[0] = vec2(0.0, 0.0);
    SrcQuadrilateral[1] = vec2(1.0, 0.0);
    SrcQuadrilateral[2] = vec2(1.0, 1.0);
    SrcQuadrilateral[3] = vec2(0.0, 1.0);

    DstQuadrilateral[0] = vec2(0.0, 0.0);
    DstQuadrilateral[1] = vec2(0.5, 0.0);
    DstQuadrilateral[2] = vec2(1.0, 1.0);
    DstQuadrilateral[3] = vec2(0.0, 1.0);
#endif

	gl_Position = matViewProjection*vec4(vertexPosition,0.0,1.0);
	textureCoordinate = inputTextureCoordinate;

#if RECT_QUAD
    recess = -1.0;
    invmap = -1.0;

    bool recesses[4];
    int count = 0;
    for (int i = 0; i < 4; ++i)
    {
        if (!onLineRight(DstQuadrilateral[(i + 3) - (i + 3) / 4 * 4], DstQuadrilateral[(i + 1) - (i + 1) / 4 * 4], DstQuadrilateral[i]))
        {
            recesses[i] = true;
            ++count;
        }
        else
        {
            recesses[i] = false;
        }
    }
    if (count == 1)
    {
        for (int i = 0; i < 4; ++i)
        {
            if (recesses[i])
            {
                recess = float(i);
                break;
            }
        }
    }
    if (count == 2)
    {
        if (recesses[0] && recesses[1])
        {
            vec2 potA = DstQuadrilateral[0] - DstQuadrilateral[1];
            vec2 potB = DstQuadrilateral[2] - DstQuadrilateral[3];
            float a = atan(potA.y, potA.x);
            float b = atan(potB.y, potB.x);
            recess = a > b ? 0.0 : 1.0;
        }
        else if (recesses[1] && recesses[2])
        {
            vec2 potA = DstQuadrilateral[1] - DstQuadrilateral[2];
            vec2 potB = DstQuadrilateral[3] - DstQuadrilateral[0];
            float a = atan(potA.y, potA.x);
            float b = atan(potB.y, potB.x);
            recess = a < b ? 1.0 : 2.0;
        }
        else if (recesses[2] && recesses[3])
        {
            vec2 potA = DstQuadrilateral[3] - DstQuadrilateral[2];
            vec2 potB = DstQuadrilateral[1] - DstQuadrilateral[0];
            float a = atan(potA.y, potA.x);
            float b = atan(potB.y, potB.x);
            recess = a > b ? 2.0 : 3.0;
        }
        else if (recesses[0] && recesses[3])
        {
            vec2 potA = DstQuadrilateral[0] - DstQuadrilateral[3];
            vec2 potB = DstQuadrilateral[2] - DstQuadrilateral[1];
            float a = atan(potA.y, potA.x);
            float b = atan(potB.y, potB.x);
            recess = a < b ? 0.0 : 3.0;
        }
    }
    else if (count == 3)
    {
        invmap = 1.0;
        for (int i = 0; i < 4; ++i)
        {
            if (!recesses[i])
            {
                recess = float(i);
                break;
            }
        }
    }
    else if (count == 4)
    {
        invmap = 1.0;
    }

    quadDir[0] = DstQuadrilateral[1] - DstQuadrilateral[0];
    quadDir[1] = DstQuadrilateral[2] - DstQuadrilateral[3];
    quadDir[2] = DstQuadrilateral[3] - DstQuadrilateral[0];
    quadDir[3] = DstQuadrilateral[2] - DstQuadrilateral[1];

    quadDir[4].x = -quadDir[1].y * DstQuadrilateral[0].x + quadDir[0].y * DstQuadrilateral[3].x + quadDir[1].x * DstQuadrilateral[0].y - quadDir[0].x * DstQuadrilateral[3].y;
    quadDir[4].y = -quadDir[3].y * DstQuadrilateral[0].x + quadDir[2].y * DstQuadrilateral[1].x + quadDir[3].x * DstQuadrilateral[0].y - quadDir[2].x * DstQuadrilateral[1].y;

    quadDir[5].x = DstQuadrilateral[3].x * DstQuadrilateral[0].y - DstQuadrilateral[0].x * DstQuadrilateral[3].y;
    quadDir[5].y = DstQuadrilateral[1].x * DstQuadrilateral[0].y - DstQuadrilateral[0].x * DstQuadrilateral[1].y;

    quadDir[6].x = 2.0 * (quadDir[1].y * quadDir[0].x - quadDir[1].x * quadDir[0].y);
    quadDir[6].y = 2.0 * (quadDir[2].y * quadDir[3].x - quadDir[2].x * quadDir[3].y);

    if (abs(quadDir[6].x) < 0.000001)
        quadDir[6].x = 0.0;

    if (abs(quadDir[6].y) < 0.000001)
        quadDir[6].y = 0.0;
#endif
}
 