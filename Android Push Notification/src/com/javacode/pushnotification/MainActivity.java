package com.javacode.pushnotification;

import java.util.*;
import java.lang.String;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.javacode.pushnotification.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.javacode.pushnotification.CommonUtilities.SENDER_ID;

import com.javacode.pushnotification.AlertDialogManager;
import com.javacode.pushnotification.ConnectionDetector;
import com.javacode.pushnotification.MainActivity;
import com.javacode.pushnotification.R;
import com.javacode.pushnotification.ServerUtilities;
import com.javacode.pushnotification.CommonUtilities;
import com.javacode.pushnotification.WakeLocker;
import com.google.android.gcm.GCMRegistrar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

public class MainActivity extends Activity {

	// label to display gcm messages
	TextView lblMessage;

	// LoadMore button
	Button btnLoadMore;

	// Asyntask
	AsyncTask<Void, Void, Void> mRegisterTask;

	// Alert dialog manager
	AlertDialogManager alert = new AlertDialogManager();

	// Connection detector
	ConnectionDetector cd;

	// Progress Dialog
	private ProgressDialog pDialog;

	// Creating JSON Parser object
	JSONParser jsonParser = new JSONParser();

	ArrayList<HashMap<String, String>> inboxList;

	ArrayAdapter<String> adapter_array;

	// products JSONArray
	JSONArray inbox = null;

	ListView lv;

	MyMessageAdapter dataAdapter;

	// Inbox JSON url
	private static final String INBOX_URL = "http://10.0.2.2/getAllNotifications.php";

	// Delete Notifications url
	private static final String DELETE_URL = "http://10.0.2.2/deleteNotifications.php";

	// ALL JSON node names
	private static final String TAG_MESSAGES = "messages";
	private static final String TAG_ID = "id";
	private static final String TAG_FROM = "from";
	// private static final String TAG_EMAIL = "email";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_DATE = "date";
	private static final String TAG_SUCCESS = "success";
	private static final String TAG_ERROR = "error";

	public static String name;
	public static String email;

	public static String min_id = "1";
	public static String max_id = "1";

	public static String reg_id;

	ListAdapter adapter;

	ArrayList<Message> messageList = new ArrayList<Message>();

	ArrayList<Integer> checkedNotifications = new ArrayList<Integer>();

	int checkedCount = 0;

	int deleteSituation = 0;

	String update_state = "Load Inbox";

	String delete_messsage_id = null;

	boolean isLoading = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		cd = new ConnectionDetector(getApplicationContext());

