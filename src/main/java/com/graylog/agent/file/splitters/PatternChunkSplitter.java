package com.graylog.agent.file.splitters;

import com.google.common.collect.AbstractIterator;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Charsets.UTF_8;

public class PatternChunkSplitter extends ContentSplitter {

    public static final String CK_SPLITTER_PATTERN = "patternsplitter_pattern";

    private Pattern pattern;

    @Override
    public void configure(ContentSplitterConfiguration configuration) {
        final String regex = configuration.getString(CK_SPLITTER_PATTERN);
        pattern = Pattern.compile(regex, Pattern.MULTILINE);
    }

    @Override
    public Iterable<String> split(final ByteBuf buffer, final Charset charset, final boolean includeRemainingData) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new AbstractIterator<String>() {
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
                                buffer.skipBytes(substring.getBytes(UTF_8).length); // TODO performance
                                return substring;
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
                        return channelBuffer.toString(charset);
                    }
                };
            }
        };
    }
}
