package net.daum.remotesync;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

class TestUtils {
	protected InputStream istream(String s) {
		return new ByteArrayInputStream(s.getBytes());
	}
}
