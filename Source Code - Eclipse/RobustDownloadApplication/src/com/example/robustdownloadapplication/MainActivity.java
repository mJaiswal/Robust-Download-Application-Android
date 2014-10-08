package com.example.robustdownloadapplication;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Build;
import android.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream.GetField;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Timestamp;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.widget.TextView;

@SuppressLint("NewApi")
public class MainActivity extends ActionBarActivity {

	String ActivityTAG = "MyActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.Fragment_Container, new Download_UI_Fragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class Download_UI_Fragment extends Fragment  {

		static //Log TAG for Fragment
		String FragmentTAG = "In MyFragment";
		static String LogTAG = "My Download App";
		
	
		//start download button
		  Button startDownloadButton;
		  Button getStatusButton;
				  
		//Progress Bar and AsyncTask Class
		  private static ProgressBar downloadProgressBar;
		  private static int ProgressStatusValue = 0;
		  
		//DispalayInfo
		  private static TextView ProgressPercentageText;
		  private static TextView DisplayInfoTextView;
		  
		  
		// Download Manager
		private static DownloadManager downloadManager;
		private long downloadReference;
		
		
		//String for Identifying the instance of this fragment in this activity
		private static final String FragmentID = "MyFragmentID";
		
		//SHared Preferences to save the the download and check on resume
		private static SharedPreferences FragmentPrefs;
		
		//Measurement Variables and logfile variables
		private static Date ActualDownloadStartTime = null;
		private static Date ActualDownloadCompleteTime = null;
		private static long TimeTakenToDownload = -1;
		
		private static long Throughput = -1; // totalbyes downloadwed / total time taken.
		private static long Latency= -1;//STart time at which the first byte arrived
		
		private static long TOTAL_FILE_SIZE;
		
		//IntentFilters for our broadcast receiver
		IntentFilter intentFilter = new IntentFilter();
		{
			intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
			intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			//intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
			//intentFilter.addAction("android.net.wifi.STATE_CHANGE");
		}
		
		/* A weak reference is used so that the fragment and the async task are loosely coupled. 
		 * If you don’t use a weak reference, the async task will not be garbage collected because 
		 * the fragment maintains a reference to it.
		 */
		private WeakReference<ProgressAsyncTask> ProgressAsyncTaskWeakRef;
		
				
		
		/*
		 * Manage rotation as the fragment can be set to retainInstance 
		 * and survive on configuration changes (rotation).
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
		       
			super.onCreate(savedInstanceState);

		        setRetainInstance(true);
		        
		}

		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View FragView  = inflater.inflate(R.layout.fragment_main, container,
					false);	
						
			return FragView;
		}
		
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			
			super.onActivityCreated(savedInstanceState);
			
			//Configure the DOwnloadManager object
			downloadManager = (DownloadManager)getView().getContext().getSystemService(DOWNLOAD_SERVICE);
			Log.i(FragmentTAG, "Configured the DOwnloadManager object");
			
			
			//Get the Button object filled and start the onClickListner
			startDownloadButton = (Button) getView().findViewById(R.id.StartButton);
			startDownloadButton.setOnClickListener(new OnClickListener() {
				
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						onClickStartDownloadButton(v);
						
					}
				});
			
			getStatusButton = (Button) getView().findViewById(R.id.GetStatusButton);
			getStatusButton.setOnClickListener(new OnClickListener() {
				
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						GetDownloadStatusInToast(v);
						
					}
				});
					
			//Initialize the ProgressBar
			downloadProgressBar = (ProgressBar) getView().findViewById(R.id.DownloadProgressBar);
			
			//Initialize the DisplayInfo and ProgressText
			ProgressPercentageText = (TextView) getView().findViewById(R.id.DisplayInfo);
			DisplayInfoTextView =  (TextView) getView().findViewById(R.id.InfoTextView);
			
			
			//Register your Broadcast receiver
			getView().getContext().registerReceiver(broadcastReceiver, intentFilter);
			
			
			//Saving the Fragments Preference to check later
			FragmentPrefs = PreferenceManager.getDefaultSharedPreferences(getView().getContext());	
			Log.i(FragmentTAG, "Saved the Fragmnets Preference to check later");
			
			
			
		}
		
		/*
		@Override
		public void onResume(){
		
			super.onResume();
			
			
		}*/
		
