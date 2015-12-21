package it.bitrocket.krakenexchange;

import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class KrakenAPI
{
	/**
	 * UpdateUI interface is the bridge from
	 * Android Volley backend and the User Interface
	 *
	 * Defining update() is necessary to permits Volley
	 * to update certain UI element using the received response
	 */
	public interface UpdateUI
	{
		void update(JSONObject jsonObject);
		void error();
	}

	/**
	 * METHOD_* is the KrakenAPI formal method
	 */
	public static final String METHOD_TIME          = "Time";
	public static final String METHOD_ASSETS        = "Assets";
	public static final String METHOD_ASSETPAIRS    = "AssetPairs";
	public static final String METHOD_TICKER        = "Ticker";
	public static final String METHOD_ORDERBOOK     = "Depth";
	public static final String METHOD_TRADES        = "Trades";
	public static final String METHOD_BALANCE       = "Balance";
	public static final String METHOD_TRADEBALANCE  = "TradeBalance";
	public static final String METHOD_OPENORDERS    = "OpenOrders";
	public static final String METHOD_CLOSEDORDERS  = "ClosedOrders";
	public static final String METHOD_ADDORDER      = "AddOrder";
	public static final String METHOD_CANCELORDER   = "CancelOrder";

	public static final String STORAGE_NAME             = "hxf35vyfkv";
	public static final String STORAGE_TAG_NONCE        = "nonce";
	public static final String STORAGE_TRADABLEPAIRS    = "tradable_pairs";

	/**
	 * define two available API request method
	 */
	private static final String PUBLIC = "public";
	private static final String PRIVATE = "private";

	/**
	 * version: Kraken API version, default to 0
	 * entrypoint: Kraken API base URL
	 */
	public String version, entrypoint, time;

	/**
	 * api_store: ApiTokenStore instance where api's key and secret
	 * could be found
	 */
	private ApiTokenStore api_store;

	public KrakenAPI()
	{
		this.entrypoint = "https://api.kraken.com";
		this.version = "0";
		this.api_store = new ApiTokenStore();
	}

	/**
	 * Fire a public API call
	 *
	 * Public API call does not need header information or signing
	 * Do not use this method directly, instead use one of the granular methods
	 *
	 * @param method
	 * @param postfields
	 * @param ui
	 */
	public void queryPublic(String method, Map<String, String> postfields, UpdateUI ui)
	{
		ApplicationController.getInstace()
				.addToRequestQueue(buildRequest(
										buildURL(method, KrakenAPI.PUBLIC),
										postfields,
										ui
				));
	}

	/**
	 * Fire a private API Call
	 *
	 * Private API call need extra signature into the header section
	 * Do not use this method directly, instead use on of the granular methods
	 *
	 * @param method
	 * @param postfields
	 * @param ui
	 */
	public void queryPrivate(String method, Map<String, String> postfields, String nonce, UpdateUI ui)
	{
		String url  = buildURL(method, KrakenAPI.PRIVATE);
		String api_path  = buildApiPath(method, KrakenAPI.PRIVATE);

		ApplicationController.getInstace()
				.addToRequestQueue(buildRequest(
										url,
										buildHeader(getApiSign(api_path, nonce, postfields)),
										postfields,
										ui
				));
	}

	/**
	 * Method to build the Kraken Api URL
	 *
	 * This method is useful if you want to get the correct
	 * api url necessary to work with the "method"
	 *
	 * @param method
	 * @param security
	 * @return api entrypoint URL for given method
	 */
	public String buildURL(String method, String security)
	{
		return entrypoint + buildApiPath(method, security);
	}

	public String buildApiPath(String method, String security)
	{
		return "/" + version + "/" + security + "/" + method;
	}

	public String buildNonce()
	{
		// Read and increment the Nonce
		long nonce = ApplicationController.getInstace()
				.storage().direct()
				.getLong(Storage.KRAKEN_NONCE_TAG, ((long) 0)) + ((long) 1);

		// Save the updated Nonce
		ApplicationController.getInstace()
				.storage()
				.putLong(Storage.KRAKEN_NONCE_TAG, nonce);

		return Long.toString(nonce);
	}

	public Map<String, String> buildHeader(String sign)
	{
		Map<String, String> headerfields = new HashMap<String, String>();

		headerfields.put("API-Key", api_store.api_key);
		headerfields.put("API-Sign", sign);

		return headerfields;
	}

	public String getApiSign(String url, String nonce, Map<String,String> postfields)
	{
		// Build the postfield string
		String str_postfields = "";
		if (!postfields.isEmpty())
		{
			for (Map.Entry<String, String> entry : postfields.entrySet())
			{
				str_postfields = str_postfields.concat(entry.getKey() + "=" + entry.getValue() + "&");
			}

			str_postfields = str_postfields.substring(0, str_postfields.length() - 1);
		}

		// Get the base64 decoded value of api_secret
		byte[] decoded_api_secret = Base64.decode(api_store.api_secret, Base64.DEFAULT);

		// Build the payload
		MessageDigest digester = null;

		byte[] hashed_sha256_payload = null;
		String hash_sha256_payload = nonce + str_postfields;

		try
		{
			digester = MessageDigest.getInstance("SHA256");
			digester.update(hash_sha256_payload.getBytes());
			hashed_sha256_payload = digester.digest();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}

		// Merging URL + previous hashed payload
		byte[] payload = url.getBytes();
		byte[] t = new byte[hashed_sha256_payload.length + payload.length];

		System.arraycopy(payload, 0, t, 0, payload.length);
		System.arraycopy(hashed_sha256_payload, 0, t, payload.length, hashed_sha256_payload.length);

		// Initialize the HMAC (SHA512) engine
		Mac hmac = null;
		SecretKeySpec key = new SecretKeySpec(decoded_api_secret, "HmacSHA512");
		byte[] sign = "".getBytes();

		try
		{
			hmac = Mac.getInstance("HmacSHA512");
			hmac.init(key);
			sign = hmac.doFinal(t);
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		catch (InvalidKeyException e)
		{
			e.printStackTrace();
		}


		// Base64 Encoding of Sign
		return Base64.encodeToString(sign, Base64.NO_WRAP);
	}

	/**
	 * Build an API request
	 *
	 * This method is useful when working with public api call
	 *
	 * @param url
	 * @param postfields
	 * @param ui
	 * @return JsonObjectRequest ready to be queued by Volley
	 */
	public KrakenRequest buildRequest(String url, Map<String,String> postfields, UpdateUI ui)
	{
		Map<String, String> headerfields = Collections.emptyMap();

		return buildRequest(url, headerfields, postfields, ui);
	}

	/**
	 * Build an API request
	 *
	 * This method is useful when working with private api call
	 *
	 * @param url
	 * @param headerfields
	 * @param postfields
	 * @param ui
	 * @return
	 */
	public KrakenRequest buildRequest(String url, final Map<String, String> headerfields, final Map<String, String> postfields, final UpdateUI ui)
	{
		KrakenRequest request = new KrakenRequest
				(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject jsonResponse) {
						ui.update(jsonResponse);
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {

						String errorMessage = VolleyErrorHelper.getMessage(error, ApplicationController.getInstace().getApplicationContext());

						if (BuildConfig.DEBUG)
						{
							Log.d("VolleyError", errorMessage);
						}

						Toast.makeText(ApplicationController.getInstace().getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
						ui.error();
					}
				})
		{

			@Override
			protected Map<String, String> getParams() {
				return postfields;
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				return headerfields;
			}
		};

		return request;
	}

	// ************************************************************************
	// GRANULARITY MODE: ON
	// These methods automatically fire an API request to Kraken.com

	public void getTime(UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();
		this.queryPublic(METHOD_TIME, postfields, ui);
	}


	/**
	 * https://www.kraken.com/help/api#get-asset-info
	 *
	 * @param ui UpdateUI callback to update elements into UI
	 */
	public void getAssets(UpdateUI ui)
	{
		getAssets("all", "currency", "all", ui);
	}

	/**
	 * https://www.kraken.com/help/api#get-asset-info
	 *
	 * @param info info to retrieve (optional, default: all)
	 * @param aclass asset class (defualt: currency)
	 * @param asset asset to get details on
	 * @param ui @param ui UpdateUI callback to update elements into UI
	 */
	public void getAssets(String info, String aclass, String asset, UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();

		/*postfields.put("info", info);
		postfields.put("aclass", aclass);
		postfields.put("asset", asset);*/

		this.queryPublic(METHOD_ASSETS, postfields, ui);
	}

	/**
	 * https://api.kraken.com/0/public/AssetPairs
	 *
	 * Retrieve a list of tradable currency pairs
	 *
	 * @param ui
	 */
	public void getAssetPairs(UpdateUI ui)
	{
		Map<String, String> postfields = Collections.emptyMap();

		this.queryPublic(METHOD_ASSETPAIRS, postfields, ui);
	}

	/**
	 * https://api.kraken.com/0/public/AssetPairs
	 *
	 * Retrieve detail about a tradable currency pair
	 *
	 * @param pair
	 * @param ui
	 */
	public void getAssetPairs(String pair, UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();

		postfields.put("pair", pair);

		this.queryPublic(METHOD_ASSETPAIRS, postfields, ui);
	}

	/**
	 *
	 */
	/**
	 * https://api.kraken.com/0/public/AssetPairs
	 *
	 * Retrieve a quotation
	 *
	 * @param pair
	 * @param ui
	 */
	public void getTicker(String pair, UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();

		postfields.put("pair", pair);

		this.queryPublic(METHOD_TICKER, postfields, ui);
	}

	/**
	 * https://api.kraken.com/0/public/Trades
	 *
	 * Retrieve recent trades about a tradable pair currencies
	 *
	 * @param pair
	 * @param ui
	 */
	public void getTrades(String pair, UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.add(Calendar.MINUTE, -10); // we get last 10 mins trades
		long since = cal.getTimeInMillis() / 1000 * 1000000000;

		postfields.put("pair", pair);
		postfields.put("since", String.valueOf(since));

		this.queryPublic(METHOD_TRADES, postfields, ui);
	}

	public void getOrderBook(String pair, int count, UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();

		postfields.put("pair", pair);
		postfields.put("count", String.valueOf(count));

		this.queryPublic(METHOD_ORDERBOOK, postfields, ui);
	}

	/**
	 * https://api.kraken.com/0/private/Balance
	 *
	 * Retrieve user balance
	 *
	 * @param ui
	 */
	public void getBalance(UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();
		String nonce = buildNonce();

		postfields.put("nonce", nonce);

		this.queryPrivate(METHOD_BALANCE, postfields, nonce, ui);
	}

	/**
	 * https://api.kraken.com/0/private/TradeBalance
	 *
	 * Retrieve virtual balance expressed in "asset" currency
	 *
	 * @param ui
	 */
	public void getTradeBalance(String asset, UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();
		String nonce = buildNonce();

		postfields.put("nonce", nonce);
		postfields.put("asset", asset);

		this.queryPrivate(METHOD_TRADEBALANCE, postfields, nonce, ui);
	}

	/**
	 * https://api.kraken.com/0/private/OpenOrders
	 *
	 * Retrieve user's open orders
	 *
	 * @param ui
	 */
	public void getOpenOrders(UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();
		String nonce = buildNonce();

		postfields.put("nonce", nonce);

		this.queryPrivate(METHOD_OPENORDERS, postfields, nonce, ui);
	}

	/**
	 * https://api.kraken.com/0/private/ClosedOrders
	 *
	 * Retrieve user's closed orders
	 *
	 * @param ui
	 */
	public void getClosedOrders(UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();
		String nonce = buildNonce();

		postfields.put("nonce", nonce);

		this.queryPrivate(METHOD_CLOSEDORDERS, postfields, nonce, ui);
	}

	public void openOrder(String asset, String action, String ordertype, String volume, String price, UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();
		String nonce = buildNonce();

		postfields.put("nonce", nonce);

		postfields.put("type", action);
		postfields.put("volume", volume);
		postfields.put("pair", asset);
		postfields.put("ordertype", ordertype);

		// DEBUG
		//postfields.put("validate", "1");

		if (ordertype == FragmentOrderDialog.ORDERTYPE_LIMIT)
		{
			postfields.put("price", price);
		}

		this.queryPrivate(METHOD_ADDORDER, postfields, nonce, ui);
	}

	public void cancelOrder(String orderId, UpdateUI ui)
	{
		Map<String, String> postfields = new HashMap<String, String>();
		String nonce = buildNonce();

		postfields.put("nonce", nonce);
		postfields.put("txid", orderId);

		this.queryPrivate(METHOD_CANCELORDER, postfields, nonce, ui);
	}

	// ************************************************************************
	// Non-API methods

/*	public void cacheTradablePairs()
	{
		this.getAssetPairs(new UpdateUI() {
			@Override
			public void update(JSONObject jsonObject)
			{
				try
				{
					JSONObject result = jsonObject.getJSONObject("result");

					SharedPreferences.Editor editor = storage.edit();
					editor.putString(STORAGE_TRADABLEPAIRS, result.toString());

					editor.commit();

				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

			}
		});
	}*/

	/*public JSONObject readCachedTradablePairs()
	{
		if (storage.contains(STORAGE_TRADABLEPAIRS))
		{
			String data = storage.getString(STORAGE_TRADABLEPAIRS, "");
			if (data != null)
			{
				try
				{
					return new JSONObject(data);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		}

		//return readCachedTradablePairs();
		return null;
	}*/
}