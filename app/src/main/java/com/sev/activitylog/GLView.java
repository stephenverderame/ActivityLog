package com.sev.activitylog;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.View;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//used to clean up openGL resources
//probably a better java way to do this
interface Destructor {
    public void destructor();
}
public class GLView extends GLSurfaceView {
    private final GLRenderer renderer;
    public GLView(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        renderer = new GLRenderer(getContext());
        setRenderer(renderer);
//        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


    }
    @Override
    public void onVisibilityChanged(View v, int vis){
        super.onVisibilityChanged(v, vis);
        if(vis == 8){ //when view is invisible
            renderer.destructor();
        }
    }


}
class GLRenderer implements GLSurfaceView.Renderer, Destructor {

    private final float[] view = new float[16];
    private float[] proj = new float[16];
    private Context resCtx;
    private GLShader basic;
    private Cube box;
    private GLShader skybox;
    private Skybox sky;
    public GLRenderer(Context resCtx){
        this.resCtx = resCtx;

    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) { //when gl is initialized - called once
        basic = new GLShader(resCtx, R.string.basic_vertex_glsl, R.string.basic_frag_glsl);
        skybox = new GLShader(resCtx, R.string.skybox_vert_glsl, R.string.skybox_frag_glsl);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClearColor(0.529f, 0.808f, 0.922f, 1.0f);
        box = new Cube(basic);
        sky = new Skybox(skybox, new int[] {R.drawable.bluecloud_rt, R.drawable.bluecloud_lf, R.drawable.bluecloud_up, R.drawable.bluecloud_dn, R.drawable.bluecloud_ft, R.drawable.bluecloud_bk}, resCtx); //https://opengameart.org/content/cloudy-skyboxes
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) { //when view changes (device orientation change)
        GLES30.glViewport(0, 0, width, height);
        float aspectRatio = (float)width / height;
        Matrix.frustumM(proj, 0, -aspectRatio, aspectRatio, -1, 1, 1, 50);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        Matrix.setLookAtM(view, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0);
        float[] viewProj = new float[16];
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0);
        skybox.use();
        skybox.setMat4("view", view);
        skybox.setMat4("proj", proj);
        sky.draw(skybox);
        basic.use();
        basic.setMat4("viewProj", viewProj);
        basic.setBool("useColor", true);
        basic.setVec4("color", 1.0f, 0.0f, 0.0f, 1.0f);
        box.clearModel();
        box.scale(0.5f, 0.5f, 0.5f);
        box.translate(0.f, 0.f, 0.f);
        box.rotate((System.currentTimeMillis() % 4000L) * 0.09f, 0, 1, 0);
        box.draw(basic);
    }

    @Override
    public void destructor() {
        skybox.destructor();
        basic.destructor();
        box.destructor();
        sky.destructor();
    }
}

