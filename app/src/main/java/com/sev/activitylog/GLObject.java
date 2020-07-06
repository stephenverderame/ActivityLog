package com.sev.activitylog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class GLObject implements Destructor {
    protected float[] model = new float[16];
    protected int shaderID;
    protected boolean isModelDirty;
    public abstract void draw(GLRendererShaderManager shader);
    public abstract void drawGeometry(GLRendererShaderManager shader);
    public GLObject(){
        Matrix.setIdentityM(model, 0);
        isModelDirty = false;
        shaderID = 0;
    }
    public GLObject(int shaderId){
        this();
        shaderID = shaderId;
    }
    public void scale(float x, float y, float z){
        Matrix.scaleM(model, 0, x, y, z);
        isModelDirty = true;
    }
    public void translate(float x, float y, float z){
        Matrix.translateM(model, 0, x, y, z);
        isModelDirty = true;
    }
    public void rotate(float radians, float x, float y, float z){
        Matrix.rotateM(model, 0, radians, x, y, z);
        isModelDirty = true;
    }
    public void clearModel(){
        Matrix.setIdentityM(model, 0);
        isModelDirty = true;
    }
    public void setShaderID(int id) {this.shaderID = id;}
}
class Cube extends GLObject {
    protected ByteBuffer buffer;
    protected int[] vao, vbo, ebo;
    protected float r, g, b, a;
    protected boolean color;
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
    public void draw(GLRendererShaderManager shaders) {
        GLShader shader = shaders.getShader(shaderID);
        shader.use();
        shader.setMat4("model", model);
        shader.setBool("useColor", color);
        if(isModelDirty){
            float[] mat = new float[16];
            Matrix.invertM(mat, 0, model, 0);
            float[] normalMat = new float[16];
            Matrix.transposeM(normalMat, 0, mat, 0);
            shader.setMat4("normalMatrix", normalMat);
        }
        if(color) shader.setVec4("color", r, g, b, a);
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
        GLES30.glBindVertexArray(0);
    }

    @Override
    public void drawGeometry(GLRendererShaderManager shaders) {
        GLShader shader = shaders.getShader(shaderID);
        shader.use();
        shader.setMat4("model", model);
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
        GLES30.glBindVertexArray(0);
    }

    public Cube(){
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
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(2);
        color = true;
    }
    public void setColor(float r, float g, float b, float a){
        this.r = r;
        this.a = a;
        this.g = g;
        this.b = b;
        color = true;
    }
    public void disableColor(){
        color = false;
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
     * @param textureIds an array of resource ids to images. In order of +x, -x, +y, -y, +z, -z
     * @param ctx
     */
    public Skybox(int[] textureIds, Resources ctx) {
        super();
        GLES30.glGenTextures(1, cubemap, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemap[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE);
        for(int i = 0; i < 6; ++i){
            Bitmap bmp = BitmapFactory.decodeResource(ctx, textureIds[i]);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, bmp, 0); //+x, -x, +y, -y, +z, -z
            bmp.recycle();
        }
        Matrix.scaleM(model, 0, 10,10, 10);
    }
    public Skybox(int[] textureIds, Resources ctx, int shaderId){
        this(textureIds, ctx);
        shaderID = shaderId;
    }

    @Override
    public void draw(GLRendererShaderManager shaders) {
        GLShader shader = shaders.getShader(shaderID);
        shader.use();
        shader.setMat4("model", model);
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubemap[0]);
        GLES30.glDepthMask(false);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
        GLES30.glDepthMask(true);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, 0);
    }

    @Override
    public void drawGeometry(GLRendererShaderManager shaders) {
    }

    @Override
    public void destructor() {
        super.destructor();
        GLES30.glDeleteBuffers(1, ebo, 0);
        GLES30.glDeleteTextures(1, cubemap, 0);
    }
}
class DirectionalLight extends GLObject {
    private float[] color;
    private float[] pos;
    private float ambient, spec, diff;
    boolean isDirty;
    public DirectionalLight(){
        color = new float[3];
        pos = new float[3];
        isDirty = true;
    }
    @Override
    public void draw(GLRendererShaderManager shaders) {
        if(isDirty){
            GLShader shader = shaders.getShader(shaderID);
            shader.use();
            shader.setVec3("sun.position", pos);
            shader.setVec3("sun.color", color);
            shader.setFloat("sun.ambientFac", ambient);
            shader.setFloat("sun.specFac", spec);
            shader.setFloat("sun.diffFac", diff);
            isDirty = false;

        }
    }

