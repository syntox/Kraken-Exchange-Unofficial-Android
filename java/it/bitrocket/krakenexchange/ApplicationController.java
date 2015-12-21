
package it.bitrocket.krakenexchange;

import android.app.Application;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class ApplicationController extends Application
{
	/**
	 *  bitRocket IT - URL
	 *
	 *  bitRocket.it
	 */
	public static final String BITROCKETITURL = "http://www.bitrocket.it";

	/**
	 * Log or request TAG
	 */
	public static final String TAG = "t73m8b_KrakenExchange_Request";
	private static final long FIVE_MINUTES = 1000 * 60 * 5;

	/**
	 * Global request queue for Volley
	 */
	private RequestQueue mRequestQueue;

	/**
	 * Global storage solution
	 */
	private static Storage sStorage;

	/**
	 * Kraken.com API "factory"
	 */
	private static KrakenAPI sKrakenAPI;

	/**
	 * A singleton instance of the application class for easy access in other places
	 */
	private static ApplicationController sInstance;


	/**
	 * @return ApplicationController singleton instance
	 */
	public static synchronized ApplicationController getInstace()
	{
		return sInstance;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		sInstance = this;

		sStorage    = new Storage(getApplicationContext());
		sKrakenAPI  = new KrakenAPI();

		/**
		 * May, 2015
		 *
		 * new storage solution was introduced, the following procedure does the data-migration
		 * from the old storage solution to the new one
		 *
		 * Safe to remove date: September 2015
		 */
		if (!storage().isUpdatedToNewStorageSolution())
		{
			getSharedPreferences("KrakenExchange_storage", MODE_PRIVATE).edit().clear().commit();
			storage().updateToNewStorageSolution();
		}
	}

	/**
	 * get the Storage object
	 *
	 * @return the Storage instance
	 */
	public Storage storage()
	{
		return sStorage;
	}

	/**
	 * get the Kraken "factory"
	 *
	 * @return KrakenAPI object
	 */
	public KrakenAPI kraken()
	{
		return sKrakenAPI;
	}

	/**
	 * Save data into SharedPreferences pot
	 *
	 * @param tag
	 * @param data
	 * @return
	 */
	public boolean saveData(String tag, JSONObject data)
	{
		if (sStorage.direct().contains(tag))
		{
			sStorage.remove(tag);
		}

		JSONObject payload = new JSONObject();

		if (BuildConfig.DEBUG)
		{
			Log.d("cache engine", "storing " + tag);
		}

		try
		{
			payload.put("timestamp", System.currentTimeMillis());
			payload.put("payload", data);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return false;
		}

		return sStorage.putString(tag, payload.toString());
	}

	/**
	 * Retrieve the data stored into the SharedPreferences pot with "tag" tag
	 *
	 * @param tag
	 * @return
	 */
	public JSONObject readData(String tag)
	{
		JSONObject data = null;
		String jsonString = "";

		if (BuildConfig.DEBUG)
		{
			Log.d("cache engine", "reading " + tag);
		}

		try
		{
			jsonString = sStorage.direct().getString(tag, "");

			if (!jsonString.isEmpty())
			{
				JSONObject dataTmp = new JSONObject(jsonString);

				if (dataTmp.has("payload"))
				{
					data = dataTmp.getJSONObject("payload");
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return data;
	}

	/**
	 * Check if data stored under "tag" is old
	 *
	 * @param tag
	 * @return
	 */
	public boolean isDataStoredOld(String tag)
	{
		try
		{
			JSONObject data = new JSONObject(sStorage.direct().getString(tag, ""));
			Long unix_timestamp = data.getLong("timestamp");

			if ((System.currentTimeMillis() - unix_timestamp) <= FIVE_MINUTES)
			{
				return false;
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return true;
	}

	public void setEulaAccepted()
	{
		sStorage.setEulaAccepted();
	}

	public boolean isEulaAccepted()
	{
		return sStorage.isEulaAccepted();
	}

	/**
	 * @return The Volley Request queue, the queue will be created if it is null
	 */
	public RequestQueue getRequestQueue()
	{
		if (mRequestQueue == null)
		{
			mRequestQueue = Volley.newRequestQueue(getApplicationContext());
		}

		return mRequestQueue;
	}

	/**
	 * Adds the specified request to the global queue, if tag is specified
	 * then it is used else Default TAG is used.
	 *
	 * @param req
	 * @param tag
	 */
	public <T> void addToRequestQueue(Request<T> req, String tag)
	{
		req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);

		if (BuildConfig.DEBUG)
		{
			VolleyLog.d("Adding request to queue: %s", req.getUrl());
		}

		getRequestQueue().add(req);
	}

	/**
	 * Adds the specified request to the global queue using the Default TAG.
	 *
	 * @param req
	 */
	public <T> void addToRequestQueue(Request<T> req)
	{
		addToRequestQueue(req, "");
	}

	/**
	 * Cancels all pending requests by the specified TAG, it is important
	 * to specify a TAG so that the pending/ongoing requests can be cancelled.
	 *
	 * @param tag
	 */
	public void cancelPendingRequests(Object tag)
	{
		if (mRequestQueue != null) {
			mRequestQueue.cancelAll(tag);
		}
	}
}
