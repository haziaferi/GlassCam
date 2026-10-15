package glasscam.gl.shader.vertex;

import glasscam.gl.shader.Shader;

public class PreviewVertexShader extends Shader {
    public PreviewVertexShader(float glesVersion) {
        super(glesVersion);
    }

    @Override
    public String getShaderName() {
        return "preview";
    }

    @Override
    public ShaderType getShaderType() {
        return ShaderType.vertex;
    }
}
