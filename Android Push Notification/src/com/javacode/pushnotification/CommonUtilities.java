package com.javacode.pushnotification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.Intent;

public final class CommonUtilities {

	// give your server registration url here
	static final String SERVER_URL = "http://albumartindia.com/pushnotification/register.php";

	// Google project id
	static final String SENDER_ID = "343780976448";

	/**
	 * Tag used on log messages.
	 */
	static final String TAG = "AndroidHive GCM";

	static final String DISPLAY_MESSAGE_ACTION = "com.androidhive.pushnotifications.DISPLAY_MESSAGE";

	static final String EXTRA_MESSAGE = "message";

	/**
	 * Notifies UI to display a message.
	 * <p>
	 * This method is defined in the common helper because it's used both by the
	 * UI and the background service.
	 * 
	 * @param context
	 *            application's context.
	 * @param message
	 *            message to be displayed.
	 */
	static void displayMessage(Context context, String message) {
		Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
		intent.putExtra(EXTRA_MESSAGE, message);
		context.sendBroadcast(intent);
	}

	static String formateDate(String inputDate) {
		
		String outputDate = "";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date deventDate = null;
		String seventDate;
		Date dtoday = new Date();
		String stoday;
		SimpleDateFormat dateOnly = new SimpleDateFormat("dd-MM-yyyy");
		SimpleDateFormat timeOnly = new SimpleDateFormat("HH:mm");
		stoday = dateOnly.format(dtoday);
		
		try {
			deventDate = format.parse(inputDate);
			seventDate = dateOnly.format(deventDate);
			
			// System.out.println(seventDate + " = " + stoday);
			if (stoday.equals(seventDate)) {
				outputDate = timeOnly.format(deventDate).toString();
			}
			else
				outputDate = seventDate.toString();

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return outputDate;
	}
}