package com.tierconnect.riot.sdk.dao;

import com.mysema.query.jpa.JPQLQuery;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Pagination {
	static Logger logger = Logger.getLogger(Pagination.class);
	private int page;
	private int pageSize;
	
	public static final int DEFAULT_PAGE_SIZE  = 10;
	public static final int DEFAULT_START_PAGE = 1;
	public static final int DEFAULT_ALL_VALUE = -1;

	/**
	 * Calculates the pagination values for a query
	 * @param page positive value
	 * @param pageSize positive value or -1 for no pagination
	 */
	public Pagination(Integer page,Integer pageSize/*,orderBy*/){
		this.page = page == null? DEFAULT_START_PAGE : page;
		this.pageSize = pageSize == null? DEFAULT_PAGE_SIZE : pageSize;
		
		if(this.page <= 0){
			throw new UserException("Page must be a positive number");
		}
		
		if(this.pageSize == 0 || this.pageSize < DEFAULT_ALL_VALUE){
			throw new UserException("PageSize must be a positive number or -1 for no pagination");
		}
	}
	
	private int calculateOffset(){
		return (this.page - 1) * this.pageSize;
	}

	public Long getTotalPages(Long count){
		return (long) Math.ceil((double)count / this.pageSize);
	}

	public int getPage() {
		return page;
	}

	public JPQLQuery add(JPQLQuery query) {
		JPQLQuery q = query;

		if (pageSize != DEFAULT_ALL_VALUE) {
			logger.debug("Adding pagination. pageSize [" +  pageSize + "], offset [" + calculateOffset()+"].");
			q = q.limit(pageSize);
			q = q.offset(calculateOffset());
		}

		return q;
	}

	/***********************************************
	 * This does pagination of a list
     **********************************************/
	public static List<Map<String, Object>> paginationList(List list, Integer pageNumber, Integer pageSize)
	{
		List<Map<String, Object>> result = null;
		if( (pageNumber!=null && pageNumber.intValue()>0)
				&& (pageSize!=null && pageSize.intValue()>0) )
		{
			result = new ArrayList<>();
			if(list!=null && list.size()>0)
			{
				int iniValue = (pageNumber * pageSize) - pageSize;
				int endValue = iniValue+pageSize;
				if(endValue>list.size()){
					endValue = list.size();
				}
				result = list.subList(iniValue, endValue);
			}
		}else
		{
			result = list;
		}

		return result;
	}
}
