/*
 *  MaharaDroid -  Artefact uploader
 *
 *  This file is part of MaharaDroid.
 *
 *  Copyright [2010] [Catalyst IT Limited]
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  This file is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.catalyst.MaharaDroid;

import android.util.Log;

/**
 * Log configuration settings. Code that is guarded by a test of the
 * LogConfig.VERBOSE constant will be stripped out by the compiler if the test
 * can be determined at compile time to be false.<br/>
 * e.g.<br/>
 * <code>if (LogConfig.VERBOSE) Log.v(TAG, "a String to log");</code><br/>
 * will be stripped out by the compiler if LogConfig.VERBOSE is false.
 *
 * @author Grant Patterson (grant.patterson@catalyst.net.nz)
 */
public class LogConfig {

    /**
     * Whether the application should have all tags log at DEBUG level. When
     * this is true, all tags will log at DEBUG level. When this is false, only
     * tags that are explicitly enabled by the Android logging properties
     * mechanism (see android.util.Log.isLoggable()) will log at DEBUG level.<br/>
     * This should be false in release builds so that tags will only log at
     * DEBUG level when they have been explicitly told to through Android's
     * logging properties. Rather than checking this directly, classes should
     * use LogConfig.isDebug(TAG) to determine whether to log a DEBUG level.
     */
    private static final boolean DEBUG = true;

    /**
     * Whether the application should allow any tags to log at VERBOSE level.
     * Make sure this is set this to false in release builds so the compiler
     * will strip out VERBOSE log statements.<br/>
     * In general, classes should check this constant rather than checking
     * Log.isLoggable(TAG, Log.VERBOSE). The Java compiler is unable to strip
     * out calls to Log.isLoggable() (although, maybe the Dalvik compiler, or
     * the JIT can ???)
     */
    public static final boolean VERBOSE = true;

    private static final int MAX_LOG_TAG_LENGTH = 23;

    /**
     * When DEBUG logging isn't globally enabled for the application, this
     * method defers to Android's logging properties mechanism (see
     * android.util.Log.isLoggable()).
     *
     * @param logTag
     *            the log tag
     * @return whether the log tag should log at DEBUG level
     */
    public static boolean isDebug(String logTag) {

        return DEBUG || Log.isLoggable(logTag, Log.DEBUG);
    }

    public static String getLogTag(Class<?> clazz) {

        String tag = clazz.getSimpleName();

        if (tag.length() > MAX_LOG_TAG_LENGTH) {
            // trim tag
            StringBuilder trimmed = new StringBuilder(MAX_LOG_TAG_LENGTH);
            trimmed.append(tag.substring(0, MAX_LOG_TAG_LENGTH - 2));
            trimmed.append("..");

            tag = trimmed.toString();
        }

        return tag;
    }
}
