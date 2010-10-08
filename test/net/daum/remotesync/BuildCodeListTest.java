package net.daum.remotesync;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;


public class BuildCodeListTest extends TestUtils {
	final String src = "0123456789";
	final String dst = "A012B34C56789345DEFGH";
	private BuildCodeList bc;
	
	@Before
	public void setUp() throws Exception {
		SourceCodeList sc = SourceCodeList.create(istream(src), 3);
		bc = sc.generateBuildCodes(istream(dst), 4);		
	}
	@Test
	public void testCreate() throws Exception {
		System.out.println("build codes = " + bc);

		/*
		 * 012 345 678 9
		 * A 012 B34C 5 678 9 345 DEFG H
		 */
		assertEquals(9, bc.size());
		int i = 0;
		assertEquals(RawBuildCode.class, bc.get(i++).getClass());
		assertEquals(RefBuildCode.class, bc.get(i++).getClass());
		assertEquals(RawBuildCode.class, bc.get(i++).getClass());
		assertEquals(RawBuildCode.class, bc.get(i++).getClass());
		assertEquals(RefBuildCode.class, bc.get(i++).getClass());
		assertEquals(RawBuildCode.class, bc.get(i++).getClass());		
		assertEquals(RefBuildCode.class, bc.get(i++).getClass());
		assertEquals(RawBuildCode.class, bc.get(i++).getClass());
		assertEquals(RawBuildCode.class, bc.get(i++).getClass());
	}

	
	@Test
	public void testPatchFromSourceFile() throws Exception {
		ByteArraySourceFileAccess sfa = new ByteArraySourceFileAccess(src.getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		long r = bc.patch(sfa, out);
		System.out.println("patched = " + out.toString());
		System.out.println("len = " + out.size());
		assertEquals(r, dst.length());
		assertEquals(dst.length(), out.size());
		assertEquals(dst, out.toString());
	}
	
	
	@Test
	public void testPackAndUnpack() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bc.pack(out);
		
		// A 012 B34C 5 678 9 345 DEFG H
		assertEquals((1+2+4+4) + (4+3+7+4+3+4+3+7+4), out.size());
	
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		/* pack test */ {
			assertEquals(BuildCodeList.VERSION1, in.read());
			assertEquals(0, in.read());
			assertEquals(3, in.read());
			assertEquals(0, in.read());
			assertEquals(0, in.read());
			assertEquals(0, in.read());
			assertEquals(9, in.read());
			assertEquals(0, in.read());
			assertEquals(0, in.read());
			assertEquals(0, in.read());
			assertEquals(39, in.read());
		}
		
		/* unpack test */ {
			in.reset();
			BuildCodeList unpacked = BuildCodeList.unpack(in);
			assertEquals(9, unpacked.size());
			assertEquals(RefBuildCode.class, bc.get(4).getClass());
			assertEquals(2, ((RefBuildCode)bc.get(4)).getIndex());
		}
		
	}	

}
