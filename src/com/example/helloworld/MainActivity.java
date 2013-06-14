package com.example.helloworld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
//from http://www.drdobbs.com/tools/hacking-for-fun-programming-a-wearable-a/240007471
// MAC Address:	e4:ce:8f:37:44:4e

public class MainActivity extends Activity {
	MainActivity master = this;
	int REQUEST_ENABLE_BT;
	BluetoothAdapter mBluetoothAdapter;
	BluetoothDevice laptop;
	Button blueButton;
	Button orangeButton;
	BluetoothSocket mmSocket = null;
	private boolean taskComplete = false;
	// private static final UUID MY_UUID =
	// UUID.fromString("00001105-0000-1000-8000-00805F9B34FB");
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Layout Views
	private TextView incomingMessage;

	// Device info
	private String deviceType = android.os.Build.DEVICE;
	private static final String DROIDX = "cdma_shadow";
	private static final String GOGGLES = "limo";
	private static final String NEXUS = "grouper";

	private String currentDevice;

	private String[] deviceAddresses = new String[7];
	private static final int LAPTOP_INDEX = 0;
	private static final int DROIDX_INDEX = 1;
	private static final int GOGGLES_INDEX = 2;
	private static final int NEXUS_INDEX = 6;
	private String connectionAddress;

	public int messageIndex = 0;
	public boolean isPhone = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		enableBlueTooth();
		registerDevices();
		if (currentDevice.equals("Goggles")) {
			setContentView(R.layout.goggles_main);
		} else {
			setContentView(R.layout.activity_main);
		}
		blueButton= (Button)findViewById(R.id.blue_button);
		blueButton.setOnClickListener(blueListener);
		orangeButton= (Button)findViewById(R.id.orange_button);
		orangeButton.setOnClickListener(orangeListener);
		
		if (D)
			Log.i("debugging", "bluetooth enabled");
		//registerDevices();

