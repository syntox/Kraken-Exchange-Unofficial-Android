package it.bitrocket.krakenexchange;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
public class FragmentTrading extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener
{
	private static final String ARG_SECTION_NUMBER                  = "trading_section";

	private static final int TABLE_HEAD_TEXT_SIZE_SP                = 14;
	private static final int TABLE_BODY_TEXT_SIZE_SP                = 16;
	private static final int TABLE_ROW_PADDING_LEFT_DIP             = 12;

	private static final float TABLE_OPENORDERS_ELEMENT_WEIGHT      = 0.2f;
	private static final float TABLE_CLOSEDORDERS_ELEMENT_WEIGHT    = 0.2f;

	public static final String STORAGE_TAG_OPENORDERS               = "kraken_trading_openorders";
	public static final String STORAGE_TAG_CLOSEDORDERS             = "kraken_trading_closedorders";

	public static final String ORDER_CLOSED                         = "closed";
	public static final String ORDER_CANCELED                       = "canceled";

	private KrakenAPI mKrakenApi;
	private Storage mStorage;

	private SwipeRefreshLayout mSwipeLayout;
	private FloatingActionButton mFloatingActionButton;
	private int mTableRowPaddingLeftDip;

	/**
	 * constructor
	 */
	public FragmentTrading()
	{
		// Required empty public constructor
	}

