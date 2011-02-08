package net.daum.remotesync;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static net.daum.remotesync.PackUtil.read32bit;
import static net.daum.remotesync.PackUtil.write32bit;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// TODO: use MessageUpdate for CheckSum.

/**
 * 블럭단위별 해쉬코드. 빠른 계산을 위한 32비트 해쉬값과, 정확한 계산을 위한 SHA1 해쉬값을 함께 관리한다. 
 *   
 * @author dante
 *
 */
public class Signature {
	private Long fast = null;
	private byte[] strong = null;
	public byte[] content = null;
	
	/**
	 * 빠른 속도의 해쉬코드 계산. Rsync논문에 소개된 Adler-32와 유사한 알고리즘. 추후 Rolling Signature적용이 가능하다. 
	 * @param buf 해쉬코드를 계산할 데이타
	 * @return 32비트 해쉬코드
	 */
	public static int fastSignature(byte[] buf) {
		int a = 0, b = 0;
		int m = buf.length;
		for (byte c: buf) {
			a += c;
			b += m-- * c;
		}
		a &= 0xFFFF;
		b &= 0xFFFF;
		return (b << 16) | a;
	}
	
	

	
	/**
	 * 정확한 계산을 위한 SHA1 해쉬코드. 
	 * @param buf 해쉬코드를 계산할 데이타
	 * @return 160bit SHA1 해쉬코드 값
	 */
	public static byte[] strongSignature(byte[] buf) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			return md.digest(buf);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("there is no SHA1 module");
		}
	}
	
	protected Signature() {}
	
	/**
	 * 데이타 원본으로 부터 해쉬코드 객체를 준비한다. 아직 해쉬값 계산은 하지 않는다. 
	 * @param content 원본
	 */
	public Signature(byte[] content) {
		this.content = content.clone();
	}
	
	/**
	 * 빠른 해쉬코드 계산 (Adler-32와 유사)
	 * @return
	 */
	public int getFast() {
		if (fast == null && content != null) fast = (long)fastSignature(content);
		return new Long(fast).intValue();
	}

	/**
	 * 정확한 해쉬코드 계산 (SHA1)
	 * @return
	 */
	public byte[] getStrong() {
		if (strong == null && content != null) strong = strongSignature(content);
		return strong;
	}
	
	/**
	 * SHA1 해쉬코드값을 화면에 보일 수 있게 Base64 문자열로 변환 
	 * @return base64 encoded string
	 */
	public String getStrongBase64() {
		return printBase64Binary(getStrong());
	}
	
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append("{fast: 0x").append(Integer.toHexString(getFast()));
		s.append(", strong: '").append(getStrongBase64());
		s.append("'}");
		return s.toString();
	}
	
	
	void pack(OutputStream out) throws Exception {
		write32bit(out, getFast());
		out.write(getStrong());
	}
	
	static Signature unpack(InputStream in, OutputStream fileOut) throws Exception {
		byte buf[] = new byte[20];
		Signature sign = new Signature();
		sign.fast = read32bit(in, fileOut);
		in.read(buf);
		if (fileOut != null) {
			fileOut.write(buf);
		}
		sign.strong = buf;
		return sign;
	}
	
}
