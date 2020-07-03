package com.sev.activitylog;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//used to clean up openGL resources
//probably a better java way to do this
interface Destructor {
    public void destructor();
}
public class GLView extends GLSurfaceView implements Subject {
    private final GLRenderer renderer;
    private LinkedList<Observer> obs;
    public GLView(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        renderer = new GLRenderer(context.getResources(), new Runnable() {
            @Override
            public void run() {
                for(Observer o : obs)
                    o.notify(new ObserverEventArgs(ObserverNotifications.OPENGL_INIT_NOTIFY));
            }
        });
        setRenderer(renderer);
        obs = new LinkedList<>();
//        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


    }
    @Override
    public void onVisibilityChanged(View v, int vis){
        super.onVisibilityChanged(v, vis);
        if(vis == 8){ //when view is invisible
            renderer.destructor();
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent e){
        for(Observer o : obs)
            o.notify(new ObserverEventArgs(ObserverNotifications.TOUCH_NOTIFY, e));
        return true;
    }
    public void setDrawLogic(Runnable run){
        renderer.setDrawLogic(run);
    }
    public void setRenderMode(int mode){
        setRenderMode(mode);
    }
    public void setRendererScene(GLSceneComposite scene){
        renderer.setScene(scene);
    }
    public void addRendererPass(GLRenderPass p) {renderer.addRenderPass(p);}
    public void setRendererView(GLCamera camera){
        renderer.setCam(camera);
    }


    @Override
    public void attach(Observer observer) {
        obs.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        obs.remove(observer);
    }
}
class GLRenderer implements GLSurfaceView.Renderer, Destructor {

    private float[] view = new float[16];
    private float[] proj = new float[16];
    private Resources res;
    private GLRendererShaderManager shaders;
    private GLSceneComposite scene;
    private Runnable drawLogic;
    private GLObject cube;
    private Runnable onGlLoaded;
    private GLCamera cam;
    private int width, height;
    private LinkedList<GLRenderPass> renderPasses;
    public GLRenderer(Resources res, Runnable r){
        this.res = res;
        onGlLoaded = r;
        renderPasses = new LinkedList<>();

    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) { //when gl is initialized - called once
        shaders = new GLRendererShaderManager();
        shaders.setShader(res.getInteger(R.integer.basic_shader_id), new GLShader(res, R.string.basic_vertex_glsl, R.string.basic_frag_glsl));
        shaders.setShader(res.getInteger(R.integer.sky_shader_id), new GLShader(res, R.string.skybox_vert_glsl, R.string.skybox_frag_glsl));
        shaders.setShader(res.getInteger(R.integer.debug_shader_id), new GLShader(res, R.string.debug_vert_glsl, R.string.debug_frag_glsl));
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glClearColor(0.529f, 0.808f, 0.922f, 1.0f);
        onGlLoaded.run();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) { //when view changes (device orientation change)
        GLES30.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
        float aspectRatio = (float)width / height;
        Matrix.frustumM(proj, 0, -aspectRatio, aspectRatio, -1, 1, .5f, 50);
        updateViewProjMat();

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        if(cam.shouldRefresh()) updateViewProjMat();
        if(drawLogic != null) drawLogic.run();
        if(scene != null){
            int j = 1;
            for(GLRenderPass pass : renderPasses)
                pass.draw(scene, shaders, j++);
            for(int i = 0; i < renderPasses.size(); ++i)
                renderPasses.get(i).bindForReading(i + 1);
            if(renderPasses.size() >= 1) GLES30.glViewport(0, 0, width, height);
            scene.draw(shaders);
        }

    }
    private void updateViewProjMat() {
        view = cam.getMat();
        float[] viewProj = new float[16];
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0);
        GLShader skybox = shaders.getShader(res.getInteger(R.integer.sky_shader_id));
        GLShader basic = shaders.getShader(res.getInteger(R.integer.basic_shader_id));
        GLShader debug = shaders.getShader(res.getInteger(R.integer.debug_shader_id));
        skybox.use();
        skybox.setMat4("view", view);
        skybox.setMat4("proj", proj);
        basic.use();
        basic.setMat4("viewProj", viewProj);
        basic.setVec3("eyePos", 0, 2, -1);
        basic.setInt("tex", 0);
        basic.setInt("shadowMap", 1);
        debug.use();
        debug.setVec4("color", 1, 0, 0, 1);
        debug.setMat4("viewProj", viewProj);
    }


