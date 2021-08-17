/**
 * Copyright (c) William Niemiec.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package wniemiec.component.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import wniemiec.io.java.ArgumentFile;
import wniemiec.util.java.StringUtils;
import wniemiec.task.java.ProcessManager;

/**
 * Simple API for JBD (Java debugger).
 */
public class JDB {
	
	//-------------------------------------------------------------------------
	//		Attributes
	//-------------------------------------------------------------------------
	private Process process;
	private ProcessBuilder processBuilder;
	private JDBInput in;
	private JDBOutput out;
	
	
	//-------------------------------------------------------------------------
	//		Constructors
	//-------------------------------------------------------------------------
	/**
	 * Creates API for JDB.
	 * 
	 * @param		workingDirectory Directory that will serve as a reference
	 * for the execution of the process
	 * @param		classPath Class path that will be used in JDB
	 * @param		srcPath Source path that will be used in JDB
	 * @param		classSignature Class signature to begin debugging
	 * @param		classArgs Arguments passed to the main() method of classSignature
	 */
	private JDB(Path workingDirectory, String classPath, String srcPath, 
              String classSignature, String classArgs) {
		if (classPath == null)
			throw new IllegalStateException("Class path cannot be empty");
		
		if (srcPath == null)
			throw new IllegalStateException("Source path cannot be empty");

		classArgs = (classArgs == null) ? "" : classArgs;
		classSignature = (classSignature == null) ? "" : classSignature;
		
		processBuilder = new ProcessBuilder(
			"jdb",
				"-sourcepath", srcPath.replaceAll("\\s", "%20"),
				"-classpath", classPath.replaceAll("\\s", "%20"),
				classSignature, 
				classArgs
		);
		
		if (workingDirectory != null)
			processBuilder.directory(workingDirectory.toFile());
	}
	
	
	//-------------------------------------------------------------------------
	//		Builder
	//-------------------------------------------------------------------------
	public static class Builder	{
		
		private Path argumentFile; 
		private Path workingDirectory;
		private List<Path> classPath;
		private List<Path> srcPath;
		private String classSignature;
		private String classArgs;
		
		public Builder argumentFile(Path argumentFile) {
			this.argumentFile = argumentFile;
			
			return this;
		}
		
		public Builder workingDirectory(Path workingDirectory) {
			this.workingDirectory = workingDirectory;
			
			return this;
		}
		
		public Builder classPath(List<Path> classPath) {
			this.classPath = classPath;
			
			return this;
		}
		
		public Builder srcPath(List<Path> srcPath) {
			this.srcPath = srcPath;
			
			return this;
		}
		
		public Builder classSignature(String classSignature) {
			this.classSignature = classSignature;
			
			return this;
		}
		
		public Builder classArgs(String classArgs) {
			this.classArgs = classArgs;
			
			return this;
		}
		
		/**
		 * Creates JDB with provided information. It is  necessary to provide all
		 * required fields. The required fields are: <br>
		 * <ul>
		 * 	<li>Class path</li>
		 * 	<li>Source path</li>
		 * </ul>
		 * 
		 * @return		JDB with provided information
		 * 
		 * @throws		IllegalArgumentException If any required field is null
		 */
		public JDB build() {
			if (classPath == null)
				classPath = new ArrayList<>();
			
			if (srcPath == null)
				srcPath = new ArrayList<>();
			
			createArgumentFileFromClassPath();
			
			if (argumentFile == null) {
				return new JDB(
						workingDirectory, 
						StringUtils.implode(relativizePaths(classPath), File.pathSeparator), 
						StringUtils.implode(relativizePaths(srcPath), File.pathSeparator), 
						classSignature, 
						classArgs
				);
			}
			else {
				return new JDB(
						workingDirectory, 
						"@" + argumentFile, 
						StringUtils.implode(relativizePaths(srcPath), File.pathSeparator), 
						classSignature, 
						classArgs
				);
			}
		}
		
		private void createArgumentFileFromClassPath() {
			ArgumentFile argFile = new ArgumentFile(
				Path.of(System.getProperty("java.io.tmpdir")),
				"argfile-jdb"
			);

			try {
				argumentFile = argFile.create(classPath);
			} 
			catch (IOException e) {
				argumentFile = null;
			}
		}	
		
