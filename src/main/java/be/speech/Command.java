package be.speech;

public class Command {
	
	private String name;
	private String program;
	
	public Command(String name, String program) {
		setName(name);
		setProgram(program);
	}
	
	public String getName() {
		return name.toLowerCase();
	}
	public void setName(String name) {
		this.name = name.toLowerCase();
	}
	public String getProgram() {
		return program;
	}
	public void setProgram(String program) {
		this.program = program;
	}

}
