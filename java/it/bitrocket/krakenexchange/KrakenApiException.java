package it.bitrocket.krakenexchange;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class KrakenApiException extends Exception
{
	public static final String ERROR_TYPE_GENERAL   = "EGeneral";
	public static final String ERROR_TYPE_API       = "EAPI";
	public static final String ERROR_TYPE_SERVICE   = "EService";
	public static final String ERROR_YPE_ORDER      = "EOrder";
	public static final String ERROR_YPE_TRADE      = "ETrade";

	public String type, error;

	public static KrakenApiException newInstance(String krakenError)
	{
		String type = krakenError.substring(0, krakenError.indexOf(':'));
		String error = krakenError.substring(krakenError.indexOf(':') + 1);

		KrakenApiException kae = new KrakenApiException(krakenError);

		kae.type = type;
		kae.error = error;

		return kae;
	}

	public KrakenApiException(String message)
	{
		super(message);
	}

	public String getType()
	{
		return type;
	}

	public String getError()
	{
		return error;
	}
}
