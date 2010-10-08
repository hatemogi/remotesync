package net.daum.remotesync;

import static net.daum.remotesync.PackUtil.read16bit;
import static net.daum.remotesync.PackUtil.read32bit;
import static net.daum.remotesync.PackUtil.write16bit;
import static net.daum.remotesync.PackUtil.write32bit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * {@link SourceCodeList}�� ��ǥ����(newFileIn)�� ���� ������ �����ڵ� ����Ʈ. 
 * �� ��ü�� ���������� ������ ���ļ�, ��ü������ ���۹��� �ʰ�, ��ǥ����(newFile)�� 
 * ����� �� �� �ִ�.
 * 
 * <li>{@link SourceCodeList} + newFile => BuildCodeList</li>
 * <li>{@link BuildCodeList} + srcFile => newFile<li>
 *
 * @see SourceCodeList
 * @author dante
 *
 */
public class BuildCodeList extends ArrayList<BuildCode> {

	public static final int VERSION1 = 0x81;
	
	private static final long serialVersionUID = 9134363866140763474L;
	
	
	private int blockSize = RemoteSync.DEFAULT_BLOCK_SIZE;
	private long rawLimit = RemoteSync.DEFAULT_RAW_LIMIT;
	
	private BuildCodeList() {}
	
	private BuildCodeList(int blockSize, long rawLimit) {
		this.blockSize = blockSize;
		this.rawLimit = rawLimit;
	}
	

	/**
	 * ��ũ��. SourceCodeList�� ��ũ��� ���ƾ��Ѵ�. 
	 */
	public int getBlockSize() {
		return blockSize;
	}

	
	private static Map<String, Integer> generateRefTable(SourceCodeList sc) {
		Map<String, Integer> table = new HashMap<String, Integer>();
		int idx = 0;
		for (Signature sign: sc) {
			table.put(new String(sign.getStrong()), idx++);
		}
		return table;
	}
	
	private void processRawCodeIfNeeded(ByteArrayOutputStream rawbuf) {
		if (rawbuf.size() > 0) {
			add(BuildCode.createRawCode(rawbuf.toByteArray()));
			rawbuf.reset();
		}
	}
	
	private void addByteToRawBuf(ByteArrayOutputStream rawbuf, byte c) {
		if (rawbuf.size() >= rawLimit) {
			processRawCodeIfNeeded(rawbuf);
		}		
		rawbuf.write(c);
	}
	

	/**
	 * {@link SourceCodeList}�� ��ǥ����(newFileIn)�� �̿��� BuildCodeList �����ϱ�. 
	 * @param sourceCodeList 
	 * @param newFileIn ��ǥ������ InputStream
	 * @param rawLimit ���̳ʸ� ������ ���� ���ѵ� ��. �ִ� (4M-1)�� �⺻���� ����� �� �ִ�. 
	 * @return ������ BulidCodeList
	 * @throws IOException
	 */
	static final BuildCodeList create(SourceCodeList sourceCodeList, InputStream newFileIn, long rawLimit) throws IOException {
		final int blockSize = sourceCodeList.getBlockSize();
		BuildCodeList bc = new BuildCodeList(blockSize, rawLimit);
		
		Map<String, Integer> table = generateRefTable(sourceCodeList);
		
		PushbackInputStream pin = new PushbackInputStream(newFileIn, blockSize);
		byte[] buf = new byte[blockSize];
		int r = 0;
		ByteArrayOutputStream rawbuf = new ByteArrayOutputStream();
		while ((r = pin.read(buf)) > 0) {
			if (r < blockSize) {
				for (int i = 0; i < r; i++) {
					bc.addByteToRawBuf(rawbuf, buf[i]);
				}
			} else {
				Signature sign = new Signature(buf);
				Integer idx = table.get(new String(sign.getStrong()));
				if (idx != null) {
					bc.processRawCodeIfNeeded(rawbuf);
					bc.add(BuildCode.createRefCode(idx));
				} else {
					bc.addByteToRawBuf(rawbuf, buf[0]);
					pin.unread(buf, 1, r - 1);
				}
			}
		}
		bc.processRawCodeIfNeeded(rawbuf);
		return bc;
	}

	
	public long patch(SourceFileAccess src, OutputStream newFileOut) throws IOException {
		long written = 0;
		for (BuildCode bc: this) {
			written += bc.patch(src, blockSize, newFileOut);
		}
		return written;
	}
	
	private long estimatedLength() {
		long len = 0;
		for (BuildCode code: this) {
			len += code.length();
		}
		return 0;
	}

	/** 
	 * ��Ʈ��ũ�� ������ ���� Binary�������� ��ȯ�ϱ�. 
	 * @param netOut ��ȯ�� ����Ÿ�� ������ ��Ʈ��ũ ��½�Ʈ��.
	 * @return ����� ����Ʈ ��
	 * @throws IOException
	 */
	public long pack(OutputStream netOut) throws IOException {
		netOut.write(VERSION1);
		write16bit(netOut, blockSize);
		write32bit(netOut, this.size());
		write32bit(netOut, estimatedLength());
		long written = 1 + 2 + 4 + 4;
		
		for (BuildCode code: this) {
			written += code.pack(netOut);
		}
		return written;
	}

	/**
	 * ��Ʈ��ũ�� ������ Binary �������κ��� BuildCodeList����� ����.
	 * @param netIn ���̳ʸ� ������ ����ִ� ��Ʈ��ũ �Է½�Ʈ��.
	 * @return �м��س� BuildCodeList
	 * @throws IOException
	 */
	public static final BuildCodeList unpack(InputStream netIn) throws IOException {
		int header = netIn.read();
		if (header != VERSION1) {
			throw new RuntimeException("unknown buildcode version");
		}
		int blockSize = read16bit(netIn);
		long count = read32bit(netIn);
		read32bit(netIn); // length�� �����ص� ��. 
		BuildCodeList bc = new BuildCodeList(blockSize, RemoteSync.DEFAULT_RAW_LIMIT);
		while (count-- > 0) {
			bc.add(BuildCode.unpack(netIn));
		}		
		return bc;
	}
	
}
