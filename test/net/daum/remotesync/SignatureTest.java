package net.daum.remotesync;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.junit.Assert.assertEquals;
import net.daum.remotesync.Signature;

import org.junit.Test;


public class SignatureTest {
	@Test
	public void testFastSignature() {
		assertEquals(72942016, Signature.fastSignature("test".getBytes()));
		assertEquals(223150730, Signature.fastSignature("test1234".getBytes()));
	}
	
	public void assertStrongSignature(String expected, String content) {
		assertEquals(expected, printBase64Binary(Signature.strongSignature(content.getBytes())));	
	}
	
	@Test
	public void testStrongSignature() {
		assertStrongSignature("qUqP5cyxm6YcTAhz05Hph5gvu9M=", "test");
		assertStrongSignature("m8NFSdVl2VBbKH3gzSCsd74dPyw=", "test1234");

	}
	
}