    @Override
    public void drawGeometry(GLRendererShaderManager shader) {

    }

    @Override
    public void destructor() {

    }

    @Override
    public void translate(float x, float y, float z) {
        super.translate(x, y, z);
        pos[0] = x;
        pos[1] = y;
        pos[2] = z;
        isDirty = true;
    }
    public void setColor(float r, float g, float b){
        color[0] = r;
        color[1] = g;
        color[2] = b;
        isDirty = true;
    }
    public void setFactors(float ambient, float spec, float diff){
        this.ambient = ambient;
        this.spec = spec;
        this.diff = diff;
        isDirty = true;
    }
    public final float[] getPos() {return pos;}
}
class Terrain extends GLObject {

    Pos[][] heightMap;
    Future<Pos[][]> fut;
    Bitmap texture;
    boolean isInit = false;
    int[] glTexture;
    protected int[] vao, vbo, ebo;
    private static final int VERTEX_DATA_COUNT = 8;
    private static final int INDEX_DATA_COUNT = 6;
    private int[] debugVao, debugVbo;

    /**
     * Generates the terrain onto which to view
     * @param texture Map to display
     * @param dataPoints data points for map
     * @see Pos
     */
    public Terrain(Map texture, Future<Pos[][]> dataPoints){
        fut = dataPoints;
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
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, this.texture, 0);
        Log.e("Terrain", String.valueOf(GLES30.glGetError()));
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

    }
    @Override
    public void draw(GLRendererShaderManager shaders) {
        if(init()){
            GLShader shader = shaders.getShader(shaderID);
            shader.use();
            shader.setMat4("model", model);
            if(isModelDirty){
                float[] mat = new float[16];
                Matrix.invertM(mat, 0, model, 0);
                float[] normalMat = new float[16];
                Matrix.transposeM(normalMat, 0, mat, 0);
                shader.setMat4("normalMatrix", normalMat);
            }
            shader.setBool("useColor", false);
 //           shader.setInt("tex", 0);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture[0]);
            GLES30.glBindVertexArray(vao[0]);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, (heightMap.length - 1) * (heightMap[0].length - 1) * INDEX_DATA_COUNT, GLES30.GL_UNSIGNED_INT, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            GLES30.glBindVertexArray(0);
/*            shader = shaders.getShader(2);
            shader.use();
            shader.setMat4("model", model);
            GLES30.glBindVertexArray(debugVao[0]);
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, heightMap.length * heightMap[0].length * 2);
            GLES30.glBindVertexArray(0);*/
            isModelDirty = false;
        }
    }

    @Override
    public void drawGeometry(GLRendererShaderManager shaders) {
        if(init()) {
            GLShader shader = shaders.getShader(shaderID);
            shader.use();
            shader.setMat4("model", model);
            GLES30.glBindVertexArray(vao[0]);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, (heightMap.length - 1) * (heightMap[0].length - 1) * INDEX_DATA_COUNT, GLES30.GL_UNSIGNED_INT, 0);
            GLES30.glBindVertexArray(0);
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
                if(heightMap == null) return false;
                ByteBuffer buffer = ByteBuffer.allocateDirect(heightMap.length * heightMap[0].length * VERTEX_DATA_COUNT * GLM.FLOAT_SIZE);
                buffer.order(ByteOrder.nativeOrder());
                FloatBuffer fb = buffer.asFloatBuffer();
                ByteBuffer debug = ByteBuffer.allocateDirect(heightMap.length * heightMap[0].length * 6 * GLM.FLOAT_SIZE);
                debug.order(ByteOrder.nativeOrder());
                FloatBuffer debugFb = debug.asFloatBuffer();


                Pos center = new Pos((heightMap[0][0].lat + heightMap[heightMap.length - 1][heightMap[0].length - 1].lat) / 2,
                        (heightMap[0][0].lon + heightMap[heightMap.length - 1][heightMap[0].length - 1].lon) / 2, heightMap[0][0].elevation);
                float[] firstVector = center.toVec3f();
                for(int i = 0; i < heightMap.length; ++i){ //latitude (y) --> really z
                    for(int j = 0; j < heightMap[0].length; ++j){ //longitude (x)
                        fb.put((float)(heightMap[i][j].lon - center.lon));
                        fb.put((float)(heightMap[i][j].elevation - center.elevation)); //about 60 miles per latitude
                        fb.put((float)(heightMap[i][j].lat - center.lat));
                        fb.put(1.f - (float)j / (heightMap[0].length - 1)); //tex coords
                        fb.put(1.f - (float)i / (heightMap.length - 1));
                        //calc normal
                        float[] a = GLM.subtract(heightMap[i][j].toVec3f(), firstVector), b, c;
                        if(j < heightMap[0].length - 1)
                            b = GLM.subtract(heightMap[i][j + 1].toVec3f(), firstVector);
                        else
                            b = GLM.subtract(heightMap[i][j - 1].toVec3f(), firstVector);
                        if(i < heightMap.length - 1)
                            c = GLM.subtract(heightMap[i + 1][j].toVec3f(), firstVector);
                        else
                            c = GLM.subtract(heightMap[i - 1][j].toVec3f(), firstVector);
                        float[] normal = GLM.normalize(GLM.cross(GLM.subtract(b, a), GLM.subtract(c, a)));
                        if(!((j == heightMap[0].length - 1) ^ (i == heightMap.length - 1)))
                            normal = GLM.negate(normal);
                        fb.put(GLM.normalize(normal));

                        debugFb.put((float)(heightMap[i][j].lon - center.lon));
                        debugFb.put((float)(heightMap[i][j].elevation - center.elevation));
                        debugFb.put((float)(heightMap[i][j].lat - center.lat));
                        debugFb.put(normal);



                    }
                }
                fb.position(0);
                debugFb.position(0);
                ByteBuffer indexBuffer = ByteBuffer.allocateDirect((heightMap.length - 1) * (heightMap[0].length - 1) * INDEX_DATA_COUNT * GLM.INT_SIZE);
                indexBuffer.order(ByteOrder.nativeOrder());
                IntBuffer ib = indexBuffer.asIntBuffer();
                for (int i = 0; i < heightMap.length - 1; ++i) { //iterate rows
                    for (int j = 0; j < heightMap[0].length - 1; ++j) { //iterate columns
                        int base = i * heightMap[0].length + j;
                        ib.put(base);
                        ib.put(base + 1);
                        ib.put(base + heightMap[0].length);

                        ib.put(base + 1);
                        ib.put(base + heightMap[0].length + 1);
                        ib.put(base + heightMap[0].length);
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
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, heightMap.length * heightMap[0].length * VERTEX_DATA_COUNT * GLM.FLOAT_SIZE, fb, GLES30.GL_STATIC_DRAW);
                GLES30.glGenBuffers(1, ebo, 0);
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0]);
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, (heightMap.length - 1) * (heightMap[0].length - 1) * INDEX_DATA_COUNT * GLM.INT_SIZE, ib, GLES30.GL_STATIC_DRAW);
                GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, VERTEX_DATA_COUNT * GLM.FLOAT_SIZE, 0);
                GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, VERTEX_DATA_COUNT * GLM.FLOAT_SIZE, 3 * GLM.FLOAT_SIZE);
                GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, VERTEX_DATA_COUNT * GLM.FLOAT_SIZE, 5 * GLM.FLOAT_SIZE);
                GLES30.glEnableVertexAttribArray(0);
                GLES30.glEnableVertexAttribArray(1);
                GLES30.glEnableVertexAttribArray(2);
                GLES30.glBindVertexArray(0);
                Log.e("Terrain init", String.valueOf(GLES30.glGetError()));
                debugVao = new int[1];
                debugVbo = new int[1];
                GLES30.glGenVertexArrays(1, debugVao, 0);
                GLES30.glBindVertexArray(debugVao[0]);
                GLES30.glGenBuffers(1, debugVbo, 0);
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, debugVbo[0]);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, heightMap.length * heightMap[0].length * 6 * GLM.FLOAT_SIZE, debugFb, GLES30.GL_STATIC_DRAW);
                GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * GLM.FLOAT_SIZE, 0);
                GLES30.glEnableVertexAttribArray(0);
                GLES30.glBindVertexArray(0);

                Log.d("Terrain", "Terrain initialized!");

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
        GLES30.glDeleteVertexArrays(1, debugVao, 0);
        GLES30.glDeleteBuffers(1, debugVbo, 0);
    }
}
class GLSceneComposite extends GLObject {

