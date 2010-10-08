package net.daum.remotesync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** 
 * 네트워크로 송수신할때 사용할 인터페이스. 미리정한 바이너리 포맷으로 변환하는데 사용. 
 * @author dante
 *
 */
public interface Packable {
	long pack(OutputStream out) throws IOException;
	
}

final class PackUtil {

	static final void write16bit(OutputStream out, int i) throws IOException {
		out.write((i & 0xFF00) >> 16);
		out.write(i & 0xFF);
	}
	
	static final void write32bit(OutputStream out, long i) throws IOException {
		out.write((int) ((i & 0xFF000000) >> 24));
		out.write((int) (i & 0x00FF0000) >> 16);
		out.write((int) (i & 0x0000FF00) >> 8);
		out.write((int) (i & 0x000000FF) );		
	}
	
	static final int read16bit(InputStream in) throws IOException {
		int r = in.read();
		r = (r << 8) | in.read();
		return r;
	}

	static final long read32bit(InputStream in) throws IOException {
		long r = in.read();
		r = (r << 8) | in.read();
		r = (r << 8) | in.read();
		r = (r << 8) | in.read();
		return r;
	}
}