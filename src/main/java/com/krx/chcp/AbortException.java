package com.krx.chcp;

public class AbortException extends Exception {

	private static final long serialVersionUID = 6150528348435400845L;

	public AbortException(Exception e) {
		super(e);
	}
	
	public AbortException(String msg) {
		super(msg);
	}
}
