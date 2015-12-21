package it.bitrocket.krakenexchange;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.Locale;


/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class FragmentAccount extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener
{
	private static final String ARG_SECTION_NUMBER          = "account_section";
	private static final int TABLE_BODY_TEXT_SIZE_SP        = 18;

	public static final String STORAGE_TAG_BALANCE          = "kraken_account_balance";
	public static final String STORAGE_TAG_TOTALBALANCE     = "kraken_account_totalbalance";

	public int selectedCurrency;

	private KrakenAPI mKrakenApi;
	private Storage mStorage;

	private SwipeRefreshLayout mSwipeLayout;

	/**
	 * constructor
	 */
	public FragmentAccount()
	{
		// Required empty public constructor
	}

	public static FragmentAccount newInstance(int sectionNumber)
	{
		FragmentAccount fragment = new FragmentAccount();

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

		selectedCurrency = ApplicationController.getInstace()
				.storage().getInt(Storage.STORAGE_SELECTEDCURRENCY_TAG);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		ApiTokenStore ats = new ApiTokenStore();

		/**
		 * If API tokens are not setted display an empty GUI
		 * with a button that redirects to API Setup Activity
		 */
		if (!ats.isApiAlreadySet())
		{
			View block_view         =
					inflater.inflate(R.layout.fragment_account_block, container, false);

			Button gotoApiButton    =
					(Button) block_view.findViewById(R.id.fragment_account_block_button_goto_api);

			gotoApiButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
					goToApiSetup(v);
				}
			});

			return block_view;
		}

		View frag_view =  inflater.inflate(R.layout.fragment_account, container, false);

		mSwipeLayout = (SwipeRefreshLayout) frag_view.findViewById(R.id.swipe_container_account);
		mSwipeLayout.setOnRefreshListener(this);

		updateBalances(frag_view, false);

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
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				updateBalances(getView(), true);
			}
		}, 2000);
	}

	public void onTriggeredRefresh()
	{
		mSwipeLayout.post(new Runnable()
		{
			@Override
			public void run()
			{
				mSwipeLayout.setRefreshing(true);
				updateBalances(getView(), false);
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
	 * Redirect the user to the ApiSetupActivity
	 *
	 * @param view
	 */
	public void goToApiSetup(View view)
	{
		Intent intent = new Intent(getActivity(), ApiSetupActivity.class);
		startActivity(intent);
	}

	/**
	 * This API Call update the balances rows and then it call
	 * the "updateTotalBalance()" method.
	 *
	 * This is a workaround to prevent Race Condition
	 * TODO: implement custom request -> http://stackoverflow.com/questions/29012434
	 *
	 * @param view
	 * @param forceUpdate
	 */
	public void updateBalances(final View view, boolean forceUpdate)
	{
		final TableLayout table = (TableLayout) view.findViewById(R.id.fragment_account_balances_table);

		if (!forceUpdate)
		{
			JSONObject cachedData = ApplicationController.getInstace().readData(STORAGE_TAG_BALANCE);

			if (cachedData == null)
			{
				forceUpdate = true;
			}
			else
			{
				parseBalanceData(cachedData, table);
				updateTotalBalance(view, false);
			}
		}

		if (forceUpdate)
		{
			mKrakenApi.getBalance(new KrakenAPI.UpdateUI()
			{
				@Override
				public void update(JSONObject jsonObject)
				{
					try
					{
						JSONArray errors = jsonObject.getJSONArray("error");
						if(errors.length() > 0)
						{
							throw KrakenApiException.newInstance(errors.getString(0));
						}

						if (jsonObject.has("result"))
						{
							JSONObject result = jsonObject.getJSONObject("result");

							ApplicationController.getInstace().saveData(STORAGE_TAG_BALANCE, result);

							parseBalanceData(result, table);
						}

						// chain the second request in order to avoid race condition
						updateTotalBalance(view, true);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					catch (KrakenApiException e)
					{
						Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_LONG).show();
						onStopRefresh();
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
	 * Update the Total Balance row into the GUI
	 *
	 * @param view
	 * @param forceUpdate
	 */
	public void updateTotalBalance(final View view, boolean forceUpdate)
	{
		final int selecteCurrencyId         = selectedCurrency;
		final String selecteCurrencyString  = getCurrency(selectedCurrency);

		final TextView totalBalanceLabelView = (TextView) view.findViewById(R.id.fragment_account_totalbalance_label);
		final TextView totalBalanceTextView = (TextView) view.findViewById(R.id.fragment_account_totalbalance_value);

		if (!forceUpdate)
		{
			JSONObject cachedData = ApplicationController.getInstace().readData(STORAGE_TAG_TOTALBALANCE);

			int cachedCurrencyId = -1;

			if (cachedData.has("currency"))
			{
				try
				{
					cachedCurrencyId = cachedData.getInt("currency");
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			if (cachedData == null || cachedCurrencyId == -1 || cachedCurrencyId != selecteCurrencyId)
			{
				forceUpdate = true;
			}
			else
			{
				parseTotalBalanceData(cachedData, totalBalanceLabelView, totalBalanceTextView);
			}
		}

		if (forceUpdate)
		{
			mKrakenApi.getTradeBalance(selecteCurrencyString, new KrakenAPI.UpdateUI()
			{
				@Override
				public void update(JSONObject jsonObject)
				{
					try
					{
						JSONArray errors = jsonObject.getJSONArray("error");
						if(errors.length() > 0)
						{
							throw KrakenApiException.newInstance(errors.getString(0));
						}

						if (jsonObject.has("result"))
						{
							JSONObject result = jsonObject.getJSONObject("result");
							result.put("currency", selecteCurrencyId);

							ApplicationController.getInstace().saveData(STORAGE_TAG_TOTALBALANCE, result);

							parseTotalBalanceData(result, totalBalanceLabelView, totalBalanceTextView);
						}

					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					catch (KrakenApiException e)
					{
						Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_LONG).show();
					}

					onStopRefresh();
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
	 * Given a dataset of Banlance Data and a TableView Object this method parse the data
	 * and fill the "table" with rows
	 *
	 * @param data
	 * @param table
	 */
	public void parseBalanceData(JSONObject data, TableLayout table)
	{
		try
		{
			if (data.length() > 0)
			{
				DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.getDefault());
				dfs.setDecimalSeparator('.');
				dfs.setGroupingSeparator(',');
				DecimalFormat decimalFormatter = new DecimalFormat("0.0000", dfs);

				table.removeAllViewsInLayout();
				Iterator<String> keys = data.keys();

				float row_height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());

				while (keys.hasNext())
				{
					String currency = keys.next();
					float field_weight = 0.5f;

					TableRow row = new TableRow(getActivity());
					row.setLayoutParams(new TableRow.LayoutParams(
							TableRow.LayoutParams.FILL_PARENT,
					        TableRow.LayoutParams.FILL_PARENT
					));
					row.setMinimumHeight(Math.round(row_height));
					row.setWeightSum(1);

					TextView currency_label = new TextView(getActivity());
					TextView currency_value = new TextView(getActivity());

					TableRow.LayoutParams params = new TableRow.LayoutParams(
							TableRow.LayoutParams.MATCH_PARENT,
							TableRow.LayoutParams.MATCH_PARENT,
							field_weight
					);

					currency_label.setLayoutParams(params);
					currency_value.setLayoutParams(params);

					currency_label.setGravity(Gravity.CENTER_VERTICAL);
					currency_label.setTextColor(Color.BLACK);
					currency_label.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					currency_label.setText(currency);

					currency_value.setGravity(Gravity.CENTER_VERTICAL + Gravity.RIGHT);
					currency_value.setTextColor(Color.BLACK);
					currency_value.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					currency_value.setText(decimalFormatter.format(data.getDouble(currency)));
					//currency_value.setText(data.getString(currency));

					row.addView(currency_label);
					row.addView(currency_value);
					table.addView(row);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Given the data of the User's Total balance and a TableView Object this method parse the data
	 * and fill the "table" with a row
	 *
	 * @param data
	 * @param totalBalanceLabelView
	 * @param totalBalanceTextView
	 */
	public void parseTotalBalanceData(JSONObject data, TextView totalBalanceLabelView, TextView totalBalanceTextView)
	{
		if (selectedCurrency == MainActivity.CURRENCY_ZEUR)
		{
			totalBalanceLabelView
					.setText(getString(R.string.fragment_account_totalbalance_label) + " €");
		}
		else if (selectedCurrency == MainActivity.CURRENCY_ZUSD)
		{
			totalBalanceLabelView
					.setText(getString(R.string.fragment_account_totalbalance_label) + " $");
		}

		try
		{
			if (data.has("tb"))
			{
				DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.getDefault());
				dfs.setDecimalSeparator('.');
				dfs.setGroupingSeparator(',');

				DecimalFormat decimalFormatter = new DecimalFormat("0.0000", dfs);

				totalBalanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				totalBalanceTextView.setTextColor(getResources().getColor(R.color.kraken_white));
				totalBalanceTextView.setText(decimalFormatter.format(data.getDouble("tb")));
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Get the currency name string using his integer id
	 *
	 * @param currencyId
	 * @return
	 */
	private String getCurrency(int currencyId)
	{
		switch (currencyId)
		{
			case MainActivity.CURRENCY_ZEUR:
				return getString(R.string.ZEUR);
			case MainActivity.CURRENCY_ZUSD:
				return getString(R.string.ZUSD);
			default:
		}

		return null;
	}
}