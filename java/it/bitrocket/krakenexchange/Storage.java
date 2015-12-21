package it.bitrocket.krakenexchange;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * {Kraken Exchange Application}'s storage solution
 *
 * Copyright (C) 2015 by Matteo Benetti
 */
public class Storage
{
	private static final String APP_STORAGE_TAG                 = "KrakenExchangeStorage";

	public static final String STORAGE_SELECTEDCURRENCY_TAG     = "KES_CURRENCY";
	public static final String STORAGE_SELECTEDCURRENCYPAIR_TAG = "KES_CURRENCYPAIR";
	public static final String UPDATENEWSTORAGES_TAG            = "NEW_STORAGE_SOLUTION_FLAG";
	public static final String API_FLAG_TAG                     = "API_FLAG";
	public static final String API_KEY_TAG                      = "API_KEY";
	public static final String API_SECRET_TAG                   = "API_SECRET";
	public static final String KRAKEN_NONCE_TAG                 = "NONCE";

	private Context mContext;
	private SharedPreferences mSharedPrefereces;
	private SharedPreferences.Editor mEditor;

	public Storage(Context c)
	{
		mContext = c;
		mSharedPrefereces = mContext.getSharedPreferences(APP_STORAGE_TAG, Context.MODE_PRIVATE);
		mEditor = mSharedPrefereces.edit();
	}

	/**
	 * Get the SharedPregerences object directly
	 * Useful when using this wrapper is not an option
	 *
	 * @return the SharedPreferences object
	 */
	public SharedPreferences direct()
	{
		return this.mSharedPrefereces;
	}

	/**
	 * Check if the data-migration was previously done
	 *
	 * @return TRUE if migration was previously done, FALSE otherwise
	 */
	public boolean isUpdatedToNewStorageSolution()
	{
		return mSharedPrefereces.getBoolean(Storage.UPDATENEWSTORAGES_TAG, false);
	}

	/**
	 * Migrate data from to the new Storage Solution, this procedure migrate cache-data
	 * and the KrakenAPI last NONCE preventing the user to re-associate the Application with
	 * a new API key pairs
	 *
	 * @return TRUE if migration is successful, else otherwise
	 */
	public boolean updateToNewStorageSolution()
	{
		// API TOKENS
		SharedPreferences tempSharedPref = mContext.getSharedPreferences(ApiTokenStore.API_STORE_TAG, Context.MODE_PRIVATE);
		mEditor.putBoolean(Storage.API_FLAG_TAG, tempSharedPref.getBoolean(ApiTokenStore.API_FLAG, false));
		mEditor.putString(Storage.API_KEY_TAG, tempSharedPref.getString(ApiTokenStore.API_KEY, ""));
		mEditor.putString(Storage.API_SECRET_TAG, tempSharedPref.getString(ApiTokenStore.API_SECRET, ""));

		// nonce
		SharedPreferences nonceShrdPref = mContext.getSharedPreferences(KrakenAPI.STORAGE_NAME, Context.MODE_PRIVATE);
		mEditor.putLong(Storage.KRAKEN_NONCE_TAG, nonceShrdPref.getLong(KrakenAPI.STORAGE_TAG_NONCE, (long) 0));

		// update DONE
		mEditor.putBoolean(Storage.UPDATENEWSTORAGES_TAG, true);

		return mEditor.commit();
	}

	/**
	 * MACRO useful to save API token (key and secret) at once.
	 *
	 * @param key the api key
	 * @param secret the api secret
	 * @return TRUE if save procedure goes well, FALSE otherwise
	 */
	public boolean saveApiTokens(String key, String secret)
	{
		mEditor.putString(Storage.API_KEY_TAG, key);
		mEditor.putString(Storage.API_SECRET_TAG, secret);

		return mEditor.commit();
	}

	/**
	 * Clear Kraken.com cache-data
	 *
	 */
	public void clearKrakenData()
	{
		mEditor.putString(FragmentAccount.STORAGE_TAG_BALANCE, "");
		mEditor.putString(FragmentAccount.STORAGE_TAG_TOTALBALANCE, "");
		mEditor.putString(FragmentTrading.STORAGE_TAG_OPENORDERS, "");
		mEditor.putString(FragmentTrading.STORAGE_TAG_CLOSEDORDERS, "");

		mEditor.commit();
	}

	/**
	 * Check if the EULA license was previously accepted
	 *
	 * @return TRUE if it was accepted, FALSE otherwise
	 */
	public boolean isEulaAccepted()
	{
		String eulaTag = "eula_" + BuildConfig.VERSION_CODE;
		return mSharedPrefereces.getBoolean(eulaTag, false);
	}

	/**
	 * Set a flag to notify the EULA license acceptation
	 *
	 * @return TRUE if save procedure goes well, FALSE otherwise
	 */
	public boolean setEulaAccepted()
	{
		String eulaTag = "eula_" + BuildConfig.VERSION_CODE;
		return putBoolean(eulaTag, true);
	}

	/**
	 * Wrapper to remove a given tag data from the Storage
	 *
	 * @param key the Key of the "value" data to remove
	 * @return TRUE in case of success, FALSE otherwise
	 */
	public boolean remove(String key)
	{
		return mEditor.remove(key).commit();
	}

	/*
		SETTER
	*/

	public boolean putInt(String key, int value)
	{
		return mEditor.putInt(key, value).commit();
	}

	public boolean putLong(String key, long value)
	{
		return mEditor.putLong(key, value).commit();
	}

	public boolean putString(String key, String data)
	{
		return mEditor.putString(key, data).commit();
	}

	public boolean putBoolean(String key, boolean value)
	{
		return mEditor.putBoolean(key, value).commit();
	}

	/*
		GETTER
	*/

	public int getInt(String key)
	{
		return mSharedPrefereces.getInt(key, -1);
	}

	public String getString(String key)
	{
		return mSharedPrefereces.getString(key, "");
	}
}
