/*
 * Copyright (C) 2011  Francois Forster
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tcpdelay;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class writing data read to the target.
 */
public class TCPDataWriter implements Runnable {
    private static Logger LOGGER = Logger.getLogger(TCPDataWriter.class.getName());

    private BlockingQueue<TCPData> queue;
    private final Utils _Utils = new Utils();

    public TCPDataWriter(BlockingQueue<TCPData> queue) {
        this.queue = queue;
    }

    /**
     * Pick items off the queue (returns when their delay is over).
     */
    public void run() {
        while (true) {
            TCPData data;
            try {
                data = queue.take();
            } catch (InterruptedException e) {
                continue;
            }

            SocketChannel pairSc = data.getChannel();
            ByteBuffer buffer = data.getBuffer();
            int len = data.getLen();
            boolean close = false;
            try {
                if (len < 0) {
                    close = true;
                } else {
                    buffer.flip();
                    int totalWritten = 0;
                    int written;
                    while (totalWritten < len) {
                        written = pairSc.write(buffer);
                        totalWritten += written;
                        Utils.logVarArgs(LOGGER, Level.FINE, "Wrote {0} bytes to {1}", written, pairSc);
                    }
                }
            } catch (Exception e) {
                close = true;
            } finally {
                if (close && pairSc != null) {
                    closeOutput(pairSc);
                }
            }
        }
    }

    private static void closeOutput(SocketChannel sc) {
        Socket socket = sc.socket();
        if (socket.isClosed()) {
            return;
        }
        if (socket.isInputShutdown()) {
            try {
                LOGGER.log(Level.FINE, "Closing {0}", sc);
                socket.close();
            } catch (Exception e) {
                // do nothing
            }
        } else if (!socket.isOutputShutdown()) {
            try {
                LOGGER.log(Level.FINE, "Closing output {0}", sc);
                socket.shutdownOutput();
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}
