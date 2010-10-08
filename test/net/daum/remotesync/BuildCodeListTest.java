package net.daum.remotesync;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import net.daum.remotesync.BuildCodeList;
import net.daum.remotesync.ByteArraySourceFileAccess;
import net.daum.remotesync.RawBuildCode;
import net.daum.remotesync.RefBuildCode;
import net.daum.remotesync.SourceCodeList;

import org.junit.Test;


public class BuildCodeListTest extends TestUtils {
	@Test
	public void testCreate() throws Exception {
		SourceCodeList sc = SourceCodeList.create(istream("0123456789"), 3);
		BuildCodeList bc = sc.generateBuildCodes(istream("A012B34C56789345DEFGH"), 4);

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
		final String src = "0123456789";
		final String dst = "A012B34C56789345DEFGH";
		SourceCodeList sc = SourceCodeList.create(istream(src), 3);
		BuildCodeList bc = sc.generateBuildCodes(istream(dst), 4);
	
		ByteArraySourceFileAccess sfa = new ByteArraySourceFileAccess(src.getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		long r = bc.patch(sfa, out);
		System.out.println("patched = " + out.toString());
		System.out.println("len = " + out.size());
		assertEquals(r, dst.length());
		assertEquals(dst.length(), out.size());
		assertEquals(dst, out.toString());
	}

}
