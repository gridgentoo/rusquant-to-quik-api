package ru.rusquant.connection.pipe;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import ru.rusquant.connection.pipe.exceptions.PipeErrorSource;
import ru.rusquant.connection.pipe.exceptions.PipeErrorUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *    Java representation of the Windows Named Pipe
 *    Class takes care for all native logic to work with client-side of the windows named pipe
 *    Author: Aleksey Kutergin <aleksey.v.kutergin@gmail.ru>
 *    Company: Rusquant
 *    \\\\.\\pipe\\RusquantToQuikPipe
 */
public class WindowsNamedPipe implements Closeable
{
	static
	{
		System.setProperty("jna.encoding", "Cp1251");
	}

	private static final WindowsNamedPipeAPI KERNEL32_INSTANCE = (WindowsNamedPipeAPI) Native.loadLibrary("kernel32", WindowsNamedPipeAPI.class, W32APIOptions.UNICODE_OPTIONS);

	private static final int IO_BUFFER_SIZE = 4 * 1024;

	private static final int INITIAL_RETRY_COUNT = 20;

	private String pipeName;

	private String errorMessage;

	private WinNT.HANDLE handle = WinNT.INVALID_HANDLE_VALUE;

	public WindowsNamedPipe(String pipeName)
	{
		this.pipeName = pipeName;
	}


	public String getErrorMessage()
	{
		return errorMessage;
	}


	public Boolean connect()
	{
		if(this.pipeName == null || this.pipeName.isEmpty()) return Boolean.FALSE;
		if(handle != WinNT.INVALID_HANDLE_VALUE) return Boolean.TRUE;

		for(int retryCount = INITIAL_RETRY_COUNT; retryCount > 0; retryCount--)
		{
			this.handle = KERNEL32_INSTANCE.CreateFile(this.pipeName, Kernel32.GENERIC_READ | Kernel32.GENERIC_WRITE, 0, null, Kernel32.OPEN_EXISTING, 0, null);
			int errorCode = KERNEL32_INSTANCE.GetLastError();

			if(handle == WinNT.INVALID_HANDLE_VALUE)
			{
				errorMessage = PipeErrorUtils.getErrorMessageByErrorCode(PipeErrorSource.CONNECT, pipeName, errorCode);
			}
			else
			{
				errorMessage = null;
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}



	public void disconnect() throws IOException
	{
		if(!handle.equals(WinNT.INVALID_HANDLE_VALUE))
		{
			boolean result = KERNEL32_INSTANCE.CloseHandle(handle);
			int errorCode = KERNEL32_INSTANCE.GetLastError();
			handle = WinNT.INVALID_HANDLE_VALUE;

			if(!result)
			{
				throw PipeErrorUtils.getErrorByErrorCode(PipeErrorSource.DISCONNECT, pipeName, errorCode);
			}
		}
	}



	public void write(String message) throws IOException
	{
		if(message == null || message.length() == 0) return;

		WinBase.OVERLAPPED overlapped = new WinBase.OVERLAPPED();
		ByteBuffer buffer = ByteBuffer.allocate(message.length() + 1);
		IntByReference bytesWritten = new IntByReference();

		buffer.put(message.getBytes());
		buffer.put((byte) 0);

		boolean isIOOperationSuccess = KERNEL32_INSTANCE.WriteFile(handle, buffer.array(), buffer.capacity(), bytesWritten, overlapped);
		int lastError = KERNEL32_INSTANCE.GetLastError();

		if(isIOOperationSuccess)
		{
			KERNEL32_INSTANCE.FlushFileBuffers(handle);
		}
		else
		{
			if(lastError == WinNT.ERROR_IO_PENDING)
			{
				while(overlapped.Internal.intValue() == WinNT.ERROR_IO_PENDING) {}
				KERNEL32_INSTANCE.FlushFileBuffers(handle);
			}
			else
			{
				throw PipeErrorUtils.getErrorByErrorCode(PipeErrorSource.WRITE, pipeName, lastError);
			}
		}
	}


	public String read() throws IOException
	{
		WinBase.OVERLAPPED overlapped = new WinBase.OVERLAPPED();
		ByteBuffer buffer = ByteBuffer.allocate(IO_BUFFER_SIZE);
		IntByReference bytesRead = new IntByReference(buffer.capacity());

		int lastError;
		if (KERNEL32_INSTANCE.PeekNamedPipe(handle, buffer, buffer.capacity(), bytesRead, null, null))
		{
			boolean isIOOperationSuccess = KERNEL32_INSTANCE.ReadFile(handle, buffer, buffer.capacity(), bytesRead, overlapped);
			lastError = KERNEL32_INSTANCE.GetLastError();
			if(!isIOOperationSuccess)
			{
				if(lastError == WinNT.ERROR_IO_PENDING || lastError == WinNT.ERROR_MORE_DATA)
				{
					while(overlapped.Internal.intValue() == WinNT.ERROR_IO_PENDING || overlapped.Internal.intValue() == WinNT.ERROR_MORE_DATA) {}
				}
				else
				{
					throw PipeErrorUtils.getErrorByErrorCode(PipeErrorSource.READ, pipeName, lastError);
				}
			}
			if(bytesRead.getValue() > 0) { return new String(buffer.array(), 0, bytesRead.getValue()); }
		}
		else
		{
			lastError = KERNEL32_INSTANCE.GetLastError();
			throw PipeErrorUtils.getErrorByErrorCode(PipeErrorSource.READ, pipeName, lastError);
		}

		return null;
	}


	public void close() throws IOException
	{
		this.disconnect();
	}
}
