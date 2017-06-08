#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "MatDefs/FragScattering.glsllib"

uniform vec4 m_GroundColor;

//const float rGround = 40.0/255.0; 
//const float gGround = 88.0/255.0; 
//const float bGround = 16.0/255.0; 


void main() {

    vec4 groundColor = m_GroundColor; //vec4(rGround, gGround, bGround, 1.0);
    
    #ifdef USE_SCATTERING
        vec4 color = calculateGroundColor(groundColor);
    #else
        vec4 color = groundColor;
    #endif
    
    #ifndef GAMMA
        gl_FragColor = color;
    #else
        gl_FragColor.xyz = pow(color.xyz, vec3(GAMMA));        
    #endif
    gl_FragColor.a = 1.0;            
}

