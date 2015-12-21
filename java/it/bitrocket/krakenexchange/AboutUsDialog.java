package it.bitrocket.krakenexchange;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class AboutUsDialog extends android.support.v4.app.DialogFragment
{
	public static AboutUsDialog newInstance()
	{
		AboutUsDialog aud = new AboutUsDialog();
		return aud;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater = getActivity().getLayoutInflater();
		final View frag_view = inflater.inflate(R.layout.about_us, null);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(frag_view);

		final String uri_head = "bitcoin:";
		final String address = "12SL6nwWqs7JZG4BhZETNm29xnfPoZdJfF";

		Button btc = (Button) frag_view.findViewById(R.id.buttonBTC);
		btc.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				try
				{
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(uri_head+address));
					startActivity(i);
				}
				catch (ActivityNotFoundException e)
				{
					// nop
					Log.e("about-us", "bitcoin wallet app not found");
				}
			}
		});

		return builder.create();
	}
}