		// Check if Internet present
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(MainActivity.this,
					"Internet Connection Error",
					"Please connect to working Internet connection", false);
			// stop executing code by return
			return;
		}

		// Hashmap for ListView
		inboxList = new ArrayList<HashMap<String, String>>();

		lv = (ListView) findViewById(R.id.list);
		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		// LoadMore button
		btnLoadMore = new Button(this);
		btnLoadMore.setText("Load More");

		/**
		 * Listening to Load More button click event
		 * */
		btnLoadMore.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				// Starting a new async task
				update_state = "Load Old";
				new LoadInbox().execute();
			}
		});

		// Adding Load More button to lisview at bottom
		dataAdapter = new MyMessageAdapter(MainActivity.this,
				R.layout.inbox_list_item, messageList);
		lv.setAdapter(dataAdapter);
		lv.addFooterView(btnLoadMore);

		Intent i = getIntent();

		name = i.getStringExtra("name");
		email = i.getStringExtra("email");

		// Make sure the device has the proper dependencies.
		GCMRegistrar.checkDevice(this);

		// Make sure the manifest was properly set - comment out this line
		// while developing the app, then uncomment it when it's ready.
		GCMRegistrar.checkManifest(this);

		registerReceiver(mHandleMessageReceiver, new IntentFilter(
				DISPLAY_MESSAGE_ACTION));

		reg_id = GCMRegistrar.getRegistrationId(this);

		// Check if regid already presents
		if (reg_id.equals("")) {
			// Registration is not present, register now with GCM
			GCMRegistrar.register(this, SENDER_ID);
		} else {
			// Device is already registered on GCM
			if (GCMRegistrar.isRegisteredOnServer(this)) {
				// Skips registration.
				if (!isLoading) {
					new LoadInbox().execute();
				}
			} else {
				// Try to register again, but not in the UI thread.
				// It's also necessary to cancel the thread onDestroy(),
				// hence the use of AsyncTask instead of a raw thread.

				final Context context = this;

				mRegisterTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						// Register on our server
						// On server creates a new user

						if (!reg_id.equals("")) {
							GCMRegistrar.setRegisteredOnServer(context, true);
						} else {
							ServerUtilities.register(context, name, email,
									reg_id);
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						mRegisterTask = null;
						// Loading INBOX in Background Thread

					}

				};
				mRegisterTask.execute(null, null, null);

			}
		}

	}

	// Initiating Menu XML file (menu.xml)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.menu, menu);
		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_refresh:
			reg_id = GCMRegistrar.getRegistrationId(this);
			if (!isLoading) {
				update_state = "Load New";
				new LoadInbox().execute();
			}
			return true;

		case R.id.menu_delete:
			if (checkedCount != 0) {
				deleteSituation = 1;
				new DeleteNotifications().execute();
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == RESULT_OK && data != null) {

			delete_messsage_id = data.getStringExtra("message_id");
			deleteSituation = 2;

			new DeleteNotifications().execute();
		}
	}

	/**
	 * Receiving push messages
	 * */
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
			// Waking up mobile if it is sleeping
			if (!isLoading && !reg_id.equals("")) {
				update_state = "Load New";
				new LoadInbox().execute();

			}

			// Releasing wake lock
			WakeLocker.release();
		}
	};

	/**
	 * Background Async Task to Load all INBOX messages by making HTTP Request
	 * */
	class LoadInbox extends AsyncTask<String, String, String> {

		Boolean isLoaded = true;
		String err_message = "";

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Loading Inbox ...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
			isLoading = true;
		}

		/**
		 * getting Inbox JSON
		 * */
		@SuppressLint("SimpleDateFormat")
		protected String doInBackground(String... args) {
			// Building Parameters
			
			List<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("reg_id", reg_id));
			if (update_state.equals("Load Old")) {
				params.add(new BasicNameValuePair("min_id", min_id));
				
			} else if (update_state.equals("Load New")) {
				params.add(new BasicNameValuePair("max_id", max_id));
				params.add(new BasicNameValuePair("order", "ASC"));
			}

			// getting JSON string from URL
			JSONObject json = jsonParser.makeHttpRequest(INBOX_URL, "GET",
					params);

			// Check your log cat for JSON reponse
			Log.d("Inbox JSON: ", json.toString());

			try {

				int success = json.getInt(TAG_SUCCESS);

				if (success == 1) {
					inbox = json.getJSONArray(TAG_MESSAGES);
					// looping through All messages
					for (int i = 0; i < inbox.length(); i++) {
						JSONObject c = inbox.getJSONObject(i);

						// Storing each json item in variable
						String id = c.getString(TAG_ID);
						String from = c.getString(TAG_FROM);
						String subject = c.getString(TAG_SUBJECT);
						String date = CommonUtilities.formateDate(c
								.getString(TAG_DATE));

						// adding HashList to ArrayList
						if (update_state.equals("Load New")) {
							messageList.add(0, new Message(id, from, subject,
									date, false));
						} else {
							messageList.add(new Message(id, from, subject,
									date, false));
						}

						if (i == 0 && !update_state.equals("Load Old"))
							max_id = id;
						if (i == (inbox.length() - 1)
								&& !update_state.equals("Load New"))
							min_id = id;
					}
				} else {
					isLoaded = false;
					err_message = json.getString(TAG_ERROR);
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
			// dismiss the dialog after getting all products
			pDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				public void run() {

					if (isLoaded) {
						/**
						 * Updating parsed JSON data into ListView
						 * */

						dataAdapter = new MyMessageAdapter(MainActivity.this,
								R.layout.inbox_list_item, messageList);

						// updating listview
						lv.setAdapter(dataAdapter);

						// Setting new scroll position
						if (update_state.equals("Load Old")) {
							int currentPosition = lv.getFirstVisiblePosition();
							lv.setSelectionFromTop(currentPosition + 1, 0);
						} else if (update_state.equals("Load New")) {

							lv.setSelectionFromTop(0, 0);
						}
					} else {
						alert.showAlertDialog(MainActivity.this,
								"Loading failed", err_message, false);
					}

				}
			});
			isLoading = false;

		}

	}

	/**
	 * Background Async Task to Load all INBOX messages by making HTTP Request
	 * */
	class DeleteNotifications extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Deleting Notifications ...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@SuppressLint("SimpleDateFormat")
		protected String doInBackground(String... args) {

			ArrayList<String> messageIdList = new ArrayList<String>();
			ArrayList<Message> tmpmessageList = new ArrayList<Message>();

			tmpmessageList = messageList;
			String messageIds = "";
			Boolean flag = false;
			// Delete by selecting Multiple
			if (deleteSituation == 1) {
				for (int i = 0; i < messageList.size(); i++) {
					Message message = messageList.get(i);
					if (message.isSelected()) {
						if (!flag) {
							flag = true;
						}
						messageIdList.add(message.getId());
						messageList.remove(message);
						i--;
					}

				}
			} else if (deleteSituation == 2) { // Delete by opening individual
				for (int i = 0; i < messageList.size(); i++) {
					Message message = messageList.get(i);
					if (message.getId().equals(delete_messsage_id)) {
						messageIdList.add(message.getId());
						messageList.remove(message);
						flag = true;
						break;
					}

				}
			}

			if (flag) {

				messageIds = TextUtils.join(",", messageIdList);

				List<NameValuePair> params = new ArrayList<NameValuePair>();

				params.add(new BasicNameValuePair("message_ids", messageIds));

				// getting JSON string from URL
				JSONObject json = jsonParser.makeHttpRequest(DELETE_URL, "GET",
						params);
				Log.d("Delete JSON: ", json.toString());

				try {
					int success = json.getInt(TAG_SUCCESS);

					if (success == 0) {
						alert.showAlertDialog(MainActivity.this,
								"Deleting failed", json.getString(TAG_ERROR),
								false);
						messageList = tmpmessageList;
					} else if (success == 1) {
						checkedCount = 0;
					}
				} catch (JSONException e) {
					e.printStackTrace();
					messageList = tmpmessageList;
				}

			}
			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog
		 * **/
		protected void onPostExecute(String file_url) {
			// dismiss the dialog after getting all products
			pDialog.dismiss();
			lv.setAdapter(null);
			dataAdapter = new MyMessageAdapter(MainActivity.this,
					R.layout.inbox_list_item, messageList);
			lv.setAdapter(dataAdapter);
		}

	}

	private class MyMessageAdapter extends ArrayAdapter<Message> {

		private ArrayList<Message> messageList;

		public MyMessageAdapter(Context context, int textViewResourceId,
				ArrayList<Message> messageList) {
			super(context, textViewResourceId, messageList);
			this.messageList = new ArrayList<Message>();
			this.messageList.addAll(messageList);
		}

		private class ViewHolder {
			TextView id;
			TextView from;
			TextView subject;
			TextView date;
			CheckBox checkbox;
		}

		@Override
		public long getItemId(int position) {
			return super.getItemId(position);
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			ViewHolder holder = null;
			Log.v("ConvertView", String.valueOf(position));

			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.inbox_list_item, null);

				holder = new ViewHolder();
				holder.id = (TextView) convertView
						.findViewById(R.id.message_id);
				holder.from = (TextView) convertView.findViewById(R.id.from);
				holder.subject = (TextView) convertView
						.findViewById(R.id.subject);
				holder.date = (TextView) convertView.findViewById(R.id.date);
				holder.checkbox = (CheckBox) convertView
						.findViewById(R.id.checkbox);
				convertView.setTag(holder);

				holder.checkbox.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						CheckBox cb = (CheckBox) v;
						Message message = (Message) cb.getTag();
						// Toast.makeText(
						// getApplicationContext(),
						// "Clicked on Checkbox: " + cb.getText() + " is "
						// + cb.isChecked(), Toast.LENGTH_LONG)
						// .show();
						message.setSelected(cb.isChecked());
						if (cb.isChecked()) {
							checkedNotifications.add(position);
							checkedCount++;
						} else {
							checkedCount--;
						}

					}
				});

				convertView.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						Intent i = new Intent(getApplicationContext(),
								SingleMessageView.class);

						String message_id = ((TextView) view
								.findViewById(R.id.message_id)).getText()
								.toString();
						String subject = ((TextView) view
								.findViewById(R.id.subject)).getText()
								.toString();

						Toast.makeText(getApplicationContext(),
								"Subject: " + subject, Toast.LENGTH_SHORT)
								.show();

						i.putExtra("message_id", message_id);

						startActivityForResult(i, 1);
					}
				});

			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			Message message = messageList.get(position);
			holder.id.setText(message.getId());
			holder.from.setText(message.getFrom());
			holder.subject.setText(message.getSubject());
			holder.date.setText(message.getDate());
			holder.checkbox.setChecked(message.isSelected());
			holder.checkbox.setTag(message);

			return convertView;
		}

	}
}
