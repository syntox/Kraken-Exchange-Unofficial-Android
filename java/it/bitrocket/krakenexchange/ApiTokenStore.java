package it.bitrocket.krakenexchange;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class ApiTokenStore
{
	public static final String API_STORE_TAG   = "ApiPrefs";
    public static final String API_FLAG        = "api_flag";
    public static final String API_KEY         = "api_key";
    public static final String API_SECRET      = "api_secret";

	public String api_key, api_secret;

    public ApiTokenStore()
    {
	    fetchApiTokens();
    }

    /**
     * Do a formal check on App preferences to test if API pairing was already done
     *
     * @return true if API Pairing was already done, false otherwise
     */
    public boolean isApiAlreadySet()
    {
        return ApplicationController.getInstace()
                .storage().direct()
                .getBoolean(Storage.API_FLAG_TAG, false);
    }

	/**
	 * Set the parameter flag that denote if API pairing was already done
	 *
	 * @param status
	 */
    public void setApiPairingPreference(boolean status)
    {
        ApplicationController.getInstace()
                .storage()
                .putBoolean(Storage.API_FLAG_TAG, status);
    }

	/**
	 * Store the given API Key and Secret into Android SharedPreferences
	 *
	 * @param key
	 * @param secret
	 * @param erase If true the method accept empty key and secret
	 * @return true if storing procedure was successful or false if it failed
	 */
    public boolean storeApiTokens(String key, String secret, boolean erase)
    {
	    if((key.isEmpty() || secret.isEmpty()) && erase == false)
	    {
		    return false;
	    }

        if(ApplicationController.getInstace().storage().saveApiTokens(key, secret))
        {
	        fetchApiTokens();
            setApiPairingPreference(!erase);

            return true;
        }

        return false;
    }

	/**
	 * Load API key and secrent into Instance field
	 *
	 */
    public void fetchApiTokens()
    {
        api_key     = getApiKey();
        api_secret  = getApiSecret();
    }

	/**
	 * Check if everything look ok
	 *
	 */
	public void checkIntegrity()
	{
		if (isApiAlreadySet())
		{
			if (getApiKey().isEmpty() || getApiSecret().isEmpty())
			{
				invalidate();
			}
		}
		else
		{
			invalidate();
		}
	}


	/**
	 * Invalidate current API tokens
	 *
	 */
	public void invalidate()
	{
		storeApiTokens("", "", true);
	}

    private String getApiKey()
    {
        return ApplicationController.getInstace()
                .storage().direct()
                .getString(Storage.API_KEY_TAG, "");
    }

    private String getApiSecret()
    {
        return ApplicationController.getInstace()
                .storage().direct()
                .getString(Storage.API_SECRET_TAG, "");
    }
}