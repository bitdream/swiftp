package org.swiftp;

import java.io.File;
import java.io.IOException;

import android.util.Log;

public class CmdRNTO extends FtpCmd implements Runnable {
	protected String input;

	public CmdRNTO(SessionThread sessionThread, String input) {
		super(sessionThread, CmdRNTO.class.toString());
		this.input = input;
	}
	
	public void run() {
		String param = getParameter(input);
		String errString = null;
		File toFile = null;
		myLog.l(Log.DEBUG, "RNTO executing\r\n");
		mainblock: {
			if(param.charAt(0) == '/') {
				// Param is absolute path, use param as is
				toFile = new File(param);
			} else {
				// If relative path, use current directory prefix
				toFile = new File(sessionThread.getPrefix(), param);
			}
			try {
				toFile = toFile.getCanonicalFile().getAbsoluteFile();
			} catch (IOException e) {
				sessionThread.writeString("450 Invalid filename\r\n");
				errString = "Couldn't construct File object";
				break mainblock;
			}
			
			File fromFile = sessionThread.getRenameFrom();
			if(fromFile == null) {
				errString = "550 Rename error, maybe RNFR not sent\r\n";
				break mainblock;
			}
			if(!fromFile.renameTo(toFile)) {
				errString = "550 Error during rename operation\r\n";
				break mainblock;
			}
		}
		if(errString != null) {
			sessionThread.writeString(errString);
			myLog.l(Log.INFO, "RNFR failed: " + errString.trim());
		} else {
			sessionThread.writeString("250 rename successful\r\n");
		}
		sessionThread.setRenameFrom(null);
		myLog.l(Log.DEBUG, "RNTO finished");
	}
}
