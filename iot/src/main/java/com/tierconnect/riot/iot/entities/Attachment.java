package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.entities.User;

import javax.annotation.Generated;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rchirinos on 20/11/2015.
 */
@Entity
@Cacheable(value = false)
@Table(name="attachment")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class Attachment extends AttachmentBase
{

	public Map<String,Object> publicDataMap()
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "id", getId() );
		map.put( "name", getName() );
		map.put( "type", getType() );
		map.put( "size", getSize() );
		map.put( "comment", getComment() );
		map.put( "date", getDateUploaded() );
		User user = getUploadedBy();
		map.put( "uploadedBy", user.getFirstName()+" "+user.getLastName() );
		map.put("date", new SimpleDateFormat("YYYY-MM-dd").format(getDateUploaded()));
		return map;
	}
}
