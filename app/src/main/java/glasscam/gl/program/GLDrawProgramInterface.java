package glasscam.gl.program;

import glasscam.gl.shader.Shader;
import glasscam.gl.texture.GLFrameBuffer;
import glasscam.gl.texture.GLTex;

public interface GLDrawProgramInterface {
    void create(Shader vertexShader,Shader fragmentShader);
    void draw(GLTex input, GLFrameBuffer output);
}
