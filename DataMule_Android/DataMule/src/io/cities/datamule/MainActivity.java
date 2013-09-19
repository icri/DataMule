package io.cities.datamule;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

public class MainActivity extends Activity {

	static D2xxManager ftdid2xx = null;
	FT_Device ftDev = null;
	int DevCount = -1;
    int currentIndex = -1;
    int openIndex = 1;
    
//	public WifiManager wifi;
//	public BluetoothManager bluetooth;
//	BroadcastReceiver receiverWiFi = null;
//	BroadcastReceiver receiverBlue = null;
    BluetoothSocket btSocket = null;
    BluetoothDevice btDevice = null;
    OutputStream btOutStream = null;
    final String BLUETOOTH_DEVICE_NAME = "ubuntu-0";

    static boolean uartOpenedFlag = false;
    static boolean uartConfiguredFlag = false;
    static boolean btOpenedFlag = false;
    
    Spinner baudRateSpn, stopBitsSpn, dataBitsSpn, paritySpn, flowControlSpn, portNumSpn;
    ArrayAdapter<CharSequence> portNumAdp;
    Button openUartBtn, configUartBtn, openBtBtn, enDownBtn, enUpBtn;
    TextView inDataLog, outDataLog, bufDataLog;

    int baudRate; /*baud rate*/
    byte stopBit; /*1:1stop bits, 2:2 stop bits*/
    byte dataBit; /*8:8bit, 7: 7bit*/
    byte parity;  /*0: none, 1: odd, 2: even, 3: mark, 4: space*/
    byte flowControl; /*0:none, 1: flow control(CTS,RTS)*/
    int portNumber; /*port number*/
    
    static boolean enableDownFlag = true;
    static boolean enableUpFlag = false;

    static final int readLength = 512;
    int readCount = 0;
    int dataAvailable = 0;
    byte[] readData;
    char[] readDataToText, readDataToHex;
    String writeDataFromString;
    boolean readThreadGoingFlag = false;
    boolean writeThreadGoingFlag = false;
    readThread readThread;
    writeThread writeThread;

    static final int maxBufferSize = 100;
    LinkedList<String> buffer = null;
    
	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		openUartBtn = (Button) findViewById(R.id.openUartBtn);
		
		portNumSpn = (Spinner) findViewById(R.id.portNumSpn);
		portNumAdp = ArrayAdapter.createFromResource(this, R.array.port_list_1, android.R.layout.simple_spinner_item);
		portNumAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		portNumSpn.setAdapter(portNumAdp);
		portNumber = 0;
		
		configUartBtn = (Button) findViewById(R.id.configUartBtn);
		
		baudRateSpn = (Spinner) findViewById(R.id.baudRateSpn);
		ArrayAdapter<CharSequence> baudRateAdp = ArrayAdapter.createFromResource(this, R.array.baud_rate, android.R.layout.simple_spinner_item);
		baudRateAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		baudRateSpn.setAdapter(baudRateAdp);
		baudRateSpn.setSelection(7);
		baudRate = 57600; /* by default it is 57600 */

		stopBitsSpn = (Spinner) findViewById(R.id.stopBitsSpn);
		ArrayAdapter<CharSequence> stopBitsAdp = ArrayAdapter.createFromResource(this, R.array.stop_bits, android.R.layout.simple_spinner_item);
		stopBitsAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stopBitsSpn.setAdapter(stopBitsAdp);
		stopBit = 1; /* default is stop bit 1 */

		dataBitsSpn = (Spinner) findViewById(R.id.dataBitsSpn);
		ArrayAdapter<CharSequence> dataBitsAdp = ArrayAdapter.createFromResource(this, R.array.data_bits, android.R.layout.simple_spinner_item);
		dataBitsAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dataBitsSpn.setAdapter(dataBitsAdp);
		dataBitsSpn.setSelection(1);
		dataBit = 8; /* default data bit is 8 bit */

