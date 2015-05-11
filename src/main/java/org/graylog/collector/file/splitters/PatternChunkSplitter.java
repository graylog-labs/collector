/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector.file.splitters;

import com.google.common.base.Charsets;
import com.google.common.collect.AbstractIterator;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternChunkSplitter extends ContentSplitter {

    private Pattern pattern;

    public PatternChunkSplitter(String pattern) {
        this.pattern = Pattern.compile(pattern, Pattern.MULTILINE);
    }

    @Override
    public Iterable<String> split(final ByteBuf buffer, final Charset charset, final boolean includeRemainingData) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new AbstractIterator<String>() {
                    // TODO Might throw an exception if multibyte charset is used and buffer is not complete.
                    //      Use CharsetDecoder to create a CharBuffer and match on that!
                    private final String inputAsString = buffer.toString(charset);
                    final Matcher matcher = pattern.matcher(inputAsString);
                    private int positionInString = 0;

                    @Override
                    protected String computeNext() {
                        try {
                            if (!buffer.isReadable()) {
                                return endOfData();
                            }
                            if (matcher.find()) {
                                int firstByte = matcher.start();
                                if (firstByte == 0) {
                                    // advance further, the buffer begins with our pattern.
                                    if (matcher.find()) {
                                        firstByte = matcher.start();
                                    } else {
                                        if (!includeRemainingData) {
                                            // couldn't find the end of the entry (i.e. there wasn't a next line yet)
                                            return endOfData();
                                        } else {
                                            // couldn't find another line, but we are asked to finish up, include everything that remains
                                            return getRemainingContent();
                                        }
                                    }
                                }
                                if (firstByte == 0) {
                                    // still haven't found a non-zero length string, keep waiting for more data.
                                    return endOfData();
                                }
                                final String substring = inputAsString.substring(positionInString, firstByte);
                                positionInString = firstByte;
                                buffer.skipBytes(substring.getBytes(charset).length); // TODO performance
                                return asUtf8String(substring);
                            } else {
                                if (includeRemainingData) {
                                    return getRemainingContent();
                                }
                                return endOfData();
                            }
                        } catch (IllegalStateException e) {
                            // the cause contains the CharacterCodingException from the ChannelBuffer.toString() methods
                            // this usually means the buffer ended with an incomplete encoding of a unicode character.
                            // WHY U SO SUCK CHARACTER ENCODINGS?
                            // we need to wait until more data is available
                            return endOfData();
                        } finally {
                            buffer.discardReadBytes();
                        }
                    }

                    private String getRemainingContent() {
                        final ByteBuf channelBuffer = buffer.readBytes(buffer.readableBytes());
                        return asUtf8String(channelBuffer.toString(charset));
                    }

                    private String asUtf8String(String string) {
                        return new String(string.getBytes(Charsets.UTF_8));
                    }
                };
            }
        };
    }
}