    @Override
    public void destructor() {
        if(scene != null) scene.destructor();
        shaders.destructor();
        for(GLRenderPass p: renderPasses)
            p.destructor();
    }
    public void setScene(GLSceneComposite scene){
        this.scene = scene;
    }
    public void setDrawLogic(Runnable run){
        this.drawLogic = run;
    }
    public void setCam(GLCamera c){
        this.cam = c;
    }
    public void addRenderPass(GLRenderPass pass) {this.renderPasses.add(pass);}
    public void removRenderPass(GLRenderPass p) {this.renderPasses.remove(p);}
}
class GLRendererShaderManager implements Destructor {
    private HashMap<Integer, GLShader> shaders;
    private int defaultShader;
    public GLRendererShaderManager(){
        shaders = new HashMap<>();
        defaultShader = 0;
    }
    public GLRendererShaderManager(int defaultId){
        shaders = new HashMap<>();
        defaultShader = defaultId;
    }
    public void setShader(int id, GLShader shader){
        shaders.put(id, shader);
    }
    public GLShader getShader(int id){
        return shaders.containsKey(id) ? shaders.get(id) : shaders.get(defaultShader);
    }

    @Override
    public void destructor() {
        for(GLShader shader : shaders.values())
            shader.destructor();
    }
    public void setDefaultShader(int defaultId){
        defaultShader = defaultId;
    }

}
class GLCamera {
    public static final float[] UP_VECTOR = {0, 1, 0};
    private float[] position, direction, right, localUp;
    float yaw, pitch;
    private float[] view;
    private boolean dirtyBit, dirtyAngle;
    public float[] getMat(){
        if(shouldRefresh()){
            if(dirtyAngle){
                updateVectors();
            }
            float[] center = GLM.add(position, direction);
            Matrix.setLookAtM(view, 0, position[0], position[1], position[2], center[0], center[1], center[2], localUp[0], localUp[1], localUp[2]);
            dirtyBit = false;
            dirtyAngle = false;
        }
        return view;
    }
    public GLCamera(float[] eye, float[] center) {
        view = new float[16];
        position = eye;
        direction = GLM.normalize(GLM.subtract(center, eye));
        yaw = (float)Math.asin(-direction[1]);
        pitch = (float)Math.atan2(direction[0], direction[2]);
        right = GLM.normalize(GLM.cross(UP_VECTOR, direction));
        localUp = GLM.normalize(GLM.cross(direction, right));
        dirtyBit = true;
        dirtyAngle = false;
    }
    public GLCamera(float[] eye, float yaw, float pitch) {
        view = new float[16];
        position = eye;
        direction = new float[3];
        this.yaw = yaw;
        this.pitch = pitch;
        right = new float[3];
        localUp = new float[3];
        dirtyBit = true;
        dirtyAngle = true;
    }
    public void setPos(float x, float y, float z){
        position[0] = x;
        position[1] = y;
        position[2] = z;
        dirtyBit = true;
    }
    public void rotateLook(float yaw, float pitch){
        this.yaw = yaw;
        this.pitch = pitch;
        dirtyAngle = true;
    }
    private void updateVectors(){
        direction[0] = (float)(Math.cos(yaw) * Math.cos(pitch));
        direction[1] = (float)Math.sin(pitch);
        direction[2] = (float)(Math.sin(yaw) * Math.cos(pitch));
        direction = GLM.normalize(direction);
        right = GLM.normalize(GLM.cross(UP_VECTOR, direction));
        localUp = GLM.normalize(GLM.cross(direction, right));
    }
    public void translate(float x, float y, float z){
        position[0] += x;
        position[1] += y;
        position[2] += z;
        dirtyBit = true;
    }
    public void lookAt(float[] center){
        direction = GLM.normalize(GLM.subtract(center, position));
        yaw = (float)Math.asin(-direction[1]);
        pitch = (float)Math.atan2(direction[0], direction[2]);
        right = GLM.normalize(GLM.cross(UP_VECTOR, direction));
        localUp = GLM.normalize(GLM.cross(direction, right));
        dirtyBit = true;
    }
    public boolean shouldRefresh() {return dirtyBit || dirtyAngle;}
    public float getYaw() {return yaw;}
    public float getPitch() {return pitch;}

}
abstract class GLRenderPass implements Destructor {
    protected int[] fbo, tex;
    protected int renderShaderId;
    public GLRenderPass(int renderShaderId){
        this.renderShaderId = renderShaderId;
    }
    public int getRenderPassShaderId(){
        return renderShaderId;
    }
    public void bindForWriting(){
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0]);
    }
    public void bindForReading(int textureBinding){
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureBinding);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0]);

    }
    public void unbindWriting(){
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }
    public void unbindReading(){
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    /**
     * Draws the scene onto the render buffer. Function should handle all respective uniforms on other shader
     * @param scene scene to draw
     * @param sm shader manager to update uniforms in other shaders
     * @param passIndex index onto which texture this pass should bind to
     */
    public abstract void draw(GLSceneComposite scene, GLRendererShaderManager sm, int passIndex);
    public abstract float[] getViewProjMat();
}
class GLShadowPass extends GLRenderPass {
    private float[] eye, center;
    private float[] viewProj;
    private GLRendererShaderManager shaderManager;
    private Resources rs;
    public static final int SHADOWMAP_RES = 2048;
    public GLShadowPass(DirectionalLight caster, float[] center, Resources resources) {
        super(resources.getInteger(R.integer.depth_shader_id));
        eye = caster.getPos();
        this.rs = resources;
        this.center = center;
        fbo = new int[1];
        tex = new int[1];
        GLES30.glGenFramebuffers(1, fbo, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0]);
        GLES30.glGenTextures(1, tex, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT, SHADOWMAP_RES, SHADOWMAP_RES, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_FLOAT, null);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, tex[0], 0);
        GLES30.glReadBuffer(GLES30.GL_NONE);
        int[] drawBuffer = new int[] {GLES30.GL_NONE};
        GLES30.glDrawBuffers(1, drawBuffer, 0);
        int res;
        if((res = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)) != GLES30.GL_FRAMEBUFFER_COMPLETE){
            Log.e("Shadow Map", "Framebuffer error " + res + " " + GLES30.glGetError());
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        float[] proj = new float[16];
        float[] view = new float[16];
        viewProj = new float[16];
        Matrix.orthoM(proj, 0, -5, 5, -5, 5, 1, 20);
//        Matrix.frustumM(proj, 0, -1, 1, -1, 1, 1, 20);
        Matrix.setLookAtM(view, 0, eye[0], eye[1], eye[2], center[0], center[1], center[2], 0, 1, 0);
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0);
        shaderManager = new GLRendererShaderManager(renderShaderId);
        shaderManager.setShader(renderShaderId, new GLShader(resources, R.string.debug_vert_glsl, R.string.depth_frag_glsl));
        shaderManager.getShader(renderShaderId).use();
        shaderManager.getShader(renderShaderId).setMat4("viewProj", viewProj);
    }

    @Override
    public void destructor() {
        GLES30.glDeleteFramebuffers(1, fbo, 0);
        GLES30.glDeleteTextures(1, tex, 0);
        shaderManager.destructor();
    }

    @Override
    public void draw(GLSceneComposite scene, GLRendererShaderManager sm, int index) {
        GLShader shader = sm.getShader(rs.getInteger(R.integer.basic_shader_id));
        shader.use();
        shader.setMat4("lightSpaceMatrix", viewProj);
//        shader.setInt("shadowMap", index);
        this.shaderManager.getShader(renderShaderId).use();
        this.shaderManager.getShader(renderShaderId).setMat4("viewProj", viewProj);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0]);
        GLES30.glViewport(0, 0, SHADOWMAP_RES, SHADOWMAP_RES);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);
        scene.drawGeometry(shaderManager);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public float[] getViewProjMat() {
        return viewProj;
    }
    public void setCenter(float[] center){
        this.center = center;
    }
}

