package it.bitrocket.krakenexchange;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class EulaDialog
{
	private Activity mActivity;

	public EulaDialog(Activity context)
	{
		mActivity = context;
	}

	public void show()
	{
		if (ApplicationController.getInstace().isEulaAccepted() == false)
		{
			LayoutInflater inflater = mActivity.getLayoutInflater();
			View dialogLayout = inflater.inflate(R.layout.eula, null);

			TextView eula = (TextView) dialogLayout.findViewById(R.id.eula_textview);
			eula.setText(getEulaText());
			eula.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

			String title = mActivity.getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME;

			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
					.setTitle(title)
					.setView(dialogLayout)
					.setCancelable(false)
					.setOnKeyListener(new DialogInterface.OnKeyListener() {
						@Override
						public boolean onKey(DialogInterface dialoginterface, int keyCode, KeyEvent event)
						{
							if ((keyCode == KeyEvent.KEYCODE_HOME))
							{
								return false;
							}
							else
							{
								return true;
							}
						}
					})
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						public void onCancel(DialogInterface dialog)
						{
							mActivity.finish();
						}
					})
					.setPositiveButton(R.string.accept, new Dialog.OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							ApplicationController.getInstace().setEulaAccepted();
							dialogInterface.dismiss();
						}
					})
					.setNegativeButton(R.string.decline, new Dialog.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							mActivity.finish();
						}

					});

			builder.create().show();
		}
	}

	public String getEulaText()
	{
		InputStream is = mActivity.getResources().openRawResource(R.raw.eula);

		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		StringBuilder total = new StringBuilder();
		String line;

		try
		{
			while ((line = r.readLine()) != null)
			{
				total.append(line);
				total.append('\n');
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return total.toString();
	}
}
