package uk.ac.horizon.protestapp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Start the main activity when the device starts.
 **/
public class StartMyActivityAtBootReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Intent aIntent = new Intent(context, MainActivity.class);
			aIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(aIntent);
		}
	}
}