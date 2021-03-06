package net.daum.remotesync;

import static net.daum.remotesync.PackUtil.write16bit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * {@link SourceCodeList}와 목표파일의 차이값을 관리하는 단위 객체. 
 * 각각의 BuildCode는, 원본파일과 특정 블럭이 일치한다면, 해당 블럭의 인덱스값을 보관한다. 
 * 일치하지 않는 영역에 대해서는 바이트 배열을 그대로 보관한다. 
 * 
 * @author dante
 * @see BuildCodeList SourceCodeList
 */
abstract public class BuildCode implements Packable {
	
	/**
	 * 원본의 특정블럭과 일치하는 영역에 대한 코드 표현.
	 * @param index 일치하는 블럭의 인덱스
	 */
	public static BuildCode createRefCode(int index) {
		return new RefBuildCode(index);
	}

	/**
	 * 원본에서 찾을수 없는 데이타영역에 대한 코드표현.
	 * @param data 일치하지 않는 데이타 바이트 배열.
	 */
	public static BuildCode createRawCode(byte[] data) {
		return new RawBuildCode(data);
	}
	
	
	public static BuildCode unpack(InputStream in, OutputStream fileOut) throws Exception {
		int firstByte = in.read();
		if (fileOut != null) {
			fileOut.write(firstByte);
		}
		
		int secondByte = in.read();
		if (fileOut != null) {
			fileOut.write(secondByte);
		}
		
		int thirdByte = in.read();
		if (fileOut != null) {
			fileOut.write(thirdByte);
		}
		
		int headerData = firstByte << 16;
		headerData = headerData | secondByte << 8;
		headerData = headerData | thirdByte;
		
		int header = headerData & 0x00C00000;
		header = header >> 16;
		int data = headerData & 0x003FFFFF;
				
//		int header = first & 0xC0;
//		int data = ((first & 0x3F) << 16) | read16bit(in);
		if (header == RefBuildCode.HEADER) {
			return createRefCode(data);
		} else if (header == RawBuildCode.HEADER) {
			ByteArrayOutputStream dynamicBuf = new ByteArrayOutputStream();
			int readBytes = 0;
			while (true) {
				int r = in.read();
				if (fileOut != null) {
					fileOut.write(r);
				}
				
				if (r == -1) {
					break;
				} else if (readBytes < data) {
					dynamicBuf.write((byte)r);
					readBytes++;
				}
				
				if (readBytes == data) {
					break;
				}
			}
				
			if (readBytes != data) throw new RuntimeException("couldn't read enough bytes for the raw code");
			return createRawCode(dynamicBuf.toByteArray());
		} else {
			throw new RuntimeException("unknown header = " + header);
		}
	}

	protected OutputStream debugOut;
	
	/**
	 * 
	 * @return 빌드코드의 크기
	 */
	abstract public long length();
	
	/** 
	 * 원본파일을 참고해서, 목표파일 만들어내기.  
	 * @param src 원본파일을 읽을 수 있는 인터페이스
	 * @param blockSize 빌드코드를 생성하는데 사용한 블럭크기 
	 * @param out 목표파일이 저장될 OutputStream
	 * @return 실제 쓰여진 바이트 수
	 * @throws IOException
	 */
	abstract public long patch(SourceFileAccess src, int blockSize, OutputStream out) throws Exception;

	public void setDebugFile(OutputStream out) {
		debugOut = out;
	}
}


/**
 * 소스파일의 참조블럭을 가리키는 빌드코드. 
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

	public long pack(OutputStream out) throws Exception {
		int header = 0x80;
		out.write(header | ((index >>> 16) & 0x3F));
		write16bit(out, index & 0xFFFF);
		return 3;
	}
	
	@Override
	public long patch(SourceFileAccess src, int blockSize, OutputStream out) throws Exception {
		src.seek(index * blockSize);
		byte buf[] = new byte[blockSize];
		long r = src.read(buf);
		assert r == blockSize: "referenced block must have the block-sized length";
		out.write(buf);
		if (debugOut != null) {
			Signature s = new Signature(buf);
			debugOut.write(("" + getIndex()).getBytes());
			debugOut.write(": ".getBytes());
			debugOut.write(s.getStrongBase64().getBytes());
			debugOut.write("\n".getBytes());
		}
		return r;
	}

}

/**
 * 소스파일에서 찾을 수 없는 데이타를 표현하는 빌드코드.
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
		return "{raw:" + new String(content) + "}";
	}
	
	public long pack(OutputStream out) throws Exception {
		int header = 0x00;
		out.write(header | ((content.length >>> 16) & 0x3F));
		write16bit(out, content.length & 0xFFFF);
		out.write(content);
		return 3 + content.length;
	}

	@Override
	public long patch(SourceFileAccess src, int blockSize, OutputStream out) throws Exception {
		out.write(content);
		return content.length;
	}

}