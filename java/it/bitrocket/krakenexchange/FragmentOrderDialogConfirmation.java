package it.bitrocket.krakenexchange;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Copyright (C) 2015 by Matteo Benetti
 */
public class FragmentOrderDialogConfirmation extends android.support.v4.app.DialogFragment
{
	private Dialog mOrderConfirmationDialog;
	private DialogInterface.OnClickListener mPosCallback, mNegCallback;
	private String mSubtitle;

	public static FragmentOrderDialogConfirmation newInstance(String message,
	                                                          DialogInterface.OnClickListener positiveCallback,
	                                                          DialogInterface.OnClickListener negativeCallback)
	{
		FragmentOrderDialogConfirmation odcf = new FragmentOrderDialogConfirmation();

		odcf.setMessage(message);
		odcf.setPositiveCallback(positiveCallback);
		odcf.setNegativeCallback(negativeCallback);

		return odcf;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater         = getActivity().getLayoutInflater();
		AlertDialog.Builder builder     = new AlertDialog.Builder(getActivity());

		final View frag_view            = inflater.inflate(R.layout.order_dialog_confirmation,null);
		final TextView subtitle         = (TextView) frag_view.findViewById(R.id.dialog_order_confirmation_message);

		subtitle.setText(mSubtitle);
		subtitle.setTextColor(getResources().getColor(R.color.kraken_blue));

		// Setup the Dialog Builder
		builder.setTitle(R.string.dialog_order_confirmation_title)
			.setView(frag_view)
			.setPositiveButton(android.R.string.ok, mPosCallback)
			.setNegativeButton(android.R.string.cancel, mNegCallback);

		mOrderConfirmationDialog = builder.create();

		return mOrderConfirmationDialog;
	}

	public void setMessage(String message)
	{
		mSubtitle = message;
	}

	public void setPositiveCallback(DialogInterface.OnClickListener positiveCallback)
	{
		mPosCallback = positiveCallback;
	}

	public void setNegativeCallback(DialogInterface.OnClickListener negativeCallback)
	{
		mNegCallback = negativeCallback;
	}
}
