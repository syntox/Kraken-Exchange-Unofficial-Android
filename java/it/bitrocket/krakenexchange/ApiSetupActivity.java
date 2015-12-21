package it.bitrocket.krakenexchange;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class ApiSetupActivity extends ActionBarActivity
{
    private static final int ZXING_ACTION_SCAN_CODE = 49374;

    private static final CharSequence PROTOCOL_HEADER           = "kraken://apikey?";
    private static final CharSequence PROTOCOL_HEADER_KEY       = "key=";
    private static final CharSequence PROTOCOL_HEADER_SECRET    = "secret=";

    private static final CharSequence API_PAIRING_SUCCESS   = "Yay! Pairing done!";
    private static final CharSequence API_PAIRING_ERROR     = "Ops! Something goes wrong!";
    private static final CharSequence GENERAL_ERROR         = "Ops! Something goes wrong!";

    private ApiTokenStore mApiTokenStore;

    private EditText mETKey, mETSec;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_api);

	    mApiTokenStore = new ApiTokenStore();

        mETKey = (EditText) findViewById(R.id.api_setup_content_key);
        mETSec = (EditText) findViewById(R.id.api_setup_content_secret);

        TextView tv = (TextView) findViewById(R.id.api_setup_activity_title);

        if (mApiTokenStore.isApiAlreadySet())
        {
            tv.setText(R.string.api_settings_message_setted);
	        displayTokens();
        }
	    else
        {
	        Button reset = (Button) findViewById(R.id.api_setup_acion_reset);
	        reset.setEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //getMenuInflater().inflate(R.menu.api_setup_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    /**
     * This method manage the returning payload from Zxing Activity
     *
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        // Error flag
        int error = 1;

        // Check if request code is the expected one
        if (requestCode == ZXING_ACTION_SCAN_CODE)
        {
            // If Zxing QR Scanner got errors display a Toast Message
            if (resultCode == 0)
            {
	            Toast.makeText(getApplicationContext(),
	                           R.string.api_activity_qr_scan_error, Toast.LENGTH_LONG).show();
                return;
            }

            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

            if (scanResult != null)
            {
                String payload = scanResult.getContents();

                if (!payload.isEmpty() &&
		                payload.startsWith((String) ApiSetupActivity.PROTOCOL_HEADER))
                {

                    String tokens[] = payload.substring(ApiSetupActivity.PROTOCOL_HEADER.length()).split("&");

                    if (tokens.length == 2)
                    {
                        String key = tokens[0].substring(ApiSetupActivity.PROTOCOL_HEADER_KEY.length());
                        String secret = tokens[1].substring(ApiSetupActivity.PROTOCOL_HEADER_SECRET.length());

                        if (!key.isEmpty() && !secret.isEmpty())
                        {
                            error = 0;

                            mApiTokenStore.api_key = key;
                            mApiTokenStore.api_secret = secret;

                            Toast.makeText(getApplicationContext(),
                                           R.string.api_activity_qr_scan_ok, Toast.LENGTH_SHORT).show();

                            displayTokens();
	                        saveApiManual();
                        }
                    }

                }
            }
        }

        if (error == 1)
        {
	        Toast.makeText(getApplicationContext(),
	                       R.string.api_pairing_error, Toast.LENGTH_LONG).show();
            return;
        }

    }

    /**
     * Scan Kraken API QR Code via Zxing Barcode Scanner
     *
     *  @param view
     */
    public void scanApi(View view)
    {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    /**
     * Save api tokens
     *
     * @param view
     */
    public void saveApi(View view)
    {
        if (mApiTokenStore.storeApiTokens(mApiTokenStore.api_key, mApiTokenStore.api_secret, false))
        {
            Toast.makeText(getApplicationContext(),
                           R.string.api_pairing_success, Toast.LENGTH_LONG).show();


            Intent home = new Intent(this, MainActivity.class);
            startActivity(home);

            return;
        }
        else
        {
            Toast.makeText(getApplicationContext(), R.string.api_pairing_error, Toast.LENGTH_SHORT).show();
        }
    }

    public void saveApiManual()
    {
        if (mApiTokenStore.storeApiTokens(mETKey.getText().toString(), mETSec.getText().toString(), false))
        {
            Toast.makeText(getApplicationContext(),
                           R.string.api_pairing_success, Toast.LENGTH_LONG).show();


            Intent home = new Intent(this, MainActivity.class);
            startActivity(home);

            return;
        }
        else
        {
            Toast.makeText(getApplicationContext(), R.string.api_pairing_error, Toast.LENGTH_SHORT).show();
        }
    }

	/**
	 * Reset Api tokens
	 *
	 * @param view
	 */
	public void resetApi(View view)
	{
		mApiTokenStore.invalidate();

		ApplicationController.getInstace().storage().clearKrakenData();

		Toast.makeText(this, R.string.api_unlink_done, Toast.LENGTH_LONG).show();
	}

    /**
     * Display Api key and secret into the TextView components
     */
    public void displayTokens()
    {
        mETKey.setText(mApiTokenStore.api_key);
        mETSec.setText(mApiTokenStore.api_secret);
    }

	/**
	 * Display masked Api Key and Secret in UI's TextEdit views
	 *
	 */
	public void displayTokensWithMask()
    {
		mETKey.setText(mask(mApiTokenStore.api_key));
		mETSec.setText(mask(mApiTokenStore.api_secret));
	}

	/**
	 * Mask the given String
	 *
	 * @param payload
	 * @return Masked payload
	 */
	public String mask(String payload)
	{
		int visible_chars_start = 4;
		int visible_chars_end = 4;

		String result = "****";

		if (!payload.isEmpty())
		{
			result = payload.substring(0, visible_chars_start)
					+ result
					+ payload.substring(payload.length() - visible_chars_end, payload.length());
		}

		return result;
	}
}