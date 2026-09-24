/*
 * Copyright © 2018 YanZhenjie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fabscreen.libraries.legacy.lib.fabserver;

import androidx.annotation.NonNull;

import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.ComponentRegister;
import com.yanzhenjie.andserver.util.Executors;

import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.io.Charsets;
import org.apache.httpcore.ExceptionLogger;
import org.apache.httpcore.config.ConnectionConfig;
import org.apache.httpcore.config.SocketConfig;
import org.apache.httpcore.impl.bootstrap.HttpServer;
import org.apache.httpcore.impl.bootstrap.ServerBootstrap;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class FabServer {
    public static Builder newBuilder() {
        return newBuilder("default");
    }

    public static Builder newBuilder(@NonNull String group) {
        return new Builder(group);
    }

    private final String mGroup;
    private final InetAddress mInetAddress;
    private final int mPort;
    private final int mTimeout;
    private final ServerListener mListener;
    private final ProgressListener mProgressListener;

    private HttpServer mHttpServer;
    private boolean isRunning;

    private FabServer(Builder builder) {
        this.mGroup = builder.group;
        this.mInetAddress = builder.inetAddress;
        this.mPort = builder.port;
        this.mTimeout = builder.timeout;
        this.mListener = builder.listener;
        this.mProgressListener = builder.progressListener;
    }

    /**
     * Server running status.
     *
     * @return return true, not return false.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Start the server.
     */
    public void startup() {
        if (isRunning) return;

        Executors.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                FabDispatcherHandler handler = new FabDispatcherHandler(AndServer.getContext());
                ComponentRegister register = new ComponentRegister(AndServer.getContext());

                register.register(handler, mGroup);
                handler.setProgressListener(mProgressListener);

                mHttpServer = ServerBootstrap.bootstrap()
                        .setSocketConfig(SocketConfig.custom()
                                .setSoKeepAlive(true)
                                .setSoReuseAddress(true)
                                .setSoTimeout(mTimeout)
                                .setTcpNoDelay(true)
                                .build())
                        .setConnectionConfig(
                                ConnectionConfig.custom().setBufferSize(4 * 1024).setCharset(Charsets.UTF_8).build())
                        .setLocalAddress(mInetAddress)
                        .setListenerPort(mPort)
                        .setServerInfo("AndServer/2.0.0")
                        .registerHandler("*", handler)
                        .setExceptionLogger(ExceptionLogger.NO_OP)
                        .create();
                try {
                    isRunning = true;
                    mHttpServer.start();

                    Executors.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null) mListener.onStarted();
                        }
                    });
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            mHttpServer.shutdown(3, TimeUnit.SECONDS);
                        }
                    });
                } catch (IOException e) {
                    Executors.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null) mListener.onException(e);
                        }
                    });
                }
            }
        });
    }

    /**
     * Quit the server.
     */
    public void shutdown() {
        if (!isRunning) return;

        Executors.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                if (mHttpServer != null) {
                    mHttpServer.shutdown(3, TimeUnit.MINUTES);
                    isRunning = false;
                    Executors.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null) mListener.onStopped();
                        }
                    });
                }
            }
        });
    }

    public static class Builder {

        private String group;
        private InetAddress inetAddress;
        private int port;
        private int timeout;
        private ServerListener listener;
        private ProgressListener progressListener;

        private Builder(String group) {
            this.group = group;
        }

        /**
         * Specified server need to monitor the ip address.
         */
        public Builder inetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
            return this;
        }

        /**
         * Specify the port on which the server listens.
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Connection and response timeout.
         */
        public Builder timeout(int timeout, TimeUnit timeUnit) {
            long timeoutMs = timeUnit.toMillis(timeout);
            this.timeout = (int) Math.min(timeoutMs, Integer.MAX_VALUE);
            return this;
        }

        /**
         * Set the server listener.
         */
        public Builder listener(ServerListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder progressListener(ProgressListener listener) {
            this.progressListener = listener;
            return this;
        }

        /**
         * Create a server.
         */
        public FabServer build() {
            return new FabServer(this);
        }
    }

    public interface ServerListener {

        /**
         * When the server is started.
         */
        void onStarted();

        /**
         * When the server stops running.
         */
        void onStopped();

        /**
         * An error occurred while starting the server.
         */
        void onException(Exception e);
    }
}
