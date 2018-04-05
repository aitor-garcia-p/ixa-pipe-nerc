package eus.ixa.ixa.pipe.nerc;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;

public class CustomRegexpNameFinderTest {

	@Test
	public void testRebuildTextFromKafTokens() throws Exception {
		List<WF>kafTokens=Lists.newArrayList(
				buildFakeKafToken("this", 0),
				buildFakeKafToken("is", 5),
				buildFakeKafToken("a", 8),
				buildFakeKafToken("super", 10),
				buildFakeKafToken("-", 15),
				buildFakeKafToken("test", 16),
				buildFakeKafToken(".", 20)
				);
		String expected="this is a super-test.";
		String actual=CustomRegexpNameFinder.rebuildTextFromKafTokens(kafTokens);
		assertEquals(expected, actual);
	}

	@Test
	public void testWhiteSpaces() throws Exception {
		assertEquals("", CustomRegexpNameFinder.whiteSpaces(-1));
		assertEquals("", CustomRegexpNameFinder.whiteSpaces(0));
		assertEquals(" ", CustomRegexpNameFinder.whiteSpaces(1));
		assertEquals("  ", CustomRegexpNameFinder.whiteSpaces(2));
		assertEquals("   ", CustomRegexpNameFinder.whiteSpaces(3));
		assertEquals("    ", CustomRegexpNameFinder.whiteSpaces(4));
	}

	private WF buildFakeKafToken(String form,int offset) {
		KAFDocument k=new KAFDocument("en", "N/A");
		return k.newWF(offset, form, 1);
	}
	
}
