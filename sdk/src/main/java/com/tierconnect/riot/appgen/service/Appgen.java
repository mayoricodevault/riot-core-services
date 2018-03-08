package com.tierconnect.riot.appgen.service;

import java.io.File;
import java.io.IOException;

import com.tierconnect.riot.appgen.model.AppgenPackage;
import com.tierconnect.riot.appgen.model.Application;
import com.tierconnect.riot.appgen.model.Clazz;
import com.tierconnect.riot.appgen.model.Property;

public abstract class Appgen
{
	/**
	 * When adding a new class, also add to hibernate.cfg.xml and RiotResetEasyApplication.java
	*/
	public void genApplication(File indir, File outdir ) throws IOException
	{
		Application app = getApplication();
		System.out.println( "Generating application ..." );
		for( AppgenPackage ap : app.getPackages() )
		{
			System.out.println( "Generating package " + ap.getPackageName() );
			for( Clazz clazz : ap.getClasses() )
			{
				System.out.println( "Generating class " + clazz.getClassName() );
				GenModelBase.generate( clazz, outdir );
				GenModel.generate( clazz, indir, outdir );
				GenDAOBase.generate( clazz, outdir );
				GenDAO.generate( clazz, indir, outdir );
                if (clazz.isGenerateService()) {
                    GenServiceBase.generate(clazz, outdir);
                    GenService.generate(clazz, indir, outdir);
                }
                if (clazz.isGenerateController()) {
                    GenControllerBase.generate(clazz, outdir);
                    GenController.generate(clazz, indir, outdir);
                }
			}
		}
	}

	abstract public Application getApplication() throws IOException;
	
	public Property property( String type, String name )
	{
		return property( type, name, null );
	}
	
	public Property property( String type, String name, String [] annotations )
	{
		Property p = new Property();
		p.setVisibility( "public" );
		p.setName( name );
		p.setType( type );
		p.setAnnotations( annotations );
        p.setDerived(false);
		return p;
	}
	
	public Property property( String visibility, String type, String name, String [] annotations )
	{
		Property p = new Property();
		p.setVisibility( visibility );
		p.setName( name );
		p.setType( type );
		p.setAnnotations( annotations );
        p.setDerived(false);
		return p;
	}

    public Property property( String visibility, String type, String name, String [] annotations, boolean derived)
    {
        Property p = new Property();
        p.setVisibility( visibility );
        p.setName( name );
        p.setType( type );
        p.setAnnotations( annotations );
        p.setDerived(derived);
        return p;
    }

}
