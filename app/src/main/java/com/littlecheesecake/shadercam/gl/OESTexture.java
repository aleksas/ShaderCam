package com.littlecheesecake.shadercam.gl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.IntBuffer;

/**
 * This class defines the OES Texture that can be attached to SurfaceTexture
 * which is updated to the most recent camera frame image when requested.
 * @author yulu
 *
 */
public class OESTexture {
	private int mTextureHandle;
	private int mBufferHandle;

	public OESTexture() {
		// TODO Auto-generated constructor stub
		
	}

	public int getTextureId(){
		return mTextureHandle;
	}
	
	public void init(){
		int [] handles = new int[1];
		GLES20.glGenTextures(1, handles, 0);
		mTextureHandle = handles[0];
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandle);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
	}

}
