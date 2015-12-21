package it.bitrocket.krakenexchange;

import java.util.ArrayList;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class KrakenAsset
{
	private String mStandardName, mLabel, mAsset, mBase, mQuote;

	public KrakenAsset(String std_l, String lbl, String ass, String b, String q)
	{
		mStandardName  = std_l;
		mLabel          = lbl;
		mAsset          = ass;
		mBase           = b;
		mQuote          = q;
	}

	public static ArrayList<KrakenAsset> getAssets()
	{

		return null;
	}

	public static KrakenAsset getAssetByStandardName(String name)
	{
		return null;
	}
}
