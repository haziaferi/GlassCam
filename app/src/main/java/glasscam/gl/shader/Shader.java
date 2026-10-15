package glasscam.gl.shader;

import android.opengl.GLES31;

import glasscam.utils.Log;

import java.io.IOException;

public abstract class Shader<T extends Shader> {
    private final String TAG = Shader.class.getSimpleName();

    public enum ShaderType
    {
        vertex,
        fragment,
        compute,
    }

    private int handel;
    private final float glesVersion;

    public Shader(float glesVersion)
    {
        this.glesVersion = glesVersion;
        createShader();
    }

    private void createShader()
    {
        if (getShaderType() == ShaderType.vertex)
            handel = ShaderUtil.createShader(loadShader(), getShaderName() + " vertex", GLES31.GL_VERTEX_SHADER);
        else if (getShaderType() == ShaderType.compute)
            handel = ShaderUtil.createShader(loadShader(), getShaderName() + " vertex", GLES31.GL_COMPUTE_SHADER);
        else
            handel = ShaderUtil.createShader(loadShader(),getShaderName() +" fragment",GLES31.GL_FRAGMENT_SHADER);
    }

    private String loadShader()
    {
        try {
            return ShaderUtil.getShader(glesVersion,getShaderName(),getShaderType());
        } catch (IOException e) {
            Log.e(TAG, "Shader loading failed: " + e.getMessage());
        }
        return null;
    }

    public abstract String getShaderName();
    public abstract ShaderType getShaderType();

    public int getHandel() {
        return handel;
    }
}
