package glasscam.gl.shader;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES31;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import glasscam.GlassCamApplication;

public class ShaderUtil {

    private static final String TAG = ShaderUtil.class.getSimpleName();


    public static String getShader(String name, float glesVersion)
    {
        Context context = GlassCamApplication.getContext();
        final ShaderParser parser = new ShaderParser(glesVersion);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open(name, AssetManager.ACCESS_BUFFER)));

            // do reading, usually loop until end of file reading
            String mLine;

            while ((mLine = reader.readLine()) != null) {
                if (!mLine.isEmpty() && !mLine.startsWith("//"))
                {
                    parser.parseLine(mLine);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Shader file read failed: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                    Log.w(TAG, "Reader close failed: " + e.getMessage());
                }
            }
        }
        String out = parser.getString();
        return out;
    }

    public static String getShader(String name) throws IOException {
        Context context = GlassCamApplication.getContext();
        try (InputStream stream = context.getAssets().open(name, AssetManager.ACCESS_BUFFER))
        {
            byte[] result = new byte[stream.available()];
            int bytesRead = stream.read(result);
            return new String(result, Charset.defaultCharset());
        }
    }

    public static String getShader(float glesVersion, String name, Shader.ShaderType type) throws IOException {
        String end = ".vsh";
        if (type == Shader.ShaderType.fragment)
            end = ".fsh";
        else if (type == Shader.ShaderType.compute)
            end = ".csh";

        return getShader("shader/"+name+end,glesVersion);
    }


    public static int createShader(String shader , String shadername, int shaderType)
    {
        int[] compiled = new int[1];
        int fshader = GLES31.glCreateShader(shaderType);
        GLES31.glShaderSource(fshader, shader);
        GLES31.glCompileShader(fshader);
        GLES31.glGetShaderiv(fshader, GLES31.GL_COMPILE_STATUS, compiled, 0);
        Log.d(TAG, "create shader: " + shadername + "\n" + shader);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader: " + shadername + "\n" + GLES31.glGetShaderInfoLog(fshader) + "\n" + shader);
            GLES31.glDeleteShader(fshader);
            fshader = 0;
            throw new RuntimeException("Could not compile shader: " + shadername);
        }
        return fshader;
    }
}