    private int handles;
    private class ObjectWrapper {
        public GLObject drawable;
        public boolean isRelative;
        public int handle;
        public ObjectWrapper(GLObject o, boolean r){
            drawable = o;
            isRelative = r;
            handle = handles;
        }
    }
    private LinkedList<ObjectWrapper> sceneObjects;
    public GLSceneComposite(){
        sceneObjects = new LinkedList<>();
        handles = 0;
    }
    @Override
    public void draw(GLRendererShaderManager shaders) {
        for(ObjectWrapper obj : sceneObjects){
            if(obj.isRelative){
                float[] relativeModel = new float[16];
                Matrix.multiplyMM(relativeModel, 0, model, 0, obj.drawable.model, 0);
                float[] m = obj.drawable.model;
                obj.drawable.model = relativeModel;
                obj.drawable.draw(shaders);
                obj.drawable.model = m;
            }else{
                obj.drawable.draw(shaders);
            }
        }
    }

    @Override
    public void drawGeometry(GLRendererShaderManager shader) {
        for(ObjectWrapper obj : sceneObjects){
            if(obj.isRelative){
                float[] relativeModel = new float[16];
                Matrix.multiplyMM(relativeModel, 0, model, 0, obj.drawable.model, 0);
                float[] m = obj.drawable.model;
                obj.drawable.model = relativeModel;
                obj.drawable.drawGeometry(shader);
                obj.drawable.model = m;
            }else{
                obj.drawable.drawGeometry(shader);
            }
        }
    }

