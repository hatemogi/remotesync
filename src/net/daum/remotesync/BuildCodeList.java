package net.daum.remotesync;

import static net.daum.remotesync.PackUtil.read16bit;
import static net.daum.remotesync.PackUtil.read32bit;
import static net.daum.remotesync.PackUtil.write16bit;
import static net.daum.remotesync.PackUtil.write32bit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;

import net.daum.disk.file.handler.stream.StreamHandler;
import net.daum.disk.file.utils.StreamReadResult;

class ReferenceTable {
	Map<Integer, Map<String, Integer>> table;
	
	protected ReferenceTable() {}
	
	ReferenceTable(SourceCodeList sc) {
		table = new HashMap<Integer, Map<String, Integer>>(65535);
		int idx = 0;
		for (Signature sign: sc) {
			Map<String, Integer> inner = table.get(sign.getFast());
			if (inner == null) {
				inner = new HashMap<String, Integer>(3);
				table.put(sign.getFast(), inner);
			}
			inner.put(new String(sign.getStrong()), idx++);
		}
	}
	
	int lookup(RollingSignature sign) {
		Map<String, Integer> inner = table.get(sign.getFast());
		if (inner == null) return -1;
		Integer i = inner.get(new String(sign.getStrong()));
		if (i == null) return -1;
		return i;
	}
}

class ArrayReferenceTable extends ReferenceTable {
	Object[] table;
	Integer[] index;
	
	ArrayReferenceTable(SourceCodeList sc) {
		table = new Object[sc.size()];
		index = new Integer[65536];
		int idx = 0;
		for (Signature sign: sc) {
			Object[] inner = new Object[3];
			inner[0] = new Integer(sign.getFast());
			inner[1] = new String(sign.getStrong());
			inner[2] = new Integer(idx);
			table[idx++] = inner;
		}
		Arrays.sort(table, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				Object[] s1 = (Object[])o1;
				Object[] s2 = (Object[])o2;
				return ((Integer)s1[0] & 0xFFFF) - ((Integer)s2[0] & 0xFFFF);
			}}
		);
		int prevValue = -1;
		for (int i = 0; i < table.length; i++) {
			int value = (Integer)(((Object[])table[i])[0]) & 0xFFFF;
			if (prevValue != value) {
				index[value] = i;
				prevValue = value;
			}
		}
	}
	
	int lookup(RollingSignature sign) {
		int base = sign.getFast() & 0xFFFF;
		Integer idx = index[base];
		if (idx == null) return -1;
		while (idx < table.length) {
			Object[] o = (Object[])table[idx];
			int fast = (Integer)o[0];
			if (fast == sign.getFast() && ((String)o[1]).equals(new String(sign.getStrong()))) {
				return (Integer)o[2];
			}
			if ((fast & 0xFFFF) != base) return -1;
			idx++;
		}
		return -1;
	}
}

