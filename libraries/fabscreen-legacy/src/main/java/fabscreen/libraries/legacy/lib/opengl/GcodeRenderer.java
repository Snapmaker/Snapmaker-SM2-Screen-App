package fabscreen.libraries.legacy.lib.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import fabscreen.libraries.legacy.lib.parser.Position;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class GcodeRenderer implements GLSurfaceView.Renderer {
    private static String TAG = "GcodeRenderer";
    private int program;
    private int vPosition;
    private int vColor;
    private float[] productMatrix = new float[16];
    private int umvpMatrix;

    private BehaviorSubject<Boolean> mIsGcodeRenderCompleteSubject = BehaviorSubject.createDefault(false);

    private static float coordinates[] = {
            -230.0f, 0, 0,
            230.0f, 0, 0,
            0, -250.0f, 0,
            0, 250.0f, 0,
            0, 0, -240.0f,
            0, 0, 240.0f
    };

    private ArrayList<Position> mToolPath;
    private int mVerticesLength = 0;
    private float mAzimuthAngle = 270;
    private float mZenithAngle = 90;

    // VerticesShader
    private static final String verticesShader
            = "attribute vec3 vPosition;            \n"
            + "attribute vec3 vColor;               \n"
            + "uniform mat4 matrix;                 \n"
            + "varying vec3 varyColor;              \n"
            + "void main(){                         \n"
            + "   gl_Position = matrix * vec4(vPosition,1);\n"
            + "   varyColor = vColor;               \n"
            + "}";

    // FragmentShader
    private static final String fragmentShader
            = "precision mediump float;         \n"
            + "varying vec3 varyColor;          \n"
            + "void main(){                     \n"
            + "   gl_FragColor = vec4(varyColor,1);     \n"
            + "}";

    private int loadShader(int shaderType, String sourceCode) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, sourceCode);
            GLES20.glCompileShader(shader);

            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("ES20_ERROR", "Could not compile shader " + shaderType + ":");
                Log.e("ES20_ERROR", GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        // create program
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);

            GLES20.glLinkProgram(program);

            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("ES20_ERROR", "Could not link program: ");
                Log.e("ES20_ERROR", GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private FloatBuffer getVerticesColor(@NonNull ArrayList<Position> toolPath) {
        int colorIndex = 0;
        float baseColor[][] = {
                {0, 0.7f, 0.8f},
                {0, 0.5f, 0.5f},
                {0, 0.7f, 1.0f},
                {0, 0.4f, 0.8f},
                {0.2f, 0.5f, 0.7f}
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(mVerticesLength * 3 * 4 + coordinates.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vColorBuf = vbb.asFloatBuffer();

        for (int i = 0; i < coordinates.length / 3; i++) {
            vColorBuf.put(baseColor[3][0]);
            vColorBuf.put(baseColor[3][1]);
            vColorBuf.put(baseColor[3][2]);
        }

        for (int i = 0; i < toolPath.size(); i++) {
            Position position = toolPath.get(i);

            if (!(position.isStartPoint() || position.isEndPoint())) {
                vColorBuf.put(baseColor[colorIndex][0]);
                vColorBuf.put(baseColor[colorIndex][1]);
                vColorBuf.put(baseColor[colorIndex][2]);
            }

            if (i % 5 != 0) {
                colorIndex++;
            } else {
                colorIndex = 0;
            }
            vColorBuf.put(baseColor[colorIndex][0]);
            vColorBuf.put(baseColor[colorIndex][1]);
            vColorBuf.put(baseColor[colorIndex][2]);
        }
        vColorBuf.position(0);

        return vColorBuf;
    }

    private FloatBuffer getVertices(@NonNull ArrayList<Position> toolPath) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(mVerticesLength * 3 * 4 + coordinates.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuf = vbb.asFloatBuffer();

        vertexBuf.put(coordinates);

        for (Position position : toolPath) {
            vertexBuf.put(position.x);
            vertexBuf.put(position.y);
            vertexBuf.put(position.z);

            if (!(position.isStartPoint() || position.isEndPoint())) {
                vertexBuf.put(position.x);
                vertexBuf.put(position.y);
                vertexBuf.put(position.z);
            }
        }
        vertexBuf.position(0);

        return vertexBuf;
    }

    private static FloatBuffer getDefaultVertices() {
        ByteBuffer vbb = ByteBuffer.allocateDirect(coordinates.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuf = vbb.asFloatBuffer();
        vertexBuf.put(coordinates);
        vertexBuf.position(0);

        return vertexBuf;
    }

    private static FloatBuffer getDefaultVerticesColor() {
        float color[] = {0, 1.0f, 0};
        ByteBuffer vbb = ByteBuffer.allocateDirect(4 * coordinates.length);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vColorBuf = vbb.asFloatBuffer();

        for (int i = 0; i < coordinates.length / 3; i++) {
            vColorBuf.put(color);
        }
        vColorBuf.position(0);

        return vColorBuf;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        program = createProgram(verticesShader, fragmentShader);

        vPosition = GLES20.glGetAttribLocation(program, "vPosition");
        vColor = GLES20.glGetAttribLocation(program, "vColor");
        umvpMatrix = GLES20.glGetUniformLocation(program, "matrix");

        GLES20.glClearColor(0, 0, 0, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        float[] projectionMatrix = new float[16];
        float[] viewMatrix = new float[16];

        FloatBuffer vertices;
        FloatBuffer verticesColor;

        if (mToolPath != null) {
            vertices = getVertices(mToolPath);
            verticesColor = getVerticesColor(mToolPath);
        } else {
            vertices = getDefaultVertices();
            verticesColor = getDefaultVerticesColor();
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        if (mToolPath == null) {
            Matrix.frustumM(projectionMatrix, 0, -120, 120, -120, 120, 200, 500);
            Matrix.setLookAtM(viewMatrix, 0, 115, -275, 240, 115, 125, 120, 0, 0, 1);
            Matrix.multiplyMM(productMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        }

        GLES20.glUniformMatrix4fv(umvpMatrix, 1, false, productMatrix, 0);

        GLES20.glUseProgram(program);

        GLES20.glVertexAttribPointer(vPosition, 3, GLES20.GL_FLOAT, false, 3 * 4, vertices);
        GLES20.glVertexAttribPointer(vColor, 3, GLES20.GL_FLOAT, false, 3 * 4, verticesColor);

        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glEnableVertexAttribArray(vColor);

        GLES20.glLineWidth(2);
        if (mToolPath == null) {
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, coordinates.length / 3);
        } else {
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, coordinates.length / 3 + mVerticesLength);
        }

        GLES20.glDisableVertexAttribArray(vPosition);
        GLES20.glDisableVertexAttribArray(vColor);

        if (mToolPath != null) {
            mIsGcodeRenderCompleteSubject.onNext(true);
        }
    }

    public void setToolPath(ArrayList<Position> toolPath) {
        mToolPath = toolPath;
        mVerticesLength = getRendererVerticesLength(mToolPath);
    }

    private int getRendererVerticesLength(ArrayList<Position> toolPath) {
        int length = 0;
        for (Position position : toolPath) {
            length++;
            if (!(position.isStartPoint() || position.isEndPoint())) {
                length++;
            }
        }
        return length;
    }

    public void setCamera(float centerX, float centerY, float centerZ, float radius) {
        float[] projectionMatrix = new float[16];
        float[] viewMatrix = new float[16];

        Matrix.frustumM(projectionMatrix, 0, -radius, radius, -radius, radius, radius, 3 * radius);
        Matrix.setLookAtM(viewMatrix, 0, centerX, centerY - 2 * radius, centerZ, centerX, centerY, centerZ, 0, 0, 1.0f);
        Matrix.multiplyMM(productMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public void setModelRotate(float centerX, float centerY, float centerZ, float angle, float rotateX, float rotateY, float rotateZ) {
        Matrix.translateM(productMatrix, 0, centerX, centerY, centerZ);
        Matrix.rotateM(productMatrix, 0, angle, rotateX, rotateY, rotateZ);
        Matrix.translateM(productMatrix, 0, -centerX, -centerY, -centerZ);
    }

    /**
     * Rotate the camera with spherical coordinate and seeAt Point . The center point of model is using world space coordinate.
     *
     * @param centerX    The x coordinate of center point we are looking at.
     * @param centerY    The y coordinate of center point we are looking at.
     * @param centerZ    The z coordinate of center point we are looking at.
     * @param horizontalAngle Horizontal degree with Z coordinate and radius
     * @param verticalAngle Vertical degree with X coordinate and radius
     * @param radius The max radius of the model
     */
    public void setCameraRotate(float centerX, float centerY, float centerZ, float horizontalAngle, float verticalAngle, float radius) {
        float[] projectionMatrix = new float[16];
        float[] viewMatrix = new float[16];
        float eyeX;
        float eyeY;
        float eyeZ;

        mAzimuthAngle += horizontalAngle;
        mAzimuthAngle = (mAzimuthAngle % 360 == 0) ? 0 : mAzimuthAngle;

        // Limit zenith angle to avoid changing camera vector
        mZenithAngle += verticalAngle;
        if (mZenithAngle <= 0 || mZenithAngle >= 180) {
            mZenithAngle = (verticalAngle < 0 ? 0.1f : 179.9f);
        }

        // calculate the camera cartesian coordinates from spherical coordinates
        // x = r * sinθ * cosφ. azimuthAngle zenithAngle
        // y = r * sinθ * sinφ.
        // z = r * cosθ.
        eyeX = (float) (2 * radius * Math.sin(Math.PI / 180 * mZenithAngle) * Math.cos(Math.PI / 180 * mAzimuthAngle)) + centerX;
        eyeY = (float) (2 * radius * Math.sin(Math.PI / 180 * mZenithAngle) * Math.sin(Math.PI / 180 * mAzimuthAngle)) + centerY;
        eyeZ = (float) (2 * radius * Math.cos(Math.PI / 180 * mZenithAngle)) + centerZ;

        Matrix.frustumM(projectionMatrix, 0, -radius, radius, -radius, radius, radius, 3 * radius);
        Matrix.setLookAtM(viewMatrix, 0,
                eyeX, eyeY, eyeZ,
                centerX, centerY, centerZ,
                0, 0, 1);
        Matrix.multiplyMM(productMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public Observable<Boolean> getIsGcodeRenderComplete() {
        return mIsGcodeRenderCompleteSubject;
    }
}
