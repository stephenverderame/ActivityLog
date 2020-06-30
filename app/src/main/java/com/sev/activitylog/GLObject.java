package com.sev.activitylog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class GLObject implements Destructor {
    protected float[] model = new float[16];
    protected ByteBuffer buffer;
    protected int[] vao, vbo, ebo;
    public abstract void draw(GLShader GLShader);
    public GLObject(){
        Matrix.setIdentityM(model, 0);
    }
    public void scale(float x, float y, float z){
        Matrix.scaleM(model, 0, x, y, z);
    }
    public void translate(float x, float y, float z){
        Matrix.translateM(model, 0, x, y, z);
    }
    public void rotate(float radians, float x, float y, float z){
        Matrix.rotateM(model, 0, radians, x, y, z);
    }
    public void clearModel(){
        Matrix.setIdentityM(model, 0);
    }
}
class Cube extends GLObject {
    protected GLShader GLShader;
    private float cubeVerts[] = { //vec3 - vertex       vec3 - normal       vec2 - texCoords
            // Back face
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f, 0.0f, 0.0f, -1.0f,// Bottom-left
            0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.0f, 0.0f, -1.0f,// top-right
            0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f, 0.0f, -1.0f,// bottom-right
            0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.0f, 0.0f, -1.0f,// top-right
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f, 0.0f, 0.0f, -1.0f,// bottom-left
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 0.0f, 0.0f, -1.0f,// top-left
            // Front face
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f, 0.0f,  0.0f, 1.0f,// bottom-left
            0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  0.0f, 1.0f,// bottom-right
            0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 0.0f,  0.0f, 1.0f,// top-right
            0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 0.0f,  0.0f, 1.0f,// top-right
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f,// top-left
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f, 0.0f,  0.0f, 1.0f,// bottom-left
            // Left face
            -0.5f,  0.5f,  0.5f,  1.0f, 0.0f, -1.0f,  0.0f,  0.0f,// top-right
            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, -1.0f,  0.0f,  0.0f,// top-left
            -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, -1.0f,  0.0f,  0.0f,// bottom-left
            -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, -1.0f,  0.0f,  0.0f,// bottom-left
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f, -1.0f,  0.0f,  0.0f,// bottom-right
            -0.5f,  0.5f,  0.5f,  1.0f, 0.0f, -1.0f,  0.0f,  0.0f,// top-right
            // Right face
            0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 1.0f,  0.0f,  0.0f,// top-left
            0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 1.0f,  0.0f,  0.0f,// bottom-right
            0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f,  0.0f,  0.0f,// top-right
            0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 1.0f,  0.0f,  0.0f,// bottom-right
            0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 1.0f,  0.0f,  0.0f,// top-left
            0.5f, -0.5f,  0.5f,  0.0f, 0.0f, 1.0f,  0.0f,  0.0f,// bottom-left
            // Bottom face
            -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  -1.0f,  0.0f,// top-right
            0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.0f,  -1.0f,  0.0f,// top-left
            0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  -1.0f,  0.0f,// bottom-left
            0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  -1.0f,  0.0f,// bottom-left
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f, 0.0f,  -1.0f,  0.0f,// bottom-right
            -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  -1.0f,  0.0f,// top-right
            // Top face
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  1.0f,  0.0f,// top-left
            0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  1.0f,  0.0f,// bottom-right
            0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.0f,  1.0f,  0.0f,// top-right
            0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  1.0f,  0.0f,// bottom-right
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  1.0f,  0.0f,// top-left
            -0.5f,  0.5f,  0.5f,  0.0f, 0.0f,  0.0f,  1.0f,  0.0f// bottom-left
    };
    @Override
    public void draw(GLShader GLShader) {
        GLShader.use();
        GLShader.setMat4("model", model);
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
    }
    public Cube(GLShader GLShader){
        this.GLShader = GLShader;
        GLShader.use();
        int vertexPos = GLShader.getAttribute("pos");
        vao = new int[1];
        vbo = new int[1];
        ebo = new int[1];
        GLES30.glGenVertexArrays(1, vao, 0);
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        buffer = ByteBuffer.allocateDirect(cubeVerts.length * 8);
        buffer.order(ByteOrder.nativeOrder()); //sets endianess to device endianess
        FloatBuffer fb = buffer.asFloatBuffer();
        fb.put(cubeVerts);
        fb.position(0); //points to first element
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, cubeVerts.length * 4, fb, GLES30.GL_STATIC_DRAW);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 8 * 4, 0);
        GLES30.glEnableVertexAttribArray(0);
    }

    @Override
    public void destructor() {
        GLES30.glDeleteVertexArrays(1, vao, 0);
        GLES30.glDeleteBuffers(1, vbo, 0);
    }
}
class Skybox extends Cube {

    int[] cubemap = new int[1];

