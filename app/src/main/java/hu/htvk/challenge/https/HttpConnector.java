package hu.htvk.challenge.https;

import hu.htvk.challenge.R;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URI;

import android.content.Context;
import android.util.Log;

public class HttpConnector {

    private static final String BACKENDURL="";
	private static final String TAG = "HttpConnector";

	private static OkHttpClient client;

	private synchronized static OkHttpClient getClient() {
		if (client == null) {
			client = new OkHttpClient();
		}
		return client;
	}

	public static String execute(Context context, RequestBody requestBody) throws IOException {
		return executePost(context, requestBody, 3600000);
	}

	public static String execute(Context context, URI uri) throws IOException {
        RequestBody entity = null;
		return executePost(context, entity, 3600000);
	}

	private static String executePost(Context context, RequestBody requestBody, int soTimeOut)
			throws IOException {
		String retVal = null;
        String uri = context.getResources().getString(R.string.server);
		try {
            Request request = new Request.Builder()
                    .url(HttpUrl.get(uri))
                    .post(requestBody)
                    .build();

            Response response = getClient().newCall(request).execute();

			if (response.isSuccessful()) {
				retVal = response.body().string();
			}
		} catch (IOException e) {
			Log.e(TAG, "IO error on http call to "+uri, e);
			throw e;
		} catch (Exception e) {
			Log.e(TAG, "Error on http call to "+uri, e);
			throw new IOException("Error on http call "+ e);
		}
		return retVal;
	}

}