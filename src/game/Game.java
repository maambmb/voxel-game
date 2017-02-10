package game;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import game.block.BlockShader;
import game.block.World;
import game.gfx.TextureRef;
import game.gui.FontMap;
import game.gui.GUIDepth;
import game.gui.GUIShader;
import game.gui.Glyph;
import game.gui.console.DebugConsole;
import game.input.InputCapturer;
import game.input.Key;
import util.Vector3fl;
import util.Vector3in;

public class Game {

    public class DestroyMessage {}

    public static class UpdateMessage {

        public long totalMs;
        public long deltaMs;

        public UpdateMessage( long prevTime, long currTime ) {
            this.deltaMs = currTime - prevTime;
            this.totalMs = currTime;
        }

    }

    private boolean gameOver;
    private long prevTime;
    private long currTime;

    private void createCtx() {
        ContextAttribs attribs = new ContextAttribs( Config.OPENGL_VERSION, Config.LWJGL_VERSION )
            .withForwardCompatible( true )
            .withProfileCore( true );
        try {
            Display.setDisplayMode( new DisplayMode( Config.GAME_WIDTH, Config.GAME_HEIGHT ) );
            Display.create( new PixelFormat(), attribs );
        } catch( Exception e ) {
            throw new RuntimeException( "Unable to create display" );
        }
    }

    private void updateCtx() {
        Display.sync( Config.FPS );
        Display.update();
        if( Display.isCloseRequested() )
            this.gameOver = true;
    }
    
    private void destroyCtx() {
        Display.destroy();
    }

    public void run() {

    	// grab the mouse to prevent mouse from leaving window
        Mouse.setGrabbed( true );

        // set up game ctx
        this.createCtx();

        // record the start of the game
        this.prevTime = System.currentTimeMillis();

        // create the global msg listener
        Listener.init();

        // initialize all enums that need it
        Vector3in.CubeNormal.init();
        Key.init();
        TextureRef.init();
        Glyph.init();
        FontMap.init();
        
        // create the world object (manager of chunks)
        World.init();

        // create remaining global entities
        InputCapturer.init();
        BlockShader.init();
        GUIShader.init();
        Camera.init();
        DebugConsole.init();
        Environment.init();

        // create the random block spawner (for test purposes)
        new RandomBlockSpawner();

        // loop until we detect a close (done in updateCtx)
        while( !this.gameOver ) {
        	
        	// trigger game over if escape key is pressed
            if( InputCapturer.GLOBAL.isKeyDown( Key.KEY_ESCAPE ))
            	this.gameOver = true;
            // if backtick key is pressed, toggle the debug console
            if( InputCapturer.GLOBAL.isKeyPressed( Key.KEY_GRAVE ))
            	DebugConsole.GLOBAL.toggle();

            // take a new reading of the time, and create an update message with this and the previous reading
            // (to form a delta timestep) - then set this new reading as the previous one so we're
            // ready for the next loop iteration
            this.currTime = System.currentTimeMillis();
            Listener.GLOBAL.listen( new UpdateMessage( this.prevTime, this.currTime ) );
            this.prevTime = this.currTime;

            // setup opengl context by enabling the depth buffer and clearing the screen
			Vector3fl fogColor = Environment.GLOBAL.fogColor.toVector3fl().divide( 0xFF );
			GL11.glEnable( GL11.GL_DEPTH_TEST );
			GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );
			GL11.glEnable( GL11.GL_CULL_FACE );
			GL11.glClearColor( fogColor.x, fogColor.y, fogColor.z, 1 );

			// then draw blocks using the block shader
            BlockShader.GLOBAL.use();
            Listener.GLOBAL.listen( new BlockShader.BlockShaderPreRenderMessage() );
            Listener.GLOBAL.listen( new BlockShader.BlockShaderRenderMessage() );

            // now we want to draw 2D UI elements, so disable to depth test
			GL11.glDisable( GL11.GL_DEPTH_TEST );

			// draw the gui using the gui shaders
            GUIShader.GLOBAL.use();
            // broadcast the render message n times, one for each GUI Depth (primitive layering)
            for( GUIDepth d : GUIDepth.values() )
				Listener.GLOBAL.listen( new GUIShader.GUIShaderRenderMessage( d ) );

            // update chunks models that have been changed
            World.WORLD.refresh();
            
            this.updateCtx();
            this.prevTime = this.currTime;
        }
        

        // if we've exited the loop, the game is about to end. Try and clean up all resources by destroying everything
        Listener.GLOBAL.listen( new DestroyMessage() );

        // finally destroy the display ctx
        this.destroyCtx();

    }
    
    public static void main( String[] args ) {
        Game g = new Game();
        g.run();
        System.out.println("Game Terminated");
    }

}
