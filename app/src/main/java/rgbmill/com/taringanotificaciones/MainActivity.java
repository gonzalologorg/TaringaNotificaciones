package rgbmill.com.taringanotificaciones;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity  {

    public LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout = (LinearLayout) findViewById(R.id.listNotifications);
        TextView welcome = (TextView)findViewById(R.id.upView);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("user_key", Context.MODE_PRIVATE);
        String userName = sharedPref.getString("user_data", "null");
        if(!userName.equals("null")) {
            welcome.setText("@" + userName);
            if(!sharedPref.getString("avatar_link", "null").equals("null")) {
                AsyncHttpClient client = new AsyncHttpClient();
                Log.d("LINK", sharedPref.getString("avatar_link", "null"));
                client.get(sharedPref.getString("avatar_link", "null"), new FileAsyncHttpResponseHandler(MainActivity.this) {
                    @Override
                    public void onFailure(int i, Header[] headers, Throwable throwable, File file) {

                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, File response) {
                        Bitmap image = BitmapFactory.decodeFile(response.getAbsolutePath());
                        ImageView iv = (ImageView) findViewById(R.id.userAvatar);
                        iv.setImageBitmap(image);
                    }
                });
            }
        }
        else {
            welcome.setText("Bienvenido");
            Intent myIntent = new Intent(MainActivity.this, login_act.class);
            startActivity(myIntent);
        }

        sharedPref.edit().putInt("notifNumber", 0).apply();

        startService(new Intent(this, NotifRSS.class));

        Context context = getApplicationContext();

        String userKey = sharedPref.getString("user_key", "null");
        String user = sharedPref.getString("user_data", "null");
        String pass = sharedPref.getString("user_pass", "null");

        layout.removeAllViews();

        new userNotifications(user,pass,userKey,getApplicationContext(),this).execute();
    }

    public void sendLogin(View view) {
        Intent myIntent = new Intent(MainActivity.this, login_act.class);
        startActivity(myIntent);
    }

    public void openSettings(View view){
        Intent myIntent = new Intent(MainActivity.this, settings_page.class);
        startActivity(myIntent);
    }

    public void fetchNotifications(View view){
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences("user_key", Context.MODE_PRIVATE);
        String userKey = sharedPref.getString("user_key", "null");
        String user = sharedPref.getString("user_data", "null");
        String pass = sharedPref.getString("user_pass", "null");

        Log.d("LOGING","Fetching: " + userKey + " " + user + " " + pass);
        layout.removeAllViews();

        new userNotifications(user,pass,userKey,getApplicationContext(),this).execute();

    }

    public void clearData(View view){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        SharedPreferences settings = getApplicationContext().getSharedPreferences("user_key", Context.MODE_PRIVATE);
                        settings.edit().clear().commit();
                        Intent myIntent = new Intent(MainActivity.this, login_act.class);
                        startActivity(myIntent);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Estas seguro que quieres salir?").setPositiveButton("Si", dialogClickListener).setNegativeButton("No", dialogClickListener).show();

    }

    public void addNotification(JSONObject data,JSONObject user) throws JSONException {

        if(!data.getJSONObject("event").getString("statusView").equals("")) {
            notificationFragment notif = new notificationFragment();
            Bundle args = new Bundle();

            args.putString("param1", user.getString("nick"));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                args.putString("param2", String.valueOf(Html.fromHtml(data.getString("subtitle"), 63)));
            } else {
                args.putString("param2", String.valueOf(Html.fromHtml(data.getString("subtitle"))));
            }

            args.putString("param3", user.getJSONObject("avatar").getString("big"));
            args.putString("param4", data.getJSONObject("object").getString("link"));
            args.putBoolean("isNotification", data.getJSONObject("event").getString("statusView").equals("unread"));
            notif.setArguments(args);


            getFragmentManager().beginTransaction().add(layout.getId(), notif).commit();
        }

    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}

class userNotifications extends AsyncTask<Void, Void, Boolean> {

    final String user;
    final String pass;
    final String key;
    final Context mContext;
    final MainActivity mother;

    userNotifications(String user, String pass, String key, Context mContext, MainActivity mother) {
        this.user = user;
        this.pass = pass;
        this.key = key;
        this.mother = mother;
        this.mContext = mContext;
    }

    @Override
    protected Boolean doInBackground(Void... p) {

        RequestParams params = new RequestParams();
        params.put("key", key);
        params.put("action", "last");
        params.put("template", false);
        if(!key.equals("null")) {
            new postData("https://www.taringa.net/notificaciones-ajax.php", params, mContext, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                    String page = new String(bytes);
                    if (!page.equals("")) {
                        try {
                            JSONArray data = new JSONArray(page);
                            for (int index = 0; index < data.length(); index++) {
                                JSONObject notification = new JSONObject(data.get(index).toString());
                                int owner = notification.getJSONObject("event").getInt("owner");
                                final JSONObject finalNotification = notification;
                                Log.d("USER", "http://api.taringa.net/user/view/" + String.valueOf(owner));
                                new postData("http://api.taringa.net/user/view/" + String.valueOf(owner), mContext, new AsyncHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(int i, Header[] headers, byte[] bytes) {
                                        JSONObject response = null;
                                        try {
                                            response = new JSONObject(new String(bytes));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            mother.addNotification(finalNotification, response);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                                        Log.e("ERROR", throwable.toString());
                                    }

                                }, true);

                            }
                            //Log.d("DEBUG","Lots of notifications! " + String.valueOf(data.length()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("DEBUG", "I'm uncapable of read them");
                        }
                    } else {
                        Log.d("DEBUG", "No notifications!");
                    }
                }

                @Override
                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                }
            });
        }
        return true;
    }
}