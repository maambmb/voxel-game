package game.input;

import game.Component;
import game.Entity;

public class InputListenerComponent implements Component {

	public InputPriority priority;

	public InputListenerComponent( InputPriority priority ) {
		this.priority = priority;
	}

	public boolean canListen() {
		return Input.GLOBAL.canListen( this );
	}
	
	public void startListening() {
		Input.GLOBAL.addListener( this );
	}
	
	public void stopListening() {
		Input.GLOBAL.removeListener( this );
	}

	@Override
	public void setup(Entity e) {
	}

	@Override
	public void init() {
		
	}

	@Override
	public void destroy() {
		this.stopListening();
	}
	
	
	
}