/**
 * {@link SourceCodeList}와 목표파일(newFileIn)을 토대로 생성한 빌드코드 리스트. 
 * 이 객체와 원본파일의 내용을 합쳐서, 전체내용을 전송받지 않고도, 목표파일(newFile)을 
 * 만들어 낼 수 있다.
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
	private static final int HEADER_SIZE = 1 + 2 + 4 + 4;
	
	private int blockSize = RemoteSync.DEFAULT_BLOCK_SIZE;
	private long rawLimit = RemoteSync.DEFAULT_RAW_LIMIT;
	
	private BuildCodeList() {}
	
	private BuildCodeList(int blockSize, long rawLimit) throws Exception {
		this.blockSize = blockSize;
		this.rawLimit = rawLimit;
	}
	

	/**
	 * 블럭크기. SourceCodeList의 블럭크기와 같아야한다. 
	 */
	public int getBlockSize() {
		return blockSize;
	}

	private long processRawCodeIfNeeded(ByteArrayOutputStream rawbuf) {
		if (rawbuf.size() > 0) {
			BuildCode bc = BuildCode.createRawCode(rawbuf.toByteArray());
			add(bc);
			rawbuf.reset();
			return bc.length();
		}
		return 0;
	}
	
	private long addByteToRawBuf(ByteArrayOutputStream rawbuf, byte c) {
		long size = 0;
		if (rawbuf.size() >= rawLimit) {
			size = processRawCodeIfNeeded(rawbuf);
		}		
		rawbuf.write(c);
		return size;
	}
	
	private long addBytesToRawBuf(ByteArrayOutputStream rawbuf, byte[] buf, int length) {
		long size = 0;
		for (int i = 0; i < length; i++) {
			size += addByteToRawBuf(rawbuf, buf[i]);
		}
		return size;
	}
	

	/**
	 * {@link SourceCodeList}와 목표파일(newFileIn)을 이용해 BuildCodeList 생성하기. 
	 * @param sourceCodeList 
	 * @param newFileIn 목표파일의 InputStream
	 * @param rawLimit 바이너리 포맷을 위해 제한된 값. 최대 (4M-1)의 기본값을 사용할 수 있다. 
	 * @return 생성된 BulidCodeList
	 * @throws IOException
	 */
	static final BuildCodeList create(SourceCodeList sourceCodeList, InputStream newFileIn, long rawLimit, boolean oversizeCheck) throws Exception {
		final int blockSize = sourceCodeList.getBlockSize();
		BuildCodeList bc = new BuildCodeList(blockSize, rawLimit);
		
		ReferenceTable table = new ReferenceTable(sourceCodeList);
		// 외부에서 이미 Buffered처리 되어있다면, 다시 할 필요 없습니다. 
		InputStream in = newFileIn;

		ByteArrayOutputStream rawbuf = new ByteArrayOutputStream();
		RollingSignature sign = new RollingSignature();

		byte[] buf = new byte[blockSize];
		long startTimestamp = System.currentTimeMillis();
		long estimatedSize = HEADER_SIZE;
		StreamReadResult readResult = new StreamReadResult();
		
		StreamHandler.readIntoBufferBuffered(in, buf, blockSize, readResult);
		int r = readResult.readCount;
//		int r = in.read(buf);
		long readSize = r;
		if (r < blockSize) {
			estimatedSize += bc.addBytesToRawBuf(rawbuf, buf, r);
		} else {
			sign.init(buf);
			while (true) {
				// 현재 lookup부분이 create수행시간의 20%를 차지. 성능개선을 한다면 lookup중점적으로 필요.
				int idx = table.lookup(sign);
				if (idx < 0) {
					// no match
					r = in.read();
					if (r < 0) {
						estimatedSize += bc.addBytesToRawBuf(rawbuf, sign.getContent(), blockSize);
						break;
					} else {
						readSize++;
						estimatedSize += bc.addByteToRawBuf(rawbuf, sign.getFirst());						
					}
					sign.roll((byte)r);
				} else {
					// match
					estimatedSize += bc.processRawCodeIfNeeded(rawbuf);
					BuildCode c = BuildCode.createRefCode(idx);
					bc.add(c);
					estimatedSize += c.length();
					
					StreamHandler.readIntoBufferBuffered(in, buf, blockSize, readResult);
					r = readResult.readCount;
					
					readSize += r;
					if (r < blockSize || readResult.noMoreToRead()) {
						bc.addBytesToRawBuf(rawbuf, buf, r);
						break;
					}
					sign.init(buf);
				}
				
				if (System.currentTimeMillis() - startTimestamp > 3000) {
//					throw RemoteSyncException.TIMEOUT;
				}
				
			}
		}
		bc.processRawCodeIfNeeded(rawbuf);
		if (oversizeCheck && estimatedSize > readSize) throw RemoteSyncException.OVERSIZE;
		return bc;
	}

	
	public long patch(SourceFileAccess src, OutputStream newFileOut) throws Exception {
		long written = 0;
		OutputStream out = null; // had been here for debug purpose.
		for (BuildCode bc: this) {
			bc.setDebugFile(out);
			written += bc.patch(src, blockSize, newFileOut);
		}
		
		if (out != null) {
			out.close();
		}
		return written;
	}
	
	private long estimatedLength() {
		long len = 0;
		for (BuildCode code: this) {
			len += code.length();
		}
		return len;
	}

	/** 
	 * 네트워크로 보내기 위해 Binary포맷으로 변환하기. 
	 * @param netOut 변환한 데이타를 저장할 네트워크 출력스트림.
	 * @return 출력한 바이트 수
	 * @throws IOException
	 */
	public long pack(OutputStream netOut) throws Exception {
		netOut.write(VERSION1);
		write16bit(netOut, blockSize);
		write32bit(netOut, this.size());
		write32bit(netOut, estimatedLength());
		long written = HEADER_SIZE;
		
		for (BuildCode code: this) {
			written += code.pack(netOut);
		}
		return written;
	}

	/**
	 * 네트워크로 수신한 Binary 포맷으로부터 BuildCodeList만들어 내기.
	 * @param netIn 바이너리 포맷이 담겨있는 네트워크 입력스트림.
	 * @return 분석해낸 BuildCodeList
	 * @throws IOException
	 */
	public static final BuildCodeList unpack(InputStream netIn, OutputStream fileOut) throws Exception {
		int header = netIn.read();
		if (fileOut != null) {
			fileOut.write(header);
		}
		if (header != VERSION1) {
			throw new RuntimeException("unknown buildcode version");
		}
		
		int blockSize = read16bit(netIn, fileOut);
		long count = read32bit(netIn, fileOut);
		
		read32bit(netIn, fileOut); // length는 무시해도 됨. 
		BuildCodeList bc = new BuildCodeList(blockSize, RemoteSync.DEFAULT_RAW_LIMIT);
		while (count-- > 0) {
			bc.add(BuildCode.unpack(netIn, fileOut));
		}		
		return bc;
	}
	
}
