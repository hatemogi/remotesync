package net.daum.remotesync;

/** 
 * 원본파일을 접근할 인터페이스. {@link BuildCodeList#patch}에서 사용한다. 
 * 파일내용의 특정위치를 seek하고, 바이트블럭을 읽을 수 있는 메소드를 구현해야한다. 
 *  
 * RandomFileAccess객체를 감싸서 사용해도 된다. Tenth파일 입력스트림을 연결해서 사용하면 된다. 
 * 
 * @author dante
 * @see BuildCodeList
 */
public interface SourceFileAccess {
	/** 
	 * 파일의 특정 오프셋으로 이동. 
	 */
	void seek(long offset);
	
	/**
	 * 현재 오프셋에서 블럭단위 읽기. 
	 * @param buf 읽은 블럭을 저장할 바이트 배열. 이 배열의 크기만큼 읽는다. 
	 * @return 실제로 읽은 바이트 수. 요청한 크기보다 작게 읽혔을 수 있다. 
	 */
	long read(byte[] buf);
}


/**
 * 바이트 배열을 이용한 참고 구현. 테스트케이스에서 사용함. 
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
