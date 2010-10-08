package net.daum.remotesync;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static net.daum.remotesync.PackUtil.read32bit;
import static net.daum.remotesync.PackUtil.write32bit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * �������� �ؽ��ڵ�. ���� ����� ���� 32��Ʈ �ؽ�����, ��Ȯ�� ����� ���� SHA1 �ؽ����� �Բ� �����Ѵ�. 
 *   
 * @author dante
 *
 */
public class Signature {
	private Integer fast = null;
	private byte[] strong = null;
	private byte[] content = null;
	
	/**
	 * ���� �ӵ��� �ؽ��ڵ� ���. Rsync���� �Ұ��� Adler-32�� ������ �˰���. ���� Rolling Signature������ �����ϴ�. 
	 * @param buf �ؽ��ڵ带 ����� ����Ÿ
	 * @return 32��Ʈ �ؽ��ڵ�
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
	 * ��Ȯ�� ����� ���� SHA1 �ؽ��ڵ�. 
	 * @param buf �ؽ��ڵ带 ����� ����Ÿ
	 * @return 160bit SHA1 �ؽ��ڵ� ��
	 */
	public static byte[] strongSignature(byte[] buf) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			return md.digest(buf);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("there is no SHA1 module");
		}
	}
	
	private Signature() {}
	
	/**
	 * ����Ÿ �������� ���� �ؽ��ڵ� ��ü�� �غ��Ѵ�. ���� �ؽ��� ����� ���� �ʴ´�. 
	 * @param content ����
	 */
	public Signature(byte[] content) {
		this.content = content.clone();
	}
	
	/**
	 * ���� �ؽ��ڵ� ��� (Adler-32�� ����)
	 * @return
	 */
	public int getFast() {
		if (fast == null && content != null) fast = fastSignature(content);
		return fast;
	}

	/**
	 * ��Ȯ�� �ؽ��ڵ� ��� (SHA1)
	 * @return
	 */
	public byte[] getStrong() {
		if (strong == null && content != null) strong = strongSignature(content);
		return strong;
	}
	
	/**
	 * SHA1 �ؽ��ڵ尪�� ȭ�鿡 ���� �� �ְ� Base64 ���ڿ��� ��ȯ 
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
	
	
	void pack(OutputStream out) throws IOException {
		write32bit(out, getFast());
		out.write(getStrong());
	}
	
	static Signature unpack(InputStream in) throws IOException {
		byte buf[] = new byte[20];
		Signature sign = new Signature();
		sign.fast = (int)read32bit(in);
		in.read(buf);
		sign.strong = buf;
		return sign;
	}
	
}
