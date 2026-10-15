package glasscam.gl.program;

import glasscam.gl.shader.Shader;

public interface GLComputeProgramInterace{
    void setComputeShader(Shader shader);
    void compute(int width, int height, int input, int output);
}
