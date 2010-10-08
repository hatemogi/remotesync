package net.daum.remotesync;

/** 
 * ���������� ������ �������̽�. {@link BuildCodeList#patch}���� ����Ѵ�. 
 * ���ϳ����� Ư����ġ�� seek�ϰ�, ����Ʈ���� ���� �� �ִ� �޼ҵ带 �����ؾ��Ѵ�. 
 *  
 * RandomFileAccess��ü�� ���μ� ����ص� �ȴ�. Tenth���� �Է½�Ʈ���� �����ؼ� ����ϸ� �ȴ�. 
 * 
 * @author dante
 * @see BuildCodeList
 */
public interface SourceFileAccess {
	/** 
	 * ������ Ư�� ���������� �̵�. 
	 */
	void seek(long offset);
	
	/**
	 * ���� �����¿��� ������ �б�. 
	 * @param buf ���� ���� ������ ����Ʈ �迭. �� �迭�� ũ�⸸ŭ �д´�. 
	 * @return ������ ���� ����Ʈ ��. ��û�� ũ�⺸�� �۰� ������ �� �ִ�. 
	 */
	long read(byte[] buf);
}


/**
 * ����Ʈ �迭�� �̿��� ���� ����. �׽�Ʈ���̽����� �����. 
 * @author dante
 *
 */
class ByteArraySourceFileAccess implements SourceFileAccess {
	private long pos = 0;
	private byte[] content;
	
	ByteArraySourceFileAccess(byte[] content) {
		this.content = content.clone();
		System.out.println("ByteArraySourceFileAccess = " + new String(this.content));
	}
	
	@Override
	public void seek(long pos) {
		assert pos <= content.length;
		this.pos = pos;
	}

	@Override
	public long read(byte[] buf) {
		if (pos >= content.length) return 0;
		int length = (int)Math.min(buf.length, content.length - pos);
		System.arraycopy(content, (int)pos, buf, 0, length);
		pos += length;
		return length;
	}
	
}
