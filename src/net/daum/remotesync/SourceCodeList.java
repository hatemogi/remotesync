package net.daum.remotesync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static net.daum.remotesync.PackUtil.*;


/**
 * ���� ���Ϸ� ���� �����, ���� �ؽ��ڵ� ����Ʈ�� ArrayList�� ���� {@link Signature}�� ��� �ִ�.
 * �� ��ü�� �������� {@link BuildCodeList}�� ���� �� �ְ�, BuildCodeList�� SourceFile�� ������, 
 * ��ǥ������ �����س� �� �ִ�. 
 * <ol>
 * <li>SourceFile => SourceCodeList</li>
 * <li>SourceCodeList + TargetFile => {@link BuildCodeList}</li>
 * <li>{@link BuildCodeList} + SourceFile => TargetFile</li>
 * </ol>
 * 
 * <p>
 * ��, �� ���������� ������ ����ȭ�� ��, �糡�ܿ����� ������ ���ϳ����� �𸣴� ���¿���, �ּ����� ������ 
 * �ְ������ �ֽź��� ������ ����� �� �� �ִ�. 
 * </p>
 * 
 * <p>
 * �������, machineA�� �������� srcFile�� �����ϰ� �ְ�, machineB�� ���ο� ���� newFile�� �ø��� ��Ȳ�� ���캸��. 
 * (���� Ŭ���̾�Ʈ�̰� ������������, ���� �ֽź��� �����ִ����� �߿��ϴ�.)
 * </p>
 * 
 * �̶�, ������ ����ȭ�ϱ� ���� ������ ������ ����. 
 * <ol>
 * <li>machineA�� srcFile�� ���� SourceCodeList�� �����ؼ� machineB���� �����Ѵ�. 
 * <li>machineB�� ���޹��� SourceCodeList�� newFile�� ������ �������� {@link BuildCodeList}�� �ۼ��� machineA���� �����Ѵ�. 
 * <li>machineA�� machineB�κ��� ���� {@link BuildCodeList}�� srcFile�� ������ ������ newFile�� �����س���. 
 * <li>�̷ν�, machineA�� machineB�� newFile�� ���� ������ ������ ���� �ȴ�. 
 * </ol>
 * 
 * <p>
 * ��, machineA�� machineB�� �ְ�޴� ����Ÿ�� SourceCodeList�� BuildCodeList���̸�, srcFile�� newFile��
 * ������ ��ġ�κ��� ���� ���� �ش絥������ ũ��� �۾�����. ũ�Ⱑ ������ ���� ����, machineB�� newFile�� ����
 * ��ü�� machineA���� �����ϴ� ��쿡 ���� ��Ʈ��ũ �ۼ��� ����Ÿ ũ�Ⱑ �۾�����. 
 * </p>
 * <p>
 * ��Ʈ��ũ�� �ۼ���ó���� ���� {@link #pack}/{@link #unpack} �޼ҵ带 �����ߴ�. 
 * </p>
 * <p>
 * �ش� �˰����� Rsync�� ���� ���� �״�� �����ߴ�.  
 * </p>
 * @author dante
 * @see Signature
 */

public class SourceCodeList extends ArrayList<Signature> {
	public static final int VERSION1 = 0x71;
	
	private static final long serialVersionUID = -5885331537042946256L;
	private int blockSize = RemoteSync.DEFAULT_BLOCK_SIZE;
	
	private SourceCodeList() {}
	
	private SourceCodeList(int blockSize) {
		this.blockSize = blockSize;
	}
	
	/**
	 * �������Ϸ� ���� �ؽ��ڵ帮��Ʈ�� �����Ѵ�. ���������� ������ ��ũ�⸸ŭ ������ 
	 * {@link Signature}�� �����ؼ� �����Ѵ�.  
	 * @param in ���������� InputStream
	 * @param blockSize ��ũ�� (4096) 
	 * @return ���� {@link Signature}��ü�� �߰��� {@link SourceCodeList}
	 * @throws IOException
	 */
	public static final SourceCodeList create(InputStream in, int blockSize) throws IOException {
		SourceCodeList sc = new SourceCodeList(blockSize);
		byte[] buf = new byte[blockSize];
		int r = 0;
		while ((r = in.read(buf)) > 0) {
			if (r == blockSize) {
				sc.add(new Signature(buf));
			} 
		}
		return sc;		
	}

	/**
	 * �ؽ��ڵ带 �����ϴµ� ���� ��ũ��. ���� �۾��� �������� �ȴ�. 
	 * @return ��ũ�� ����Ʈ ��
	 */
	public int getBlockSize() {
		return blockSize;
	}

	
	/**
	 * ��Ʈ��ũ���� ������ binary data�� ���� SourceCodeList �����. 
	 * @param in pack���� ����� binary data (�Ƹ��� ��Ʈ��ũ�� �������� ��Ʈ��)
	 * @return �ش� ����Ÿ���� ����� SourceCodeList.
	 * @throws IOException
	 */
	public static final SourceCodeList unpack(InputStream in) throws IOException {
		int version = in.read();
		if (version != VERSION1) {
			throw new RuntimeException("SOURCE_CODES Version mismatch");
		}
		int blockSize = read16bit(in);
		long count = read32bit(in);
		SourceCodeList sc = new SourceCodeList(blockSize);
		for (long i = 0; i < count; i++) {
			sc.add(Signature.unpack(in));
		}
		return sc;
	}
	
	/**
	 * ��Ʈ��ũ�� �۽��ϱ� ���� binary data �����.
	 * @param out ���̳ʸ� ����Ÿ�� �� OutputStream (�ַ�, ��Ʈ��ũ �ƿ�ǲ��Ʈ��)
	 * @return �� ����Ʈ �� 
	 * @throws IOException
	 */
	public long pack(OutputStream out) throws IOException {
		out.write(VERSION1);
		write16bit(out, blockSize);
		write32bit(out, this.size());
		long written = 1 + 2 + 4;
		for (Signature sign: this) {
			write32bit(out, sign.getFast());
			out.write(sign.getStrong());
			written += 24;
		}
		return written;
	}
	
	/**
	 * ��ǥ������ �о, �ҽ��ڵ���� ������ �м�.
	 * @param newFileIn ��ǥ������ InputStream
	 * @param rawLimit ��ġ���� �ʴ� �κ��� �ִ�ũ��. ���� binary ������ ��������, �ִ� 4MB�̸����� ǥ���� �� �ִ�.  
	 *                 �ִ�ũ���̻��� �κ��� �߰ߵǸ�, �ΰ��̻��� �����ڵ�� �ɰ��� �����Ѵ�.
	 * @return ������ {@link BuildCodeList}
	 * @throws IOException
	 */
	public BuildCodeList generateBuildCodes(InputStream newFileIn, long rawLimit) throws IOException {
		return BuildCodeList.create(this, newFileIn, rawLimit);
	}
	
	/**
	 * ��ǥ������ �о, �ҽ��ڵ���� ������ �м�. ��ġ���� �ʴ� �κ��� �ִ�ũ��� 4MB - 1byte�� �ִ밪 ����.
	 * @param targetFileIn ��ǥ���� InputStream
	 * @return ������ {@link BuildCodeList}
	 * @throws IOException
	 */
	public BuildCodeList generateBuildCodes(InputStream targetFileIn) throws IOException {
		return generateBuildCodes(targetFileIn, RemoteSync.DEFAULT_RAW_LIMIT);
	}
	
}
