Robust-Download-Application-Android
===================================

Sample Download Application on Android as a part of my course assignment and projects

ROBUST DOWNLOAD APP 
1.1	USER INTERFACE ELEMENTS ON THE SCREEN

•	My App has a simple self-explanatory UI. 
•	Enter URL: User must enter the url via the input provide. By default the “DEFAULT” url provided to us would be filled in the input space.
•	Start Download Button: After entering the url, the user can click on the “Start Download” button to begin the download. Proceeding which the various UI updates and network status updates and download status updates would be seen.
•	Status View: Below this the network status and connection info is displayed.
•	Download Progress View: This is the download progress bar. It would update and show the progress in ‘PERCENTAGE’. 
o	It also has the ProgressBar to graphically show the download progress.
Note: Various Text color changes have been used to make the quick updates easily noticeable.
1.2	GENERAL ARCHITECTURE

1.2.1	Components Structure

The App has the following architecture:-
1.	Main Activity 
a.	Fragment_Main
i.	AsyncTask
ii.	Broadcast Receiver

•	Main Activity is  used for primarily holding the fragment , so that the rotation of the phone’s screen does not boder.
•	Frament_Main is the main fragment and the only fragment used in the App. This has all the classes and methods declared and defined.
•	AsyncTask  is used for following
o	To keep the track of the download progress, by calculating the percentage of the file downloaded.
o	To update Progress Download Bar in the UI View.
o	To log.
o	To Calculate the throughputs every 10 sec, using timer
o	To calculate the latency
•	Broadcast Receiver is used  as for follows :
o	for receiving the actions of the following, 
	Connectivity changes in the Network
	Download Complete Action of the Download Manager System Service.
o	Updating the UI with the network status and file download completion status.
o	To log. 
o	Calculates the final Total Throughput.

1.2.2	System Services Used

•	Download Manager
•	Connectivity Manager
•	Wifi Service Manager

1.3	SOME METHODS

1.3.1	ConnectionCheckAndDisplay()
Is used to check the WIFI and Mobile network connection and update the UI accordingly.
1.3.2	GetDownloadStatusInToast(View)
It queries the Download Manager’s Download Instance using shared preference, to get the status of the Download Instance. It shows the status as TOAST popups in the UI.
1.3.3	isDownloadAlreadyExists(Cursor)
Is used to check if the Download Instance already exisits.
1.3.4	onActivityCreated(Bundle)
Is used in the Fragment_Main for initializing all the UI elements of UI in Java. Also the Download Manager is initialized here. Also the instance of the Download Manager is saved as a SharedParameter.
1.3.5	onClickStartDownloadButton(View)
This is the main function which starts all the download using the Download Manager. 
1.3.6	startProgressAsyncTask()
This Method is used to start the Async Task from the fragment. 

•	The onReceive() method of the Broadcast Receiver is overridden to implement the logic of responding to the intents of Connection Change and Download Complete.
•	The ProgressAsyncTask class is the AsynTask class.


1.4	FINDINGS

•	Download Manager is a very robust service which takes care of the download completely also checks the connection and retries. Its provides various method, not much intents.
There is no Intent for Unsuccessful download.
Since the Download Instance as to be always Queried using a Cursor, the accuracy of the values like bytes downloaded and time could be not very robust.

•	AsynTasks don’t loose their identity if used via fragments, as even if the UI is not there AsyncTAsks perform and when the UI with fragment is back , it references to the same original fragment.

•	LogCat wonth save until we “Cntrl+A” in eclipse logcat view and then save it as text.


2	MEASUREMENTS AND GRAPHS AND ANALYSIS

2.1	SCENARIOS

2.1.1	Only WIFI Connected 
2.1.2	WIFI Disabled, Only 4G Connected
2.1.3	WIFI Disabled then WIFI Enabled
2.1.4	WIFI Disabled then WIFI Enabled then WIFI disabled then 4G Enabled
2.1.5	4G Enabled then WIFI Enabled – Some data download on 4G , Some data on WIFI.

Note: Log files are attached. Log files have similar scenario names.

	Scenario 1	Scenario 2	Scenario 3	Scenario 4	Scenario 5
					
Throughput(bytes/sec)	265948.99	506084.75	1035527.27	40863.29	31220.83
					
Latency(ms)	712	184	202	12295	168
					

