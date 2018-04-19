package eus.ixa.ixa.pipe.nerc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import ixa.kaflib.WF;
import opennlp.tools.util.Span;

/**
 * Custom class to detect and add named entities based on custom regular
 * expressions. Note that this class does not implement the NameFinder
 * interface, because it needs extra params (non-tokenized text). It would be
 * too cumbersome to add a new method overload to the interface and all the
 * implementing classes just for that...
 * 
 * @author agarciap
 *
 */
public class CustomRegexpNameFinder {

	private final Map<Pattern,String> compiledRegexps;

	/**
	 * Reads a file with one regexp per line, tab separated from the entity type it
	 * detects. For example: aaabbbccc TAB email
	 * 
	 * @param pathToRegexpsFile
	 */
	public CustomRegexpNameFinder(String pathToRegexpsFile) {
		super();
		this.compiledRegexps = Maps.newHashMap();
		File regexpsFile=new File(pathToRegexpsFile);
		try {
			List<String> lines = FileUtils.readLines(regexpsFile, StandardCharsets.UTF_8);
			loadCompiledRegexpsMap(lines);

		} catch (Exception e) {
			System.err.println("Error loading custom regular expression file at: " + regexpsFile.getAbsolutePath());
			System.err.println(e.getClass()+":"+e.getMessage());
			//e.printStackTrace();
		}

	}
	
	public CustomRegexpNameFinder(List<String>tabSeparatedRagexpsAndEntityTypes) {
		this.compiledRegexps = Maps.newHashMap();
		loadCompiledRegexpsMap(tabSeparatedRagexpsAndEntityTypes);

	}
	
	private void loadCompiledRegexpsMap(List<String>tabSeparatedRagexpsAndEntityTypes) {
		for (String line : tabSeparatedRagexpsAndEntityTypes) {
			if(line.trim().isEmpty()) {
				//skip empty lines
				continue;
			}
			try {
				// line format: regexp TAB entity_type
				String[] regexpAndType = line.split("\t");
				Pattern compiledRegexp = Pattern.compile(regexpAndType[0].trim());
				String type = regexpAndType[1];
				this.compiledRegexps.put(compiledRegexp,type);
			} catch (Exception e) {
				// print a message, but ignore any error and continue
				System.err.println("Error parsing regexp from custom regexps file: " + e.getMessage());
			}
		}
	}

	public Span[] nercToSpans(List<WF> kafTokens) {
		List<Span>spans=Lists.newArrayList();
		String rebuiltText=rebuildTextFromKafTokens(kafTokens);
		//System.out.println("Rebuilt text: "+rebuiltText);
		for(Entry<Pattern,String> entry:compiledRegexps.entrySet()) {
			Matcher m=entry.getKey().matcher(rebuiltText);
			while(m.find()) {
				int start = m.start();
				int end=m.end();
				Span span=buildSpan(start, end, kafTokens, entry.getValue());
				spans.add(span);
			}
		}		
		return spans.toArray(new Span[spans.size()]);
	}
	
	//utility methods, they can be safely static
	protected static String rebuildTextFromKafTokens(List<WF>kafTokens) {
		StringBuilder sb=new StringBuilder();
		int currentOffset=0;
		for(WF kafToken:kafTokens) {
			int offsetDiff=kafToken.getOffset()-currentOffset;
			//System.out.println("Token: "+kafToken.getForm()+" Offset diff: "+offsetDiff);
			sb.append(whiteSpaces(offsetDiff));
			sb.append(kafToken.getForm());
			currentOffset=kafToken.getOffset()+kafToken.getForm().length();
		}
		return sb.toString();
	}
	
	protected static String whiteSpaces(int n) {
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<n;i++) {
			sb.append(' ');
		}
		return sb.toString();
	}
	
	/**
	 * Given character offsets of a match, find the corresponding tokens, and use their indexes to build a Span
	 * @param chStart
	 * @param chEnd
	 * @param kafTokens
	 * @return
	 */
	protected static Span buildSpan(int chStart, int chEnd, List<WF>kafTokens, String entityTpe) {
		int startToken=-1;
		int endToken=-1;
		for(int tokenIndex=0;tokenIndex<kafTokens.size();tokenIndex++) {
			WF kafToken=kafTokens.get(tokenIndex);
			if (tokenInRange(chStart, chEnd, kafToken)) {
				if(startToken<0) {
					startToken=tokenIndex;
				}
				endToken=tokenIndex;
			}
		}
		//System.out.println("SPAN: "+startToken+" , "+endToken);
		//add a +1 to the endToken number, later a copyRange will we done
		Span span=new Span(startToken, endToken+1, entityTpe);
		return span;
	}
	
	protected static boolean tokenInRange(int chStart, int chEnd, WF token) {
		return token.getOffset()>=chStart && token.getOffset()+token.getForm().length()<=chEnd;
	}

}