    /**
     * Generates a skybox
     * @param GLShader
     * @param textureIds an array of resource ids to images. In order of +x, -x, +y, -y, +z, -z
     * @param ctx
     */
    public Skybox(GLShader GLShader, int[] textureIds, Context ctx) {
        super(GLShader);
        GLES30.glGenTextures(1, cubemap, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemap[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE);
        for(int i = 0; i < 6; ++i){
            Bitmap bmp = BitmapFactory.decodeResource(ctx.getResources(), textureIds[i]);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, bmp, 0); //+x, -x, +y, -y, +z, -z
            bmp.recycle();
        }
    }

    @Override
    public void draw(GLShader GLShader) {
        GLShader.use();
        Matrix.setIdentityM(model, 0);
        Matrix.scaleM(model, 0, 10,10, 10);
        GLShader.setMat4("model", model);
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemap[0]);
        GLES30.glDepthMask(false);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
        GLES30.glDepthMask(true);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, 0);
    }

    @Override
    public void destructor() {
        super.destructor();
        GLES30.glDeleteBuffers(1, ebo, 0);
        GLES30.glDeleteTextures(1, cubemap, 0);
    }
}
class Terrain extends GLObject {

    Pos[][] heightMap;
    Future<Pos[][]> fut;
    Bitmap texture;
    boolean isInit = false;
    int[] glTexture;
    GLShader GLShader;

    /**
     * Generates the terrain onto which to view
     * @param GLShader
     * @param texture Map to display
     * @param dataPoints data points for map
     * @see Pos
     */
    public Terrain(GLShader GLShader, Map texture, Future<Pos[][]> dataPoints){
        fut = dataPoints;
        this.GLShader = GLShader;
        Rect size = texture.calcPreservingDestination(new Rect(0, 0, 1920, 1080));
        this.texture = Bitmap.createBitmap(size.right - size.left, size.bottom - size.top, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(this.texture);
        Paint paint = new Paint();
        paint.setStrokeWidth(2);
        paint.setColor(Color.RED);
        texture.draw(canvas, size, paint);
        glTexture = new int[1];
        GLES30.glGenTextures(1, glTexture, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, this.texture, 0);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }
    @Override
    public void draw(GLShader GLShader) {
        if(init()){
            GLShader.use();
            GLShader.setMat4("model", model);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture[0]);
            GLES30.glBindVertexArray(vao[0]);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, (heightMap.length - 1) * (heightMap[0].length - 1) * 6, GLES30.GL_UNSIGNED_INT, 0);
        }
    }

    /**
     * Checks if future is done, if so initializes data
     * Should be called every draw to poll for future updates
     * @return if data is initialized
     */
    private boolean init()  {
        if(!isInit && fut.isDone()){
            try {
                heightMap = fut.get();
                ByteBuffer buffer = ByteBuffer.allocateDirect(heightMap.length * heightMap[0].length * 3 * 4);
                buffer.order(ByteOrder.nativeOrder());
                FloatBuffer fb = buffer.asFloatBuffer();

                for(int i = 0; i < heightMap.length; ++i){
                    for(int j = 0; j < heightMap[0].length; ++j){
                        fb.put((float)heightMap[i][j].lat);
                        fb.put((float)(heightMap[i][j].elevation * RideOverview.METERS_MILES_CONVERSION / 60)); //about 60 miles per latitude
                        fb.put((float)heightMap[i][j].lon);
                        fb.put((float)i / heightMap.length);
                        fb.put((float)j / heightMap[0].length);

                    }
                }
                fb.position(0);
                ByteBuffer indexBuffer = ByteBuffer.allocateDirect((heightMap.length - 1) * (heightMap[0].length - 1) * 6 * 4);
                indexBuffer.order(ByteOrder.nativeOrder());
                IntBuffer ib = indexBuffer.asIntBuffer();
                for (int i = 0; i < heightMap.length - 1; ++i) {
                    for (int j = 0; j < heightMap[0].length - 1; ++j) {
                        int base = i * heightMap.length + j;
                        ib.put(base);
                        ib.put(base + 1);
                        ib.put(base + heightMap.length);

                        ib.put(base + 1);
                        ib.put(base + heightMap.length + 1);
                        ib.put(base + heightMap.length);
                    }
                }
                ib.position(0);
                isInit = true;
                vao = new int[1];
                GLES30.glGenVertexArrays(1, vao, 0);
                GLES30.glBindVertexArray(vao[0]);
                vbo = new int[1];
                ebo = new int[1];
                GLES30.glGenBuffers(1, vbo, 0);
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, heightMap.length * heightMap[0].length * 5 * 4, fb, GLES30.GL_STATIC_DRAW);
                GLES30.glGenBuffers(1, ebo, 0);
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0]);
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, (heightMap.length - 1) * (heightMap[0].length - 1) * 6, ib, GLES30.GL_STATIC_DRAW);
                GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0);
                GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 3 * 4);
                GLES30.glEnableVertexAttribArray(0);
                GLES30.glEnableVertexAttribArray(1);

                return true;
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else if(isInit) return true;
        return false;
    }

    @Override
    public void destructor() {
        GLES30.glDeleteVertexArrays(1, vao, 0);
        GLES30.glDeleteBuffers(1, vbo, 0);
        GLES30.glDeleteBuffers(1, ebo, 0);
        GLES30.glDeleteTextures(1, glTexture, 0);
    }
}