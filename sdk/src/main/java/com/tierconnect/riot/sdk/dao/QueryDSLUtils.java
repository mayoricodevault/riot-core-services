package com.tierconnect.riot.sdk.dao;

import java.util.ArrayList;
import java.util.List;

public class QueryDSLUtils {

	@SuppressWarnings("rawtypes")
	/**
	 * Converts a QueryDSL path to a String without the root part
	 * @param path  for example QUser.user.name or QUser.user.group.id
	 * @return a String of the path without the root part 
	 * for example: getPath(QUser.user.name) returns "name"
	 *              getPath(QUser.user.group.id) returns "group.id"
	 * if you want the full path then you can call toString() directly on path
	 * for example: QUser.user.name.toString() returns "user1.name"
	 *              QUser.user.group.id.toString() returns "user1.group.id"               
	 */
	public static String getPath(com.mysema.query.types.Path path) {
		List<String> parts = new ArrayList<>();
		com.mysema.query.types.Path pathX = path;
		do {
			parts.add(pathX.getMetadata().getName());
			if (pathX.getMetadata().getParent() != null) {
				pathX = pathX.getMetadata().getParent();
			} else {
				break;
			}
		} while (true);
		StringBuilder sb = new StringBuilder();
		for (int i = parts.size() - 2; i >= 0; i--) {
			sb.append(parts.get(i) + (i == 0 ? "" : "."));
		}
		return sb.toString();
	}
	
}
