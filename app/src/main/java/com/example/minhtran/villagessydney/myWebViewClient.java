package com.example.minhtran.villagessydney;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.HttpAuthHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class myWebViewClient extends WebViewClient {

    Context mContext;
    private boolean isLoggedIn;
    public static final String ACTION_TRANSFER_URL = "ACTION_TRANSFER_URL";
    public static final String URL = "URL";
    final ProgressDialog pd;

    /** Constructor to set the context */
    myWebViewClient(Context c) {
        mContext = c;
        pd = ProgressDialog.show(mContext, "", "Loading", true);

    }

   public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
        Log.i("LOGIN", "on  http auth request");
       // handler.proceed(" test@moble.com.au", "test@moble.com.au");
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Log.i("LOGIN", "on override url");
        return super.shouldOverrideUrlLoading(view, request);

    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Log.i("LOGIN", "on page started" + url);
        pd.show();

    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        pd.dismiss();
        Log.i("LOGIN", "on page finished" + url + isLoggedIn);
        if (!isLoggedIn && url.equals("https://www.villages.sydney/app-login") ) {
            new JsonTask().execute("https://www.villages.sydney/account?json=1");
        }

    }

    public void saveDetails(String JSON_str) {
        Log.i("JSON", "START");
        String email = "";
        String fname = "";
        String lname = "";
        String photoURL = "";
        SharedPreferences sharedPref = mContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        JSONObject JSON_obj = null;
        try {
            JSON_obj = new JSONObject(JSON_str);
            if (JSON_obj.length() > 0) {
                Log.i("LOGIN", "JSON Logged in");
                try {
                    email = JSON_obj.getString("email");
                    fname = JSON_obj.getString("first_name");
                    lname = JSON_obj.getString("last_name");
                    photoURL = JSON_obj.getString("photo_url");

                    Log.i("JS", email + " " + fname + " " + lname);
                    editor.putString("EMAIL", email);
                    editor.putString("FNAME", fname);
                    editor.putString("LNAME", lname);
                    editor.putString("IMGURL", photoURL);
                    editor.putBoolean("isLoggedIn", true);
                    editor.commit();
                    transferURL("https://www.villages.sydney");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i("LOGIN ", "JSON Not logged in");
                editor.putBoolean("isLoggedIn", false);
                editor.commit();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void transferURL(String url) {
        Intent intent = new Intent();
        intent.setAction(ACTION_TRANSFER_URL);
        intent.putExtra(URL, url);
        mContext.sendBroadcast(intent);
    }


    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            saveDetails(result);
        }
    }

}
