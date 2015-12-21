package it.bitrocket.krakenexchange;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.view.menu.ActionMenuItemView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class MainActivity extends ActionBarActivity
		implements NavigationDrawerFragment.NavigationDrawerCallbacks
{
	/**
	 * Application constant value
	 */
	public static final int CURRENCYPAIR_XXBTZEUR = 1;
	public static final int CURRENCYPAIR_XXBTZUSD = 2;
	public static final int CURRENCY_ZEUR = 1;
	public static final int CURRENCY_ZUSD = 2;

	private final String FRAGMENT_MARKETS_TAG = "market_frag";
	private final String FRAGMENT_ACCOUNT_TAG = "account_frag";
	private final String FRAGMENT_TRADING_TAG = "trading_frag";
	private final String FRAGMENT_ORDER_TAG   = "order_frag";

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	/**
	 * Used to store data
	 */
	private Storage mStorage;

	/**
	 * Current fragment object and fragment's tag
	 */
	private Fragment mCurrentFragment;
	private String mCurrentFragmentTag;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTitle      = getTitle();
		mStorage    = ApplicationController.getInstace().storage();

		mNavigationDrawerFragment = (NavigationDrawerFragment)
				getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

		mNavigationDrawerFragment.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout),
				new NavigationDrawerFragment.DrawerStateAction() {

					@Override
					public void open()
					{
						if (mCurrentFragment.getClass() == FragmentTrading.class)
						{
							((FragmentTrading) mCurrentFragment).hideFab();
						}
					}

					@Override
					public void close()
					{
						if (mCurrentFragment.getClass() == FragmentTrading.class)
						{
							((FragmentTrading) mCurrentFragment).showFab();
						}
					}
				}
		);

		/**
		 * Initialize the selected currency for FragmentAccount
		 */
		if (mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCY_TAG) == -1)
		{
			mStorage.putInt(Storage.STORAGE_SELECTEDCURRENCY_TAG, MainActivity.CURRENCY_ZEUR);
		}

		/**
		 * Initialize the selected currency pair for FragmentMarkets
		 */
		if (mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG) == -1)
		{
			mStorage.putInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG, MainActivity.CURRENCYPAIR_XXBTZEUR);
		}

		new EulaDialog(this).show();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if(mCurrentFragment != null && mCurrentFragment.getClass() == FragmentTrading.class)
		{
			((FragmentTrading) mCurrentFragment).showFab();
		}

	}

	@Override
	public void onNavigationDrawerItemSelected(int position)
	{
		FragmentManager fragmentManager = getSupportFragmentManager();

		Fragment newFragment = null;
		String newFragmentTag = "";

		switch (position)
		{
			case 0: // Markets

				newFragmentTag  = FRAGMENT_MARKETS_TAG;
				newFragment     = fragmentManager.findFragmentByTag(newFragmentTag);

				if (newFragment == null)
				{
					newFragment = FragmentMarkets.newInstance(position + 1);
				}

				break;

			case 1: // Account

				newFragmentTag  = FRAGMENT_ACCOUNT_TAG;
				newFragment     = fragmentManager.findFragmentByTag(newFragmentTag);

				if (newFragment == null)
				{
					newFragment = FragmentAccount.newInstance(position + 1);
				}

				break;

			case 2: // Trading

				newFragmentTag  = FRAGMENT_TRADING_TAG;
				newFragment     = fragmentManager.findFragmentByTag(newFragmentTag);

				if (newFragment == null)
				{
					newFragment = FragmentTrading.newInstance(position + 1);
				}

				break;

			case 3: // Send Feedback

				feedback();
				break;

			case 4: // About Us

				aboutUs();
				break;

			default:
		}

		if (newFragment != null)
		{
			Log.d("ItemSelected", "begin transaction");

			FragmentTransaction ft = fragmentManager.beginTransaction();

			if (mCurrentFragment != null)
			{
				Log.d("ItemSelected", "hiding current fragment: " + mCurrentFragmentTag);

				ft.hide(mCurrentFragment);
			}

			if(fragmentManager.findFragmentByTag(newFragmentTag) != null)
			{
				Log.d("ItemSelected", "showing already added fragment: " + newFragmentTag);

				ft.show(newFragment);
			}
			else
			{
				Log.d("ItemSelected", "adding new fragment: " + newFragmentTag);

				ft.add(R.id.container, newFragment, newFragmentTag);
			}

			ft.commit();

			mCurrentFragment    = newFragment;
			mCurrentFragmentTag = newFragmentTag;
		}
	}

	public void onSectionAttached(int number)
	{
		switch (number)
		{
			case 1:
				mTitle = getString(R.string.title_section1);
				break;
			case 2:
				mTitle = getString(R.string.title_section2);
				break;
			case 3:
				mTitle = getString(R.string.title_section3);
				break;
		}
	}

	public void restoreActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (!mNavigationDrawerFragment.isDrawerOpen())
		{
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();

			return true;
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		MenuItem currencyItem = menu.findItem(R.id.action_select_currency);
		MenuItem currencyPairItem = menu.findItem(R.id.action_select_pair);

		/**
		 * Update the currencyItem & currencyPairItem menu
		 */
		if (!mNavigationDrawerFragment.isDrawerOpen() && mCurrentFragment != null)
		{
			if (mCurrentFragment.getClass() == FragmentMarkets.class)
			{
				currencyPairItem.setVisible(true);

				if (mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG) == MainActivity.CURRENCYPAIR_XXBTZEUR)
				{
					currencyPairItem.setTitle(getString(R.string.currency_pair_btceur));
				}
				else if (mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG) == MainActivity.CURRENCYPAIR_XXBTZUSD)
				{
					currencyPairItem.setTitle(getString(R.string.currency_pair_btcusd));
				}
			}
			else
			{
				currencyPairItem.setVisible(false);
			}

			if (mCurrentFragment.getClass() == FragmentAccount.class)
			{
				currencyItem.setVisible(true);

				if (mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCY_TAG) == MainActivity.CURRENCY_ZEUR)
				{
					currencyItem.setTitle(getString(R.string.currency_eur));
				}
				else if (mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCY_TAG) == MainActivity.CURRENCY_ZUSD)
				{
					currencyItem.setTitle(getString(R.string.currency_usd));
				}
			}
			else
			{
				currencyItem.setVisible(false);
			}
		}
		else
		{
			menu.clear();
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		ActionMenuItemView currencyPairMenu = (ActionMenuItemView) findViewById(R.id.action_select_pair);
		ActionMenuItemView currencyMenu = (ActionMenuItemView) findViewById(R.id.action_select_currency);

		switch(id)
		{
			case R.id.action_pair_api:
				{
					fireApiActivity();
					return true;
				}
			case R.id.currency_btceur:
				{
					mStorage.putInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG, MainActivity.CURRENCYPAIR_XXBTZEUR);
					currencyPairMenu.setTitle(getString(R.string.currency_pair_btceur));
					updateFragmentMarketSelectedCurrencyPair();
					triggerFragmentMarketsUpdate();

				}
				break;
			case R.id.currency_btcusd:
				{
					mStorage.putInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG, MainActivity.CURRENCYPAIR_XXBTZUSD);
					currencyPairMenu.setTitle(getString(R.string.currency_pair_btcusd));
					updateFragmentMarketSelectedCurrencyPair();
					triggerFragmentMarketsUpdate();
				}
				break;
			case R.id.currency_eur:
				{
					mStorage.putInt(Storage.STORAGE_SELECTEDCURRENCY_TAG, MainActivity.CURRENCY_ZEUR);
					currencyMenu.setTitle(getString(R.string.currency_eur));
					updateFragmentAccountSelectedCurrency();
					triggerFragmentAccountUpdate();

				}
				break;
			case R.id.currency_usd:
				{
					mStorage.putInt(Storage.STORAGE_SELECTEDCURRENCY_TAG, MainActivity.CURRENCY_ZUSD);
					currencyMenu.setTitle(getString(R.string.currency_usd));
					updateFragmentAccountSelectedCurrency();
					triggerFragmentAccountUpdate();
				}
				break;
			default:
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Update the selectedCurrencyPair field into FragmentMarkets fragment instance
	 */
	private void updateFragmentMarketSelectedCurrencyPair()
	{
		FragmentMarkets frag = (FragmentMarkets) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_MARKETS_TAG);

		if (frag != null)
		{
			frag.selectedCurrencyPair = mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCYPAIR_TAG);
		}
	}

	/**
	 * Update the selectedCurrency field into FragmentAccount fragment instance
	 */
	private void updateFragmentAccountSelectedCurrency()
	{
		FragmentAccount frag = (FragmentAccount) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_ACCOUNT_TAG);

		if (frag != null)
		{
			frag.selectedCurrency = mStorage.getInt(Storage.STORAGE_SELECTEDCURRENCY_TAG);
		}
	}

	/**
	 * Trigger an update via mSwipeLayoutRefresh on FragmentMarkets fragment instance
	 */
	private void triggerFragmentMarketsUpdate()
	{
		FragmentMarkets frag = (FragmentMarkets) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_MARKETS_TAG);

		if (frag != null) frag.onTriggeredRefresh();
	}

	/**
	 * Trigger an update via mSwipeLayoutRefresh on FragmentAccount fragment instance
	 */
	private void triggerFragmentAccountUpdate()
	{
		FragmentAccount frag = (FragmentAccount) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_ACCOUNT_TAG);

		if (frag != null) frag.onTriggeredRefresh();
	}


	private void fireApiActivity()
	{
		Intent intent = new Intent(getApplicationContext(), ApiSetupActivity.class);
		startActivity(intent);
	}

	private void feedback()
	{
		String to = getResources().getString(R.string.feedback_email_to);
		String subject = getResources().getString(R.string.feedback_email_subject);

		String appInfo = "APPLICATION_ID = " +BuildConfig.APPLICATION_ID
				+ "\nBUILD_TYPE = "	+ BuildConfig.BUILD_TYPE
				+ "\nVERSION_CODE = " + BuildConfig.VERSION_CODE
				+ "\nVERSION_NAME = " + BuildConfig.VERSION_NAME
				+ "\n\nWrite here your feedback:\n";

		Intent mEmail = new Intent(Intent.ACTION_SEND);
		mEmail.putExtra(Intent.EXTRA_EMAIL, new String[]{
				to
		});
		mEmail.putExtra(Intent.EXTRA_SUBJECT, subject);
		mEmail.putExtra(Intent.EXTRA_TEXT, appInfo);
		mEmail.setType("message/rfc822");

		startActivity(Intent.createChooser(mEmail, "Choose an email client"));
	}

	private void aboutUs()
	{
		//Intent openUrl = new Intent(Intent.ACTION_VIEW, Uri.parse(ApplicationController.BITROCKETITURL));
		//startActivity(openUrl);

		/*AlertDialog.Builder b = new AlertDialog.Builder(this);
		AlertDialog d = b
				.setTitle("bitRocket IT")
				.setMessage("bitcoin:12SL6nwWqs7JZG4BhZETNm29xnfPoZdJfF").create();

		d.show();*/

		String uri_head = "bitcoin:";
		String address = "12SL6nwWqs7JZG4BhZETNm29xnfPoZdJfF";

		/*try
		{
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(uri_head+address));
			startActivity(i);
		}
		catch (ActivityNotFoundException anfe)
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			{
				ClipboardManager clipboard =
						(ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText(address, address);
				clipboard.setPrimaryClip(clip);
			}
			else
			{
				@SuppressWarnings("deprecation")
				android.text.ClipboardManager clipboard =
						(android.text.ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(address);
			}

			Toast.makeText(this, R.string.error_no_wallet_app, Toast.LENGTH_LONG).show();
		}*/

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			ClipboardManager clipboard =
					(ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText(address, address);
			clipboard.setPrimaryClip(clip);

			Toast.makeText(this, R.string.bitcoin_address_clipboard, Toast.LENGTH_LONG).show();
		}
		else
		{
			@SuppressWarnings("deprecation")
			android.text.ClipboardManager clipboard =
					(android.text.ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(address);

			Toast.makeText(this, R.string.bitcoin_address_clipboard, Toast.LENGTH_LONG).show();
		}

		AboutUsDialog aud = AboutUsDialog.newInstance();
		aud.show(getSupportFragmentManager(), "aboutUsDialog");

		return;
	}

	public void donate(View view)
	{
		Log.d("donate", "here");
	}

}