package net.daum.remotesync;

import java.io.RandomAccessFile;

public class SourceRandomFileAccess implements SourceFileAccess {
	private RandomAccessFile f; 
	
	public SourceRandomFileAccess(String filename) throws Exception {
		f = new RandomAccessFile(filename, "r");
	}
	
	@Override
	public void seek(long offset) throws Exception {
		f.seek(offset);
	}

	@Override
	public long read(byte[] buf) throws Exception {
		return f.read(buf);
	}
	
	public void close() throws Exception {
		f.close();
	}
}