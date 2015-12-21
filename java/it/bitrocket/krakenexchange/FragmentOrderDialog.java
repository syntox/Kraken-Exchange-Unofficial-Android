package it.bitrocket.krakenexchange;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.HashMap;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class FragmentOrderDialog extends android.support.v4.app.DialogFragment
{
	public static final String ACTION_BUY           = "Buy";
	public static final String ACTION_SELL          = "Sell";
	public static final String ORDERTYPE_LIMIT      = "limit";
	public static final String ORDERTYPE_MARKET     = "market";

	public static final String DEFAULT_ASSET        = "XXBTZEUR";
	public static final String XXBTZUSD_ASSET        = "XXBTZUSD";

	public static final String XBTEUR = "XBTEUR";
	public static final String XBTUSD = "XBTUSD";

	private AlertDialog mOrderDialog;
	private KrakenAPI.UpdateUI mCallback;

	private TextView mSubtitle;

	private String mSubtitleString, mAction, mVolume, mAsset, mOrdertype, mPrice;
	private String mRequestAction, mRequestVolume, mRequestAsset, mRequestOrdertype, mRequestPrice;

	private double xbtzeurPrice, xbtzusdPrice;

	public static FragmentOrderDialog newInstance(KrakenAPI.UpdateUI callback)
	{
		FragmentOrderDialog odf = new FragmentOrderDialog();
		odf.setCallback(callback);

		return odf;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater         = getActivity().getLayoutInflater();
		AlertDialog.Builder builder     = new AlertDialog.Builder(getActivity());

		final View frag_view            = inflater.inflate(R.layout.order_dialog_main, null);

		mSubtitle                       = (TextView)    frag_view.findViewById(R.id.dialog_order_subtitle);
		final Switch action             = (Switch)      frag_view.findViewById(R.id.dialog_order_switch_type);
		final EditText volume           = (EditText)    frag_view.findViewById(R.id.dialog_order_edittext_volume);
		final TextView price_label      = (TextView)    frag_view.findViewById(R.id.dialog_order_tv2);
		final EditText price            = (EditText)    frag_view.findViewById(R.id.dialog_order_edittext_price);
		final Switch order_ordertype    = (Switch)      frag_view.findViewById(R.id.dialog_order_switch_ordertype);
		final Spinner asset_dropdown    = (Spinner)     frag_view.findViewById(R.id.dialog_order_assets_dropdown);

		// Default ordertype is "limit"
		order_ordertype.setChecked(true);

		String tmpPrices = ApplicationController.getInstace()
				.storage()
				.getString(FragmentMarkets.STORAGE_TAG_LASTPRICES);

		String[] tokens = tmpPrices.split(";");

		xbtzeurPrice = Double.valueOf(tokens[0]);
		xbtzusdPrice = Double.valueOf(tokens[1]);

		ArrayAdapter<CharSequence> adapter =
				ArrayAdapter.createFromResource(getActivity(), R.array.assets_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		asset_dropdown.setAdapter(adapter);

		asset_dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				//Log.d("item-selected", String.valueOf(parent.getItemAtPosition(position)));
				switch (String.valueOf(parent.getItemAtPosition(position)))
				{
					case XBTUSD:
						{
							mAsset = XBTUSD;
							price.setText(String.valueOf(xbtzusdPrice));

						} break;
					case XBTEUR:
					default:
						{
							mAsset = XBTEUR;
							price.setText(String.valueOf(xbtzeurPrice));
						}
				}

				updateSubtitle();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
				mAsset = XBTEUR;

				updateSubtitle();
			}
		});

		// On every user action the subtitle will be dynamically updated
		mSubtitleString = "%1$s %2$s %3$s @ %4$s %5$s";
		mAction         = "Buy";
		mVolume         = "";
		mAsset          = XBTEUR;
		mOrdertype      = "limit";
		mPrice          = "";

		/**
		 * Setup the Dialog Builder
		 */
		builder.setTitle(R.string.dialog_order_title)
			.setView(frag_view)
			.setPositiveButton(android.R.string.ok, null)
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int id){}
			});

		/**
		 * Setup a listener on action switch
		 */
		action.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (!action.isChecked())
				{
					mAction = "Buy";
				}
				else
				{
					mAction = "Sell";
				}

				updateSubtitle();
			}
		});

		/**
		 * Setup a listener on Volume text input
		 */
		volume.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){}

			@Override
			public void afterTextChanged(Editable s)
			{
				mVolume = volume.getText().toString();
				updateSubtitle();
			}
		});

		/**
		 * Setup a listener on Price text input
		 */
		price.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				mPrice = price.getText().toString();
				updateSubtitle();
			}
		});

		switch(mAsset)
		{
			case XBTUSD:
				price.setText(String.valueOf(xbtzusdPrice));
				break;
			case XBTEUR:
			default:
				price.setText(String.valueOf(xbtzeurPrice));
		}

		/**
		 * Setup a listener on Ordertype switch
		 */
		order_ordertype.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (! order_ordertype.isChecked())
				{
					price_label.setEnabled(false);
					price.setEnabled(false);

					mPrice = "";
					mOrdertype = "market";
				}
				else
				{
					price_label.setEnabled(true);
					price.setEnabled(true);

					mPrice = price.getText().toString();
					mOrdertype = "limit";
				}

				updateSubtitle();
			}
		});

		// Create the Dialog
		mOrderDialog = builder.create();

		// Calling "show()" method is necessary in order to be able to attach custom
		// "OnClickListener()" to positive and negative action Dialog's buttons
		mOrderDialog.show();

		// Attach the OnClickListener on Positive Button
		Button positive = mOrderDialog.getButton(AlertDialog.BUTTON_POSITIVE);
		positive.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Validate the user input
				if (validateDialogInput(action, volume, order_ordertype, price, asset_dropdown))
				{
					// If inputs are valid spawn a Confirmation Dialog
					final FragmentOrderDialogConfirmation odcf =
							FragmentOrderDialogConfirmation.newInstance(
									mSubtitle.getText().toString(),
									new DialogInterface.OnClickListener() // Positive Callback
									{
										@Override
										public void onClick(DialogInterface dialog, int which)
										{
											Toast.makeText(getActivity(), R.string.wait_please, Toast.LENGTH_LONG).show();


											ApplicationController.getInstace()
													.kraken()
													.openOrder(mRequestAsset, mRequestAction, mRequestOrdertype,
													           mRequestVolume, mRequestPrice, mCallback);

											mOrderDialog.dismiss();
										}
									},
									new DialogInterface.OnClickListener() // Negative Callback
									{
										@Override
										public void onClick(DialogInterface dialog, int which)
										{
										}
									}
							);

					odcf.show(getFragmentManager(), "orderDialogConfirmation");
				}

			}
		});

		// Return the Dialog
		return mOrderDialog;
	}

	public void setCallback(KrakenAPI.UpdateUI callback)
	{
		mCallback = callback;
	}

	private void updateSubtitle()
	{
		mSubtitle.setText(String.format(mSubtitleString, mAction, mVolume, mAsset, mOrdertype, mPrice));
		mSubtitle.setTextColor(getResources().getColor(R.color.kraken_blue));
	}

	private boolean validateDialogInput(Switch action, EditText volume, Switch order_ordertype, EditText price, Spinner asset)
	{
		if (action.isChecked())  // Sell
		{
			mRequestAction = ACTION_SELL.toLowerCase();
		}
		else // Buy
		{
			mRequestAction = ACTION_BUY.toLowerCase();
		}

		switch (String.valueOf(asset.getSelectedItem()))
		{
			case XBTUSD:
			{
				mRequestAsset = XXBTZUSD_ASSET;
			} break;
			case XBTEUR:
			{
				mRequestAsset = DEFAULT_ASSET;
			}
			default:
		}

		if (!mRequestAsset.equals(DEFAULT_ASSET) && !mRequestAsset.equals(XXBTZUSD_ASSET))
		{
			mSubtitle.setText(R.string.dialog_error_asset);
			mSubtitle.setTextColor(getResources().getColor(R.color.order_canceled));

			return false;
		}

		if (volume.getText().length() <= 0) // error
		{
			mSubtitle.setText(R.string.dialog_error_volume);
			mSubtitle.setTextColor(getResources().getColor(R.color.order_canceled));

			return false;
		}
		else
		{
			mRequestVolume = volume.getText().toString();

			if (order_ordertype.isChecked()) // limit
			{
				mRequestOrdertype = ORDERTYPE_LIMIT.toLowerCase();

				if (price.getText().length() <= 0) // error
				{
					mSubtitle.setText(R.string.dialog_error_price);
					mSubtitle.setTextColor(getResources().getColor(R.color.order_canceled));

					return false;
				}
				else
				{
					mRequestPrice = price.getText().toString();
				}
			}
			else // market
			{
				mRequestOrdertype = ORDERTYPE_MARKET.toLowerCase();
			}
		}

		return true;
	}

}