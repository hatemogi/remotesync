/**
 * 원격 파일 동기화 라이브러리. 네트워크 양끝단의 파일의 내용을 동기화할 때 사용하며, 
 * 최신본을 갖고 있는 쪽에서는 원본과 최신본의 차이점만을 전송하고, 원본을 갖고 있는 쪽에서는
 * 해당 차이점만으로 최신본을 만들어 낼 수 있다. 
 * 
 * 상세 내용은 {@link net.daum.remotesync.SourceCodeList}의 설명을 참조.
 * 
 * @author dante
 */

package net.daum.remotesync;