	public static FragmentTrading newInstance(int sectionNumber)
	{
		FragmentTrading fragment = new FragmentTrading();

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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		ApiTokenStore ats = new ApiTokenStore();
		if (ats.isApiAlreadySet() == false)
		{
			mFloatingActionButton = null;

			View block_view = inflater.inflate(R.layout.fragment_trading_block, container, false);
			Button gotoApiButton = (Button) block_view.findViewById(R.id.fragment_trading_block_button_goto_api);

			gotoApiButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
					goToApiSetup(v);
				}
			});

			return block_view;

		}

		final View frag_view =  inflater.inflate(R.layout.fragment_trading, container, false);

		// Activate swipe layout
		mSwipeLayout = (SwipeRefreshLayout) frag_view.findViewById(R.id.swipe_container_trading);
		mSwipeLayout.setOnRefreshListener(this);

		// Build and add to GUI a Floating Action Button to create new Order
		FloatingActionButton.Builder fabBuilder = new FloatingActionButton.Builder(getActivity())
				.withDrawable(getResources().getDrawable(R.drawable.ic_action_new))
				.withButtonColor(getResources().getColor(R.color.kraken_blue))
				.withGravity(Gravity.BOTTOM | Gravity.RIGHT)
				.withMargins(0, 0, 16, 16);

		mFloatingActionButton = fabBuilder.create();


		/**
		 * The Floating Action Button will spawn an OrderDialogFragment (Dialog).
		 *
		 * If the user confirm the Dialog (to adding the new order) the OrderDialogFragment's
		 * "onClickListener()" will try to submit an API request using the KrakenAPI Class.
		 * Using the code below we pass the KrakenAPI.UpdateUI() callback to OrderDialogFragment
		 * in order to control the application flow inside the OrderDialogFragment Dialog.
		 *
		 * If API request go successful the code triggers the SwipeRefreshLayout and the onRefresh() method
		 * elsewhere an inline AlertDialog will be spawn with a generic error message
		 *
		 */
		mFloatingActionButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				// Show input dialog to create a new Trading Order
				// Set here the callback of the Kraken API Request
				FragmentOrderDialog odf = FragmentOrderDialog.newInstance(new KrakenAPI.UpdateUI()
				{
					@Override
					public void update(JSONObject jsonObject)
					{
						try
						{
							JSONArray error = jsonObject.getJSONArray("error");
							if (error.length() > 0)
							{
								String errorMessage = error.getString(0);
								errorMessage = errorMessage.substring(errorMessage.indexOf(':') + 1);

								// Spawn error dialog
								new AlertDialog.Builder(getActivity())
										.setTitle("Kraken Error")
										.setMessage(errorMessage)
										.create()
										.show();

							}
							else if (jsonObject.has("result"))
							{
								JSONObject result = jsonObject.getJSONObject("result");
								if (result.length() > 0)
								{
									// Update the GUI triggering the SwipeRefreshLayout
									mSwipeLayout.post(new Runnable()
									{
										@Override
										public void run()
										{
											Toast.makeText(getActivity(),
											               R.string.order_add_successful,
											               Toast.LENGTH_LONG).show();

											mSwipeLayout.setRefreshing(true);
											onRefreshOpenOrder(false);
										}
									});
								}
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

				// Spawn the OrderDialogFragment Dialog
				odf.show(getFragmentManager(), "orderDialog");
			}
		});

		/**
		 * In order to get the FAB Button works it must be attached on Activity container.
		 * This means that with this Application (based on Fragment) when whe change the
		 * fragment using the NavigationDrawer the FAB Button will be present in all of the
		 * fragment spawned by MainActivity.
		 *
		 * Using FloatingActionButton "hideFloatingActionButton()" and "showFloatingActionButton()" methods
		 * the app can hide and show it only if the user is using this fragment "FragmentTrading"
		 */
		ViewGroup root = (ViewGroup) getActivity().findViewById(android.R.id.content);
		root.addView(mFloatingActionButton, fabBuilder.getParams());
		root.setMotionEventSplittingEnabled(false);

		updateOpenOrders(frag_view, true, false);

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
	public void onHiddenChanged(boolean hidden)
	{
		super.onHiddenChanged(hidden);

		if (hidden)
		{
			hideFab();
		}
		else
		{
			showFab();
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		hideFab();
	}

	public void showFab()
	{
		if (mFloatingActionButton != null)
		{
			mFloatingActionButton.showFloatingActionButton();
		}
	}

	public void hideFab()
	{
		if (mFloatingActionButton != null)
		{
			mFloatingActionButton.hideFloatingActionButton();
		}
	}

	/**
	 * onRefresh() is called by the SwipeRefreshLayout, forcing a network update,
	 * when the user scroll down the view
	 */
	@Override
	public void onRefresh()
	{
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				updateOpenOrders(getView(), true, true);
			}
		}, 2000);
	}

	/**
	 * Refresh the list of Open Orders using Handler(), forcing a network update
	 *
	 * If refreshUpdateClosedOrders is set to true "updateOpenOrders()" will call
	 * the updateClosedOrders() method.
	 *
	 * @param refreshUpdateClosedOrders
	 */
	public void onRefreshOpenOrder(final boolean refreshUpdateClosedOrders)
	{
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				updateOpenOrders(getView(), refreshUpdateClosedOrders, true);
			}
		}, 2000);
	}

	/**
	 * Refresh the list of Closed Orders using Handler(), forcing a network update
	 */
	public void onRefreshClosedOrder()
	{
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				updateClosedOrders(getView(), true);
			}
		}, 2000);
	}

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
	 * Refresh the OpenOrders list
	 *
	 * @param view
	 * @param callUpdateClosedOrderMethod
	 * @param forceUpdate
	 */
	public void updateOpenOrders(final View view, final boolean callUpdateClosedOrderMethod, boolean forceUpdate)
	{
		final TableLayout table = (TableLayout) view.findViewById(R.id.trading_openorders_table_content);

		if (!forceUpdate)
		{
			JSONObject cachedData = ApplicationController.getInstace().readData(STORAGE_TAG_OPENORDERS);

			if (cachedData == null)
			{
				forceUpdate = true;
			}
			else
			{
				parseOpenOrdersData(cachedData, table, mKrakenApi);
				updateClosedOrders(view, false);
			}
		}

		if (forceUpdate)
		{

			mKrakenApi.getOpenOrders(new KrakenAPI.UpdateUI()
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
							if (result.length() > 0)
							{
								ApplicationController.getInstace().saveData(STORAGE_TAG_OPENORDERS, result);
								parseOpenOrdersData(result, table, mKrakenApi);
							}
						}

						if (callUpdateClosedOrderMethod)
						{
							updateClosedOrders(view, true);
						}
						else
						{
							onStopRefresh();
						}

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
	 * Refresh the ClosedOrders list
	 *
	 * @param view
	 * @param forceUpdate
	 */
	public void updateClosedOrders(View view, boolean forceUpdate)
	{
		final TableLayout table = (TableLayout) view.findViewById(R.id.trading_closedorders_table_content);

		if (!forceUpdate)
		{
			final JSONObject cachedData = ApplicationController.getInstace().readData(STORAGE_TAG_CLOSEDORDERS);

			if (cachedData == null)
			{
				forceUpdate = true;
			}
			else
			{
				parseClosedOrdersData(cachedData, table);
			}
		}

		if (forceUpdate)
		{
			mKrakenApi.getClosedOrders(new KrakenAPI.UpdateUI()
			{
				@Override
				public void update(JSONObject jsonObject)
				{
					try
					{
						if (jsonObject.has("result"))
						{
							JSONArray errors = jsonObject.getJSONArray("error");
							if (errors.length() > 0)
							{
								throw KrakenApiException.newInstance(errors.getString(0));
							}

							JSONObject result = jsonObject.getJSONObject("result");
							if (result.length() > 0)
							{
								ApplicationController.getInstace().saveData(STORAGE_TAG_CLOSEDORDERS, result);
								parseClosedOrdersData(result, table);
							}
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
	 * Parse API Response of the OpenOrders API Call
	 *
	 * @param data
	 * @param table
	 * @param kraken
	 */
	public void parseOpenOrdersData(JSONObject data, TableLayout table, final KrakenAPI kraken)
	{
		initOpenOrdersTable(table);

		try
		{
			JSONObject open_orders = data.getJSONObject("open");

			if (open_orders.length() > 0)
			{
				float row_height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
				float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());

				DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.getDefault());
				dfs.setDecimalSeparator('.');
				dfs.setGroupingSeparator(',');
				DecimalFormat df = new DecimalFormat("#####.##", dfs);

				Iterator<String> keys = open_orders.keys();

				while (keys.hasNext())
				{
					final String order_id = keys.next();
					JSONObject order = open_orders.getJSONObject(order_id);
					JSONObject order_desc = order.getJSONObject("descr");

					String type, ordertype, pair, price, volume, currency;

					type = order_desc.getString("type");
					ordertype = order_desc.getString("ordertype");
					pair = order_desc.getString("pair");
					price = order_desc.getString("price");
					volume = order.getString("vol");
					currency = "";

					if (pair.contains("XBT"))
					{
						//currency = "฿";
					}

					if (ordertype.equals("market"))
					{
						price = order.getString("price");

						if (order.getString("oflags").equals("viqc") && pair.contains("EUR"))
						{
							currency = "€";
						}
					}

					final TableRow row = new TableRow(getActivity());
					row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT));
					row.setWeightSum(1);

					TextView tv_type    = new TextView(getActivity());
					TextView tv_pair    = new TextView(getActivity());
					TextView tv_price   = new TextView(getActivity());
					TextView tv_volume  = new TextView(getActivity());

					// iv_action is the Delete Order action button
					// callbacks are defined below
					final ImageView iv_action = new ImageView(getActivity());

					TableRow.LayoutParams params = new TableRow.LayoutParams(
							TableRow.LayoutParams.MATCH_PARENT,
							TableRow.LayoutParams.FILL_PARENT,
							TABLE_OPENORDERS_ELEMENT_WEIGHT
					);

					tv_type.setLayoutParams(params);
					tv_pair.setLayoutParams(params);
					tv_price.setLayoutParams(params);
					tv_volume.setLayoutParams(params);
					iv_action.setLayoutParams(params);

					tv_type.setPadding(getTableRowPaddingLeft(), 0, 0, 0);
					tv_type.setHeight(Math.round(row_height));
					tv_type.setGravity(Gravity.CENTER_VERTICAL);
					tv_type.setTextColor(Color.BLACK);
					tv_type.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_type.setText(type + "/" + ordertype);

					tv_pair.setGravity(Gravity.CENTER_VERTICAL);
					tv_pair.setTextColor(Color.BLACK);
					tv_pair.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_pair.setText(pair);

					tv_price.setGravity(Gravity.CENTER_VERTICAL);
					tv_price.setTextColor(Color.BLACK);
					tv_price.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_price.setText(df.format(Float.valueOf(price)));

					tv_volume.setGravity(Gravity.CENTER_VERTICAL);
					tv_volume.setTextColor(Color.BLACK);
					tv_volume.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_volume.setText(df.format(Float.valueOf(volume)) + currency);

					iv_action.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_discard));
					iv_action.setClickable(true);
					iv_action.setOnTouchListener(new View.OnTouchListener()
					{
						@Override
						public boolean onTouch(View v, MotionEvent event)
						{
							switch (event.getAction())
							{
								case MotionEvent.ACTION_UP:
								case MotionEvent.ACTION_CANCEL:
									row.setAlpha(1f);
									row.setBackgroundColor(getResources().getColor(R.color.kraken_white));
									break;
								case MotionEvent.ACTION_DOWN:
									row.setAlpha(0.6f);
									row.setBackgroundColor(getResources().getColor(R.color.order_canceled));
									break;
							}

							return v.onTouchEvent(event);
						}
					});

					iv_action.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							// Spawn the confirmation dialog
							new AlertDialog.Builder(getActivity())
									.setTitle(R.string.order_deletion_dialog_title)
									.setMessage("Order: " + order_id.substring(0, 6))
									.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

										@Override
										public void onClick(DialogInterface dialog, int which)
										{
											Toast.makeText(getActivity(), R.string.wait_please, Toast.LENGTH_LONG).show();

											kraken.cancelOrder(order_id, new KrakenAPI.UpdateUI()
											{
												@Override
												public void update(JSONObject jsonObject)
												{
													try
													{
														if (jsonObject.getJSONArray("error").length() == 0)
														{
															mSwipeLayout.post(new Runnable()
															{
																@Override
																public void run()
																{
																	Toast.makeText(getActivity(),
																	               "Order " + order_id.substring(0, 6) + " deleted!",
																	               Toast.LENGTH_LONG).show();

																	mSwipeLayout.setRefreshing(true);
																	onRefreshOpenOrder(true);
																}
															});
														}
														else
														{
															Toast.makeText(getActivity(),
															               R.string.order_deletion_error,
															               Toast.LENGTH_LONG).show();
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
									})
									.setNegativeButton(android.R.string.cancel, null)
									.create()
									.show();
						}
					});

					row.addView(tv_type);
					row.addView(tv_pair);
					row.addView(tv_price);
					row.addView(tv_volume);
					row.addView(iv_action);

					//row.setPadding(0, 0, 0, Math.round(padding));

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
	 * Parse API Response of the ClosedOrders API Call
	 *
	 * @param data
	 * @param table
	 */
	public void parseClosedOrdersData(JSONObject data, TableLayout table)
	{
		initClosedOrdersTable(table);

		try
		{
			JSONObject closed_orders = data.getJSONObject("closed");

			if (closed_orders.length() > 0)
			{
				float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());

				DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.getDefault());
				dfs.setDecimalSeparator('.');
				dfs.setGroupingSeparator(',');
				DecimalFormat df = new DecimalFormat("0.0000", dfs);

				Iterator<String> keys = closed_orders.keys();

				// show only last 15 order
				int i = 0;

				while (keys.hasNext() && i <= 15)
				{
					i++;

					String type, ordertype, pair, price, volume, status, currency;

					String order_id         = keys.next();
					JSONObject order        = closed_orders.getJSONObject(order_id);

					JSONObject order_desc   = order.getJSONObject("descr");

					type        = order_desc.getString("type");
					ordertype   = order_desc.getString("ordertype");
					pair        = order_desc.getString("pair");
					price       = order_desc.getString("price");
					volume      = order.getString("vol_exec");
					status      = order.getString("status");
					currency    = "";

					if (pair.contains("XBT"))
					{
						//currency = "฿";
					}

					if (ordertype.equals("market"))
					{
						price = order.getString("price");

						if (order.getString("oflags").equals("viqc") && pair.contains("EUR"))
						{
							currency = "€";
						}
					}

					//Log.d(order_id, type+"/"+ordertype+";"+pair+";"+price+";"+price2+";"+volume+";"+status+";");

					TableRow row = new TableRow(getActivity());
					row.setWeightSum(1);
					row.setLayoutParams(new TableRow.LayoutParams(5));

					TextView tv_type    = new TextView(getActivity());
					TextView tv_pair    = new TextView(getActivity());
					TextView tv_price   = new TextView(getActivity());
					TextView tv_volume  = new TextView(getActivity());
					TextView tv_status  = new TextView(getActivity());

					TableRow.LayoutParams params = new TableRow.LayoutParams(
							TableRow.LayoutParams.MATCH_PARENT,
							TableRow.LayoutParams.FILL_PARENT,
							TABLE_CLOSEDORDERS_ELEMENT_WEIGHT
					);

					tv_type.setLayoutParams(params);
					tv_pair.setLayoutParams(params);
					tv_price.setLayoutParams(params);
					tv_volume.setLayoutParams(params);

					tv_type.setPadding(getTableRowPaddingLeft(),0,0,0);
					tv_type.setTextColor(Color.BLACK);
					tv_type.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_type.setText(type + "/" + ordertype);

					tv_pair.setTextColor(Color.BLACK);
					tv_pair.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_pair.setText(pair);

					tv_price.setTextColor(Color.BLACK);
					tv_price.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_price.setText(df.format(Float.valueOf(price)));

					tv_volume.setTextColor(Color.BLACK);
					tv_volume.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_volume.setText(df.format(Float.valueOf(volume)) + currency);

					tv_status.setTextColor(Color.BLACK);
					tv_status.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_BODY_TEXT_SIZE_SP);
					tv_status.setText(status);

					ImageView circleicon = new ImageView(getActivity());

					circleicon.setLayoutParams(params);

					if (status.equalsIgnoreCase(ORDER_CLOSED))
					{
						//row.setBackgroundColor(getResources().getColor(R.color.order_closed));
						//tv_status.setBackgroundColor(getResources().getColor(R.color.order_closed));
						circleicon.setImageDrawable(getResources().getDrawable(R.drawable.circle_icon_green));
					}
					else if (status.equalsIgnoreCase(ORDER_CANCELED))
					{
						//row.setBackgroundColor(getResources().getColor(R.color.order_canceled));
						//tv_status.setBackgroundColor(getResources().getColor(R.color.order_canceled));
						circleicon.setImageDrawable(getResources().getDrawable(R.drawable.circle_icon_red));
					}

					row.addView(tv_type);
					row.addView(tv_pair);
					row.addView(tv_price);
					row.addView(tv_volume);
					row.addView(circleicon);


					row.setPadding(0,0,0, Math.round(padding));

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
	 * Initialize the Open Orders table in order to prepare it to get fulfilled by
	 * parseOpenOrdersData() method
	 *
	 * @param table
	 */
	private void initOpenOrdersTable(TableLayout table)
	{
		TableRow headrow = new TableRow(getActivity());

		TextView h1 = new TextView(getActivity());
		TextView h2 = new TextView(getActivity());
		TextView h3 = new TextView(getActivity());
		TextView h4 = new TextView(getActivity());
		TextView h5 = new TextView(getActivity());

		TableRow.LayoutParams params = new TableRow.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.MATCH_PARENT,
				TABLE_OPENORDERS_ELEMENT_WEIGHT
		);

		headrow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT));
		headrow.setWeightSum(1);

		h1.setLayoutParams(params);
		h1.setPadding(getTableRowPaddingLeft(), 0, 0, 0);
		h1.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h1.setText(R.string.orders_table_heading_type);

		h2.setLayoutParams(params);
		h2.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h2.setText(R.string.orders_table_heading_pair);

		h3.setLayoutParams(params);
		h3.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h3.setText(R.string.orders_table_heading_price);

		h4.setLayoutParams(params);
		h4.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h4.setText(R.string.orders_table_heading_volume);

		h5.setLayoutParams(params);
		h5.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h5.setText(""); // placeholder

		headrow.addView(h1);
		headrow.addView(h2);
		headrow.addView(h3);
		headrow.addView(h4);
		headrow.addView(h5);

		table.removeAllViewsInLayout();
		table.addView(headrow);
	}

	/**
	 * Initialize the Closed Orders table in order to prepare it to get fulfilled by
	 * parseClosedOrdersData() method
	 *
	 * @param table
	 */
	private void initClosedOrdersTable(TableLayout table)
	{
		TableRow headrow = new TableRow(getActivity());

		TextView h1 = new TextView(getActivity());
		TextView h2 = new TextView(getActivity());
		TextView h3 = new TextView(getActivity());
		TextView h4 = new TextView(getActivity());
		TextView h5 = new TextView(getActivity());

		TableRow.LayoutParams params = new TableRow.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.FILL_PARENT,
				TABLE_CLOSEDORDERS_ELEMENT_WEIGHT
		);

		headrow.setWeightSum(1);
		headrow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT));

		h1.setLayoutParams(params);
		h1.setPadding(getTableRowPaddingLeft(), 0, 0, 0);
		h1.setGravity(Gravity.CENTER_VERTICAL);
		h1.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h1.setText(R.string.orders_table_heading_type);

		h2.setLayoutParams(params);
		h2.setGravity(Gravity.CENTER_VERTICAL);
		h2.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h2.setText(R.string.orders_table_heading_pair);

		h3.setLayoutParams(params);
		h3.setGravity(Gravity.CENTER_VERTICAL);
		h3.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h3.setText(R.string.orders_table_heading_price);

		h4.setLayoutParams(params);
		h4.setGravity(Gravity.CENTER_VERTICAL);
		h4.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h4.setText(R.string.orders_table_heading_vol_exec);

		h5.setLayoutParams(params);
		h5.setGravity(Gravity.CENTER_VERTICAL);
		h5.setTextSize(TypedValue.COMPLEX_UNIT_SP, TABLE_HEAD_TEXT_SIZE_SP);
		h5.setText(R.string.orders_table_heading_status);

		headrow.addView(h1);
		headrow.addView(h2);
		headrow.addView(h3);
		headrow.addView(h4);
		headrow.addView(h5);

		table.removeAllViewsInLayout();
		table.addView(headrow);
	}

	/**
	 * Retrieve the value (in Pixels) of the Left padding used to get a good alignment
	 *
	 * @return
	 */
	private int getTableRowPaddingLeft()
	{
		if (mTableRowPaddingLeftDip == 0)
		{
			mTableRowPaddingLeftDip = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TABLE_ROW_PADDING_LEFT_DIP, getResources().getDisplayMetrics()));
		}

		return mTableRowPaddingLeftDip;
	}
}