package com.dbash.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.dbash.util.Rect;

// This class encapsulates the camera and viewport in which presenters draw.
// If you have more than one camera, you have more than one cameraViewPort.
// basically each cameraViewport is a window onto a different virtual world that will be visible in a 
// certain area of the screen when you tell objects to draw themselves using it.
//
// 'Viewport' coordinates is the area on the screen where stuff will draw.
//
// When you create a camera in libgdx, the width and height you supply is the size of the 'window' in world coordinates
// For our purposes, we want the screen coordinates and the world coordinates to have a 1:1 correspondence, so we set the
// camera width and height = the viewport width and height, thus ensuring things get drawn with the correct aspect ratio
// and pixel-perfect scale.
//
// This class extends the normal Orthographic Camera so you can call specific camera operations on it
// if you wish, such as clipping.  TODO, might wrap clipping with this class so that Presenter dont have to have platform
// dependencies if they use clipping (move scissor clipping code form scrollingList presenter into here).
//
// To start drawing to this viewport, call use().  Then any any sprites thrown at it form that point will use its view + projection
// and be displayed in its viewport.
// 
public class CameraViewPort extends OrthographicCamera {
	
    String vertexShader = "attribute vec4 a_position;\n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "\n" +
            "uniform mat4 u_projTrans;\n" +
            "\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "\n" +
            "void main() {\n" +
            "    v_color = a_color;\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "}";

    String fragmentShader = "#ifdef GL_ES\n" +
            "    precision mediump float;\n" +
            "#endif\n" +
            "\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "\n" +
            "void main() {\n" +
            "  vec4 c = v_color * texture2D(u_texture, v_texCoords);\n" +
            "  float grey = (c.r + c.g + c.b) / 3.0;\n" +
            "  gl_FragColor = vec4(grey, grey, grey, c.a);\n" +
            "}";

    
	
	public Rect viewPort;
	ShaderProgram grayscaleShader;
	Rectangle vp;
	float cx;
	float cy; // camera position
	public int vx, vy, vw, vh;  // int versions of viewport.  
	
	public CameraViewPort(Rect viewPort) {
		super(viewPort.width, viewPort.height);
		grayscaleShader = new ShaderProgram(vertexShader, fragmentShader);
		vp = new Rectangle();
		moveViewport(viewPort);
		moveCamera(viewPort.width/2, viewPort.height/2);  // auto center on the middle of the viewport for convenience
	}
	
	// the idea here is to move the position of the camera and/or viewport *before* drawing to it.
	public void use(SpriteBatch spriteBatch) {
		// first set the area into which this viewport will draw on the screen
		Gdx.gl.glViewport(vx, vy, vw, vh);
		position.set(cx, cy, 0);
		update();         // update the matrices
		// todo apply(Gdx.gl20);  // I guess this applies those matrices or something
		
		// then direct the spritebatch passed in to use this camera to project its sprites onto that area of the screen,
		// rather than its own inbuilt one.
		spriteBatch.setProjectionMatrix(combined);  // combined projection (orthographic) and view (camera position) matrices
	}
	
	// move the position of the cameras view of the world.
	// set the position but dont bother updating anything until its time to draw as indicated by use()
	public void moveCamera(float x, float y) {
		this.cx = x;
		this.cy = y;
	}
	
	// move the position of the window on the screen where stuff will be displayed.
	// set the position but dont bother updating anything until its time to draw as indicated by use()
	public void moveViewport(Rect viewPort) {
		this.viewPort = new Rect(viewPort);
		vx =(int)viewPort.x;
		vy =(int)viewPort.y;
		vw =(int)viewPort.width;
		vh =(int)viewPort.height;
		vp.x = viewPort.x;
		vp.y = viewPort.y;
		vp.width = viewPort.width;
		vp.height = viewPort.height; 
	}
	
	// anything drawn after this, until endClipping is called, will be clipped to the Rect provided.
	// I think it might be a tad expensve because it flushes the draw buffer, so dont go crazy with the clipping.
	public void startClipping(SpriteBatch spriteBatch, Rect clipRect) {
		Rectangle clipBounds = new Rectangle(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
		spriteBatch.flush(); // cause stuff drawn so far to be draw so it wont be clipped.
		Rectangle scissors = new Rectangle();
		//Rectangle vp = ScissorStack.getViewport();
		ScissorStack.calculateScissors(this, vp.x, vp.y, vp.width, vp.height, spriteBatch.getTransformMatrix(), clipBounds, scissors);
		ScissorStack.pushScissors(scissors);	
	}
	
	public void endClipping(SpriteBatch spriteBatch) {
		spriteBatch.flush();   
		ScissorStack.popScissors();  // remove the clipping window for further drawing.
	}
	
	public void startGreyScale(SpriteBatch spriteBatch) {
		//enable grayscale
		spriteBatch.setShader(grayscaleShader);
	}
	
	public void endGreyScale(SpriteBatch spriteBatch) {
		spriteBatch.setShader(null);
	}
}
