package net.daum.remotesync;

/** 
 * 패키지 상수값.
 * @author dante
 */
public interface RemoteSync {
//	int DEFAULT_BLOCK_SIZE = 8192;
//	int DEFAULT_BLOCK_SIZE = 4096;
	int DEFAULT_BLOCK_SIZE = 1024;
	long DEFAULT_RAW_LIMIT = (1 << 22) - 1;
	String DEFAULT_FILE_INTEGRITY_CHECK_METHOD = "SHA-1";
}