		incomingMessage = (TextView) findViewById(R.id.incoming_message);
	}

	public ConnectedThread connectedThread;

	public void startConnectionThread() {
		connectedThread = new ConnectedThread(mmSocket);
		Log.i("debugging", "connected thread running");
	}

	public void registerDevices() {
		deviceAddresses[LAPTOP_INDEX] = "E4:CE:8F:37:44:4F";
		deviceAddresses[DROIDX_INDEX] = "D0:37:61:40:1F:F2";
		deviceAddresses[GOGGLES_INDEX] = "64:9C:8E:6B:02:D6";
		deviceAddresses[NEXUS_INDEX] = "10:BF:48:E8:EF:3A";

		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1000);

		if (!mBluetoothAdapter.isDiscovering()) {
			if (D)
				Log.i("debugging", "didn't discovery again");
			startActivity(discoverableIntent);
		}
		if (deviceType.equals(DROIDX)) {
			currentDevice = "DroidX";
			connectionAddress = deviceAddresses[GOGGLES_INDEX];
			initiateSocketServer();
		}
		if (deviceType.equals(NEXUS)) {
			currentDevice = "Nexus";
			connectionAddress = deviceAddresses[GOGGLES_INDEX];
			if (D)
				Log.i("debugging", "connecting to address: "
						+ connectionAddress);
			initiateSocketServer();
		}
		if (deviceType.equals(GOGGLES)) {
			currentDevice = "Goggles";
			// connectionAddress = deviceAddresses[DROIDX_INDEX];
			connectionAddress = deviceAddresses[NEXUS_INDEX];
			if (D)
				Log.i("debugging", "connecting to address: "
						+ connectionAddress);
			initiateClient();
			// connectionAddress = deviceAddresses[LAPTOP_INDEX];
		}

	}

	@Override
	public void onStop() {
		super.onStop();
		if (mmSocket != null) {
			try {
				mmSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void initiateSocketServer() {
		if (D)
			Log.i("debugging", "initiating server socket");
		new AcceptThread().run();

	}

	public void initiateClient() {
		if (D)
			Log.i("debugging", "iniating clientt");
		queryDevices();
		if (mBluetoothAdapter.isDiscovering()) {
			if (D)
				Log.i("debugging", "canceled discovery in initiateClient");
			mBluetoothAdapter.cancelDiscovery();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	OutputStream mmOutputStream;
	InputStream mmInputStream;

	OnClickListener sendListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {

			if (taskComplete) {
				Log.i("debugging", "clicked");
				try {
					mmOutputStream = mmSocket.getOutputStream();
					messageIndex++;
					String msg = currentDevice + " message " + messageIndex;

					byte[] send = msg.getBytes();
					mConnectedThread.write(send);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	};
	
	OnClickListener orangeListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {

			if (taskComplete) {
				Log.i("debugging", "clicked");
				try {
					mmOutputStream = mmSocket.getOutputStream();
					messageIndex++;
					String msg = "orange";

					byte[] send = msg.getBytes();
					mConnectedThread.write(send);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	};


	OnClickListener blueListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {

			if (taskComplete) {
				Log.i("debugging", "clicked");
				try {
					mmOutputStream = mmSocket.getOutputStream();
					messageIndex++;
					String msg = "blue";

					byte[] send = msg.getBytes();
					mConnectedThread.write(send);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	};






	public void enableBlueTooth() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
		}
		if (!mBluetoothAdapter.isEnabled()) {
			// Device is not connected to BlueTooth
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	BluetoothDevice serverDevice;
	BroadcastReceiver mReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			Log.i("debugging", "in broadcast receiver");
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (D) {
					Log.i("debugging", "Device is: " + device.getName());
					Log.i("debugging",
							"Device address:  " + device.getAddress());
				}

				// Add the name and address to an array adapter to show in a
				// ListView
				if (device.getAddress().equals(connectionAddress)) {
					// connectToDevice(device);
					if (D)
						Log.i("debugging", "started the connection task");
					serverDevice = device;
					mBluetoothAdapter.cancelDiscovery();
					try {
						new ConnectThread(serverDevice).run();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}
		}
	};

	public void queryDevices() {
		Log.i("debugging", "querying devices");
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		// If there are paired devices
		boolean found = false;
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				Log.i("debugging", "in for");
				if (D) {
					Log.i("debugging", "Device is: " + device.getName());
					Log.i("debugging",
							"Device address:  " + device.getAddress());
				}

				// Add the name and address to an array adapter to show in a
				// ListView\
				if (device.getAddress().equals(connectionAddress)) {
					// connectToDevice(device);
					Log.i("debugging", "starting connection task");
					found = true;
					serverDevice = device;
					try {
						new ConnectThread(serverDevice).run();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
					// ConnectionTask cTask = new ConnectionTask(master);
					// cTask.execute(device, null, null);

				}
			}

		}
		if (!found) {
			Log.i("debugging", "starting discovery");
			mBluetoothAdapter.startDiscovery();
		}

		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter); // Don't forget to unregister
												// during onDestroy
		registered = true;

	}

	boolean registered = false;

	public void onDestroy() {
		super.onDestroy();

		 if (mReceiver!=null) unregisterReceiver(mReceiver);

		if (mmSocket != null) {
			try {
				mmSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			Log.i("debugging", "accept 1");
			mAcceptThread = this;
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client
				// code

				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
						"dreamy", MY_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mmServerSocket = tmp;
			if (D)
				Log.i("debugging", "instantiated Accept Thread");
		}

		public void run() {
			BluetoothSocket socket = null;
			if (D)
				Log.i("debugging", "in run of Accept Thread");
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					socket = mmServerSocket.accept();

					mmSocket = socket;

					// startConnectionThread();
				} catch (IOException e) {
					Log.i("debugging", "exception in AcceptThread.run()");
					e.printStackTrace();
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					Log.i("debugging", "in accept thread we have a socket!");
					// Do work to manage the connection (in a separate thread)
					connected(socket);

					try {
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
			}
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) throws IOException,
				InterruptedException {

			// device.fetchUuidsWithSdp();
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;
			if (D)
				Log.i("debugging",
						"Instantiating connect thread to " + device.getName());
			// Get a BluetoothSocket to connect with the given BluetoothDevice
			if (Build.VERSION.SDK_INT < 9) { // VK:
												// Build.Version_Codes.GINGERBREAD
												// is not accessible yet so
												// using raw int value
				// VK: 9 is the API Level integer value for Gingerbread
				if (D)
					Log.i("debugging", "first build");
				try {
					tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} else {
				// GOGGLES USE THIS
				if (D)
					Log.i("debugging", "second build");
				Method m = null;
				try {

					m = device.getClass().getMethod(
							"createInsecureRfcommSocketToServiceRecord",
							new Class[] { UUID.class });
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {

					tmp = (BluetoothSocket) m.invoke(device, (UUID) MY_UUID);
					tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			mConnectThread = this;
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection

			if (mBluetoothAdapter.isDiscovering()) {
				if (D)
					Log.i("debugging", "canceled discovery in run");
				mBluetoothAdapter.cancelDiscovery();
			}

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();

				// startConnectionThread();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				Log.i("debugging", "unable to connect in ConnectThread.run");
				connectException.printStackTrace();
				try {
					mmSocket.close();
				} catch (IOException closeException) {
				}
				return;
			}
			Log.i("debugging", "SUCCESSFULLY CONNECTED");

			connected(mmSocket);
			// Do work to manage the connection (in a separate thread)
			// manageConnectedSocket(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			/**
			 * try { mmSocket.close(); } catch (IOException e) { }
			 */
		}

	}

	public ConnectThread mConnectThread;

	private class ConnectedThread extends Thread {
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.i("debugging",
						"exception in instantiation of ConnectThread");
				e.printStackTrace();
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
			taskComplete = true;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					Log.i("debugging", "reading from inputstream");
					bytes = mmInStream.read(buffer);

					// Send the obtained bytes to the UI Activity
					Log.i("debugging", "obtaining message from handler");
					mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
							.sendToTarget();
				} catch (IOException e) {
					Log.i("debugging", "disconnected", e);
					// connectionLost();
					// Start the service over to restart listening mode
					// BluetoothChatService.this.start();
					break;
				}
			}
		}

		public void read() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()

			try {
				// Read from the InputStream
				bytes = mmInStream.read(buffer);
				// Send the obtained bytes to the UI activity
				mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
						.sendToTarget();
			} catch (IOException e) {

			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			Log.i("debugging", "in connectedthread.write");
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			/**
			 * try { mmSocket.close(); } catch (IOException e) { }
			 */
		}
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MESSAGE_WRITE:
				Log.i("debugging", "message write in handler");
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				break;
			case MESSAGE_READ:
				Log.i("debugging", "message read in handler");
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				if (readMessage.equals("blue")){
					incomingMessage.setText(readMessage);
					incomingMessage.setTextColor(getResources().getColor(R.color.sky_light));
				}
				if (readMessage.equals("orange")){
					incomingMessage.setText(readMessage);
					incomingMessage.setTextColor(getResources().getColor(R.color.tangerine_light));
				}
				break;

			}
		}
	};

	private ConnectedThread mConnectedThread = null;
	private AcceptThread mAcceptThread = null;

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 */
	public synchronized void connected(BluetoothSocket socket) {

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		if (D)
			Log.i("debugging", "Starting connected thread");
		mConnectedThread.start();
	}

}
