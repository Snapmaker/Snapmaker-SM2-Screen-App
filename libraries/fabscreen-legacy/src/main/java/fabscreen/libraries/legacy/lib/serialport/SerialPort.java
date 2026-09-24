/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package fabscreen.libraries.legacy.lib.serialport;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {
    static {
        System.loadLibrary("serial-port");
    }

    private static final String DEFAULT_SU_PATH = "/system/xbin/su";

    private static void runCommand(String cmd) throws  IOException, InterruptedException {
        if (!cmd.endsWith("\n")) {
            cmd = cmd + "\n";
        }

        // First use must request superuser
        Process process = Runtime.getRuntime().exec(DEFAULT_SU_PATH);
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        os.writeBytes(cmd);
        os.writeBytes("exit\n");
        os.flush();
        os.close();
        process.waitFor();
    }

    // Do not remove or rename the field mFd: it is used by native method close();
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudrate, int flags) throws IOException, InterruptedException {
		// Check access permission
        if (!device.canRead() || !device.canWrite()) {
            runCommand("chmod 777 " + device.getAbsolutePath());
        }

        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    public SerialPort(String devicePath, int baudrate, int flags) throws IOException, InterruptedException {
        this(new File(devicePath), baudrate, flags);
    }

    public SerialPort(File device, int baudrate) throws IOException, InterruptedException {
        this(device, baudrate, 0);
    }

    public SerialPort(String devicePath, int baudrate) throws IOException, InterruptedException {
        this(new File(devicePath), baudrate, 0);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    // JNI
    private native static FileDescriptor open(String path, int baudrate, int flags);

    public native void close();
}
