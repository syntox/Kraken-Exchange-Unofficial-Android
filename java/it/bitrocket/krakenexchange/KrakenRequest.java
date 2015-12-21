package it.bitrocket.krakenexchange;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class KrakenRequest extends Request<JSONObject>
{
	private Listener<JSONObject> listener;
	private Map<String, String> params;

	public KrakenRequest(String url, Map<String, String> params,
	                     Listener<JSONObject> reponseListener, ErrorListener errorListener)
	{
		super(Method.GET, url, errorListener);
		this.listener = reponseListener;
		this.params = params;
	}

	public KrakenRequest(int method, String url, Map<String, String> params,
	                     Listener<JSONObject> reponseListener, ErrorListener errorListener)
	{
		super(method, url, errorListener);
		this.listener = reponseListener;
		this.params = params;
	}

	/**
	 * Subclasses must implement this to parse the raw network response
	 * and return an appropriate response type. This method will be
	 * called from a worker thread.  The response will not be delivered
	 * if you return null.
	 *
	 * @param response Response from the network
	 * @return The parsed response, or null in the case of an error
	 */
	@Override
	protected Response<JSONObject> parseNetworkResponse(NetworkResponse response)
	{
		try
		{
			String jsonString = new String(response.data,
			                               HttpHeaderParser.parseCharset(response.headers));
			return Response.success(new JSONObject(jsonString),
			                        HttpHeaderParser.parseCacheHeaders(response));
		}
		catch (UnsupportedEncodingException e)
		{
			return Response.error(new ParseError(e));
		}
		catch (JSONException je)
		{
			return Response.error(new ParseError(je));
		}
	}

	/**
	 * Subclasses must implement this to perform delivery of the parsed
	 * response to their listeners.  The given response is guaranteed to
	 * be non-null; responses that fail to parse are not delivered.
	 *
	 * @param response The parsed response returned by
	 *                 {@link #parseNetworkResponse(com.android.volley.NetworkResponse)}
	 */
	@Override
	protected void deliverResponse(JSONObject response)
	{
		listener.onResponse(response);
	}

	@Override
	public byte[] getBody() throws AuthFailureError
	{
		Map<String, String> params = getParams();

		if (params != null && params.size() > 0)
		{
			return encodeParameters(params, getParamsEncoding());
		}

		return null;
	}

	/**
	 * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
	 */
	private byte[] encodeParameters(Map<String, String> params, String paramsEncoding)
	{
		StringBuilder encodedParams = new StringBuilder();
		try
		{
			for (Map.Entry<String, String> entry : params.entrySet())
			{
				encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
				encodedParams.append('=');
				encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
				encodedParams.append('&');
			}

			String parameters = encodedParams
					.toString()
					.substring(0, encodedParams.toString().length() - 1);

			return parameters.getBytes(paramsEncoding);

			/*
			return encodedParams
					.toString()
					.getBytes(paramsEncoding);
			*/
		}
		catch (UnsupportedEncodingException uee)
		{
			throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
		}
	}
}