package net.daum.remotesync;

/** 
 * ��Ű�� �����.
 * @author dante
 */
public interface RemoteSync {
	int DEFAULT_BLOCK_SIZE = 4096;
	long DEFAULT_RAW_LIMIT = (1 << 24) - 1;
}
