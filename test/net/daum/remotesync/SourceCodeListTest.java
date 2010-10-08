package net.daum.remotesync;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.daum.remotesync.Signature;
import net.daum.remotesync.SourceCodeList;

import org.junit.Before;
import org.junit.Test;

public class SourceCodeListTest extends TestUtils {
	private SourceCodeList sc;
	
	@Before
	public void setUp() throws Exception {
		sc = SourceCodeList.create(istream("0123456789ABCDEFGHI"), 3);
	}
	
	@Test
	public void testCreate() throws Exception {
		// 012 345 678 9AB CDE FGH I
		assertEquals(6, sc.size());
		assertEquals(Signature.fastSignature("012".getBytes()), sc.get(0).getFast());
		assertEquals("NROe+JSyi3O+oCJ1UWaiOTPH2cs=", sc.get(1).getStrongBase64());
	}
	
	@Test
	public void testPack() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		sc.pack(out);
		assertEquals(1+2+4+24*6, out.size());
		
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		assertEquals(SourceCodeList.VERSION1, in.read());
		assertEquals(0, in.read());
		assertEquals(3, in.read());
		assertEquals(0, in.read());
		assertEquals(0, in.read());
		assertEquals(0, in.read());
		assertEquals(6, in.read());
		
	}	
}
