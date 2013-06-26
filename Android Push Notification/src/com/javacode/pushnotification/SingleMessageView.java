package com.javacode.pushnotification;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.javacode.pushnotification.MainActivity.DeleteNotifications;
import com.javacode.pushnotification.MainActivity.LoadInbox;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class SingleMessageView extends Activity {

	// Progress Dialog
	private ProgressDialog pDialog;

	// Creating JSON Parser object
	JSONParser jsonParser = new JSONParser();

	// tracks JSONArray
	JSONArray inbox = null;

	// ALL JSON node names
	
	private static final String TAG_FROM = "from";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_MESSAGE = "message";

	// Message id
	String message_id = null;

	String from, subject, message;

	// Inbox JSON url
	private static final String MESSAGE_URL = "http://10.0.2.2/getSingleNotification.php";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_message_view);

		Intent i = getIntent();
		message_id = i.getStringExtra("message_id");

		// calling background thread
		new LoadSingleMessage().execute();
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.single_message_view_menu, menu);
		return true;
	}
	
	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		

		case R.id.menu_delete:
			Intent resultIntent = new Intent();
			resultIntent.putExtra("message_id", message_id);
			setResult(Activity.RESULT_OK, resultIntent);
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	class LoadSingleMessage extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(SingleMessageView.this);
			pDialog.setMessage("Loading Message ...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		/**
		 * getting song json and parsing
		 * */
		protected String doInBackground(String... args) {
			// Building Parameters
			// Building Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("message_id", message_id));

			// getting JSON string from URL
			String json = jsonParser.makeHttpRequest(MESSAGE_URL, "GET", params).toString();

			// Check your log cat for JSON reponse
			Log.d("Single Notification JSON: ", json);

			try {
				JSONObject jObj = new JSONObject(json);
				if (jObj != null) {
					from= jObj.getString(TAG_FROM);
					subject = jObj.getString(TAG_SUBJECT);
					message = jObj.getString(TAG_MESSAGE);
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog
		 * **/
		protected void onPostExecute(String file_url) {
			// dismiss the dialog after getting song information
			pDialog.dismiss();

			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				public void run() {

					TextView txt_from = (TextView) findViewById(R.id.from);
					TextView txt_subject = (TextView) findViewById(R.id.subject);
					TextView txt_message = (TextView) findViewById(R.id.message);

					// displaying song data in view
					txt_from.setText(Html.fromHtml("<b>From:</b> " + from));
					txt_subject.setText(Html.fromHtml("<b>Subject:</b> "+ subject));
					txt_message.setText(Html.fromHtml("<b>Message:</b> "+ message));

					// Change Activity Title with Song title
					setTitle(subject);
				}
			});

		}

	}

}
