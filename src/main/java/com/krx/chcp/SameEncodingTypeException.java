package com.krx.chcp;

public class SameEncodingTypeException extends Exception {

	private static final long serialVersionUID = 6150528348435400845L;

	public SameEncodingTypeException(Exception e) {
		super(e);
	}
	
	public SameEncodingTypeException(String msg) {
		super(msg);
	}
}
