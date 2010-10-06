package nz.co.catalyst.MaharaDroid;

/**
 * Copyright (c) 2008 Mark S. Kolich
 * http://mark.kolich.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
*/

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class JavaMD5Sum {
	
	public static final String MD5_ALGORITHM = "MD5";
	
	public static void main ( String [] args ) {
		try {
			System.out.println( "string 1 = " + computeSum("string 1") );
			System.out.println( "string 2 = " + computeSum("string 2") );
			System.out.println( "string 3 = " + computeSum("string 3") );
		} catch (NoSuchAlgorithmException e) {
			System.err.print( e.getStackTrace() );
		}
	}

	/**
	 * Uses Java to compute the MD5 sum of a given input String.
	 * @param input
	 * @return
	 */
	public static final String computeSum(String input)
		throws NoSuchAlgorithmException {

		if (input == null) {
			throw new IllegalArgumentException("Input cannot be null!");
		}

		StringBuffer sbuf = new StringBuffer();
		MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
		byte [] raw = md.digest(input.getBytes());
		
		for (int i = 0; i < raw.length; i++) {
			int c = (int) raw[i];
			if (c < 0) {
				c = (Math.abs(c) - 1) ^ 255;
			}
			String block = toHex(c >>> 4) + toHex(c & 15);
			sbuf.append(block);
		}
		
		return sbuf.toString();
		
	}

	private static final String toHex(int s) {
		if (s < 10) {
			return new StringBuffer().append((char)('0' + s)).toString();
		} else {
			return new StringBuffer().append((char)('A' + (s - 10))).toString();
		}
	}

}
