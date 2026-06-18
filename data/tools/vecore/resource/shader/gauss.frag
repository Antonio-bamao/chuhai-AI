
uniform sampler2D u_RGBTexture;
uniform vec2 u_resolution;
uniform vec2 u_direction;
uniform vec2 pointLB;
uniform vec2 pointLT;
uniform vec2 pointRT;
uniform vec2 pointRB;

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

vec4 blur9(sampler2D image, vec2 uv, vec2 resolution, vec2 direction) 
{
            vec4 color = vec4(0.0);
            vec2 off1 = vec2(1.3846153846) * direction;
            vec2 off2 = vec2(3.2307692308) * direction;
            vec4 imageColor = texture2D(image, uv);
            color += imageColor * 0.2270270270;
            color += texture2D(image, uv + (off1 / resolution)) * 0.3162162162;
            color += texture2D(image, uv - (off1 / resolution)) * 0.3162162162;
            color += texture2D(image, uv + (off2 / resolution)) * 0.0702702703;
            color += texture2D(image, uv - (off2 / resolution)) * 0.0702702703;
            return color;//vec4(color.rgb,imageColor.a);
}

vec4 blur13(sampler2D image, vec2 uv, vec2 resolution, vec2 direction)
{
            vec4 color = vec4(0.0);
            vec2 off1 = vec2(1.411764705882353) * direction;
            vec2 off2 = vec2(3.2941176470588234) * direction;
            vec2 off3 = vec2(5.176470588235294) * direction;
            vec4 imageColor = texture2D(image, uv);
            color += imageColor * 0.1964825501511404;
            color += texture2D(image, uv + (off1 / resolution)) * 0.2969069646728344;
            color += texture2D(image, uv - (off1 / resolution)) * 0.2969069646728344;
            color += texture2D(image, uv + (off2 / resolution)) * 0.09447039785044732;
            color += texture2D(image, uv - (off2 / resolution)) * 0.09447039785044732;
            color += texture2D(image, uv + (off3 / resolution)) * 0.010381362401148057;
            color += texture2D(image, uv - (off3 / resolution)) * 0.010381362401148057;
            return color;//vec4(color.rgb,imageColor.a);
}

void main()
{
    vec2 uv = vec2(gl_FragCoord.xy / u_resolution.xy);
	if(1 == isPointInRect(uv.x*100.0,uv.y*100.0))
        gl_FragColor = blur9(u_RGBTexture, uv, u_resolution.xy, u_direction);
     else
		gl_FragColor = texture2D(u_RGBTexture,uv);
	//gl_FragColor = texture2D(u_RGBTexture, uv);
}