		@Override
		public void onDestroy(){
			super.onDestroy();
			
			//UnRegister Receiver
			//getView().getContext().unregisterReceiver(broadcastReceiver);
			
		}
		
		//Function to writes logs to file
		public static void logToFile(Context context, String logMessageTag, String logMessage)
	    {
		    try
		       {
		          // Gets the log file from the root of the primary storage. If it does 
		          // not exist, the file is created.
			       File mylogFile = new File(Environment.getExternalStorageDirectory(), "DownloadApp.txt" );
			       if (!mylogFile.exists())
			          mylogFile.createNewFile();
			      
			       // Write the message to the log with a timestamp
			       BufferedWriter writer = new BufferedWriter(new FileWriter(mylogFile, true));
			       writer.write(String.format("at [%2s]:%3s\r\n", logMessageTag, logMessage));
			       writer.close();
                  // Refresh the data so it can be seen when the device is plugged in a 
                  // computer. May have to unplug and replug to see the latest 
                  // changes
			       MediaScannerConnection.scanFile(context, 
			                                      new String[] { mylogFile.toString() }, 
			                                      null, 
			                                      null);
	
		       }
		    catch (IOException e)
		       {
		    		e.printStackTrace();
		       }
	    }
				
					
		
		//Async Task which would use the Download
		private  class ProgressAsyncTask extends AsyncTask<Void, Integer , Void> 
		{

			Boolean isPrinted = false;
			
			/* A weak reference is used so that the fragment and the async task are loosely coupled. 
			 * If you don’t use a weak reference, the async task will not be garbage collected because 
			 * the fragment maintains a reference to it.
			 */
			private WeakReference<Download_UI_Fragment> Download_UI_FragmentWeakRef;
			private ProgressAsyncTask (Download_UI_Fragment fragment ) {
	            this.Download_UI_FragmentWeakRef = new WeakReference<Download_UI_Fragment>( fragment);
	        }
			
			
			
