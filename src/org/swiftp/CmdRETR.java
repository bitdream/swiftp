package org.swiftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;

public class CmdRETR extends FtpCmd implements Runnable {
	//public static final String message = "TEMPLATE!!";
	protected String input;
	
	public CmdRETR(SessionThread sessionThread, String input) {
		super(sessionThread, CmdRETR.class.toString());
		this.input = input;
	}
	
	public void run() {
		myLog.l(Log.DEBUG, "RETR executing");
		String param = getParameter(input);
		File fileToRetr;
		if(param.charAt(0) == '/') {
			// The RETR command gave an absolute path
			fileToRetr = new File(param);
		} else {
			fileToRetr = new File(sessionThread.getPrefix(), param);
		}
		boolean err = false;
		String errString = null;
		// Check on and report the various possible error conditions
		try {
			fileToRetr = fileToRetr.getCanonicalFile();
		} catch (IOException e) {
			err = true;
			errString = "550 File name error\r\n";
		}
		if(fileToRetr.isDirectory()) {
			myLog.l(Log.DEBUG, "Ignoring RETR for directory");
			errString = "550 Can't RETR a directory\r\n";
			err = true;
		} else if(!fileToRetr.exists()) {
			myLog.l(Log.INFO, "Can't RETR nonexistent file: " + 
					fileToRetr.getAbsolutePath());
			errString = "550 File does not exist\r\n";
			err = true;
		} else if(!fileToRetr.canRead()) {
			myLog.l(Log.INFO, "Failed RETR permission (canRead() is false)");
			errString = "550 No read permissions\r\n";
			err = true;
		} else if(!sessionThread.isBinaryMode()) {
			myLog.l(Log.INFO, "Failed RETR in text mode");
			errString = "550 Text mode RETR not supported\r\n";
			err = true;
		}
		if(!err) {
			try {
				FileInputStream in = new FileInputStream(fileToRetr);
				byte[] buffer = new byte[Defaults.getDataChunkSize()];
				int bytesRead;
				switch(sessionThread.initDataSocket()) {
				case 1:
					myLog.l(Log.DEBUG, "RETR opened data socket");
					break;
				case 2:
					err = true;
					errString = "425 Only PASV mode is supported\r\n";
					myLog.l(Log.INFO, "Failed RETR without PASV");
					break;
				case 0:
				default:
					err = true;
					errString = "425 Error opening socket\r\n";
					myLog.l(Log.INFO, "");
					break;
				}
				if(!err) {
					sessionThread.writeString("150 Sending file\r\n");
					while((bytesRead = in.read(buffer)) != -1) {
						if(sessionThread
						   .sendViaDataSocket(buffer, bytesRead) == false) 
						{
							errString = "426 Data socket error\r\n";
							err = true;
							break;
						}
					}
				}
			} catch (FileNotFoundException e) {
				err = true;
				errString = "550 File not found\r\n";
			} catch(IOException e) {
				err = true;
				errString = "425 Network error\r\n";
			} finally {
				sessionThread.closeDataSocket();
			}
		}
		if(err) {
			sessionThread.writeString(errString);
		} else {
			sessionThread.writeString("226 Transmission finished\r\n");
		}
		sessionThread.closeDataSocket();
		myLog.l(Log.DEBUG, "RETR executing");
	}
}
