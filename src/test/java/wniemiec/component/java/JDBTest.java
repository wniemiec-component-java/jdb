package wniemiec.component.java;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class JDBTest {
	
	//-------------------------------------------------------------------------
	//		Attributes
	//-------------------------------------------------------------------------
	private List<String> commands;
	private String testMethodClassSignature;
	private int invocationLine;
	private boolean displayMessages;
	
	
	//-------------------------------------------------------------------------
	//		Constructor
	//-------------------------------------------------------------------------
	public JDBTest() {
		testMethodClassSignature = "wniemiec.component.java.testfiles.Calculator";
		invocationLine = 8;
		displayMessages = false;
	}

	
	//-------------------------------------------------------------------------
	//		Tests
	//-------------------------------------------------------------------------
	@Test
	public void test() throws IOException, InterruptedException {
		JDB jdb = initializeJDB();
		
		String line = stopAfterBreakpoint(jdb);
		assertTrue(line.contains("8    		sum(2, 3);"));

		jdb.quit();
	}


	//-------------------------------------------------------------------------
	//		Methods
	//-------------------------------------------------------------------------
	private JDB initializeJDB() {
		Path workingDirectory = Path.of(".", "target", "test-classes").normalize().toAbsolutePath();
		
		List<Path> classpaths = List.of(workingDirectory);
		Path sourcePath = workingDirectory
				.resolve(Path.of("..", "..", "src", "test", "java"))
				.normalize();
		List<Path> sourcePaths = List.of(
				workingDirectory.relativize(sourcePath)
		);
		
		return new JDB.Builder()
				.workingDirectory(workingDirectory)
				.classPath(classpaths)
				.srcPath(sourcePaths)
				.build();
	}
	
	private String stopAfterBreakpoint(JDB jdb) throws IOException {
		jdb.run().send(buildInitCommand());
		
		String line = jdb.read();

		while (!isBreakpoint(line) && jdb.isRunning()) {
			line = jdb.read();
			displayMessage(line);
		}
		
		jdb.send("cont");
		
		line = jdb.read();
		displayMessage(line);
		
		return line;
	}
	
	private String[] buildInitCommand() {
		commands = new ArrayList<>();
		
		clearBreakpoints();
		initializeBreakpoint();
		initializeRunClass();
		
		return commands.toArray(new String[] {});
	}
	
	private void clearBreakpoints() {
		commands.add("clear");
	}
	
	private void initializeRunClass() {
		StringBuilder command = new StringBuilder();
		
		command.append("stop at");
		command.append(" ");
		command.append(testMethodClassSignature);
		command.append(":");
		command.append(invocationLine);
		
		commands.add(command.toString());
	}

	private void initializeBreakpoint() {
		StringBuilder command = new StringBuilder();
	
		command.append("run");
		command.append(" ");
		command.append(testMethodClassSignature);
		
		commands.add(command.toString());
	}
	
	private boolean isBreakpoint(String line) {
		return line.contains("Breakpoint");
	}
	
	private void displayMessage(String message) {
		if (!displayMessages)
			return;

		System.out.println(message);
	}
}
