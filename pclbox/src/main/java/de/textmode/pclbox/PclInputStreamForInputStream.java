package de.textmode.pclbox;

/*
 * Copyright 2017 Michael Knigge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


//import java.io.;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Implementation of {@link PclInputStream} for underlying {@link InputStream}s.
 * This implementation uses the {@link InputStream#reset()} and {@link InputStream#skip(long)}
 * to seek within the file.
 */
final class PclInputStreamForInputStream implements PclInputStream {

    //For files > 2 GB
    private final InputStream input;
    //For files < 2GB. Extremely faster than FileInputStream. Uses memory mapping.
    private final MappedByteBuffer mbb;
    //Only used for test cases or stdin (currently unimplemented). Faster than InputStream.
    private final ByteBuffer bb;
    private long position;

    static int getInputLength(InputStream inputStream) {
        try {
            if (inputStream instanceof FilterInputStream) {
                FilterInputStream filtered = (FilterInputStream)inputStream;
                Field field = FilterInputStream.class.getDeclaredField("in");
                field.setAccessible(true);
                InputStream internal = (InputStream) field.get(filtered);
                return getInputLength(internal);
            } else if (inputStream instanceof ByteArrayInputStream) {
                ByteArrayInputStream wrapper = (ByteArrayInputStream)inputStream;
                Field field = ByteArrayInputStream.class.getDeclaredField("buf");
                field.setAccessible(true);
                byte[] buffer = (byte[])field.get(wrapper);
                return Math.toIntExact(buffer.length);
            } else if (inputStream instanceof FileInputStream) {
                FileInputStream fileStream = (FileInputStream)inputStream;
                return Math.toIntExact(fileStream.getChannel().size());
            }
        } catch (NoSuchFieldException | IllegalAccessException | IOException exception) {
            // Ignore all errors and just return -1.
        }
        return -1;
    }

