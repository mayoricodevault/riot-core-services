package com.tierconnect.riot.iot.servlet;

import org.imgscalr.Scalr;
import scala.runtime.StringFormat;

import java.awt.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.Line2D;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Usage:
 * 
 * For HEX color colors:
 * 
 * http://localhost:8080/riot-core-services/imageServlet?color=#00AA00 <= must
 * URL encode the '#', so use:
 * http://localhost:8080/riot-core-services/imageServlet?color=%2300AA00
 * 
 * Or:
 * 
 * http://localhost:8080/riot-core-services/imageServlet?color=[24-bit base 10 number]
 * 
 * @author tcrown
 * 
 */
public class ImageServlet extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    private static final String ALERT_SRC = "/alert.png";
    private static final String BABY_SRC = "/baby.png";
    private static final String BABY_TRACKING_SRC = "/babytracking.png";
    private static final String CAMERA_SRC = "/camera.png";
    private static final String CAPTAIN_SRC = "/captain.png";
    private static final String CAR_SRC = "/car.png";
    private static final String CLOTHING_SRC = "/crewmember.png";
    private static final String CONTRACTOR_SRC = "/contractor.png";
    private static final String CORN_SRC = "/corn.png";
    private static final String CREWMEMBER_SRC = "/clothing.png";
    private static final String DESKTOP_SRC = "/desktop.png";
    private static final String DOCTOR1_SRC = "/doctor1.png";
    private static final String DOCTOR2_SRC = "/doctor2.png";
    private static final String DOCUMENTS_SRC = "/documents.png";
    private static final String ELECTRONICS_OFFICE_SRC = "/electronicsoffice.png";
    private static final String ENGINE_SRC = "/engine.png";
    private static final String FEMALE_SRC = "/female.png";
    private static final String FEMALE_VISITOR_SRC = "/femalevisitor.png";
    private static final String HAZARD_SRC = "/hazard.png";
    private static final String HOME_FURNITURE_SRC= "/homefurniture.png";
    private static final String HOSPITAL_ASSETS_SRC = "/hospitalassets.png";
    private static final String HUNTER_SRC = "/hunter.png";
    private static final String IPAD_SRC = "/ipad.png";
    private static final String IPHONE_SRC = "/iphone.png";
    private static final String IV_PUMP_SRC = "/ivpump.png";
    private static final String JACKET_SRC = "/jacket.png";
    private static final String KID_SRC = "/kid.png";
    private static final String LAPTOP_SRC = "/laptop.png";
    private static final String MALE_SRC = "/male.png";
    private static final String MALE_VISITOR_SRC = "/malevisitor.png";
    private static final String MEDICAL_EQUIPMENT_SRC = "/medicalequipment.png";
    private static final String MOVIES_BOOKS_SRC = "/moviesbooks.png";
    private static final String NURSE_HAT_SRC = "/nursehat.png";
    private static final String OIL_PIPE_SRC = "/oilpipe.png";
    private static final String OTHERS_SRC = "/others.png";
    private static final String PANTS_SRC = "/pants.png";
    private static final String PATIENT_SRC = "/patient.png";
    private static final String PIN_SRC = "/generic.png";
    private static final String PILL_SRC = "/pill.png";
    private static final String PLANE_SRC = "/plane.png";
    private static final String PLANT_SRC = "/plant.png";
    private static final String SPORTS_FITNESS_SRC = "/sportsfitness.png";
    private static final String STETHOSCOPE_SRC = "/stethoscope.png";
    private static final String TAG_SRC = "/tag.png";
    private static final String VALVE_SRC = "/valve.png";
    private static final String WHEELCHAIR_SRC = "/wheelchair.png";
    private static final String WORKER_SRC = "/worker.png";
    private static final String ZOMBIE_SRC = "/zombie.png";



    private static final int IMG_WIDTH = 74;
    private static final int IMG_HEIGHT = 76;

	ServletConfig config;

	float o = 1f/9f;
	
	public void init( final ServletConfig config )
	{
		// final String context = config.getServletContext().getRealPath( "/" );
		this.config = config;
	}

	public void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException
	{
		Color colorin = new Color( 140, 140, 140 );
		Color colorin2 = new Color( 120, 120, 120 );
        Color colorin3 = new Color(153, 153, 153 );


		Color colorout = null;
		Color colorout2 = null;
        Color colorout3 = null;
		boolean validHexColor = false;

		String colorString = req.getParameter( "color" );
		String iconType = req.getParameter( "iconType" );
    String pinStyle = "modernV2"; //req.getParameter( "pinStyle" );
		String op = req.getParameter( "op" );

		
		if( op == null )
		{
			o = 1f / 9f;
		}
		else
		{
			o = Float.parseFloat( op );
		}

		if( !colorString.startsWith( "#" ) && isNumeric( colorString ) && colorString.length() > 0 )
		{
			long color = Long.parseLong( colorString );
			colorString = Long.toHexString( color );
			String zeros = "000000";
			colorString = "#" + zeros.substring( 0, zeros.length() - (colorString.length()) ) + colorString;
		}
		if( colorString.length() == 7 )
		{
			String redString = colorString.substring( 1, 3 );
			String greenString = colorString.substring( 3, 5 );
			String blueString = colorString.substring( 5, 7 );
			int r = Integer.parseInt( redString, 16 );
			int g = Integer.parseInt( greenString, 16 );
			int b = Integer.parseInt( blueString, 16 );
			colorout  = new Color( r, g, b );
			colorout2 = new Color( r / 2, g / 2, b / 2 );
            colorout3 = new Color( r , g , b );
			validHexColor = true;
			System.out.println( String.format( "rgb=%d %d %d", r, g, b ) );
		}

		// Get the absolute path of the image
		ServletContext sc = config.getServletContext();
		String filename = sc.getRealPath( "/images-pins/templates/"+ pinStyle +"" + getSrcImage( iconType ) );


		File file = new File( filename );
		File dir = file.getParentFile().getParentFile().getParentFile().getParentFile();

		BufferedImage img = null;
		try
		{
			img = ImageIO.read( file );
		}
		catch( IOException e )
		{
		}

        if( iconType != null && !iconType.isEmpty() && iconType.toLowerCase().equals("pin") ) {
            iconType = "generic";
        }

        System.out.println(iconType);

		String fnameOutUpperCase = "/images-pins/processed/"+ pinStyle +"/pin-" + iconType + colorString.toUpperCase() + ".png";
        String fnameOutLowerCase = "/images-pins/processed/"+ pinStyle +"/pin-" + iconType + colorString.toLowerCase() + ".png";

        File foutUpperCase = new File( dir, fnameOutUpperCase );
//        File foutLowerCase = new File( dir, fnameOutLowerCase );

        File fout;
        String fnameOut;
        if( foutUpperCase.exists() ) {
            fnameOut = fnameOutUpperCase;
            fout = new File( dir, fnameOut );
        }else {
            fnameOut = fnameOutLowerCase;
            fout = new File( dir, fnameOut );
        }

        if(fout.exists() &&  ImageIO.read(fout) != null) {
            //The image exist in cache
            System.out.println("Image Cached: " + fnameOut);
            Image image = ImageIO.read(fout);
            FileOutputStream fos = new FileOutputStream( fout );
            ImageIO.write(toBufferedImageWithOutAntiAlias(image), "png", fos);
        } else {
            Image img2 = changeColor( img, colorin, colorin2, colorin3, colorout, colorout2, colorout3);
            FileOutputStream fos = new FileOutputStream( fout );
            System.out.println( "ImageServlet: writing image file='" + fout.getAbsolutePath() + "' img=" + img2 );
            if (validHexColor) {
                BufferedImage originalImage = toBufferedImage(img2);
                int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
                ImageIO.write(resizeImageWithHint(originalImage, type), "png", fos);
            } else {
                BufferedImage originalImage = toBufferedImage(img);
                int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
                ImageIO.write(resizeImageWithHint(originalImage, type), "png", fos);
            }
        }
		// Get the MIME type of the image
		String mimeType = sc.getMimeType( fnameOut );
		if( mimeType == null )
		{
			sc.log( "Could not get MIME type of " + filename );
			resp.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
			return;
		}

		// Set content type
		resp.setContentType( mimeType );

		file = new File( dir, fnameOut );
		resp.setContentLength( (int) file.length() );

		// Open the file and output streams
		FileInputStream in = new FileInputStream( file );
		OutputStream out = resp.getOutputStream();

		// Copy the contents of the file to the output stream
		byte[] buf = new byte[1024];
		int count = 0;
		while( (count = in.read( buf )) >= 0 )
		{
			out.write( buf, 0, count );
		}
		in.close();
		out.close();
	}

	private String getSrcImage( String iconType )
	{
        iconType = "/" + iconType + ".png";
		if( iconType == null || iconType.isEmpty() ) return PIN_SRC;
		iconType = iconType.toLowerCase();

        if( ALERT_SRC.contains(iconType) ) return ALERT_SRC;
        if( BABY_SRC.contains(iconType) ) return BABY_SRC;
        if( BABY_TRACKING_SRC.contains(iconType) ) return BABY_TRACKING_SRC;
        if( CAMERA_SRC.contains(iconType) ) return CAMERA_SRC;
        if( CAPTAIN_SRC.contains(iconType) ) return CAPTAIN_SRC;
        if( CAR_SRC.contains(iconType) ) return CAR_SRC;
        if( CLOTHING_SRC.contains(iconType) ) return CLOTHING_SRC;
        if( CONTRACTOR_SRC.contains(iconType) ) return CONTRACTOR_SRC;
        if( CORN_SRC.contains(iconType) ) return CORN_SRC;
        if( CREWMEMBER_SRC.contains(iconType) ) return CREWMEMBER_SRC;
        if( DESKTOP_SRC.contains(iconType) ) return DESKTOP_SRC;
        if( DOCTOR1_SRC.contains(iconType) ) return DOCTOR1_SRC;
        if( DOCTOR2_SRC.contains(iconType) ) return DOCTOR2_SRC;
        if( DOCUMENTS_SRC.contains(iconType) ) return DOCUMENTS_SRC;
        if( ELECTRONICS_OFFICE_SRC.contains(iconType) ) return ELECTRONICS_OFFICE_SRC;
        if( ENGINE_SRC.contains(iconType) ) return ENGINE_SRC;
        if( FEMALE_SRC.contains(iconType) ) return FEMALE_SRC;
        if( FEMALE_VISITOR_SRC.contains(iconType) ) return FEMALE_VISITOR_SRC;
        if( HAZARD_SRC.contains(iconType) ) return HAZARD_SRC;
        if( HOME_FURNITURE_SRC.contains(iconType) ) return HOME_FURNITURE_SRC;
        if( HOSPITAL_ASSETS_SRC.contains(iconType) ) return HOSPITAL_ASSETS_SRC;
        if( HUNTER_SRC.contains(iconType) ) return HUNTER_SRC;
        if( IPAD_SRC.contains(iconType) ) return IPAD_SRC;
        if( IPHONE_SRC.contains(iconType) ) return IPHONE_SRC;
        if( IV_PUMP_SRC.contains(iconType) ) return IV_PUMP_SRC;
        if( JACKET_SRC.contains(iconType) ) return JACKET_SRC;
        if( KID_SRC.contains(iconType) ) return KID_SRC;
        if( LAPTOP_SRC.contains(iconType) ) return LAPTOP_SRC;
        if( MALE_SRC.contains(iconType) ) return MALE_SRC;
        if( MALE_VISITOR_SRC.contains(iconType) ) return MALE_VISITOR_SRC;
        if( MEDICAL_EQUIPMENT_SRC.contains(iconType) ) return MEDICAL_EQUIPMENT_SRC;
        if( MOVIES_BOOKS_SRC.contains(iconType) ) return MOVIES_BOOKS_SRC;
        if( NURSE_HAT_SRC.contains(iconType) ) return NURSE_HAT_SRC;
        if( OIL_PIPE_SRC.contains(iconType) ) return OIL_PIPE_SRC;
        if( OTHERS_SRC.contains(iconType) ) return OTHERS_SRC;
        if( PANTS_SRC.contains(iconType) ) return PANTS_SRC;
        if( PATIENT_SRC.contains(iconType) ) return PATIENT_SRC;
        if( PIN_SRC.contains(iconType) ) return PIN_SRC;
        if( PILL_SRC.contains(iconType) ) return PILL_SRC;
        if( PLANE_SRC.contains(iconType) ) return PLANE_SRC;
        if( PLANT_SRC.contains(iconType) ) return PLANT_SRC;
        if( SPORTS_FITNESS_SRC.contains(iconType) ) return SPORTS_FITNESS_SRC;
        if( STETHOSCOPE_SRC.contains(iconType) ) return STETHOSCOPE_SRC;
        if( TAG_SRC.contains(iconType) ) return TAG_SRC;
        if( VALVE_SRC.contains(iconType) ) return VALVE_SRC;
        if( WHEELCHAIR_SRC.contains(iconType) ) return WHEELCHAIR_SRC;
        if( WORKER_SRC.contains(iconType) ) return WORKER_SRC;
        if( ZOMBIE_SRC.contains(iconType) ) return ZOMBIE_SRC;

		return PIN_SRC;
	}

	private Image changeColor( final BufferedImage im,
                               final Color in1, final Color in2, final Color in3,
                               final Color color1, final Color color2, final Color color3)
	{
		final ImageFilter filter = new RGBImageFilter()
		{
			// the color we are looking for
			// NOTE: the current pin image is afu, so these do not work
//			public int markerRGB1 = in1.getRGB() & 0x00FFFFFF;
//			public int markerRGB2 = in2.getRGB() & 0x00FFFFFF;
//            public int markerRGB3 = in3.getRGB() & 0x00FFFFFF;

			// using instead:
			public int markerRGB1 = in1.getRGB() | 0xFF000000;
			public int markerRGB2 = in2.getRGB() | 0xFF000000;
            public int markerRGB3 = in3.getRGB() | 0xFF000000;

			public final int filterRGB( final int x, final int y, final int rgb )
			{
				// System.out.println( "image: " + x + "," + y + "=" +
				// Integer.toHexString( rgb ) );
//
//                System.out.println(">> " + String.format("%05X", rgb) + " == " + String.format("%05X", markerRGB1));
//                System.out.println(">> " + String.format("%05X", rgb) + " == " + String.format("%05X", markerRGB2));
//                System.out.println(">> " + String.format("%05X", rgb) + " == " + String.format("%05X", markerRGB3));

				if( rgb == markerRGB1 )
				{
					return color1.getRGB();
				}
				else if( rgb == markerRGB2 )
				{
					return color2.getRGB();
				}
                else if( rgb == markerRGB3 )
                {
                    return color3.getRGB();
                }
				else
				{
					return rgb;
				}
			}
		};

		final ImageProducer ip = new FilteredImageSource( im.getSource(), filter );

		return Toolkit.getDefaultToolkit().createImage( ip );
	}

	private BufferedImage toBufferedImage( Image img )
	{
		// if( img instanceof BufferedImage )
		// {
		// return (BufferedImage) img;
		// }

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage( img.getWidth( null ), img.getHeight( null ),
				BufferedImage.TYPE_INT_ARGB );

		// Draw the image on to the buffered image
		Graphics2D g2 = bimage.createGraphics();

		boolean b = g2.drawImage( img, 0, 0, null );

		if( !b )
		{
			System.out.println( "IS: b=" + b );
			try
			{
				Thread.sleep( 10 );
			}
			catch( InterruptedException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke((float) 5.0));
		//g2.draw( new Line2D.Double( 0, 0, 37, 37 ) );

		g2.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY );
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		//g2.draw( new Line2D.Double( 0, 37, 37, 0 ) );

		Kernel kernel = new Kernel( 3, 3,

		new float[] {

		o, o, o,

		o, o, o,

		o, o, o } );

		BufferedImageOp op = new ConvolveOp( kernel );

		bimage = op.filter( bimage, null );

		g2.dispose();

		// Return the buffered image
		return bimage;
	}

    private BufferedImage toBufferedImageWithOutAntiAlias(Image img)
    {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }

	public static boolean isNumeric( String str )
	{
		try
		{
			Integer.parseInt( str );
		}
		catch( NumberFormatException e )
		{
			return false;
		}
		// only got here if we didn't return false
		return true;
	}
    private static BufferedImage resizeImageWithHint(BufferedImage originalImage, int type){
        BufferedImage imageResult = Scalr.resize(originalImage, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH,
                                    37, 37, Scalr.OP_ANTIALIAS);
        return imageResult;
    }
}
