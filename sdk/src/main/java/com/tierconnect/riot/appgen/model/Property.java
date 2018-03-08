package com.tierconnect.riot.appgen.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "aps_property")
public class Property 
{
	static Set<String> primitives;
	
	static
	{
		primitives = new HashSet<String>();
		primitives.add( "byte" );
		primitives.add( "short" );
		primitives.add( "int" );
		primitives.add( "long" );
		primitives.add( "float" );
		primitives.add( "double" );
		primitives.add( "char" );
		primitives.add( "String" );
		primitives.add( "boolean" );
		
		primitives.add( "Byte" );
		primitives.add( "Short" );
		primitives.add( "Integer" );
		primitives.add( "Long" );
		primitives.add( "Float" );
		primitives.add( "Double" );
		primitives.add( "Character" );
		primitives.add( "Boolean" );
        primitives.add( "BigDecimal" );

		primitives.add( "Date" );
	}
	
	@Id
	@GeneratedValue
	protected Long id;
	
	String visibility; 
	
	String name;
	
	String type;

    boolean derived;
	
	//@ManyToOne
	//Clazz clazz;

	String [] annotations;
	
	public Long getId() 
	{
		return id;
	}

	public void setId( Long id ) 
	{
		this.id = id;
	}

	public String getVisibility() 
	{
		return visibility;
	}

	public void setVisibility( String visibility ) 
	{
		this.visibility = visibility;
	}

	public String getName() 
	{
		return name;
	}

	public void setName( String name ) 
	{
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String[] getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String[] annotations) {
		this.annotations = annotations;
	}
	
	public String getVariableName()
	{
		return this.getName();
	}
	
	public String getGetterName()
	{
		return ( this.getShortType().equals( "boolean" ) ? "is" : "get" ) + ucfirst( getName() );
	}
	
	public String getSetterName()
	{
		return "set" + ucfirst( getName() );
	}
	
	public String ucfirst( String str )
	{
		return str.substring( 0, 1 ).toUpperCase() + str.substring( 1, str.length() );
	}
	
	public String getShortType() 
	{
		int i = type.lastIndexOf( '.' );
		if( i == -1 )
			return type;
		return type.substring( i + 1, type.length() );
	}
	
	public boolean isPrimitive()
	{
		return !isDerived() && primitives.contains( this.getType() );
	}
	
	public boolean isEntity()
	{
		return !isDerived() && Clazz.has( this.getType() );
	}

    public boolean isDerived()
    {
        return derived;
    }

    public void setDerived(boolean derived)
    {
        this.derived = derived;
    }

	public boolean isArray() {
		return this.getType().contains("<") || this.getType().contains("[");
	}

	public boolean isLob() {
		return this.getType().toLowerCase().contains("lob");
	}
}
