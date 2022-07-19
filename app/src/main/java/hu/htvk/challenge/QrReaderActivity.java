/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 * 
 * Created by lisah0 on 2012-02-24
 */
package hu.htvk.challenge;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import hu.htvk.challenge.https.HttpConnector;
import hu.htvk.challenge.json.AuthData;
import hu.htvk.challenge.json.FetchData;
import hu.htvk.challenge.json.ProgramData;
import hu.htvk.challenge.json.Result;
import hu.htvk.challenge.utils.Constants;
import okhttp3.FormBody;
import okhttp3.RequestBody;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

public class QrReaderActivity extends Activity {
	private AuthData ad;
	private ProgramData pd;

	final Context context = this;
	private Camera mCamera;
	protected Parameters mCameraParameters;
	private CameraPreview mPreview;
	private Handler autoFocusHandler;

//	Button scanButton;
	Button vakuButton;
//	ImageView im;
//	TextView tv1;
	FrameLayout cameraLayout;

	//TextView scanText;

	ImageScanner scanner;

	private String previousText;
	
	static {
		System.loadLibrary("iconv");
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		ad = getIntent().getParcelableExtra(Constants.AUTHDATA_EXTRA);
		pd = getIntent().getParcelableExtra(Constants.PROGRAMDATA_EXTRA);

		autoFocusHandler = new Handler();
		mCamera = getCameraInstance();
		mCameraParameters = mCamera.getParameters();
		mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		mCameraParameters.setPreviewFormat(ImageFormat.NV21);
		mCamera.setParameters(mCameraParameters);
		
		/* Instance barcode scanner */
		scanner = new ImageScanner();
		scanner.setConfig(0, Config.X_DENSITY, 2);
		scanner.setConfig(0, Config.Y_DENSITY, 2);

		mPreview = new CameraPreview(this, mCamera, previewCb, null);
		FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
		preview.addView(mPreview);

		//scanText = (TextView) findViewById(R.id.scanText);

//		scanButton = (Button) findViewById(R.id.ScanButton);
//		scanButton.setVisibility(View.VISIBLE);
//		
//		scanButton.setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//				if (barcodeScanned) {
//					((LinearLayout) QrReaderActivity.this.findViewById(R.id.ll1))
//							.setBackgroundColor(QrReaderActivity.this
//									.getResources().getColor((R.color.black)));
//					scanButton.setVisibility(View.GONE);
//					barcodeScanned = false;
//					//scanText.setText(getResources().getString(R.string.text_scanning));
//					mCamera.setPreviewCallback(previewCb);
//					mCamera.startPreview();
//					previewing = true;
//					mCamera.autoFocus(autoFocusCB);
//					cameraLayout.setVisibility(View.VISIBLE);
//					//scanText.setVisibility(View.VISIBLE);
//				}
//			}
//		});

		vakuButton = (Button) findViewById(R.id.VakuButton);
		vakuButton.setVisibility(View.VISIBLE);
		vakuButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mCamera.stopPreview();
				Parameters cp = mCamera.getParameters();
				if (vakuButton.isActivated()) {
					vakuButton.setActivated(false);
					cp.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					Log.i(getClass().getName(), "CameraFlash switched off.");
					mCamera.setParameters(cp);
				} else	if (cp.getFlashMode()!=null) {
					Log.i(getClass().getName(), "CameraFlash state: "+cp.getFlashMode());
					cp.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
					Log.i(getClass().getName(), "CameraFlash switched on.");
					mCamera.setParameters(cp);
					vakuButton.setActivated(true);
				}
				mCamera.startPreview();
			}
		});
	}

	public void onPause() {
		releaseCamera();
		super.onPause();
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
		}
		return c;
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

//	private Runnable doAutoFocus = new Runnable() {
//		public void run() {
//			if (previewing)
//				mCamera.autoFocus(autoFocusCB);
//		}
//	};

	PreviewCallback previewCb = new PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera camera) {
//			Camera.Parameters parameters = camera.getParameters();
			Size size = mCameraParameters.getPreviewSize();
			Log.i("htvkreader", "CameraSize : "+size.width+"x"+ size.height);
			Image barcode = new Image(size.width, size.height, "NV21");
			barcode.setData(data);
			Image converted = barcode.convert("Y800");
			
			int result = scanner.scanImage(converted);

			
			
			if (result != 0) {

				SymbolSet syms = scanner.getResults();
				String qrtext = "";
				for (Symbol sym : syms) {
					qrtext = sym.getData();
					if(!qrtext.equals(previousText)){

							checkQrCode(qrtext);

							previousText = qrtext;
						    //scanText.setText(qrtext + " " + getString(R.string.text_scanned));
					}
				}

			}
		}
	};

	// Mimic continuous auto-focusing
//	AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
//		public void onAutoFocus(boolean success, Camera camera) {
//			autoFocusHandler.postDelayed(doAutoFocus, 1000);
//		}
//	};

	
	private void checkQrCode(String qrtext) {
		if (qrtext.startsWith(Constants.QRPREFIX)) {
		    String qrcode=qrtext.substring(Constants.QRPREFIX.length());

			RequestBody body = new FormBody.Builder(Charset.forName("UTF-8"))
					.add("email",ad.getUserName())
					.add("regcode",ad.getPassword())
                    .add("op", "checkin")
					.add("qrcode", qrcode)
					.build();

			try {
				new Comm1AsynchTask().execute(body);
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
		}
	}
	

	private void askExit() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				this);
		alertDialogBuilder.setTitle(getResources().getString(R.string.text_exit));
		alertDialogBuilder
				.setMessage(getResources().getString(R.string.text_confirm))
				.setCancelable(false)
				.setPositiveButton(getResources().getString(R.string.text_yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								releaseCamera();
								QrReaderActivity.this.finish();
								new LoginActivity.Comm2AsynchTask(pd,ad,QrReaderActivity.this).execute();
							}
						})
				.setNegativeButton(getResources().getString(R.string.text_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			askExit();
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	

	private class Comm1AsynchTask extends AsyncTask<RequestBody, Integer, String> {
		
		@Override
		protected String doInBackground(RequestBody... params) {
			try{
				String ret = HttpConnector.execute(getApplicationContext(),
						params[0]);
				
				return ret;
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String ret) {
			try{
				Log.d(getClass().getName(), ret);
				Result result = new Result();
				if (ret.length() > 0) {
					JSONObject resultObject = new JSONObject(ret);
					result.setResultCode(resultObject.getInt(Constants.TAG_RESULTCODE));
					Log.d(getClass().getName(), "" + result.getResultCode());
					if (result.getResultCode() >0) {
						FetchData fd = new FetchData(resultObject);
						result.setFetchData(fd);
						
						Intent intent = new Intent(getApplicationContext(),SzekcioListaActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra(Constants.FETCHDATA_EXTRA,fd);
						intent.putExtra(Constants.AUTHDATA_EXTRA, ad);
						intent.putExtra(Constants.PROGRAMDATA_EXTRA, pd);
						startActivity(intent);
					} else {
						Toast toast = Toast.makeText(QrReaderActivity.this,
								getString(R.string.upload_error),
								Toast.LENGTH_SHORT);
						toast.show();
					}
				}
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
//			} finally {
//				scanButton.setVisibility(View.VISIBLE);
			}
			
		}
		
	} 
}