		paritySpn = (Spinner) findViewById(R.id.paritySpn);
		ArrayAdapter<CharSequence> parityAdp = ArrayAdapter.createFromResource(this, R.array.parity, android.R.layout.simple_spinner_item);
		parityAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		paritySpn.setAdapter(parityAdp);
		parity = 0; /* default is none */

		flowControlSpn = (Spinner) findViewById(R.id.flowControlSpn);
		ArrayAdapter<CharSequence> flowControlAdp = ArrayAdapter.createFromResource(this, R.array.flow_control, android.R.layout.simple_spinner_item);
		flowControlAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		flowControlSpn.setAdapter(flowControlAdp);
		flowControl = 0; /* default flow control is is none */
		
		openBtBtn = (Button) findViewById(R.id.openBtBtn);
		
		enDownBtn = (Button) findViewById(R.id.enDownBtn);
		enUpBtn = (Button) findViewById(R.id.enUpBtn);

		inDataLog = (TextView) findViewById(R.id.inDataLog);
		outDataLog = (TextView) findViewById(R.id.outDataLog);
		bufDataLog = (TextView) findViewById(R.id.bufDataLog);
		
		/* set the adapter listeners for baud */
		baudRateSpn.setOnItemSelectedListener(new MyOnBaudRateSelectedListener());
		/* set the adapter listeners for stop bits */
		stopBitsSpn.setOnItemSelectedListener(new MyOnStopBitsSelectedListener());
		/* set the adapter listeners for data bits */
		dataBitsSpn.setOnItemSelectedListener(new MyOnDataBitsSelectedListener());
		/* set the adapter listeners for parity */
		paritySpn.setOnItemSelectedListener(new MyOnParitySelectedListener());
		/* set the adapter listeners for flow control */
		flowControlSpn.setOnItemSelectedListener(new MyOnFlowControlSelectedListener());
		/* set the adapter listeners for port number */
		portNumSpn.setOnItemSelectedListener(new MyOnPortNumSelectedListener());

		// Setup UART manager
    	try
    	{
    		ftdid2xx = D2xxManager.getInstance(this);
    	}
    	catch (D2xxManager.D2xxException ex)
    	{
    		ex.printStackTrace();
    	}

    	// Reaction to attach/detach
		IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);
        this.registerReceiver(iUsbReceiver, filter);
        
		readData = new byte[readLength];
		readDataToText = new char[readLength];
		readDataToHex = new char[readLength*2];

		buffer = new LinkedList<String>();
		
		openUartBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(!uartOpenedFlag)
				{
					if(DevCount <= 0)
						createDeviceList();
					else
						connectFunction();
				}
				else disconnectFunction();
			}
		});

		configUartBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(DevCount <= 0 || ftDev == null)
		    	{
		    		Toast.makeText(getApplicationContext(), "Device not open yet...", Toast.LENGTH_SHORT).show();		    	
		    	}
				else
				{
					SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
				}
			}
		});
		
		openBtBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(!btOpenedFlag)
				{
/*					// Setup WiFi
					wifi = (WifiManager) DeviceUARTContext.getSystemService(Context.WIFI_SERVICE);

					// Get WiFi status
					WifiInfo info = wifi.getConnectionInfo();
					sinksText.append(info.toString() + ", ");

					// List available networks
					List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
					for (WifiConfiguration config : configs) {
						sinksText.append("[ " + config.toString() + " ]");
					}

					// Register Broadcast Receiver
					if (receiver == null)
						receiver = new WiFiScanReceiver(DeviceUARTContext, wifi);

					DeviceUARTContext.registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

					wifi.startScan(); */
				
					BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
					if (mBluetoothAdapter == null) {
						Toast.makeText(getApplicationContext(), "Device does not support Bluetooth.", Toast.LENGTH_SHORT).show();
						return;
					}
				
					if (!mBluetoothAdapter.isEnabled()) {
						Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableBtIntent, 1);
					}
				
					Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
					// If there are paired devices
					if (pairedDevices.size() > 0) {
						// Loop through paired devices
						for (BluetoothDevice device : pairedDevices) {
							// Add the name and address to an array adapter to show in a ListView
							outDataLog.setText("Discovered: " + device.getName() + " : " + device.getAddress());
							if(device.getName().equals(BLUETOOTH_DEVICE_NAME))
								btDevice = device;
						}
					}
				
					// Get a BluetoothSocket to connect with the given BluetoothDevice
					try {			    	
						// MY_UUID is the app's UUID string, also used by the server code
						btSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

						// Cancel discovery because it will slow down the connection
						mBluetoothAdapter.cancelDiscovery();
			        
						// Connect the device through the socket. This will block
						// until it succeeds or throws an exception
						btSocket.connect();
			 
						btOpenedFlag = true;
						openBtBtn.setText("Close Bluetooth");
						
						btOutStream = btSocket.getOutputStream();

						if(false == writeThreadGoingFlag)
						{
							writeThread = new writeThread(writeHandler);
							writeThreadGoingFlag = true;
							writeThread.start();
						
							Toast.makeText(getApplicationContext(), "Data upload has started.", Toast.LENGTH_SHORT).show();
						}
					}
					catch (IOException e)
					{
						Toast.makeText(getApplicationContext(), "Bluetooth communication failed.", Toast.LENGTH_LONG).show();
					}
			    
					// Create a BroadcastReceiver for ACTION_FOUND
