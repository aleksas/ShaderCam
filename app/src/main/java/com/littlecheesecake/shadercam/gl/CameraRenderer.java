package com.littlecheesecake.shadercam.gl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.util.AttributeSet;

import com.littlecheesecake.shadercameraexample.R;

public class CameraRenderer extends GLSurfaceView implements 
								GLSurfaceView.Renderer, 
								SurfaceTexture.OnFrameAvailableListener{
	private Context mContext;
	private ReentrantLock mLock;
	
	/**
	 * Camera and SurfaceTexture
	 */
	private Camera mCamera;
	private SurfaceTexture mSurfaceTexture;

	private final OESTexture mCameraTexture = new OESTexture();
	private final Shader mOffscreenShader = new Shader();
	private int mWidth, mHeight;
	private boolean updateTexture = false;
	
	/**
	 * OpenGL params
	 */
	private float[] mTransformM = new float[16];
	private float[] mOrientationM = new float[16];
	private float[] mRatio = new float[2];
	private boolean mCameraPermission;
	private boolean mSurfaceChanged;

	private ShortBuffer mBackgroundQuadVertexBuffer;
	private FloatBuffer mTriangleVertexBuffer;
	private FloatBuffer mTriangleColorBuffer;

	static final int VALUES_PER_TRIANGLE_VERTEX = 3;
    static final float mTriangleCoords[] = {
			-0.5f,  0.5f, 0.0f,
			-0.5f, -0.5f, 0.0f,
			0.5f, -0.5f, 0.0f
	 };

    static final int VALUES_PER_COLOR = 4;
    static float triangleColors[] = {
			1.0f, 0.0f, 0.0f, 0.5f,
			0.0f, 1.0f, 0.0f, 0.5f,
			0.0f, 0.0f, 1.0f, 0.5f
	};

    static final int VALUES_PER_QUAD_VERTEX = 2;
    static final byte mQuadCoords[] = {
			-1, 1,
			-1, -1,
			1, 1,
			1, -1
	};

	static final int mFloatSize = Float.SIZE / Byte.SIZE;
	static final int mShortSize = Short.SIZE / Byte.SIZE;

    static final int triangleVertexCount = mTriangleCoords.length / VALUES_PER_TRIANGLE_VERTEX;
    static final int quadVertexCount = mQuadCoords.length / VALUES_PER_QUAD_VERTEX;

    public CameraRenderer(Context context) {
		super(context);
		init(context);
	}
	
	public CameraRenderer(Context context, AttributeSet attrs){
		super(context, attrs);
		init(context);
	}

	public void setCameraPermission(boolean value) {
		mCameraPermission = value;
		if (mCameraPermission) {
			lazyCameraStart();
		}
	}
	
	private void init(Context context){
		mCameraPermission = false;
		mSurfaceChanged = false;
        mContext = context;

        mLock = new ReentrantLock();
		
		setPreserveEGLContextOnPause(true);
		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		ByteBuffer shortBuffer1 = ByteBuffer.allocateDirect(mQuadCoords.length * mShortSize); // (# of coordinate values * 2 bytes per short)
		shortBuffer1.order(ByteOrder.nativeOrder());
		shortBuffer1.put(mQuadCoords).position(0);
		mBackgroundQuadVertexBuffer = shortBuffer1.asShortBuffer();
		mBackgroundQuadVertexBuffer.position(0);

		ByteBuffer floatBuffer1 = ByteBuffer.allocateDirect(mTriangleCoords.length * mFloatSize); // (# of coordinate values * 4 bytes per float)
		floatBuffer1.order(ByteOrder.nativeOrder());
		mTriangleVertexBuffer = floatBuffer1.asFloatBuffer();
		mTriangleVertexBuffer.put(mTriangleCoords);
		mTriangleVertexBuffer.position(0);

		ByteBuffer floatBuffer2 = ByteBuffer.allocateDirect(triangleColors.length * mFloatSize); // (# of coordinate values * 4 bytes per float)
		floatBuffer2.order(ByteOrder.nativeOrder());
		mTriangleColorBuffer = floatBuffer2.asFloatBuffer();
		mTriangleColorBuffer.put(triangleColors);
		mTriangleColorBuffer.position(0);
	}
	
	@Override
	public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture){
		updateTexture = true;
		requestRender();
	}

	@Override
	public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//load and compile shader
		
		try {
			mOffscreenShader.setProgram(R.raw.vshader, R.raw.fshader, mContext);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void lazyCameraStart() {
		if (mCameraPermission && mSurfaceChanged) {

			//set camera para-----------------------------------
			int camera_width =0;
			int camera_height =0;

			if(mCamera != null){
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}

			{
				mCamera = Camera.open();
				try {
					mCamera.setPreviewTexture(mSurfaceTexture);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}

				Camera.Parameters param = mCamera.getParameters();
				List<Size> psize = param.getSupportedPreviewSizes();
				if (psize.size() > 0) {
					int i;
					for (i = 0; i < psize.size(); i++) {
						if (psize.get(i).width < mWidth || psize.get(i).height < mHeight)
							break;
					}
					if (i > 0)
						i--;
					param.setPreviewSize(psize.get(i).width, psize.get(i).height);

					camera_width = psize.get(i).width;
					camera_height = psize.get(i).height;

				}

				//get the camera orientation and display dimension------------
				if (mContext.getResources().getConfiguration().orientation ==
						Configuration.ORIENTATION_PORTRAIT) {
					Matrix.setRotateM(mOrientationM, 0, 90.0f, 0f, 0f, 1f);
					mRatio[1] = camera_width * 1.0f / mHeight;
					mRatio[0] = camera_height * 1.0f / mWidth;
				} else {
					Matrix.setRotateM(mOrientationM, 0, 0.0f, 0f, 0f, 1f);
					mRatio[1] = camera_height * 1.0f / mHeight;
					mRatio[0] = camera_width * 1.0f / mWidth;
				}

				//start camera-----------------------------------------
				mCamera.setParameters(param);
				mCamera.startPreview();

				//start render---------------------
				requestRender();
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight= height;

		//generate camera texture------------------------
		mCameraTexture.init();

		//set up surfacetexture------------------
		SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
		mSurfaceTexture = new SurfaceTexture(mCameraTexture.getTextureId());
		mSurfaceTexture.setOnFrameAvailableListener(this);
		if(oldSurfaceTexture != null){
			oldSurfaceTexture.release();
		}

		mSurfaceChanged = true;
		lazyCameraStart();
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		mLock.lock();
		try {
			//render the texture to FBO if new frame is available
			if (updateTexture) {
				mSurfaceTexture.updateTexImage();
				mSurfaceTexture.getTransformMatrix(mTransformM);

				updateTexture = false;

				GLES20.glViewport(0, 0, mWidth, mHeight);

				mOffscreenShader.useProgram();

				int uTransformM = mOffscreenShader.getHandle("uTransformM");
				int uOrientationM = mOffscreenShader.getHandle("uOrientationM");
				int uRatioV = mOffscreenShader.getHandle("ratios");

				GLES20.glEnable(GLES20.GL_BLEND);
				GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

				GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
				GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
				GLES20.glUniform2fv(uRatioV, 1, mRatio, 0);

                int aPosition = mOffscreenShader.getHandle("aPosition");
                int aColor = mOffscreenShader.getHandle("aColor");

				renderCameraTexture(aPosition);
				renderTriangle(aPosition, aColor);
			}
		} finally {
			mLock.unlock();
		}
	}

    private void renderCameraTexture(int aPosition){
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTexture.getTextureId());
		GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, VALUES_PER_QUAD_VERTEX, GLES20.GL_BYTE, false, 0, mBackgroundQuadVertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, quadVertexCount);
		GLES20.glDisableVertexAttribArray(aPosition);
    }

    private void renderTriangle(int aPosition, int aColor){
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glEnableVertexAttribArray(aColor);
		GLES20.glVertexAttribPointer(aPosition, VALUES_PER_TRIANGLE_VERTEX, GLES20.GL_FLOAT, false, 0, mTriangleVertexBuffer);
		GLES20.glVertexAttribPointer(aColor, VALUES_PER_COLOR, GLES20.GL_FLOAT, false, 0, mTriangleColorBuffer);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, triangleVertexCount);
		GLES20.glDisableVertexAttribArray(aColor);
		GLES20.glDisableVertexAttribArray(aPosition);
    }
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void onDestroy(){
		mLock.lock();
		try {
			updateTexture = false;

			if(mCamera != null){
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
			if (mSurfaceTexture != null) {
				mSurfaceTexture.release();
				mSurfaceTexture = null;
			}
		} finally {
			mLock.unlock();
		}
	}
}
