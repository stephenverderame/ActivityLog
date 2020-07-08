package com.sev.activitylog;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import java.util.HashMap;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//used to clean up openGL resources
//probably a better java way to do this
interface Destructor {
    public void destructor();
}
public class GLView extends GLSurfaceView implements Subject, Destructor {
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
/*    @Override
    public void onVisibilityChanged(View v, int vis){
        super.onVisibilityChanged(v, vis);
        if(vis == 8){ //when view is invisible
            renderer.destructor();
        }
    }*/
    @Override
    public boolean onTouchEvent(MotionEvent e){
        for(Observer o : obs)
            o.notify(new ObserverEventArgs(ObserverNotifications.TOUCH_NOTIFY, e));
        return true;
    }

    public void setDrawLogic(Runnable run){
        renderer.setDrawLogic(run);
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

    @Override
    public void destructor() {
        renderer.destructor();
    }
}
class GLRenderer implements GLSurfaceView.Renderer, Destructor {

    private float[] view = new float[16];
    private float[] proj = new float[16];
    private float[] viewProj = new float[16];
    private final Resources res;
    private GLRendererShaderManager shaders;
    private GLSceneComposite scene;
    private Runnable drawLogic;
    private final Runnable onGlLoaded;
    private GLCamera cam;
    private int width, height;
    private LinkedList<GLRenderPass> renderPasses;
    public GLRenderer(Resources res, Runnable r){
        this.res = res;
        onGlLoaded = r;
        renderPasses = new LinkedList<>();
        shaders = new GLRendererShaderManager();

    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) { //when gl is initialized - called once
        shaders.setShader(res.getInteger(R.integer.basic_shader_id), new GLShader(res, R.raw.basic_vertex, R.raw.basic_frag));
        shaders.setShader(res.getInteger(R.integer.sky_shader_id), new GLShader(res, R.raw.skybox_vert, R.raw.skybox_frag));
        shaders.setShader(res.getInteger(R.integer.debug_shader_id), new GLShader(res, R.raw.debug_vert, R.raw.debug_frag));
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
        final float aspectRatio = (float)width / height;
        cam.setProjection(aspectRatio, -1);
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
            for(GLRenderPass pass : renderPasses) {
                pass.draw(scene, shaders, j++, this);
            }
            for(int i = 0; i < renderPasses.size(); ++i)
                renderPasses.get(i).bindForReading(i + 1);
            if(renderPasses.size() >= 1) GLES30.glViewport(0, 0, width, height);
            scene.draw(shaders);
        }

    }
    private void updateViewProjMat() {
        view = cam.getView();
        proj = cam.getProj();
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
    //Code to be run on ever frame
    public void setDrawLogic(Runnable run){
        this.drawLogic = run;
    }
    public void setCam(GLCamera c){
        this.cam = c;
    }
    public void addRenderPass(GLRenderPass pass) {this.renderPasses.add(pass);}
    public void removRenderPass(GLRenderPass p) {this.renderPasses.remove(p);}
    public float aspectRatio() {return (float)width / height;}
    public GLCamera getCam() {return cam;}
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
class  GLCamera {
    public static final float[] UP_VECTOR = {0, 1, 0};
    private static float viewNear = 0.5f;
    private float[] position, direction, right, localUp;
    float yaw, pitch, fov, aspect, viewDistance;
    private float[] view, proj;
    private boolean dirtyBit, dirtyAngle, dirtyProj;
    public float[] getView(){
        if(dirtyBit || dirtyAngle){
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
    public float[] getProj(){
        if(dirtyProj){
            Matrix.perspectiveM(proj, 0, fov, aspect, viewNear, viewDistance);
            dirtyProj = false;
        }
        return proj;
    }
    public GLCamera(float[] eye, float[] center) {
        view = new float[16];
        proj = new float[16];
        position = eye;
        direction = GLM.normalize(GLM.subtract(center, eye));
        yaw = (float)Math.asin(-direction[1]);
        pitch = (float)Math.atan2(direction[0], direction[2]);
        right = GLM.normalize(GLM.cross(UP_VECTOR, direction));
        localUp = GLM.normalize(GLM.cross(direction, right));
        dirtyBit = true;
        dirtyAngle = false;
        dirtyProj = false;
        fov = 90;
        viewDistance = 20;
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
        dirtyProj = false;
        fov = 90;
        viewDistance = 50;
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
        final float maxVerticalRads = 1.553343f; //89 degrees in radians
        if(this.pitch < -maxVerticalRads) this.pitch = -maxVerticalRads;
        if(this.pitch > maxVerticalRads) this.pitch = maxVerticalRads;
        dirtyAngle = true;
    }
    public void rotatePosAboutFocus(float x, float y, float z, float r){
        float[] mat = new float[16];
        float[] target = GLM.add(position, direction);
        Matrix.setIdentityM(mat, 0);
//        Matrix.translateM(mat, 0, position[0], position[1], position[2]);
        Matrix.rotateM(mat, 0, x, y, z, r);
//        Matrix.translateM(mat, 0, position[0], position[1], position[2]);
        float[] inV = new float[] {position[0], position[1], position[2], 1};
        float[] outV = new float[4];
        Matrix.multiplyMV(outV, 0, mat, 0, inV, 0);
        position[0] = outV[0];
        position[1] = outV[1];
        position[2] = outV[2];
        dirtyBit = true;
    }
    private void updateVectors(){
        direction[0] = (float)(Math.cos(yaw) * Math.cos(pitch));
        direction[1] = (float)Math.sin(pitch);
        direction[2] = (float)(Math.sin(yaw) * Math.cos(pitch));
        direction = GLM.normalize(direction);
        right = GLM.normalize(GLM.cross(UP_VECTOR, direction));
        localUp = GLM.normalize(GLM.cross(direction, right));
    }
    public void translate(float right, float vert, float forward){
        float[] forw = GLM.normalize(GLM.cross(this.right, UP_VECTOR));
        float fac = fov < 90 ? (fov / 90.f) : 1.f;
        right *= fac;
        forward *= fac;
        vert *= fac;
        position = GLM.add(position, GLM.scale(this.right, right));
        position = GLM.add(position, GLM.scale(forw, forward));
        dirtyBit = true;
    }
    public float[] getPos() {return position;}
    public void lookAt(float[] center){
        direction = GLM.normalize(GLM.subtract(center, position));
        yaw = (float)Math.asin(-direction[1]);
        pitch = (float)Math.atan2(direction[0], direction[2]);
        right = GLM.normalize(GLM.cross(UP_VECTOR, direction));
        localUp = GLM.normalize(GLM.cross(direction, right));
        dirtyBit = true;
    }
    public boolean shouldRefresh() {return dirtyBit || dirtyAngle || dirtyProj;}
    public float getYaw() {return yaw;}
    public float getPitch() {return pitch;}
    public float getFov() {return fov;}
    public void setFov(float f) {
        fov = f;
        if(fov < 1)
            fov = 1;
        if(fov > 270)
            fov = 270;
        dirtyProj = true;
    }
    public void setProjection(float aspectRatio, float viewDistance){
        if(aspectRatio >= 0)
            this.aspect = aspectRatio;
        if(viewDistance >= 0)
            this.viewDistance = viewDistance;
        dirtyProj = true;
    }
    public float getViewDistance() {return viewDistance;}
    public float getNearPlane() {return viewNear;}

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
    public abstract void draw(GLSceneComposite scene, GLRendererShaderManager sm, int passIndex, GLRenderer renderer);
    public abstract float[] getViewProjMat();
}
class GLShadowPass extends GLRenderPass {
    private float[] center;
    private float[] viewProj;
    private GLRendererShaderManager shaderManager;
    private final Resources rs;
    public static final int SHADOWMAP_RES = 2048;
    private final DirectionalLight caster;
    float[] minBounds, maxBounds;
    public GLShadowPass(final DirectionalLight caster, final float[] center, Resources resources) {
        super(resources.getInteger(R.integer.depth_shader_id));
        this.caster = caster;
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
        viewProj = new float[16];
        shaderManager = new GLRendererShaderManager(renderShaderId);
        shaderManager.setShader(renderShaderId, new GLShader(resources, R.raw.debug_vert, R.raw.depth_frag));
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
    public void draw(GLSceneComposite scene, GLRendererShaderManager sm, int index, GLRenderer renderer) {
        viewProj = computeShadowFrustrum(renderer.getCam().getView(), caster.getPos(), center, /*renderer.getCam().getNearPlane(), renderer.getCam().getViewDistance(), renderer.getCam().getFov(),*/ 0.1f, 3f,  45, renderer.aspectRatio());
        GLShader shader = sm.getShader(rs.getInteger(R.integer.basic_shader_id));
        shader.use();
        shader.setMat4("lightSpaceMatrix", viewProj);
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
    //based off CSM - compute tight fitting light frustrums. Only using one cascade here
    float[] computeShadowFrustrum(float[] camViewProj, float[] lightPos, float[] center, float near, float far, float fov, float aspectRatio){
        lightPos = new float[] {-10, 3, 0}; //manual placement to get shadows
//        fov = 45;
 //       near = 2;
        float[] viewInv = new float[16];
        Matrix.invertM(viewInv, 0, camViewProj, 0);
//        final float scaleX = 1.0f / viewInv[0], scaleY = 1.f / viewInv[5], scaleZ = 1.f / viewInv[11];
//        final float nearX = scaleX * near, nearY = scaleY * near, farX = scaleX * far, farY = scaleY * far, nearZ = scaleZ * near, farZ = scaleZ * far;
/*        float[][] cube = new float[][] {
            {-nearX, nearY, near, 1}, {nearX, nearY, near, 1}, {nearX, -nearY, near, 1}, {-nearX, -nearY, near, 1},
                {-farX, farY, far, 1}, {farX, farY, far, 1}, {farX, -farY, far, 1}, {-farX, -farY, far, 1}
        };*/
//        near = 3;
/*        float[][] cube = new float[][] {
                {-near, near, -near, 1}, {near, near, -near, 1}, {near, -near, -near, 1}, {-near, -near, -near, 1},
                {-near, near, near, 1}, {near, near, near, 1}, {near, -near, near, 1}, {-near, -near, near, 1}
        };*/
/*        aspectRatio = 1/aspectRatio;
        final float nx = near * (float)Math.tan(Math.toRadians(fov / 2)), ny = near * (float)Math.tan(Math.toRadians(fov * aspectRatio / 2)),
        fx = far * (float)Math.tan(Math.toRadians(fov / 2)), fy = far * (float)Math.tan(Math.toRadians(fov * aspectRatio / 2));
        float[][] cube = new float[][] {
                {-nx, ny, near, 1}, {nx, ny, near, 1}, {nx, -ny, near, 1}, {-nx, -ny, near, 1},
                {-fx, fy, far, 1}, {fx, fy, far, 1}, {fx, -fy, far, 1}, {-fx, -fy, far, 1}
        };*/
        float[] lightView = new float[16];
        float[] direction = GLM.normalize(GLM.subtract(center, lightPos));
        float[] right = GLM.normalize(GLM.cross(direction, new float[]{0, 1, 0}));
        float[] up = GLM.normalize(GLM.cross(direction, right));
        Matrix.setLookAtM(lightView, 0, lightPos[0], lightPos[1], lightPos[2], center[0], center[1], center[2], up[0], up[1], up[0]);
 //       Matrix.setLookAtM(lightView, 0, lightPos[0], lightPos[1], lightPos[2], center[0], center[1], center[2], 0, 1, 0);
 /*       for(int i = 0; i < 8; ++i){
            float[] copy = GLM.copy(cube[i]);
            float[] out = new float[4];
            Matrix.multiplyMV(out, 0, viewInv, 0, copy, 0);
            out = GLM.scale(out, 1.f / out[3]);
            Matrix.multiplyMV(cube[i], 0, lightView, 0, copy, 0);
        }
        float orthoRight = Integer.MIN_VALUE, orthoUp = Integer.MIN_VALUE, orthoLeft = Integer.MAX_VALUE, orthoDown = Integer.MAX_VALUE, orthoNear = Integer.MAX_VALUE, orthoFar = Integer.MIN_VALUE;
        for(int i = 0; i < 8; ++i){
            float x;
            if(cube[i][0] > orthoRight){
                orthoRight = cube[i][0];
            }
            if(cube[i][1] > orthoUp){
                orthoUp = cube[i][1];
            }
            if(cube[i][0] < orthoLeft){
                orthoLeft = cube[i][0];
            }
            if(cube[i][1] < orthoDown){
                orthoDown = cube[i][1];
            }
            if(Math.abs(cube[i][2]) < orthoNear){
                orthoNear = Math.abs(cube[i][2]);
            }
            if(Math.abs(cube[i][2]) > orthoFar){
                orthoFar = Math.abs(cube[i][2]);
            }
        }
/*        float[][] objBounds = new float[][] {{-.025f, -.010439033f, -.033333335f, 1.0f}, {0.025f, 0.0019883872f, 0.0333333335f, 1.f}};
        objBounds[0] = GLM.scale(objBounds[0], 100f);
        objBounds[1] = GLM.scale(objBounds[1], 100f);
        float[] out = new float[4];
        float[] out2 = new float[4];
        Matrix.multiplyMV(out, 0, lightView, 0, objBounds[0], 0);
        Matrix.multiplyMV(out2, 0, lightView, 0, objBounds[1], 0);
        orthoLeft = Math.min(out[0], out2[0]); orthoRight = Math.max(out[0], out2[0]); orthoDown = Math.min(out[1], out2[1]); orthoUp = Math.max(out[1], out2[1]); orthoNear = Math.min(out[2], out2[2]); orthoFar = Math.max(out[2], out2[2]);
*/
        float[] shadowProj = new float[16];
//        Matrix.orthoM(shadowProj, 0, orthoLeft, orthoRight, orthoDown, orthoUp, 5, 30);
        Matrix.orthoM(shadowProj, 0, -10, 10, -3, 3, 7, 30);
//        Matrix.orthoM(shadowProj, 0, -10, 10, -5, 5, 5, 30);
        float[] shadowViewProj = new float[16];
        Matrix.multiplyMM(shadowViewProj, 0, shadowProj, 0, lightView, 0);
        return shadowViewProj;
    }
}