			//Countdown timer for every 10 seconds , every 10 seconds it checks.
			CountDownTimer ThroughputTimer = new CountDownTimer(720000, 10000) 
			{
				
				int BytesSoFar;
				long Throughput;
				@Override
				public void onTick(long millisUntilFinished) {
					// TODO Auto-generated method stub
					
					//Query the Download Manager filtering with the unique ID
					
					DownloadManager.Query dmQuery = new DownloadManager.Query();
					dmQuery.setFilterById(FragmentPrefs.getLong(FragmentID, 0));
					
					//Get the Query result 
					Cursor dmQueryResults = downloadManager.query(dmQuery);
					
		            if (dmQueryResults.moveToFirst()) 
		            {
		            	BytesSoFar = (int) dmQueryResults
		                        .getLong(dmQueryResults
		                                .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
		                
		              Throughput = BytesSoFar/10; //every 10 second
		              
		              Log.i(LogTAG,"at "+ Calendar.getInstance().getTime().toString()+ " Throughput[10sec interval] is "+ Throughput+" bytes/sec");
		            }
					
				}
				
				@Override
				public void onFinish() {
					// TODO Auto-generated method stub
				}
					
		            
			};
		
			
			@Override
	        protected void onPreExecute() {
	            super.onPreExecute();
	            
	            //Set the ProgressBar
	            ProgressStatusValue = 0;

	          //make visible the TextView and the Progress Bar
	            
	            ProgressPercentageText.setVisibility(View.VISIBLE);
	            downloadProgressBar.setMax(100);
	            downloadProgressBar.setVisibility(View.VISIBLE);	
	            
	        }
	 
			
			
	        @Override
	        protected Void doInBackground(Void... params) {

	            //TODO: your background code
	        	
	        	ThroughputTimer.start();
	        	
	                       
	        	while(ProgressStatusValue < 100)
	        	{
	        		try
	        		{
	        			ProgressStatusValue = getProgressPercentage();
	        			publishProgress(ProgressStatusValue);
	        			
		        		
	        		}
	        		catch(Exception e){
	                    Log.d("Exception", e.toString());
	        		}
	        	}   
	        	
	        	ThroughputTimer.cancel();
	            return null;
	        }
	        
	        @Override
	        protected void onProgressUpdate(Integer... Value) {
	        	
	        	ProgressPercentageText.setText("Download Progress ...."+ Value[0].toString() + "%");
	        	
        		downloadProgressBar.incrementProgressBy(Value[0].intValue());
	        }
	                

	        @Override
	        protected void onPostExecute(Void response) {
	            super.onPostExecute(response);
	            if (this.Download_UI_FragmentWeakRef.get() != null) {
	                             //TODO: treat the result
	            	//ProgressPercentageText.setText("");
	            	//make GONE
	            	//ProgressPercentageText.setVisibility(View.INVISIBLE);
	            	downloadProgressBar.setProgress(0);
		        	downloadProgressBar.setVisibility(View.INVISIBLE);
	            }
	        }
	        
	    //Timer for Calculating the Throughput at regular Intervals 
	        
	        
	        
	      //Get The Progress Percentage Value
		 private int getProgressPercentage() 
		 {
			 	
		        int DOWNLOADED_BYTES_SO_FAR_INT = 0, TOTAL_BYTES_INT = 0, PERCENTAGE = 0;
		        

		        try {
		            
		        	//Query the Download Manager filtering with the unique ID
					DownloadManager.Query dmQuery = new DownloadManager.Query();
					dmQuery.setFilterById(FragmentPrefs.getLong(FragmentID, 0));
					
					//Get the Query result 
					Cursor dmQueryResults = downloadManager.query(dmQuery);
					
		            if (dmQueryResults.moveToFirst()) {
		                DOWNLOADED_BYTES_SO_FAR_INT = (int) dmQueryResults
		                        .getLong(dmQueryResults
		                                .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
		                
		                TOTAL_BYTES_INT = (int) dmQueryResults
		                        .getLong(dmQueryResults
		                                .getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
		                
		                //Set and show download start Time only once
		                if(TOTAL_BYTES_INT != -1  && ActualDownloadStartTime == null)
		                {
		                	//Download Started
		                	ActualDownloadStartTime = Calendar.getInstance().getTime();
		                	
		                	Log.i(LogTAG, "at "+ Calendar.getInstance().getTime().toString() + " Actual Download of File Started.");
		                	
		                	TOTAL_FILE_SIZE = TOTAL_BYTES_INT;
		                	Log.i(FragmentTAG, "Total Size of File : "+ TOTAL_FILE_SIZE);
		                	Log.i(LogTAG, "Total Size of File : "+ TOTAL_FILE_SIZE);
		            	
		                		                	
		                }
		                if(DOWNLOADED_BYTES_SO_FAR_INT >0 && ActualDownloadStartTime != null && Latency == -1)
		                {
		                	//Calculating Latency
		                	Latency = (Calendar.getInstance().getTime().getTime() - ActualDownloadStartTime.getTime());
		                	Log.i(LogTAG, "Latency is "+ Latency +" ms" );
		                	//reset Latency to -1
		                	
		                	
		                }
		                
		                
		                
		            }

		           	//Calculate the percentage            
		            PERCENTAGE = (DOWNLOADED_BYTES_SO_FAR_INT * 100 / TOTAL_BYTES_INT);
		            
		           // Log.d(FragmentTAG, "PERCENTAGE % " + PERCENTAGE );

		        } catch (Exception e) {
		            e.printStackTrace();
		        }

		        return PERCENTAGE;
		    }
		 
		 
		
	        
	    }//end of AsynTask class
		
		
		 private void ConnectionCheckAndDisplay()
		 {
			 
			 //Its From Network Change
			   ConnectivityManager conMngr = (ConnectivityManager)getView().getContext().getSystemService(CONNECTIVITY_SERVICE);
			   
			   android.net.NetworkInfo wifi = conMngr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			   android.net.NetworkInfo mobile = conMngr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			   
			   if (!wifi.isConnected())// Not connected
			   {
				      				   
				   //Update in TextView
				   DisplayInfoTextView.setText("WIFI is NOT connected!");
				   DisplayInfoTextView.setTextColor(Color.RED);
				   DisplayInfoTextView.refreshDrawableState();
				   
				   if(mobile.isConnected())
				   {
					 //Update in TextView
  				   DisplayInfoTextView.setText("Mobile Network is connected.");
  				   DisplayInfoTextView.setTextColor(Color.GREEN);
  				   DisplayInfoTextView.refreshDrawableState();
				   }
			
			   }
			   else if(wifi.isConnected()) //Wifi is Connected
			   {
				 
				   
				   WifiManager wifiManager = (WifiManager) getView().getContext().getSystemService (Context.WIFI_SERVICE);
				   WifiInfo wifiInfo = wifiManager.getConnectionInfo (); 		
				   
				   Log.i(LogTAG, "at "+ Calendar.getInstance().getTime().toString() +" WIFI SSID is " + wifiInfo.getSSID());
				   
				   //Update in TextView
				   DisplayInfoTextView.setText("WIFI is connected to "+ wifiInfo.getSSID() );
				   DisplayInfoTextView.setTextColor(Color.GREEN);
				   DisplayInfoTextView.refreshDrawableState();
				   
			   }
			   	    			   	    			   
			   else if(wifi.isAvailable() && wifi.isConnectedOrConnecting())//Connecting
			   {
				   //Update in TextView
				   DisplayInfoTextView.setText("WIFI Network Available. Trying to Re-connect.... " );
				   DisplayInfoTextView.setTextColor(Color.BLUE);
				   DisplayInfoTextView.refreshDrawableState();
				   
			   }
			   else if (!wifi.isAvailable())
			   {
				   
				   //Update in TextView
				   DisplayInfoTextView.setText("WIFI Network NOT Available !" );
				   DisplayInfoTextView.setTextColor(Color.RED);
				   DisplayInfoTextView.refreshDrawableState();
			   }
			 
		 }
		
		
		
		//Function to start the progress async task
		private void startProgressAsyncTask() {
		    ProgressAsyncTask asyncTask = new ProgressAsyncTask(this);
		    this.ProgressAsyncTaskWeakRef = new WeakReference<ProgressAsyncTask >(asyncTask );
		    asyncTask.execute();
		}	
		
		//Function to check if the AsyncTask Status
		private boolean isProgressAsyncTaskPendingOrRunning() 
		{
		    return this.ProgressAsyncTaskWeakRef != null &&
		          this.ProgressAsyncTaskWeakRef.get() != null && 
		          !this.ProgressAsyncTaskWeakRef.get().getStatus().equals(Status.FINISHED);
		}
		
		
		 
		// Function when the "Start Download" Button is Clicked
		public void onClickStartDownloadButton(View view)
		{
			Log.i(FragmentTAG, "Start Download button Clicked");
			
			try {
			
				//Check NetworkConnectivity to get the wifi STatus n SSID
				ConnectionCheckAndDisplay();
				
				
				//Query the Download Manager
				DownloadManager.Query dmQuery = new DownloadManager.Query();
				dmQuery.setFilterById(FragmentPrefs.getLong(FragmentID, 0));
				
				Cursor dmQueryResults = downloadManager.query(dmQuery);
						
			
				//Check if the Same Download Already Exists
				Log.i(FragmentTAG, "Check Download Already Exists ?");
			
				if( false == isDownloadAlreadyExists(dmQueryResults) )
				{
				
					//Initialize the URL by the user given / default string
					String urlString = ( (EditText) getView().findViewById(R.id.UrlText)).getText().toString();
					Log.i(FragmentTAG, "URL is"+ urlString );
					
					
					
					//Configure the Download Manager Request Object
					DownloadManager.Request dwnloadRequest = new DownloadManager.Request(Uri.parse(urlString));
					//WIfI or Cellular
					dwnloadRequest.setAllowedNetworkTypes(dwnloadRequest.NETWORK_MOBILE| dwnloadRequest.NETWORK_WIFI); //Only Wifi
					dwnloadRequest.setTitle("My File Download");
					dwnloadRequest.setDescription("Android Data download using DownloadManager.");
					//Setting the Downloads directory as the standard directory.
					//dwnloadRequest.setDestinationInExternalFilesDir(getView().getContext(), Environment.DIRECTORY_DOWNLOADS,"MyFile" );
					Log.i(FragmentTAG, "Configured the DOwnload Manager Request object");
					
										
					//Enqueue a new download request and save the referenceId
					 downloadReference = downloadManager.enqueue(dwnloadRequest);
					 
					//Get the Time ScheduleDownloadTaskStartTime and Put it in the logcat
					String ScheduleDownloadTaskStartTime = Calendar.getInstance().getTime().toString();
					Log.i(LogTAG, "at "+ ScheduleDownloadTaskStartTime +" Download Task Scheduled by Download Manager."  );
					
					//logToFile(getView().getContext(), ScheduleDownloadTaskStartTime, "Download Request Scheduled by Download Manager");
						
					 Log.i(FragmentTAG, "Enqueued the New download Request");
					 
					
					 //Start the Progress Bar computing AsyncTask
					 startProgressAsyncTask();
					 Log.i(FragmentTAG, "ProgressBar Async Task Started");
					 
					 //Update the SHared Preference -> Key = FragmentID ( constant String given by us) , Value is the unique ID for the download
					 FragmentPrefs.edit().putLong(FragmentID, downloadReference).commit();
					 Log.i(FragmentTAG, "Updated the Shared Preference ");
					 
				}
				else
				{
					//If Download Exists
					Log.i(FragmentTAG, " Same Download already exists ");  
										
					//Popup the toast with the status text
					raiseToast("Download already exists. Click Get Status");
					
					
				}
			
		} 
		catch (Exception e) {
			   e.printStackTrace();}

		}
		
		
		public boolean isFragmentInstanceExists()
		{
			if(FragmentPrefs.contains(FragmentID))
			{
				Log.i(FragmentTAG, "Fragment already exists");
				return true;
			}
			else
			{	
				Log.i(FragmentTAG, "Fragment Does not exist");
				return false;
			}
		}
		
		public boolean isDownloadAlreadyExists(Cursor dmQueryResults)
		{
						
			if( true == dmQueryResults.moveToFirst()  )
			{
				//If the Cursor is not empty.
				Log.i(FragmentTAG, "Download already exists");
				
				return true;
			}
			else
			{	//Cursor is Empty
				Log.i(FragmentTAG, "Download Does not exist");
				return false;
			}
		}
		
		
		
		//Return String[] ; String[0] - StatusText , String[1] - ReasonText
		public void GetDownloadStatusInToast(View v)
		{
			
			String statusText = "";
			String reasonText = "";
			//Query the Download Manager
			DownloadManager.Query dmQuery = new DownloadManager.Query();
			dmQuery.setFilterById(FragmentPrefs.getLong(FragmentID, 0));
			
			Cursor dmQueryResults = downloadManager.query(dmQuery);
			//column for status
			  int columnIndex = dmQueryResults.getColumnIndex(DownloadManager.COLUMN_STATUS);
			  int status = dmQueryResults.getInt(columnIndex);
			  
			//column for reason code if the download failed or paused
			  int columnReason = dmQueryResults.getColumnIndex(DownloadManager.COLUMN_REASON);
			  int reason = dmQueryResults.getInt(columnReason);
			 
			  
			  
			 
			  switch(status){
			  
				  case DownloadManager.STATUS_FAILED:
				   statusText = "STATUS_FAILED";
				   
				   		switch(reason){
				   		
							   case DownloadManager.ERROR_CANNOT_RESUME:
							    reasonText = "ERROR_CANNOT_RESUME";
							    break;
							   case DownloadManager.ERROR_DEVICE_NOT_FOUND:
							    reasonText = "ERROR_DEVICE_NOT_FOUND";
							    break;
							   case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
							    reasonText = "ERROR_FILE_ALREADY_EXISTS";
							    break;
							   case DownloadManager.ERROR_FILE_ERROR:
							    reasonText = "ERROR_FILE_ERROR";
							    break;
							   case DownloadManager.ERROR_HTTP_DATA_ERROR:
							    reasonText = "ERROR_HTTP_DATA_ERROR";
							    break;
							   case DownloadManager.ERROR_INSUFFICIENT_SPACE:
							    reasonText = "ERROR_INSUFFICIENT_SPACE";
							    break;
							   case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
							    reasonText = "ERROR_TOO_MANY_REDIRECTS";
							    break;
							   case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
							    reasonText = "ERROR_UNHANDLED_HTTP_CODE";
							    break;
							   case DownloadManager.ERROR_UNKNOWN:
							    reasonText = "ERROR_UNKNOWN";
							    break;
							   }
				   break;
				   
				  case DownloadManager.STATUS_PAUSED:
				   statusText = "STATUS_PAUSED";
				   					   
						switch(reason){
							   case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
							    reasonText = "PAUSED_QUEUED_FOR_WIFI";
							    break;
							   case DownloadManager.PAUSED_UNKNOWN:
							    reasonText = "PAUSED_UNKNOWN";
							    break;
							   case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
							    reasonText = "PAUSED_WAITING_FOR_NETWORK";
							    break;
							   case DownloadManager.PAUSED_WAITING_TO_RETRY:
							    reasonText = "PAUSED_WAITING_TO_RETRY";
							    break;
							   }
						
						break;

							   
				  case DownloadManager.STATUS_PENDING:
				   statusText = "STATUS_PENDING";
				   break;
				   
				  case DownloadManager.STATUS_RUNNING:
				   statusText = "STATUS_RUNNING";
				   break;
				   
				  case DownloadManager.STATUS_SUCCESSFUL:
				   statusText = "STATUS_SUCCESSFUL";
				   reasonText = "Downloaded Sucessfully";
				   break;
			  	  
			  
			  }//switch
			  
			  
			  
			//Popup the toast with the status text
			raiseToast(statusText+"\n"+reasonText);
				
 
		}
		
		
		//USed to show toast popup
		public void raiseToast(String textString){
			
			//Show the status in a Toast popup  
			Toast toast = Toast.makeText( getView().getContext(),
											textString,
					//statusText + "\n" + reasonText, 
										    Toast.LENGTH_LONG);
			
			toast.setGravity(Gravity.TOP, 25, 400);
			toast.show();
		}
				  

		
		
		private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		    
			//LogFile Variables
			String WifiNotAvailableStartTime;//Wifi Down Start time
			String WifiAvailableStartTime;   // Wifi Up Start time
			
			
			
				
			
			@Override
		    public void onReceive(Context context, Intent intent) {
		      
		    	
	    	   
	    	   if( intent.getAction() == DownloadManager.ACTION_DOWNLOAD_COMPLETE )
	    	   {
	    		   Log.i(FragmentTAG, "Its our DownloadManger's Broadcast");  
	    		   
	    		 //check if the broadcast message is for our Enqueued download
					
	 			  	long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
	    		   //If the download is complete remove it from the DownloadManager After loging	   
	    		   if(downloadReference == referenceId)
	    		   {
	    			  
	    			   ActualDownloadCompleteTime = Calendar.getInstance().getTime();
	    			   Log.i(LogTAG, "at "+ ActualDownloadCompleteTime.toString() +" Download File Completed Successfully."  );
	    			   
	    			   
	    			   //Calculate the Total Time Taken
	    			   TimeTakenToDownload = ActualDownloadCompleteTime.getTime() - ActualDownloadStartTime.getTime();
	    			   
	    			   //Calculate Throughput
	    			 		   
	    			   Log.i(LogTAG, "Total Throughput(bytes/s) : " + TOTAL_FILE_SIZE/(TimeTakenToDownload*0.001) + "bytes/sec"  );
	    			   ActualDownloadStartTime = null;
	    			   ActualDownloadCompleteTime = null;
	    			   Latency = -1;
	    			   //Update in TextView
	    			   ProgressPercentageText.setText("Download File Completed Successfully.");
	    			   ProgressPercentageText.setTextColor(Color.GREEN);
	    			   ProgressPercentageText.refreshDrawableState();
	    			   
	    			   
	    			   
	    			   //Remove the reference of Download from Download Manager as its completed
	    			   downloadManager.remove(referenceId);
	    			   //Clear the shared preferences , so that another download could begin.
	    			   FragmentPrefs.edit().clear().commit();
	    			   
	    			   Log.i(FragmentTAG, "File is downloaded Sucessfully, remove from DM"); 
	    			   
	    		   }
	    	   }//if
	    	   
	    	   
	    	   else if( intent.getAction() ==  ConnectivityManager.CONNECTIVITY_ACTION )
    		   {
    			    //Its From Network Change
    			   ConnectivityManager conMngr = (ConnectivityManager)getView().getContext().getSystemService(CONNECTIVITY_SERVICE);
    			   
    			   android.net.NetworkInfo wifi = conMngr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    			   android.net.NetworkInfo mobile = conMngr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
    			   
    			   if (!wifi.isConnected())// Not connected
    			   {
    				      				   
    				   WifiNotAvailableStartTime = Calendar.getInstance().getTime().toString();
    				   Log.i(LogTAG, "at "+ WifiNotAvailableStartTime +" WIFI is NOT connected."  ); 
    				   
    				   //Update in TextView
    				   DisplayInfoTextView.setText("WIFI is NOT connected!");
    				   DisplayInfoTextView.setTextColor(Color.RED);
    				   DisplayInfoTextView.refreshDrawableState();
    				   
    				   if(mobile.isConnected())
    				   {
    					   Log.i(LogTAG, "at "+  Calendar.getInstance().getTime().toString() +" Mobile Network is Connected."  ); 
    					   //Update in TextView
	    				   DisplayInfoTextView.setText("Mobile Network is connected.");
	    				   DisplayInfoTextView.setTextColor(Color.CYAN);
	    				   DisplayInfoTextView.refreshDrawableState();
    				   }
    			
    			   }
    			   else if(wifi.isConnected()) //Wifi is Connected
    			   {
    				 
    				   WifiAvailableStartTime  = Calendar.getInstance().getTime().toString() ;
    				   Log.i(LogTAG, "at "+ WifiAvailableStartTime +" WIFI is connected."  );
    				   
    				   WifiManager wifiManager = (WifiManager) getView().getContext().getSystemService (Context.WIFI_SERVICE);
    				   WifiInfo wifiInfo = wifiManager.getConnectionInfo (); 		
    				   
    				   Log.i(LogTAG, "at "+ Calendar.getInstance().getTime().toString() +"WIFI SSID is " + wifiInfo.getSSID());
    				   
    				   //Update in TextView
    				   DisplayInfoTextView.setText("WIFI is connected to "+ wifiInfo.getSSID() );
    				   DisplayInfoTextView.setTextColor(Color.GREEN);
    				   DisplayInfoTextView.refreshDrawableState();
    				   
    			   }
    			   	    			   	    			   
    			   else if(wifi.isAvailable() && wifi.isConnectedOrConnecting())//Connecting
    			   {
    				   Log.i(LogTAG, "at "+ Calendar.getInstance().getTime().toString() +" WIFI Network Availabe. Trying to Re-connect to WIFI"  );
    				   
    				   //Update in TextView
    				   DisplayInfoTextView.setText("WIFI Network Available. Trying to Re-connect.... " );
    				   DisplayInfoTextView.setTextColor(Color.BLUE);
    				   DisplayInfoTextView.refreshDrawableState();
    				   
    			   }
    			   else if (!wifi.isAvailable())
    			   {
    				   
    				   Log.i(LogTAG, "at "+ Calendar.getInstance().getTime().toString() +" WIFI Network NOT Available!"  );
    				   //Update in TextView
    				   DisplayInfoTextView.setText("WIFI Network NOT Available !" );
    				   DisplayInfoTextView.setTextColor(Color.RED);
    				   DisplayInfoTextView.refreshDrawableState();
    			   }
    			   	    			   
	    		  
	    			 
	    			   
    		   }//if
	    		   
   
	    	   
	    	   
		    	
		    }//onReceive
		  };
 
		
	}//end of fragment class
	
	
}
