/*
Copyright (c) 2001 Lapo Luchini.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS
OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;

// #sijapp cond.if modules_ZLIB is "true" #
import jimm.JimmException;
import protocol.net.TcpSocket;

public final class ZInputStream {
    private TcpSocket in;
    private ZBuffers buffers = new ZBuffers();
    private Inflate inflate;
    private static final int bufsize = 512;
    private byte[] buf = new byte[bufsize];

    private int inflateInit(int w, boolean nowrap) {
        inflate = new Inflate();
        return inflate.inflateInit(nowrap ? -w : w, buffers);
    }

    public ZInputStream(TcpSocket in) {
        this.in = in;
        inflateInit(Inflate.MAX_WBITS, false);
        buffers.next_in = buf;
        buffers.next_in_index = 0;
        buffers.avail_in = 0;
    }

    private boolean nomoreinput = false;

    public int read(byte[] b) throws JimmException {
        int off = 0;
        int len = b.length;
        if (0 == len) {
            return 0;
        }
        ZBuffers buffer = buffers;
        int err;
        buffer.next_out = b;
        buffer.next_out_index = off;
        buffer.avail_out = len;
        do {
            if ((0 == buffer.avail_in) && !nomoreinput) {
                // if buffer is empty and more input is avaiable, refill it
                buffer.next_in_index = 0;

                int avail = in.available();
                while (0 == avail) {
                    try { Thread.sleep(70); } catch (Exception e) {};
                    avail = in.available();
                }
                buffer.avail_in = in.read(buf, 0, Math.min(avail, buf.length));


                if (-1 == buffer.avail_in) {
                    buffer.avail_in = 0;
                    nomoreinput = true;
                }
            }
            try {
                inflate.inflate(buffer);
            } catch (ZError ex) {
                throw new JimmException(120, 8);
            }
            err = inflate.getErrCode();

            if (JZlib.Z_BUF_ERROR == err) {
                if (nomoreinput) {
                    return -1;
                }
                throw new JimmException(120, 8);
            }
            if ((nomoreinput || JZlib.Z_STREAM_END == err) && (buffer.avail_out == len)) {
                return -1;
            }
        } while (buffer.avail_out == len && JZlib.Z_OK == err);

        return len - buffer.avail_out;
    }

    /**
     * We can't count available data size after zlib processing
     * so available() always returns internal buffer size
     */
    public int available() throws JimmException {
        return (0 < in.available()) ? bufsize : 0;
    }

    public void close() {
        in = null;
    }
}
// #sijapp cond.end #
