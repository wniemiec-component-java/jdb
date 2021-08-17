![](https://github.com/wniemiec-component-java/jdb/blob/master/docs/img/logo/logo.jpg)

<h1 align='center'>JDB</h1>
<p align='center'>Simple API for JDB (Java debugger).</p>
<p align="center">
	<a href="https://github.com/wniemiec-component-java/jdb/actions/workflows/windows.yml"><img src="https://github.com/wniemiec-component-java/jdb/actions/workflows/windows.yml/badge.svg" alt=""></a>
	<a href="https://github.com/wniemiec-component-java/jdb/actions/workflows/macos.yml"><img src="https://github.com/wniemiec-component-java/jdb/actions/workflows/macos.yml/badge.svg" alt=""></a>
	<a href="https://github.com/wniemiec-component-java/jdb/actions/workflows/ubuntu.yml"><img src="https://github.com/wniemiec-component-java/jdb/actions/workflows/ubuntu.yml/badge.svg" alt=""></a>
	<a href="https://codecov.io/gh/wniemiec-component-java/jdb"><img src="https://codecov.io/gh/wniemiec-component-java/jdb/branch/master/graph/badge.svg?token=R2SFS4SP86" alt="Coverage status"></a>
	<a href="http://java.oracle.com"><img src="https://img.shields.io/badge/java-8+-D0008F.svg" alt="Java compatibility"></a>
	<a href="https://mvnrepository.com/artifact/io.github.wniemiec-component-java/jdb"><img src="https://img.shields.io/maven-central/v/io.github.wniemiec-component-java/jdb" alt="Maven Central release"></a>
	<a href="https://github.com/wniemiec-component-java/jdb/blob/master/LICENSE"><img src="https://img.shields.io/github/license/wniemiec-component-java/jdb" alt="License"></a>
</p>
<hr />

## ❇ Introduction
The Java Debugger (JDB) is a simple command-line debugger. The objective of this project is to facilitate the use of this debugger without having to worry about creating a process, redirecting output, among others.

## ❓ How to use
1. Add one of the options below to the pom.xml file: 

#### Using Maven Central (recomended):
```
<dependency>
  <groupId>io.github.wniemiec-component-java</groupId>
  <artifactId>jdb</artifactId>
  <version>LATEST</version>
</dependency>
```

#### Using GitHub Packages:
```
<dependency>
  <groupId>wniemiec.component.java</groupId>
  <artifactId>jdb</artifactId>
  <version>LATEST</version>
</dependency>
```

2. Run
```
$ mvn install
```

3. Use it
```
[...]

import wniemiec.component.java.JDB;

[...]

Path workingDirectory = Path.of(".", "bin").toAbsolutePath().normalize();
Path sourcePath = workingDirectory
		.resolve(Path.of("..", "tests"))
		.normalize();
List&lt;Path> classpaths = List.of(workingDirectory);
List&lt;Path> sourcePaths = List.of(
		workingDirectory.relativize(sourcePath)
);
		
JDB jdb = new JDB.Builder()
		.workingDirectory(workingDirectory)
		.classPath(classpaths)
		.srcPath(sourcePaths)
		.build();
jdb.run().send("stop at api.jdb.testfiles.Calculator : 8")

String line = jdb.read();

// Runs until it reaches the breakpoint on line 8
while (!line.contains("Breakpoint")) {
	line = jdb.read();
	System.out.println(line);
}

jdb.send("step into");	// Enters the function

line = jdb.read();
System.out.println(line);

[...]
```

## 📖 Documentation
|        Property        |Parameter type|Return type|Description|Default parameter value|
|----------------|-------------------------------|-----|------------------------|--------|
|run |`void`|`JDB`|Initializes JDB in a new process| - |
|quit |`void`|`void`|Stops JDB process| - |
|forceQuit |`void`|`void`|Try to stops JDB process immediately| - |
|send |`command: String`|`JDB`|Sends a command to JDB. After calling this method, it is necessary to call `read()` for JDB to process the command| - |
|send | `commands: String...`|`void`|Sends a command to JDB. After calling this method, it is necessary to call `read()` for JDB to process the command| - |
|read | `void`|`String`|Reads JDB output. This method will block until some input is available, an I/O error occurs, or the end of the stream is reached| - |
|readAll | `void`|`List<String>`|Reads all available JDB output. This method will not block if no output is available| `` |
|isReady | `void`|`boolean`|Checks if there is an output available| - |
|waitFor | `void`|`void`|Causes the current thread to block until JDB is finished| - |
|isRunning | `void`|`boolean`|Checks whether JDB is running| - |

#### <a name="commands"></a> 🎮 Commands
Here are the main commands that can be sent to the debugger using the send method.

|        Command 	| Description|
|----------------|-------------------------------|
|`stop at <class_name>:<line_number>`|Sets up a breakpoint at a particular line number|
|`stop in <class_name>:<method_name_or_variable_name>`|Sets up a breakpoint on a particular method or on a particular variable|
|`clear`|Removes all breakpoints|
|`exit`|Closes JDB|
|`run`|The execution stops at the first line of the main method.|
|`step into`|Step to the next line of the code. If the next line of the code is a function call, then it enters the function by driving the control at the top line of the function.|
|`step over`|Step to the next line of the code. If the next line is a function call, it executes that function in the background and returns the result.|
|`cont`|Continues execution of the debugged application after a breakpoint, exception, or step. It will stop only if it passes through a breakpoint.|

## 🚩 Changelog
Details about each version are documented in the [releases section](https://github.com/williamniemiec/wniemiec-component-java/jdb/releases).

## 🤝 Contribute!
See the documentation on how you can contribute to the project [here](https://github.com/wniemiec-component-java/jdb/blob/master/CONTRIBUTING.md).

## 📁 Files

### /
|        Name        |Type|Description|
|----------------|-------------------------------|-----------------------------|
|dist |`Directory`|Released versions|
|docs |`Directory`|Documentation files|
|src     |`Directory`| Source files|

## See more
* [Oracle - jdb - The Java Debugger](https://docs.oracle.com/javase/7/docs/technotes/tools/windows/jdb.html)
* [Tutorials point - JDB quick guide](https://www.tutorialspoint.com/jdb/jdb_quick_guide.htm)
