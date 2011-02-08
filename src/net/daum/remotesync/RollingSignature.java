package net.daum.remotesync;

import java.util.ArrayDeque;

public class RollingSignature extends Signature {
	private int a, b, size;
	private ArrayDeque<Byte> buf;
	
		
	public void init(byte[] content) {
		a = 0;
		b = 0;
		buf = new ArrayDeque<Byte>(content.length);
		int m = size = content.length;
		for (byte c: content) {
			a += c;
			b += m-- * c;
			buf.addLast(c);
		}
		a &= 0xFFFF;
		b &= 0xFFFF;
	}
	
	public void roll(byte adding) {
		byte deleting = buf.removeFirst();
		buf.addLast(adding);
		a = (a - deleting + adding) & 0xFFFF;
		b = (b - (size * deleting) + a) & 0xFFFF;
	}
	
	public int getFast() {
		return (b << 16) | a;		
	}
		
	public byte[] getContent() {
		byte[] content = new byte[buf.size()];
		int i = 0;
		for (Byte b: buf) {
			content[i++] = b;
		}		
		return content;
	}
	
	public byte[] getStrong() {
		return strongSignature(getContent());
	}

	
	public byte getFirst() {
		return buf.getFirst();
	}

}
