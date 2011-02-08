package net.daum.remotesync;

import java.io.InputStream;
import java.io.OutputStream;

/** 
 * 네트워크로 송수신할때 사용할 인터페이스. 미리정한 바이너리 포맷으로 변환하는데 사용. 
 * @author dante
 *
 */
public interface Packable {
	long pack(OutputStream out) throws Exception;
	
}

final class PackUtil {
	static final void write16bit(OutputStream out, int i) throws Exception {
	    out.write((i >>> 8) & 0xFF);
	    out.write(i & 0xFF);
	}
	
	static final void write32bit(OutputStream out, long i) throws Exception {
		out.write(((int)i >>> 24) & 0xFF);
		out.write(((int)i >>> 16) & 0xFF);
		out.write(((int)i >>> 8) & 0xFF);
	    out.write((int)i & 0xFF);
	}
	
	static final int read16bit(InputStream in, OutputStream fileOut) throws Exception {
		int a;
		int r = in.read();
		if (fileOut != null) {
			fileOut.write(r);
		}
		
		r = (r << 8) | (a = in.read());
		if (fileOut != null) {
			fileOut.write(a);
		}
		
		return r;
	}

	static final long read32bit(InputStream in, OutputStream fileOut) throws Exception {
		int a;
		long r = a = in.read();
		if (fileOut != null) {
			fileOut.write(a);
		}
		
		r = (r << 8) | (a = in.read());
		if (fileOut != null) {
			fileOut.write(a);
		}
		
		r = (r << 8) | (a = in.read());
		if (fileOut != null) {
			fileOut.write(a);
		}
		
		r = (r << 8) | (a = in.read());
		if (fileOut != null) {
			fileOut.write(a);
		}
		
		return r;
	}
}