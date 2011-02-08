package net.daum.remotesync;

public class RemoteSyncException extends Exception {

	private static final long serialVersionUID = -6837048736431231520L;
	public static final RemoteSyncException OVERSIZE = new RemoteSyncOversizeException();
	public static final RemoteSyncException TIMEOUT = new RemoteSyncTimeoutException();
	
	public static interface Type {
		int TIMEOUT = 1;
		int OVERSIZE = 2;
	}

	private int type;
	
	public RemoteSyncException(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
	
}

class RemoteSyncOversizeException extends RemoteSyncException {
	private static final long serialVersionUID = -4922559701045629415L;

	RemoteSyncOversizeException() {
		super(Type.OVERSIZE);
	}
}

class RemoteSyncTimeoutException extends RemoteSyncException {
	private static final long serialVersionUID = 8663889764309259328L;

	RemoteSyncTimeoutException() {
		super(Type.TIMEOUT);
	}
}