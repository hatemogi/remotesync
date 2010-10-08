package net.daum.remotesync;

import static net.daum.remotesync.PackUtil.read16bit;
import static net.daum.remotesync.PackUtil.write16bit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link SourceCodeList}�� ��ǥ������ ���̰��� �����ϴ� ���� ��ü. 
 * ������ BuildCode��, �������ϰ� Ư�� ���� ��ġ�Ѵٸ�, �ش� ���� �ε������� �����Ѵ�. 
 * ��ġ���� �ʴ� ������ ���ؼ��� ����Ʈ �迭�� �״�� �����Ѵ�. 
 * 
 * @author dante
 * @see BuildCodeList SourceCodeList
 */
abstract public class BuildCode implements Packable {
	
	/**
	 * ������ Ư������ ��ġ�ϴ� ������ ���� �ڵ� ǥ��.
	 * @param index ��ġ�ϴ� ���� �ε���
	 */
	public static BuildCode createRefCode(int index) {
		return new RefBuildCode(index);
	}

	/**
	 * �������� ã���� ���� ����Ÿ������ ���� �ڵ�ǥ��.
	 * @param data ��ġ���� �ʴ� ����Ÿ ����Ʈ �迭.
	 */
	public static BuildCode createRawCode(byte[] data) {
		return new RawBuildCode(data);
	}
	
	
	public static BuildCode unpack(InputStream in) throws IOException {
		int first = in.read();
		int header = first & 0xC0;
		int data = ((first & 0x3F) << 16) | read16bit(in);
		if (header == RefBuildCode.HEADER) {
			return createRefCode(data);
		} else if (header == RawBuildCode.HEADER) {
			byte buf[] = new byte[data];
			int r = in.read(buf);
			if (r != data) throw new RuntimeException("couldn't read enough bytes for the raw code");
			return createRawCode(buf);
		} else {
			throw new RuntimeException("unknown header = " + header);
		}
	}
	
	/**
	 * 
	 * @return �����ڵ��� ũ��
	 */
	abstract public long length();
	
	/** 
	 * ���������� �����ؼ�, ��ǥ���� ������.  
	 * @param src ���������� ���� �� �ִ� �������̽�
	 * @param blockSize �����ڵ带 �����ϴµ� ����� ��ũ�� 
	 * @param out ��ǥ������ ����� OutputStream
	 * @return ���� ������ ����Ʈ ��
	 * @throws IOException
	 */
	abstract public long patch(SourceFileAccess src, int blockSize, OutputStream out) throws IOException;
}


/**
 * �ҽ������� �������� ����Ű�� �����ڵ�. 
 * @author dante
 *
 */
class RefBuildCode extends BuildCode {
	public static final int HEADER = 0x80;

	private int index;

	RefBuildCode(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public long length() {
		return 3;
	}

	public String toString() {
		return "{ref:" + index + "}";
	}

	public long pack(OutputStream out) throws IOException {
		int header = 0x80;
		out.write(header | ((index >> 16) & 0x3F));
		write16bit(out, index & 0xFFFF);
		return 3;
	}
	
	@Override
	public long patch(SourceFileAccess src, int blockSize, OutputStream out) throws IOException {
		src.seek(index * blockSize);
		byte buf[] = new byte[blockSize];
		long r = src.read(buf);
		assert r == blockSize: "referenced block must have the block-sized length";
		out.write(buf);
		return r;
	}

}

/**
 * �ҽ����Ͽ��� ã�� �� ���� ����Ÿ�� ǥ���ϴ� �����ڵ�.
 * @author dante
 */
class RawBuildCode extends BuildCode {
	public static final int HEADER = 0x00;
	
	private final byte[] content;
	
	RawBuildCode(byte[] buf) {
		this.content = buf.clone();
	}
	
	public byte[] getData() {
		return content;
	}
	
	public long length() {
		return 3 + content.length;
	}
	
	public String toString() {
		return "{raw:" + content.length + "}";
	}
	
	public long pack(OutputStream out) throws IOException {
		int header = 0x00;
		out.write(header | ((content.length >> 16) & 0x3F));
		write16bit(out, content.length & 0xFFFF);
		out.write(content);
		return 3 + content.length;
	}

	@Override
	public long patch(SourceFileAccess src, int blockSize, OutputStream out) throws IOException {
		out.write(content);
		return content.length;
	}

}