precision highp float;
uniform sampler2D inputImageTexture;    //
uniform    vec2 texutreSize;    //
uniform float skinBeauty;    //blur 0 to 1
uniform float skinWhite;    //Bright 0 to 1
uniform float skinRed;        //Tone 0 to 1

varying vec2 sizeOfPixel;
varying vec2 coordOfCenter;
const   float PI = 3.14159265358979323846;
vec3 rgb2hsv(vec3 rgb)
{
    //
    float h;
    float s;
    float maxValue = max(rgb.r, max(rgb.g, rgb.b));
    float minValue = min(rgb.r, min(rgb.g, rgb.b));
    float v = maxValue - minValue;
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

float isSkin( vec3 rgb )
{
    vec3 hsv = rgb2hsv(rgb);
    vec3 xyz = vec3( sin(hsv.r * 2.0 * PI) * hsv.g * hsv.b,
                     cos(hsv.r * 2.0 * PI) * hsv.g * hsv.b,
                     hsv.b);

    float r1 = clamp((xyz.z - 0.15625) / (0.78125), 0.0, 1.0) * 0.15625 + 0.15625;
    float r2 = clamp((xyz.z - 0.15625) * 0.546875 / (0.52) + r1, 0.078125, 0.546875);

    float ra = 28.0/360.0 * 2.0 * PI;
    vec3 k1 = vec3(sin(ra) * r1, cos(ra) * r1, clamp(xyz.z, 0.15625 * 2.0, 0.9140625));
    vec3 k2 = vec3(sin(ra) * r2, cos(ra) * r2, clamp(xyz.z, 0.15625 * 2.0, 0.9140625));

    float lower = r1 * 0.25;//0.25;//(r1 - 0.078125) * 2.0;
    float upper = (r1) * 1.0;

    float dist = pointToLineSegment3(k1, k2, xyz);
    dist = clamp( (dist - lower) / (upper - lower), 0.0, 1.0 );
    
    return dist;
}

void main(void)
{
    vec4 center = texture2D( inputImageTexture, coordOfCenter );

    vec3 c = center.rgb;
    float sum = 1.0;
    float t = pow(skinBeauty, 0.33) * 0.4 + 0.1;
   //19
        //    1 radian=0.000000 sin=0.000000, cos=1.000000
        vec3 pix = texture2D( inputImageTexture, vec2(coordOfCenter.x, coordOfCenter.y + sizeOfPixel.y) ).rgb;
        float k = max(t - distance(pix, c), 0.0);
        vec3 c1 = k * pix;
        float sum1 = k;
        //    2 radian=1.047198 sin=0.866025, cos=0.500000
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x + 0.866025 * sizeOfPixel.x, coordOfCenter.y + 0.5 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c1 += k * pix;
        sum1 += k;
        //    3 radian=2.094395 sin=0.866025, cos=-0.500000
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x + 0.866025 * sizeOfPixel.x, coordOfCenter.y - 0.5 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c1 += k * pix;
        sum1 += k;
        //    4 radian=3.141593 sin=-0.000000, cos=-1.000000
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x, coordOfCenter.y - sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c1 += k * pix;
        sum1 += k;
        //    5 radian=4.188790 sin=-0.866025, cos=-0.500000
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x - 0.866025 * sizeOfPixel.x, coordOfCenter.y - 0.5 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c1 += k * pix;
        sum1 += k;
        //    6 radian=5.235988 sin=-0.866025, cos=0.500000
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x - 0.866025 * sizeOfPixel.x, coordOfCenter.y + 0.5 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c1 += k * pix;
        sum1 += k;

        //    7 radian=0.261799 sin=0.258819, cos=0.965926
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x + 0.258819 * 2.0 * sizeOfPixel.x, coordOfCenter.y + 0.965926 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        vec3 c2 = k * pix;
        float sum2 = k;
        //    8 radian=1.308997 sin=0.965926, cos=0.258819
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x + 0.965926 * 2.0 * sizeOfPixel.x, coordOfCenter.y + 0.258819 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    9 radian=2.356194 sin=0.707107, cos=-0.707107
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x + 0.707107 * 2.0 * sizeOfPixel.x, coordOfCenter.y - 0.707107 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    10 radian=3.403392 sin=-0.258819, cos=-0.965926
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x - 0.258819 * 2.0 * sizeOfPixel.x, coordOfCenter.y - 0.965926 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    11 radian=4.450590 sin=-0.965926, cos=-0.258819
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x - 0.965926 * 2.0 * sizeOfPixel.x, coordOfCenter.y - 0.258819 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    12 radian=5.497787 sin=-0.707107, cos=0.707107
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x - 0.707107 * 2.0 * sizeOfPixel.x, coordOfCenter.y + 0.707107 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;

        //    13 radian=0.785398 sin=0.707107, cos=0.707107
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x + 0.707107 * 2.0 * sizeOfPixel.x, coordOfCenter.y + 0.707107 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    14 radian=1.832596 sin=0.965926, cos=-0.258819
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x + 0.965926 * 2.0 * sizeOfPixel.x, coordOfCenter.y - 0.258819 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    15 radian=2.879793 sin=0.258819, cos=-0.965926
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x + 0.258819 * 2.0 * sizeOfPixel.x, coordOfCenter.y - 0.965926 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    16 radian=3.926991 sin=-0.707107, cos=-0.707107
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x - 0.707107 * 2.0 * sizeOfPixel.x, coordOfCenter.y - 0.707107 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    17 radian=4.974188 sin=-0.965926, cos=0.258819
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x - 0.965926 * 2.0 * sizeOfPixel.x, coordOfCenter.y + 0.258819 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;
        //    18 radian=6.021386 sin=-0.258819, cos=0.965926
        pix = texture2D( inputImageTexture, vec2(coordOfCenter.x - 0.258819 * 2.0 * sizeOfPixel.x, coordOfCenter.y + 0.965926 * 2.0 * sizeOfPixel.y) ).rgb;
        k = max(t - distance(pix, c), 0.0);
        c2 += k * pix;
        sum2 += k;

        c = c + c1 * 0.882497 + c2 * 0.606531;
        sum = sum + sum1 * 0.882497 + sum2 * 0.606531;

    c /= sum;

    vec3 a = (c - center.rgb);

    c = 1.0 - ( c - 0.5 ) * ( c - 0.5 ) * 4.0;
    vec3 color = center.rgb + a;
    float skin = max( 1.0 - isSkin(color), 0.0 );

    color = center.rgb + (a * c * 2.0 - a);

    center.rgb = mix(center.rgb, color, skinBeauty) ;
    if ( skinWhite > 0.0 || skinRed > 0.0 )
    {
        vec3 stage = vec3( 1.0 + 0.5 * skinWhite, 1.0 + 0.5 * skinWhite, 1.0 + 0.5 * skinWhite);
        stage.r += skinRed * 1.5 * ( stage.r - 0.75 ) * color.r;
        stage = 1.0 / stage;
        center.rgb = mix(center.rgb, pow(center.rgb, stage), skin);
    }

    gl_FragColor = center;
 }