    /**
     * Constructor that is given the underlying {@link InputStream}.
     */
    PclInputStreamForInputStream(final InputStream input) {
        if (input.getClass().equals(FileInputStream.class)) {
            FileInputStream fis = (FileInputStream) input;
            try {
                if (fis.getChannel().size() < (Integer.MAX_VALUE - 1)) {
                    this.mbb = fis.getChannel().map(FileChannel.MapMode.READ_ONLY,0,fis.getChannel().size());
                } else {
                    this.mbb = null;

                }
                this.bb = null;
            }            catch (IOException e) {
                throw new RuntimeException(e);
            }
            //This is only necessary for passing the test classes.
            //The above file channel method is currently used always, as you cannot pass stdin via PCLDumper.
        } else if (input instanceof ByteArrayInputStream) {
            try (ReadableByteChannel channel = Channels.newChannel(input)) {
                this.bb = ByteBuffer.allocate(getInputLength(input));
                channel.read(this.bb);
                //have to flip to allow reading FROM instead of reading TO the ByteBuffer bb.
                this.bb.flip();
                this.mbb = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }         else {
            //Input was larger than 2GB, default to slow streaming.
            this.mbb = null;
            this.bb = null;

        }
        this.input = input;
        this.position = 0;
    }

    @Override
    public void close() throws IOException {
        if (this.bb != null) {
            //empty buffer.
            this.bb.clear();
            java.util.Arrays.fill(this.bb.array(), (byte) 0);
            this.bb.clear();
        }
        if (this.mbb != null) {
            //close channel.
            FileInputStream fis = (FileInputStream) this.input;
            fis.getChannel().close();
        }
        //close input.
        this.input.close();

    }

    @Override
    public int read() throws IOException {
        if (this.mbb != null) {
            if (this.mbb.remaining() > 0) {
                byte b = this.mbb.get();
                int result = b & 0xff;
                //if unsigned needed ...
                //int result=Byte.toUnsignedInt(b);
                ++this.position;
                return result;
            } else {
                return -1;
            }

        }  else if (this.bb != null) {
            if (this.bb.remaining() > 0) {
                byte b = this.bb.get();
                int result = b & 0xff;
                this.position = this.bb.position();
                return result;
            } else {
                return -1;
            }
        } else {
            final int result = this.input.read();
            if (result != -1) {
                ++this.position;
            }
            return result;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (this.mbb != null) {
            int remain = this.mbb.remaining();
            if (b.length <= this.mbb.remaining()) {
                this.mbb.get(b,0,b.length);
                final int result = (remain - this.mbb.remaining());
                this.position = this.position + result;
                return result;
            } else {
                for (int i = 0; i <= this.mbb.remaining(); i++) {
                    b[i] = this.mbb.get();
                }
                final int result = (remain - this.mbb.remaining());
                this.position = this.position + result;
                return result;
            }
        } else if (this.bb != null) {
            int remain = this.bb.remaining();
            if (b.length <= this.bb.remaining()) {
                this.bb.get(b,0,b.length);
                final int result = (remain - this.bb.remaining());
                this.position = this.position + result;
                return result;
            } else {
                for (int i = 0; i <= this.bb.remaining(); i++) {
                    b[i] = this.bb.get();
                }
                final int result = (remain - this.bb.remaining());
                this.position = this.position + result;
                return result;

            }

        }        else {
            final int result = this.input.read(b);
            if (result != -1) {
                this.position = this.position + result;
            }
            return result;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.mbb != null) {
            int remain = this.mbb.remaining();
            if (b.length <= this.mbb.remaining()) {
                this.mbb.get(b,off,len);
                final int result = (remain - this.mbb.remaining());
                this.position = this.position + result;
                return result;
            } else {
                for (int i = off; i <= len; i++) {
                    b[i] = this.mbb.get();
                }
                final int result = (remain - this.mbb.remaining());
                this.position = this.position + result;
                return result;
            }
        }        else if (this.bb != null) {
            int start = this.bb.remaining();
            if (this.bb.remaining() >= b.length) {
                this.bb.get(b,off,len);
                final int result = start - this.bb.remaining();
                this.position = this.position + result;
                return result;
            } else {
                return -1;
            }

        }        else {
            final int result = this.input.read(b, off, len);
            if (result != -1) {
                this.position = this.position + result;
            }
            return result;
        }
    }

    @Override
    public void seek(long offset) throws IOException {
        if (!this.input.markSupported()) {
            throw new IOException(new StringBuilder()
                    .append("Repositioning with the PCL data stream is not supported for input streams of type ")
                    .append(this.input.getClass().getSimpleName())
                    .toString());
        }
        if (this.mbb != null) {
            this.mbb.position(0);
            this.position = 0; // after reset() we are at offset 0
            //buffers are capped at max int, so this conversion is fine.
            int offvalue = Math.toIntExact(offset);
            if (this.mbb.position() + offvalue < this.mbb.limit()) {
                this.mbb.position(this.mbb.position() + offvalue);  // now at offset returned from skip
            } else {
                if ( offvalue <= this.mbb.limit()) {
                    this.mbb.position(offvalue);
                }                 else {
                    throw new IOException(new StringBuilder()
                            .append("An error occurred when trying to position to offset ")
                            .append(offset)
                            .toString());

                }
            }
            this.position = this.mbb.position(); //update positioning variable
        }        else if (this.bb != null) {
            //This entire section is only used for test classes. Streaming input is not used otherwise.
            //note - Unlike reset, this does not create a mark unless one already exists.
            this.bb.position(0);
            this.position = 0; // after reset() we are at offset 0
            //Int conversion can be done unchecked since buffers can never exceed max_integer.
            int offvalue = Math.toIntExact(offset);
            if (this.bb.position() + offvalue < this.bb.limit()) {
                //This is what skip's intuitive behavior is and how it works if not at end of stream
                this.bb.position(this.bb.position() + offvalue);//now at the offset returned from skip
            }             else {
                /* mimicking the original behavior in an explicit, safer way. Originally, relied on
                this.position=stream.skip(offset);

                Javadocs explicitly state that stream.skip's behavior is unpredictable. stream.Skip tries to skip N.
                It will never error, no matter the conditions. It may skip less than N bytes, or skip 0 bytes.

                Expected behavior -
                If inputstream.skip(offset) exceeds the end of the stream, then it will read as many bytes as possible
                from the stream, returning the amount read. 0 or -1 are returned only if read is at end of the stream.

                Problem -
                Skip can return 0 for other reasons - so stream.skip is not fully equivalent.
                IE, these two following lines are mostly but not entirely equivalent. They differ at end of streams.
                this.input.skip(offset)
                this.bb.position(this.bb.position() + offvalue)

                -Problem presentation
                Bytebuffer position(offset) will instead always throw an exception if the offset is invalid (ie, past
                end of buffer's limit).
                Adding to the issue is that this.bb.position(0) does not always work as expected.

                 */
                if ( offvalue <= this.bb.limit()) {
                    /* Solution
                    Instead, we position the stream to the offset value, without considering the current position.
                    This lets us act as though the position were 0 even if it did not successfully move to that position
                    This prevents buffer over/under flow errors.

                    If no movement is possible, throw an error.

                    This mimics the actual behavior of this.input.skip(offset).
                    This holds true for any bytebuffer.

                    Documentation says explicitly to not rely on skip's results for IS, and to code your own version.
                    */
                    this.bb.position(offvalue);
                }                 else {
                    throw new IOException(new StringBuilder()
                            .append("An error occurred when trying to position to offset ")
                            .append(offset)
                            .toString());

                }
            }
            this.position = this.bb.position(); //update positioning variable
        }         else {
            this.input.reset();
            this.position = 0; // after reset() we are at offset 0
            this.position = this.input.skip(offset); // now we are at the offset returned from skip

            if (this.position != offset) {
                throw new IOException(new StringBuilder()
                        .append("An error occurred when trying to position to offset ")
                        .append(offset)
                        .toString());
            }
        }
    }

    @Override
    public long tell() throws IOException {
        return this.position;
    }
}
