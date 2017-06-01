package com.littlecheesecake.shadercam;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
//import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Window;
import android.view.WindowManager;


import com.littlecheesecake.shadercam.gl.CameraRenderer;
import com.littlecheesecake.shadercameraexample.R;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback{
	private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
	private CameraRenderer mRenderer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);
		mRenderer = (CameraRenderer)findViewById(R.id.renderer_view);

		int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
		if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
			requestPermission();
		} else {
			mRenderer.setCameraPermission(true);
		}
	}

	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					mRenderer.setCameraPermission(true);

				} else {

					mRenderer.setCameraPermission(false);
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	private void requestPermission() {
		// Here, thisActivity is the current activity
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.CAMERA)) {

				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

			} else {

				// No explanation needed, we can request the permission.

				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.CAMERA},
						MY_PERMISSIONS_REQUEST_READ_CONTACTS);

				// MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			}
		}
	}


	@Override
	public void onStart(){
		super.onStart();

	}
	
	
	@Override
	public void onPause(){
		super.onPause();
		mRenderer.onDestroy();
		
	}
	
	@Override
	public void onResume(){
		super.onResume();
		mRenderer.onResume();
	}

}