    @Override
    public void destructor() {
        for(ObjectWrapper o : sceneObjects)
            o.drawable.destructor();
    }

    public int addObject(GLObject obj, boolean useSceneOffset) {
        sceneObjects.add(new ObjectWrapper(obj, useSceneOffset));
        return handles++;
    }
    public GLObject get(int handle){
        int index = -1;
        if((index = getIndex(handle)) != -1)
            return sceneObjects.get(index).drawable;
        else
            return null;
    }
    public void remove(int handle){
        int index = -1;
        if((index = getIndex(handle)) != -1) {
            sceneObjects.get(index).drawable.destructor();
            sceneObjects.remove(index);
        }
    }
    private int getIndex(int handle){
        int guess;
        int start = 0, end = sceneObjects.size() - 1;
        do{
            if(start == end && sceneObjects.get(start).handle == handle) return start;
            else if(start == end) return -1;
            guess = (int)Math.round((handle - sceneObjects.get(start).handle) / (float)(sceneObjects.get(end).handle - sceneObjects.get(start).handle) * (end - start) + start);
            if(guess < start) guess = start;
            else if(guess > end) guess = end;
            if(sceneObjects.get(guess).handle == handle) return guess;
            else if(sceneObjects.get(guess).handle > handle)
                end = guess - 1;
            else if(sceneObjects.get(guess).handle < handle)
                start = guess + 1;

        } while(start < end);
        return -1;
    }

}