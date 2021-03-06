package game.gfx;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import game.Entity;
import game.Game.DestroyMessage;
import game.GlobalSubscriberComponent;
import util.FileUtils;
import util.Matrix4fl;
import util.Vector3fl;
import util.Vector3in;

public abstract class Shader extends Entity {

    protected int programId;
    private int vertexId;
    private int fragmentId;
    private final FloatBuffer matrixFloatBuffer;

    private Map<UniformVariable,Integer> uniformLookup;
    private List<AttributeVariable> attrVars;

    public Shader( String vertex, String fragment ) {
        super();
        this.matrixFloatBuffer = BufferUtils.createFloatBuffer(16);

        // create a new shader program
        this.programId = GL20.glCreateProgram();

        // create a map for uniform variables and their opengl ID
        this.uniformLookup = new HashMap<UniformVariable,Integer>();
        this.attrVars = new ArrayList<AttributeVariable>();

        // create vertex and fragment shaders
        this.vertexId   = GL20.glCreateShader( GL20.GL_VERTEX_SHADER );
        this.fragmentId = GL20.glCreateShader( GL20.GL_FRAGMENT_SHADER );

        // load the source files for each shader
        GL20.glShaderSource( this.vertexId, FileUtils.readFile( vertex ) );
        GL20.glShaderSource( this.fragmentId, FileUtils.readFile( fragment ) );

        // compile and validate the shaders
        GL20.glCompileShader( this.vertexId );
        if( GL20.glGetShaderi( this.vertexId, GL20.GL_COMPILE_STATUS ) == GL11.GL_FALSE ) {
            System.err.println( GL20.glGetShaderInfoLog( this.vertexId, 1000 ) );
            throw new RuntimeException( String.format( "shader: '%s' couldn't be compiled", vertex ) );
        }
        GL20.glCompileShader( this.fragmentId );
        if( GL20.glGetShaderi( this.fragmentId, GL20.GL_COMPILE_STATUS ) == GL11.GL_FALSE ) {
            System.err.println( GL20.glGetShaderInfoLog( this.fragmentId, 1000 ) );
            throw new RuntimeException( String.format( "shader: '%s' couldn't be compiled", fragment ) );
        }

        // attach, link and validate the shaders to the program
        GL20.glAttachShader( this.programId, this.vertexId );
        GL20.glAttachShader( this.programId, this.fragmentId );

        setupAttributeVariables();

        GL20.glLinkProgram( this.programId );
        GL20.glValidateProgram( this.programId );

        setupUniformVariables();
        
        this.listener.addSubscriber( DestroyMessage.class, this::destroy );
    	this.registerComponent(new GlobalSubscriberComponent( this ));
    	
    	this.build();
    }

    // allocate a new uniform variable for the shader program
    // and add its mapping to the lookup
    protected void createUniformVariable( UniformVariable uv ) {
        int uvId = GL20.glGetUniformLocation( this.programId, uv.name );
        this.uniformLookup.put( uv, uvId );
    }

    // setup a new attribute variable for the shader program at the VAO position
    // specified by the av enum
    protected void createAttributeVariable( AttributeVariable av ) {
        GL20.glBindAttribLocation( this.programId, av.ordinal(), av.name );
        this.attrVars.add( av );
    }
    
    public Collection<AttributeVariable> getUsedAttributeVariables() {
    	return this.attrVars;
    }
    
    public Collection<UniformVariable> getUsedUniformVariables() {
    	return this.uniformLookup.keySet();
    }

    // load a 3d float vector into a uniform variable position
    public void loadVector3fl( UniformVariable uv, Vector3fl v ) {
        int uvId = this.uniformLookup.get( uv );
        GL20.glUniform3f( uvId, v.x, v.y, v.z);
    }
    
    public void loadFloat( UniformVariable uv, float f ) {
    	int uvId = this.uniformLookup.get( uv );
    	GL20.glUniform1f( uvId, f );
    }

    // load a 3d int vector into a uniform variable position
    public void loadVector3in( UniformVariable uv, Vector3in v ) {
        int uvId = this.uniformLookup.get( uv );
        GL20.glUniform3i( uvId, v.x, v.y, v.z);
    }

    // load a 4d matrix into a uniform variable position
    public void loadMatrix4f( UniformVariable uv, Matrix4fl m ) {
        m.raw.store( this.matrixFloatBuffer );
        this.matrixFloatBuffer.flip();
        int uvId = this.uniformLookup.get( uv );
        GL20.glUniformMatrix4( uvId,  false,  this.matrixFloatBuffer);
    }

    // load a single int into a uniform variable position
    public void loadInt( UniformVariable uv, int i ) {
        int uvId = this.uniformLookup.get( uv );
        GL20.glUniform1i( uvId, i);
    }
    
    public void use() {
        GL20.glUseProgram( this.programId );
    }

    private void destroy( DestroyMessage msg ) {
    	this.destroy();
    }
    
    @Override
    // destroy the shader program when this entity is destroyed
    public void destroy() {
        super.destroy();
        GL20.glDetachShader( this.programId, this.vertexId );
        GL20.glDetachShader( this.programId, this.fragmentId );
        GL20.glDeleteShader( this.vertexId );
        GL20.glDeleteShader( this.fragmentId );
        GL20.glDeleteProgram( this.programId );
    }

    // setup all VAO variables
    protected abstract void setupAttributeVariables();

    // setup all uniform variables
    protected abstract void setupUniformVariables();
}
