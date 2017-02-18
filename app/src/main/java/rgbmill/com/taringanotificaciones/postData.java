package rgbmill.com.taringanotificaciones;

import android.content.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

import com.loopj.android.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Gonzalo Juncos on 29/01/2017.
 */


public class postData {

    String pageUrl = "";
    RequestParams params;
    Context mContext;
    SyncHttpClient client;
    JsonHttpResponseHandler callback;
    AsyncHttpResponseHandler callbackB;
    Boolean isFetch;
    Boolean asyncResponse = false;

    public postData(String url, RequestParams params, Context mContext,JsonHttpResponseHandler callback){
        this.pageUrl = url;
        this.params = params;
        this.mContext = mContext;
        this.callback = callback;
        this.isFetch = false;
        asyncResponse = false;
        doPageRequest();
    }


    public postData(String url, RequestParams params, Context mContext,AsyncHttpResponseHandler callback){
        this.pageUrl = url;
        this.params = params;
        this.mContext = mContext;
        this.callbackB = callback;
        this.isFetch = false;
        asyncResponse = true;
        doPageRequest();
    }

    public postData(String url, Context mContext,AsyncHttpResponseHandler callback,Boolean isFetch){
        this.pageUrl = url;
        this.params = params;
        this.mContext = mContext;
        this.callbackB = callback;
        this.isFetch = isFetch;
        doPageRequest();
    }

    void doPageRequest(){
        client = new SyncHttpClient();
        PersistentCookieStore myCookieStore = new PersistentCookieStore(mContext);
        client.setCookieStore(myCookieStore);
        if (!isFetch)
            client.post(pageUrl, params,!asyncResponse ? callback : callbackB);
        else
            client.get(pageUrl, null,callbackB);
    }

}
