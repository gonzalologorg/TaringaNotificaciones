package rgbmill.com.taringanotificaciones;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Gonzalo Juncos on 09/02/2017.
 */

public class NotifRSS extends Service {
    public static final int notify = 60000;  //interval between two services(Here Service run every 5 Minute)
    private Handler mHandler = new Handler();   //run on another Thread to avoid crash
    private Timer mTimer = null;    //timer handling

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        if (mTimer != null) // Cancel if already existed
            mTimer.cancel();
        else
            mTimer = new Timer();   //recreate new
        mTimer.scheduleAtFixedRate(new TimeDisplay(), 0, notify);   //Schedule task
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();    //For Cancel Timer
        Toast.makeText(this, "Service is Destroyed", Toast.LENGTH_SHORT).show();
    }

    //class TimeDisplay for handling task
    class TimeDisplay extends TimerTask {

        NotificationManager manager;
        Notification myNotication;
        String notifTail = "";

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    Context context = getApplicationContext();
                    SharedPreferences sharedPref = context.getSharedPreferences("user_key", Context.MODE_PRIVATE);
                    String userKey = sharedPref.getString("user_key", "null");
                    if(!userKey.equals("null")) {
                        new quickNotification(userKey,getApplicationContext(),TimeDisplay.this).execute();
                    }
                }
            });
        }

        public void addNotification(int index, JSONObject data,JSONObject user) throws JSONException {
            if(data.getJSONObject("event").getString("statusView").equals("unread")) {

                String did;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    did = String.valueOf(Html.fromHtml(data.getString("subtitle"), 63));
                } else {
                    did = String.valueOf(Html.fromHtml(data.getString("subtitle")));
                }

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("user_key", Context.MODE_PRIVATE);
                int notifications = sharedPref.getInt("notifNumber",0);

                notifTail = "@" + user.getString("nick") + " " + did + (notifications > 1  ? " ( +" + notifications + " Notif. )" : "");

                sharedPref.edit().putInt("notifNumber", notifications + 2).apply();

                sharedPref.edit().putString("notifications", notifTail).apply();

            }
        }

        public void buildNotification(){
            notificationFragment notif = new notificationFragment();

            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, 0);

            Notification.Builder builder = new Notification.Builder(getApplicationContext());

            builder.setAutoCancel(false);
            builder.setTicker("Notificacion");
            builder.setContentTitle("Nuevas notificaciones");


            builder.setContentText(notifTail);
            builder.setSmallIcon(R.drawable.notifcation_icon);
            builder.setContentIntent(pendingIntent);
            builder.setOngoing(true);
            builder.setAutoCancel(true);
            builder.setNumber(100);
            builder.build();

            myNotication = builder.getNotification();
            manager.notify(26, myNotication);

        }
    }

    class quickNotification extends AsyncTask<Void, Void, Boolean> {

        private final String key;
        private final Context mContext;
        private final TimeDisplay mother;

        quickNotification(String key, Context mContext, TimeDisplay mother) {
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
            new postData("https://www.taringa.net/notificaciones-ajax.php",params,mContext,new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                    String page = new String(bytes);
                    if(!page.equals("")){
                        try {
                            final JSONArray data = new JSONArray(page);
                            for(int index=0; index<data.length(); index++){
                                JSONObject notification = new JSONObject(data.get(index).toString());
                                int owner = notification.getJSONObject("event").getInt("owner");
                                final int finalIndex = index;
                                final JSONObject finalNotification = notification;
                                new postData("http://api.taringa.net/user/view/" + String.valueOf(owner),mContext,new AsyncHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(int i, Header[] headers, byte[] bytes) {
                                        JSONObject response = null;
                                        try {
                                            response = new JSONObject(new String(bytes));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            mother.addNotification(data.length()-finalIndex,finalNotification,response);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                                    }

                                },true);
                            }
                            mother.buildNotification();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                }
            });
            return true;
        }
    }
}

