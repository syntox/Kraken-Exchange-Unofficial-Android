package it.bitrocket.krakenexchange;


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;


/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class FragmentMarkets extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener
{
	private static final String ARG_SECTION_NUMBER              = "market_section";
	private static final float TABLE_ORDERBOOK_ELEMENT_WEIGHT   = 0.5f;
	private static final int TABLE_ORDERBOOK_HEAD_TEXT_SIZE_SP  = 18;
	private static final int TABLE_ORDERBOOK_BODY_TEXT_SIZE_SP  = 16;

	public static final String STORAGE_TAG_LASTPRICES   = "lastPrices";
	public static final String STORAGE_TAG_SERVERTIME   = "kraken_market_servertime";
	public static final String STORAGE_TAG_TICKER       = "kraken_market_ticker";
	public static final String STORAGE_TAG_ORDERBOOK    = "kraken_market_orderbook";

	public int selectedCurrencyPair;

	private KrakenAPI mKrakenApi;
	private Storage mStorage;

	private SwipeRefreshLayout mSwipeLayout;
	private JSONObject mTradablePairs;

	/**
	 * constructor
	 */
	public FragmentMarkets()
	{
		// Required empty public constructor
	}

	public static FragmentMarkets newInstance(int sectionNumber)
	{
		FragmentMarkets fragment = new FragmentMarkets();

		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mStorage    = ApplicationController.getInstace().storage();
		mKrakenApi  = ApplicationController.getInstace().kraken();

		selectedCurrencyPair = mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		View frag_view =  inflater.inflate(R.layout.fragment_markets, container, false);

		mSwipeLayout = (SwipeRefreshLayout) frag_view.findViewById(R.id.fragment_markets_swipe_container);
		mSwipeLayout.setOnRefreshListener(this);

		startup(frag_view, false); // try to read from cache

		return frag_view;
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		((MainActivity) activity).onSectionAttached(
				getArguments().getInt(ARG_SECTION_NUMBER));
	}

	@Override
	public void onRefresh()
	{
		new Handler().post(new Runnable()
		{
			@Override
			public void run()
			{
				startup(getView(), true);
			}
		});
	}

	public void onTriggeredRefresh()
	{
		mSwipeLayout.post(new Runnable()
		{
			@Override
			public void run()
			{
				mSwipeLayout.setRefreshing(true);
				startup(getView(), true);
			}
		});
	}

	/**
	 * Stop mSwipeLayout refresh animation
	 */
	public void onStopRefresh()
	{
		mSwipeLayout.setRefreshing(false);
	}

	/**
	 * startup method, update server time, ticker data and orderbook
	 *
	 * @param view
	 * @param forceUpdate
	 */
	public void startup(View view, boolean forceUpdate)
	{
		updateTime(view, forceUpdate);
		updateTicker(view, forceUpdate);
		updateOrderBook(view, forceUpdate);
	}

	/**
	 * Retrieve servertime data from Kraken API
	 *
	 * @param view
	 * @param forceUpdate
	 */
	public void updateTime(View view, boolean forceUpdate)
	{
		final View fragView = view;

		if (!forceUpdate)
		{
			JSONObject cachedData = ApplicationController.getInstace().readData(STORAGE_TAG_SERVERTIME);

			if (cachedData == null)
			{
				forceUpdate = true;
			}
			else
			{
				parseServertimeData(cachedData, fragView);
			}
		}

		if (forceUpdate)
		{
			mKrakenApi.getTime(new KrakenAPI.UpdateUI()
			{
				@Override
				public void update(JSONObject jsonObject)
				{
					try
					{
						JSONObject result = jsonObject.getJSONObject("result");

						ApplicationController.getInstace().saveData(STORAGE_TAG_SERVERTIME, result);
						parseServertimeData(result, fragView);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}

				@Override
				public void error()
				{
					onStopRefresh();
				}
			});
		}
	}

	/**
	 * Retrieve ticker data from Kraken API
	 *
	 * @param view
	 * @param forceUpdate
	 */
	public void updateTicker(View view, boolean forceUpdate)
	{
		final View fragView = view;

		//final String currencyPair = getCurrencyPair(selectedCurrencyPair);
		final String currencyPair = getCurrencyPair(MainActivity.CURRENCYPAIR_XXBTZEUR)
				+ ","
				+ getCurrencyPair(MainActivity.CURRENCYPAIR_XXBTZUSD);

		if (!forceUpdate)
		{
			JSONObject cachedData = ApplicationController.getInstace().readData(STORAGE_TAG_TICKER);

			if (cachedData == null || !cachedData.has(getCurrencyPair(selectedCurrencyPair)))
			{
				forceUpdate = true;
			}
			else
			{
				parseTickerData(cachedData, fragView);
			}
		}

		if (forceUpdate)
		{
			mKrakenApi.getTicker(currencyPair, new KrakenAPI.UpdateUI()
			{
				@Override
				public void update(JSONObject jsonObject)
				{
					try
					{
						JSONObject result = jsonObject.getJSONObject("result");

						ApplicationController.getInstace().saveData(STORAGE_TAG_TICKER, result);
						parseTickerData(result, fragView);

					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}

				@Override
				public void error()
				{
					onStopRefresh();
				}
			});
		}
	}

	/**
	 * Retrieve orderbook data from Kraken API
	 *
	 * @param view
	 * @param forceUpdate
	 */
	public void updateOrderBook(View view, boolean forceUpdate)
	{
		final View fragView = view;
		final String currencyPair = getCurrencyPair(selectedCurrencyPair);

		if (!forceUpdate)
		{
			JSONObject cachedData = ApplicationController.getInstace().readData(STORAGE_TAG_ORDERBOOK);

			if (cachedData == null || !cachedData.has(currencyPair))
			{
				forceUpdate = true;
			}
			else
			{
				parseOrderBook(cachedData, fragView);
			}
		}

		if (forceUpdate)
		{
			mKrakenApi.getOrderBook(currencyPair, 20, new KrakenAPI.UpdateUI()
			{
				@Override
				public void update(JSONObject jsonObject)
				{
					try
					{
						if (jsonObject.getJSONArray("error").length() == 0 && jsonObject.has("result"))
						{
							JSONObject result = jsonObject.getJSONObject("result");

							if (result.has(currencyPair)
									&& result.getJSONObject(currencyPair).has("asks")
									&& result.getJSONObject(currencyPair).has("bids"))
							{
								ApplicationController.getInstace().saveData(STORAGE_TAG_ORDERBOOK, result);
								parseOrderBook(result, fragView);
							}
						}

						onStopRefresh();
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}

				@Override
				public void error()
				{
					onStopRefresh();
				}
			});
		}
	}

	/**
	 * Parse the Server Time json data
	 *
	 * @param data
	 * @param view
	 */
	public void parseServertimeData(JSONObject data, View view)
	{
		if (!isAdded()) return;

		try
		{
			TextView krakentime = (TextView) view.findViewById(R.id.kraken_time);

			long time = data.getLong("unixtime") * (long) 1000;
			Date date = new Date(time);
			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

			krakentime.setText(format.format(date) + " CET");
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Parse the ticker json data
	 *
	 * @param data
	 * @param view
	 */
	public void parseTickerData(JSONObject data, View view)
	{
		if (!isAdded()) return;

		TextView price    = (TextView) view.findViewById(R.id.kraken_price);
		TextView hl       = (TextView) view.findViewById(R.id.market_body_detail_highlow_value);
		TextView oc       = (TextView) view.findViewById(R.id.market_body_detail_openclose_value);
		TextView ab       = (TextView) view.findViewById(R.id.market_body_detail_askbid_value);
		TextView vol      = (TextView) view.findViewById(R.id.market_body_detail_volume_value);

		JSONObject ticker = null;
		String high, low, ask, bid, open, close, volume, currency = "";

		String prices = "";

		try
		{
			Log.d("JSON", data.toString(4));

			JSONObject t;
			String p;

			t = data.getJSONObject(getCurrencyPair(MainActivity.CURRENCYPAIR_XXBTZEUR));
			p = t.getJSONArray("c").getString(0);
			p = p.substring(0, p.indexOf('.') + 3);

			prices += p;

			t = data.getJSONObject(getCurrencyPair(MainActivity.CURRENCYPAIR_XXBTZUSD));
			p = t.getJSONArray("c").getString(0);
			p = p.substring(0, p.indexOf('.') + 3);

			prices += ";" + p;

			Log.d("prices", prices);

			ApplicationController.getInstace()
					.storage()
					.putString(STORAGE_TAG_LASTPRICES, prices);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		if (mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG)
				== MainActivity.CURRENCYPAIR_XXBTZEUR)
		{
			currency = "€";
		}
		else if (mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG)
				== MainActivity.CURRENCYPAIR_XXBTZUSD)
		{
			currency = "$";
		}

		try
		{
			ticker = data.getJSONObject(getCurrencyPair(selectedCurrencyPair));

			high = ticker.getJSONArray("h").getString(0);
			high = high.substring(0, high.indexOf('.') + 3);

			low = ticker.getJSONArray("l").getString(0);
			low = low.substring(0, low.indexOf('.') + 3);

			ask = ticker.getJSONArray("a").getString(0);
			ask = ask.substring(0, ask.indexOf('.') + 3);

			bid = ticker.getJSONArray("b").getString(0);
			bid = bid.substring(0, bid.indexOf('.') + 3);

			open = ticker.getString("o");
			open = open.substring(0, open.indexOf('.') + 3);

			close = ticker.getJSONArray("c").getString(0);
			close = close.substring(0, close.indexOf('.') + 3);

			volume = ticker.getJSONArray("v").getString(0);

			price.setText(close + " " + currency);
			hl.setText(high + " / " + low + " " + currency);
			oc.setText(open + " / " + close + " " + currency);
			ab.setText("1 @ " + ask + " / " + bid + " " + currency);
			vol.setText(volume + " B");

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Parse the orderbook Json data
	 *
	 * @param data
	 * @param view
	 */
	public void parseOrderBook(JSONObject data, View view)
	{
		if (!isAdded()) return;

		TableLayout tableAsks = (TableLayout) view.findViewById(R.id.fragment_markets_orderbook_asks_table);
		TableLayout tableBids = (TableLayout) view.findViewById(R.id.fragment_markets_orderbook_bids_table);

		String currencyPair = getCurrencyPair(selectedCurrencyPair);
		JSONObject orderbook = null;

		initOrderBookTables(tableAsks, tableBids);

		try
		{
			orderbook = data.getJSONObject(currencyPair);

			if (orderbook.length() == 2 && orderbook.has("asks") && orderbook.has("bids"))
			{
				JSONArray asks = orderbook.getJSONArray("asks");
				JSONArray bids = orderbook.getJSONArray("bids");

				buildOrderBookTable(tableAsks, asks);
				buildOrderBookTable(tableBids, bids);
			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * Initialize the orderbook table
	 *
	 * @param tableAsks
	 * @param tableBids
	 */
	private void initOrderBookTables(TableLayout tableAsks, TableLayout tableBids)
	{
		TableRow headrowAsks = new TableRow(getActivity());
		TableRow headrowBids = new TableRow(getActivity());

		TextView priceAsks = new TextView(getActivity());
		TextView volumeAsks = new TextView(getActivity());
		TextView priceBids = new TextView(getActivity());
		TextView volumeBids = new TextView(getActivity());

		TableRow.LayoutParams params = new TableRow.LayoutParams(
				TableRow.LayoutParams.WRAP_CONTENT,
				TableRow.LayoutParams.WRAP_CONTENT,
				TABLE_ORDERBOOK_ELEMENT_WEIGHT
		);

		headrowAsks.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT
		));
		headrowAsks.setWeightSum(1);

		headrowBids.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT
		));
		headrowBids.setWeightSum(1);

		priceAsks.setLayoutParams(params);
		priceAsks.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_ORDERBOOK_HEAD_TEXT_SIZE_SP);
		priceAsks.setText(R.string.fragment_markets_orderbook_asksbidstables_heading_price_label);

		volumeAsks.setLayoutParams(params);
		volumeAsks.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_ORDERBOOK_HEAD_TEXT_SIZE_SP);
		volumeAsks.setText(R.string.fragment_markets_orderbook_asksbidstables_heading_volume_label);

		priceBids.setLayoutParams(params);
		priceBids.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_ORDERBOOK_HEAD_TEXT_SIZE_SP);
		priceBids.setText(R.string.fragment_markets_orderbook_asksbidstables_heading_price_label);

		volumeBids.setLayoutParams(params);
		volumeBids.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_ORDERBOOK_HEAD_TEXT_SIZE_SP);
		volumeBids.setText(R.string.fragment_markets_orderbook_asksbidstables_heading_volume_label);

		headrowAsks.addView(priceAsks);
		headrowAsks.addView(volumeAsks);
		headrowBids.addView(priceBids);
		headrowBids.addView(volumeBids);

		tableAsks.removeAllViewsInLayout();
		tableBids.removeAllViewsInLayout();

		tableAsks.addView(headrowAsks);
		tableBids.addView(headrowBids);
	}

	/**
	 * Fill the orderbook table
	 *
	 * @param table
	 * @param data
	 */
	private void buildOrderBookTable(TableLayout table, JSONArray data)
	{
		DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.getDefault());
		dfs.setDecimalSeparator('.');
		dfs.setGroupingSeparator(',');
		DecimalFormat df = new DecimalFormat("0.0000", dfs);

		TableRow.LayoutParams params = new TableRow.LayoutParams(
				TableRow.LayoutParams.WRAP_CONTENT,
				TableRow.LayoutParams.WRAP_CONTENT,
				TABLE_ORDERBOOK_ELEMENT_WEIGHT
		);

		for(int i = 0; i < data.length(); i++)
		{
			try
			{
				JSONArray singleAsk = data.getJSONArray(i);

				TableRow row = new TableRow(getActivity());
				TextView price = new TextView(getActivity());
				TextView volume = new TextView(getActivity());

				row.setLayoutParams(new TableRow.LayoutParams(
						TableRow.LayoutParams.FILL_PARENT,
						TableRow.LayoutParams.WRAP_CONTENT
				));
				row.setWeightSum(1);

				price.setLayoutParams(params);
				price.setTextColor(Color.BLACK);
				price.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_ORDERBOOK_BODY_TEXT_SIZE_SP);
				price.setText(df.format(singleAsk.getDouble(0)));

				volume.setLayoutParams(params);
				volume.setTextColor(Color.BLACK);
				volume.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_ORDERBOOK_BODY_TEXT_SIZE_SP);
				volume.setText(df.format(singleAsk.getDouble(1)));

				row.addView(price);
				row.addView(volume);

				table.addView(row);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the currency pair string using his integer id
	 *
	 * @param currencyPair
	 * @return
	 */
	private String getCurrencyPair(int currencyPair)
	{
		switch (currencyPair)
		{
			case MainActivity.CURRENCYPAIR_XXBTZEUR:
				return getString(R.string.XXBTZEUR);
			case MainActivity.CURRENCYPAIR_XXBTZUSD:
				return getString(R.string.XXBTZUSD);
			default:
		}

		return null;
	}

	public void assets()
	{
		/*
		<asset_name> = asset name
		    altname = alternate name
		    aclass = asset class
		    decimals = scaling decimal places for record keeping
		    display_decimals = scaling decimal places for output display
		 */

		mKrakenApi.getAssets(new KrakenAPI.UpdateUI()
		{
			@Override
			public void update(JSONObject jsonResponse)
			{
				try
				{
					JSONObject result = jsonResponse.getJSONObject("result");
					Iterator<String> keys = result.keys();
					//tv.setText("");
					while (keys.hasNext())
					{
						String next = keys.next();
						JSONObject currency = result.getJSONObject(next);

						//tv.append(next + " | " + currency.getString("altname") + "\n");
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				//text.setText(jsonResponse.toString());
			}

			@Override
			public void error()
			{
				onStopRefresh();
			}
		});
	}

	public void assetPairs()
	{
		final ArrayList<String> tradables = new ArrayList<String>();
		/*
		<pair_name> = pair name
		    altname = alternate pair name
		    aclass_base = asset class of base component
		    base = asset id of base component
		    aclass_quote = asset class of quote component
		    quote = asset id of quote component
		    lot = volume lot size
		    pair_decimals = scaling decimal places for pair
		    lot_decimals = scaling decimal places for volume
		    lot_multiplier = amount to multiply lot volume by to get currency volume
		    leverage = array of leverage amounts available
		    fees = fee schedule array in [volume, percent fee] tuples
		    fee_volume_currency = volume discount currency
		    margin_call = margin call level
		    margin_stop = stop-out/liquidation margin level
		*/

		mKrakenApi.getAssetPairs(new KrakenAPI.UpdateUI()
		{
			@Override
			public void update(JSONObject jsonObject)
			{
				try
				{
					JSONObject result = jsonObject.getJSONObject("result");
					Iterator<String> i = result.keys();
					//tv.setText("Tradable Pairs:\n");
					while (i.hasNext())
					{
						String key = i.next();
						tradables.add(key);

						JSONObject tradable = result.getJSONObject(key);
						//Log.d("tradable", tradable.getString("altname"));
						//tv.append(tradable.getString("base") + "/" + tradable.getString("quote") +"\n");
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			@Override
			public void error()
			{
				onStopRefresh();
			}
		});
	}
}