/*					receiverBlue = new BroadcastReceiver() {
				    	public void onReceive(Context context, Intent intent) {
				        	String action = intent.getAction();
				        	// When discovery finds a device
				        	if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				            	// Get the BluetoothDevice object from the Intent
				            	BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				            	// Add the name and address to an array adapter to show in a ListView
				            	sinksText.append("[ " + device.getName() + " : " + device.getAddress() + " ]");
				        	}
				    	}
					};
				
					// Register the BroadcastReceiver
					IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
					DeviceUARTContext.registerReceiver(receiverBlue, filter); // Don't forget to unregister during onDestroy */
				}
				else
				{
					try
					{
						btSocket.close();
						writeThreadGoingFlag = false;
						btOpenedFlag = false;
						openBtBtn.setText("Open Bluetooth");
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		});
		
        enDownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
				if(DevCount <= 0 || ftDev == null)
		    	{
		    		Toast.makeText(getApplicationContext(), "Device not open yet...", Toast.LENGTH_SHORT).show();
		    	}
				else if( uartConfiguredFlag == false)
		    	{
		    		Toast.makeText(getApplicationContext(), "UART not configured yet...", Toast.LENGTH_SHORT).show();
		    		return;
		    	}
		    	else
				{
					EnableDownload();
				}
            }
        });

        enUpBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
				if(btDevice == null || btSocket == null)
		    	{
		    		Toast.makeText(getApplicationContext(), "Device not open yet...", Toast.LENGTH_SHORT).show();
		    	}
		    	else
				{
					EnableUpload();
				}
            }
        });
	}
	
	public class MyOnBaudRateSelectedListener implements OnItemSelectedListener
    {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			baudRate = Integer.parseInt(parent.getItemAtPosition(pos).toString());
		}

		public void onNothingSelected(AdapterView<?> parent)
		{}
    }

    public class MyOnStopBitsSelectedListener implements OnItemSelectedListener
    {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			stopBit = (byte)Integer.parseInt(parent.getItemAtPosition(pos).toString());
		}

		public void onNothingSelected(AdapterView<?> parent)
		{}
    }

    public class MyOnDataBitsSelectedListener implements OnItemSelectedListener
    {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			dataBit = (byte)Integer.parseInt(parent.getItemAtPosition(pos).toString());
		}

		public void onNothingSelected(AdapterView<?> parent)
		{}
    }

    public class MyOnParitySelectedListener implements OnItemSelectedListener
    {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			String parityString = new String(parent.getItemAtPosition(pos).toString());
			if(parityString.compareTo("none") == 0)
			{
				parity = 0;
			}
			else if(parityString.compareTo("odd") == 0)
			{
				parity = 1;
			}
			else if(parityString.compareTo("even") == 0)
			{
				parity = 2;
			}
			else if(parityString.compareTo("mark") == 0)
			{
				parity = 3;
			}
			else if(parityString.compareTo("space") == 0)
			{
				parity = 4;
			}
		}

		public void onNothingSelected(AdapterView<?> parent)
		{}
    }

    public class MyOnFlowControlSelectedListener implements OnItemSelectedListener
    {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			String flowString = new String(parent.getItemAtPosition(pos).toString());
			if(flowString.compareTo("none")==0)
			{
				flowControl = 0;
			}
			else if(flowString.compareTo("CTS/RTS")==0)
			{
				flowControl = 1;
			}
			else if(flowString.compareTo("DTR/DSR")==0)
			{
				flowControl = 2;
			}
			else if(flowString.compareTo("XOFF/XON")==0)
			{
				flowControl = 3;
			}
		}

		public void onNothingSelected(AdapterView<?> parent)
		{}
    }

	public class MyOnPortNumSelectedListener implements OnItemSelectedListener
    {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			openIndex = Integer.parseInt(parent.getItemAtPosition(pos).toString()) - 1;
		}

		public void onNothingSelected(AdapterView<?> parent)
		{}
    }

	public void updatePortNumberSelector()
	{
		if(DevCount == 2)
		{
			portNumAdp = ArrayAdapter.createFromResource(this, R.array.port_list_2, android.R.layout.simple_spinner_item);
			portNumAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			portNumSpn.setAdapter(portNumAdp);
			portNumAdp.notifyDataSetChanged();
			Toast.makeText(this, "2-port device attached.", Toast.LENGTH_SHORT).show();
		}
		else if(DevCount == 4)
		{
			portNumAdp = ArrayAdapter.createFromResource(this, R.array.port_list_4, android.R.layout.simple_spinner_item);
			portNumAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			portNumSpn.setAdapter(portNumAdp);
			portNumAdp.notifyDataSetChanged();
			Toast.makeText(this, "4-port device attached.", Toast.LENGTH_SHORT).show();
		}
		else
		{
			portNumAdp = ArrayAdapter.createFromResource(this, R.array.port_list_1, android.R.layout.simple_spinner_item);
			portNumAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			portNumSpn.setAdapter(portNumAdp);
			portNumAdp.notifyDataSetChanged();	
			Toast.makeText(this, "1-port device attached.", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
    @Override
	public void onStart() {
    	super.onStart();
    	createDeviceList();
    }

	@Override
	public void onStop()
	{
		disconnectFunction();
		super.onStop();
	}

    @Override
	protected void onDestroy() {
    	this.unregisterReceiver(iUsbReceiver);
    	super.onDestroy();
	}

	public void createDeviceList()
	{
		int tempDevCount = ftdid2xx.createDeviceInfoList(this);
		
		if (tempDevCount > 0)
		{
			if( DevCount != tempDevCount )
			{
				DevCount = tempDevCount;
				updatePortNumberSelector();
			}
		}
		else
		{
			DevCount = -1;
			currentIndex = -1;
			Toast.makeText(this, "No device found.", Toast.LENGTH_SHORT).show();
		}
	}

	public void connectFunction()
	{
		int tmpProtNumber = openIndex + 1;

		if( currentIndex != openIndex )
		{
			if(null == ftDev)
			{
				ftDev = ftdid2xx.openByIndex(this, openIndex);
			}
			else
			{
				synchronized(ftDev)
				{
					ftDev = ftdid2xx.openByIndex(this, openIndex);
				}
			}
			uartConfiguredFlag = false;
		}
		else
		{
			Toast.makeText(this, "Device port " + tmpProtNumber + " already opened.", Toast.LENGTH_LONG).show();
			return;
		}

		if(ftDev == null)
		{
			Toast.makeText(this, "Opening device port "+tmpProtNumber+" failed.", Toast.LENGTH_LONG).show();
			return;
		}

		if (true == ftDev.isOpen())
		{
			currentIndex = openIndex;
			Toast.makeText(this, "Opening device port " + tmpProtNumber + " succeeded.", Toast.LENGTH_SHORT).show();
			
			uartOpenedFlag = true;
			openUartBtn.setText("Close UART");

			SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
			
			if(false == readThreadGoingFlag)
			{
				readThread = new readThread(readHandler);
				readThreadGoingFlag = true;
				readThread.start();
				
				Toast.makeText(this, "Data download has started.", Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			Toast.makeText(this, "Opening device port " + tmpProtNumber + " failed.", Toast.LENGTH_LONG).show();
		}
	}

 	public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl)
 	{
		if (ftDev.isOpen() == false) {
			Toast.makeText(this, "Device not open for configuration.", Toast.LENGTH_LONG).show();
			return;
		}

		// configure the port, reset to UART mode for 232 devices
		ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

		ftDev.setBaudRate(baud);

		switch (dataBits) {
			case 7:
				dataBits = D2xxManager.FT_DATA_BITS_7;
				break;
			case 8:
				dataBits = D2xxManager.FT_DATA_BITS_8;
				break;
			default:
				dataBits = D2xxManager.FT_DATA_BITS_8;
				break;
		}

		switch (stopBits) {
			case 1:
				stopBits = D2xxManager.FT_STOP_BITS_1;
				break;
			case 2:
				stopBits = D2xxManager.FT_STOP_BITS_2;
				break;
			default:
				stopBits = D2xxManager.FT_STOP_BITS_1;
				break;
		}

		switch (parity) {
			case 0:
				parity = D2xxManager.FT_PARITY_NONE;
				break;
			case 1:
				parity = D2xxManager.FT_PARITY_ODD;
				break;
			case 2:
				parity = D2xxManager.FT_PARITY_EVEN;
				break;
			case 3:
				parity = D2xxManager.FT_PARITY_MARK;
				break;
			case 4:
				parity = D2xxManager.FT_PARITY_SPACE;
				break;
			default:
				parity = D2xxManager.FT_PARITY_NONE;
				break;
		}

		ftDev.setDataCharacteristics(dataBits, stopBits, parity);

		short flowCtrlSetting;
		switch (flowControl) {
			case 0:
				flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
				break;
			case 1:
				flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
				break;
			case 2:
				flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
				break;
			case 3:
				flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
				break;
			default:
				flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
				break;
		}

		// TODO : flow ctrl: XOFF/XOM
		// TODO : flow ctrl: XOFF/XOM
		ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);
		
		ftDev.setLatencyTimer((byte) 16);
		ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));

		uartConfiguredFlag = true;
		Toast.makeText(this, "Configuration completed.", Toast.LENGTH_SHORT).show();
	}

	public void disconnectFunction()
	{
		DevCount = -1;
		currentIndex = -1;
		readThreadGoingFlag = false;
		try
		{
			Thread.sleep(50);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		if(ftDev != null)
		{
			synchronized(ftDev)
			{
				if( true == ftDev.isOpen())
				{
					ftDev.close();
					uartOpenedFlag = false;
					openUartBtn.setText("Open UART");
				}
			}
		}
	}

    private final BroadcastReceiver iUsbReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
			{
				notifyUSBDeviceAttach();
			}
			else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
			{
				notifyUSBDeviceDetach();
			}
		}	
	};

	public void notifyUSBDeviceAttach()
	{
		Toast.makeText(this, "USB device attached.", Toast.LENGTH_SHORT).show();
		createDeviceList();
	}
	
	public void notifyUSBDeviceDetach()
	{
		Toast.makeText(getApplicationContext(), "USB device detached.", Toast.LENGTH_SHORT).show();
		disconnectFunction();
	}

    public void EnableDownload()
    {
		if(enableDownFlag)
		{
			ftDev.stopInTask();
			inDataLog.setText("Download disabled.");
			enDownBtn.setText("Enable Data Download");
			enableDownFlag = false;
		}
		else
		{
			ftDev.purge((byte) (D2xxManager.FT_PURGE_TX));
			ftDev.restartInTask();
			enDownBtn.setText("Disable Data Download");
			enableDownFlag = true;
		}
    }

    public void EnableUpload()
    {
		if(enableUpFlag)
		{
			outDataLog.setText("Upload disabled.");
			enUpBtn.setText("Enable Data Upload");
			enableUpFlag = false;
		}
		else
		{
			enUpBtn.setText("Disable Data Upload");
			enableUpFlag = true;
		}
    }

    @SuppressLint("HandlerLeak")
	final Handler readHandler =  new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		if(dataAvailable > 0)
    		{
    			inDataLog.setText("Incoming data: [" + String.copyValueOf(readDataToHex) + "]");
    			synchronized(buffer)
    			{
    				if(buffer.size() < maxBufferSize)
    				{
    					buffer.add(String.copyValueOf(readDataToHex));
    					bufDataLog.setText("Buffer holds " + buffer.size() + " elements (max " + maxBufferSize + ").");
    				}
    				else
    					inDataLog.setText("Internal buffer is full!");
    			}
    		}
    	}    	
    };

    private class readThread extends Thread
	{
		Handler handler;

		readThread(Handler h){
			handler = h;
			this.setPriority(Thread.MIN_PRIORITY);
		}
		
	    private final char[] kDigits = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		
	    public char[] byteToHexChar(byte[] b, int offset, int num)
	    {
	        char[] hex = new char[num * 2];
	        int position = 0;
	        for (int i = offset; i < offset+num; i++) {
	          int value = (b[i] + 256) % 256;
	          int highIndex = value >> 4;
	          int lowIndex = value & 0x0f;
	          hex[position * 2 + 0] = kDigits[highIndex];
	          hex[position * 2 + 1] = kDigits[lowIndex];
	          position++;
	        }
	        return hex;
	    }
		
		@Override
		public void run()
		{
			int i;

			while(true == readThreadGoingFlag)
			{
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e)
				{
				}

				if(ftDev != null)
				{
					synchronized(ftDev)
					{
						dataAvailable = ftDev.getQueueStatus();				
						if (dataAvailable > 0) {
						
							if(dataAvailable > readLength)
							{
								dataAvailable = readLength;
							}
						
							ftDev.read(readData, dataAvailable);
							for (i = 0; i < dataAvailable; i++)
							{
								readDataToText[i] = (char) readData[i];
							}
							readDataToHex = byteToHexChar(readData, 14, 2);
							Message msg = handler.obtainMessage();
							handler.sendMessage(msg);
						}
					}
				}
			}
		}
	}

	@SuppressLint("HandlerLeak")
	final Handler writeHandler =  new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		if(writeDataFromString != null)
    		{
    			outDataLog.setText("Outgoing data: [" + writeDataFromString + "]");
    			if(enableUpFlag)
    			{
    				try
    				{
    					btOutStream.write(writeDataFromString.getBytes());
    				}
    				catch (IOException e) { }
					buffer.removeFirst();
					bufDataLog.setText("Buffer holds " + buffer.size() + " elements (max " + maxBufferSize + ").");
    			}
    			else outDataLog.setText("Uploading disabled.");
    		}
			else outDataLog.setText("Internal buffer is empty!");
    	}
    };
    
	private class writeThread extends Thread
	{
		Handler handler;

		writeThread(Handler h){
			handler = h;
			this.setPriority(Thread.MIN_PRIORITY);
		}
		
		@Override
		public void run()
		{
			while(true == writeThreadGoingFlag)
			{
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e)
				{
				}

				synchronized(buffer)
				{
					if(!buffer.isEmpty())
					{
						writeDataFromString = (String) buffer.getFirst();
						Message msg = handler.obtainMessage();
						handler.sendMessage(msg);
					}
					else writeDataFromString = null;
				}
			}
		}
	}
}
