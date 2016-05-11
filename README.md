### Abstract
A common, ligthweight and maintenance-able daemon scheduled task execution server. 

### File Instruction
- server.sh : It uses the `java` dev tool to run the `Boostrap.java` code directly.
- startup.sh : It wrappers the `server.sh` to launch a task server `process`.
- run.sh : It's a init-style script to launch the `startup.sh` script according the prompt input.

### Precondition of execution
- Creates a `lib` folder and copies all dependency jars to this folder. 
  Or through `mvn assembly:assembly` to get single jar with all dependencies.
- Creates a `logs` folder for logging output.

### How To Run
Generally, you just need following input in the shell:<br/>
`$ sh run.sh` <br/>
`$ input the listening port & scheduler period`
