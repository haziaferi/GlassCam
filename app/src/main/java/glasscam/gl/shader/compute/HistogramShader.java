package glasscam.gl.shader.compute;

import glasscam.gl.shader.Shader;

public class HistogramShader extends Shader {
    public HistogramShader(float glesVersion) {
        super(glesVersion);
    }

    @Override
    public String getShaderName() {
        return "computeHistogram";
    }

    @Override
    public ShaderType getShaderType() {
        return ShaderType.compute;
    }
}
