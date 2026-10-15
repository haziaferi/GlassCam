package glasscam.gl.shader.fragment;

import glasscam.gl.shader.Shader;

public class OesFragmentShader extends Shader {
    public OesFragmentShader(float glesVersion) {
        super(glesVersion);
    }

    @Override
    public String getShaderName() {
        return "oes";
    }

    @Override
    public ShaderType getShaderType() {
        return ShaderType.fragment;
    }
}
