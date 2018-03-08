package com.tierconnect.riot.iot.entities;

import java.util.Date;
import java.util.List;

/**
 * Created by rchirinos on 18/11/2015.
 */
public class AttachmentBean
{
	private AttachmentConfigBean configBean;
	private Long count;
	private Date lastupdate;
	private List<AttachmentFile> files;

	public AttachmentConfigBean getConfigBean()
	{
		return configBean;
	}

	public void setConfigBean( AttachmentConfigBean configBean )
	{
		this.configBean = configBean;
	}

	public Long getCount()
	{
		return count;
	}

	public void setCount( Long count )
	{
		this.count = count;
	}

	public List<AttachmentFile> getFiles()
	{
		return files;
	}

	public void setFiles( List<AttachmentFile> files )
	{
		this.files = files;
	}

	public Date getLastupdate()
	{
		return lastupdate;
	}

	public void setLastupdate( Date lastupdate )
	{
		this.lastupdate = lastupdate;
	}
}