		private List<Path> relativizePaths(List<Path> paths) {
			if (paths == null) 
				return new ArrayList<>();
			
			List<Path> relativizedClassPaths = new ArrayList<>();
			Path relativizedPath;
			
			for (int i = 0; i < paths.size(); i++) {
				if (paths.get(i).isAbsolute())
					relativizedPath = workingDirectory.relativize(paths.get(i));
				else
					relativizedPath = paths.get(i);
					
				relativizedClassPaths.add(i, relativizedPath);
			}
			
			return relativizedClassPaths;
		}
	}
	
	
	//-------------------------------------------------------------------------
	//		Methods
	//-------------------------------------------------------------------------
	/**
	 * Initializes JDB in a new process.
	 * 
	 * @return		Itself in order to allow chained calls
	 * 
	 * @throws		IOException If JDB cannot be initialized 
	 */
	public JDB run() throws IOException {
		initializeJDB();
		onShutdown();
		
		return this;
	}

	private void initializeJDB() throws IOException {
		process = processBuilder.start();
		out = new JDBOutput(process);
		in = new JDBInput(process);
	}

	private void onShutdown() {
		try {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					if (in != null)
						in.close();
					
					if (out != null)
						out.close();
					
					if (process != null)
						process.destroyForcibly();
				}
			});
		}
		catch (IllegalStateException e) {
			// Ignores any errors that occur when closing the process.
		}
	}

	/**
	 * Stops JDB process.
	 * 
	 * @throws		InterruptedException If the current thread of JDB process is 
	 * interrupted by another thread while it is stopping.
	 */
	public void quit() throws InterruptedException {
		stopStreams();
		
		if (process != null) {
			process.destroy();
			process.waitFor();
		}
	}
	
	private void stopStreams() {
		if (in != null)
			in.close();
		
		if (out != null)
			out.close();
	}
	
	/**
	 * Try to stops JDB process immediately.
	 * 
	 * @throws		IOException If the current thread of JDB process is interrupted
	 * by another thread while it is stopping.
	 */
	public void forceQuit() throws IOException {
		stopStreams();
		ProcessManager.getInstance().forceKillProcessWithPid(process.pid());
	}
	
	/**
	 * Sends a command to JDB. After calling this method, it is necessary to call
	 * {@link #read()} for JDB to process the command.
	 * 
	 * @param		command Command that will be sent to JDB
	 * 
	 * @return		Itself in order to allow chained calls.
	 * 
	 * @throws		IllegalStateException If input is closed
	 */
	public JDB send(String command) {
		if (in == null)
			throw new IllegalStateException("Input is closed");
		
		in.send(command);
		
		return this;
	}
	
	/**
	 * Sends commands to JDB. After calling this method, it is necessary to call
	 * {@link #read()} for JDB to process these commands.
	 * 
	 * @param		commands Commands that will be sent to JDB
	 * 
	 * @return		Itself in order to allow chained calls.
	 */
	public JDB send(String... commands) {
		if (in == null)
			return this;
		
		in.send(commands);
		
		return this;
	}
	
	/**
	 * Reads JDB output. This method will block until some input is
	 * available, an I/O error occurs, or the end of the stream is reached.
	 * 
	 * @return		JDB output
	 */
	public String read() {
		if (out == null)
			return "";
		
		try {
			return out.read();
		} 
		catch (IOException e) {
			return "";
		}
	}
	
	/**
	 * Reads all available JDB output. This method will not block if no output is
	 * available.
	 * 
	 * @return		List of read JDB output
	 * 
	 * @throws		IOException If it cannot read JDB output
	 * @throws		IllegalStateException If output is closed
	 */
	public List<String> readAll() throws IOException {
		if (out == null)
			return new ArrayList<>();
		
		return out.readAll();
	}
	
	/**
	 * Checks if there is an output available. 
	 * 
	 * @return		True if {@link #read()} is guaranteed not to block if
	 * called; otherwise, returns false
	 */
	public boolean isReady() {
		if (out == null)
			return false;
		
		return out.isReady();
	}
	
	/**
	 * Causes the current thread to block until JDB is finished.
	 * 
	 * @throws		InterruptedException If the current thread is interrupted 
	 * by another thread while it is waiting
	 */
	public void waitFor() throws InterruptedException {
		if (process == null)
			return;
		
		process.waitFor();
	}
	
	/**
	 * Checks whether JDB is running.
	 * 
	 * @return		True if JDB is running; false otherwise
	 */
	public boolean isRunning() {
		return (process != null) && process.isAlive();
	}
}
