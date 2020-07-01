package com.sev.activitylog;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Callable;

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
    public GLRenderer(Resources res, Runnable r){
        this.res = res;
        onGlLoaded = r;

    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) { //when gl is initialized - called once
        shaders = new GLRendererShaderManager();
        shaders.setShader(res.getInteger(R.integer.basic_shader_id), new GLShader(res, R.string.basic_vertex_glsl, R.string.basic_frag_glsl));
        shaders.setShader(res.getInteger(R.integer.sky_shader_id), new GLShader(res, R.string.skybox_vert_glsl, R.string.skybox_frag_glsl));
        shaders.setShader(res.getInteger(R.integer.simple_shader_id), new GLShader(res, R.string.simple_vert_glsl, R.string.simple_frag_glsl));
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glClearColor(0.529f, 0.808f, 0.922f, 1.0f);
        onGlLoaded.run();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) { //when view changes (device orientation change)
        GLES30.glViewport(0, 0, width, height);
        float aspectRatio = (float)width / height;
        Matrix.frustumM(proj, 0, -aspectRatio, aspectRatio, -1, 1, .5f, 50);

        //right now camera is static
        Matrix.setLookAtM(view, 0, 0, 2, -1, 0, 0, 0f, 0, 1, 0);
        float[] viewProj = new float[16];
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0);
        GLShader skybox = shaders.getShader(res.getInteger(R.integer.sky_shader_id));
        GLShader basic = shaders.getShader(res.getInteger(R.integer.basic_shader_id));
        GLShader debug = shaders.getShader(res.getInteger(R.integer.simple_shader_id));
        skybox.use();
        skybox.setMat4("view", view);
        skybox.setMat4("proj", proj);
        basic.use();
        basic.setMat4("viewProj", viewProj);
        basic.setVec3("eyePos", 0, 2, -1);
        debug.use();
        debug.setVec4("color", 1, 0, 0, 1);
        debug.setMat4("viewProj", viewProj);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        if(drawLogic != null) drawLogic.run();
        if(scene != null) scene.draw(shaders);

    }


    @Override
    public void destructor() {
        if(scene != null) scene.destructor();
        shaders.destructor();
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
}
class GLRendererShaderManager implements Destructor {
    private HashMap<Integer, GLShader> shaders;
    public GLRendererShaderManager(){
        shaders = new HashMap<>();
    }
    public void setShader(int id, GLShader shader){
        shaders.put(id, shader);
    }
    public GLShader getShader(int id){
        return shaders.get(id);
    }

    @Override
    public void destructor() {
        for(GLShader shader : shaders.values())
            shader.destructor();
    }
}
class GLCamera {
    private float[] position;
    private float[] direction;
    public float[] getMat(){
        float[] matrix = new float[16];
        return matrix;
    }
    public GLCamera(){
        position = new float[3];
        direction = new float[3];
    }
    public void setPos(float x, float y, float z){
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }
    public void rotateLook(float radians, float x, float y, float z){
        float[] mat = new float[16];
    }

}

