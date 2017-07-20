package org.opensrp.repository.lucene;

import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;

import com.mysql.jdbc.StringUtils;

public class Query {
	
	private String query = "";
	
	private FilterType filterType;
	
	public String query() {
		return query;
	}
	
	public Query(FilterType filterType) {
		this.filterType = filterType;
	}
	
	public Query(FilterType filterType, Query from) {
		this.filterType = filterType;
		if (from != null && !StringUtils.isEmptyOrWhitespaceOnly(from.query)) {
			this.query = "(" + from.query + ")";
		}
	}
	
	public Query eq(String name, String value) {
		addToQuery(name + ":" + value + " ");
		return this;
	}
	
	public Query notEq(String name, String value) {
		addToQuery("*:* -" + name + ":" + value + " ");
		return this;
	}
	
	public Query like(String field, String value) {
		addToQuery(field + ":\"" + value + "\"");
		return this;
	}
	
	public Query likeWithWildCard(String field, String value) {
		addToQuery(field + ":" + value + "* ");
		return this;
	}
	
	public Query eq(String name, DateTime value) {
		addToQuery(name + "<date>:[" + value.withTimeAtStartOfDay().toString("yyyy-MM-dd'T'HH:mm:ss") + " TO "
		        + value.plusDays(1).withTimeAtStartOfDay().toString("yyyy-MM-dd'T'HH:mm:ss") + "] ");
		return this;
	}
	
	public Query between(String name, DateTime from, DateTime to) {
		addToQuery(name + "<date>:[" + from.toString("yyyy-MM-dd'T'HH:mm:ss") + " TO "
		        + to.toString("yyyy-MM-dd'T'HH:mm:ss") + "] ");
		return this;
	}
	
	public Query between(String field, Object start, Object to) {
		addToQuery(field + ":[" + start + " TO " + to + "]");
		return this;
	}
	
	//field:(value1 OR value2 OR value3)
	public Query inList(String field, List<String> ids) {
		String idString = org.apache.commons.lang.StringUtils.join(ids, " OR ");
		addToQuery(field + ":(" + idString + ")");
		return this;
	}
	
	//field:("value1" OR "value2" OR "value3")
	public Query likeList(String field, List<String> ids) {
		String idString = quotedJoin(ids, " OR ");
		addToQuery(field + ":(" + idString + ")");
		return this;
	}
	
	private void addToQuery(String q) {
		if (!StringUtils.isEmptyOrWhitespaceOnly(query)) {
			query += filterType.name() + " " + q;
		} else
			query += q;
	}
	
	public void addToQuery(Query from) {
		if (from != null && !StringUtils.isEmptyOrWhitespaceOnly(from.query)) {
			addToQuery("(" + from.query + ")");
		}
	}
	
	private String quotedJoin(List<String> ids, String separator) {
		Iterator<String> iterator = ids.iterator();
		
		if (iterator == null) {
			return null;
		}
		if (!iterator.hasNext()) {
			return "";
		}
		String first = iterator.next();
		if (!iterator.hasNext()) {
			return "\""+first+"\"";
		}
		
		// two or more elements
		StringBuffer buf = new StringBuffer(256); // Java default is 16, probably too small
		if (first != null) {
			buf.append("\""+first+"\"");
		}
		
		while (iterator.hasNext()) {
			buf.append(separator);
			String s = iterator.next();
			if (s != null) {
				buf.append("\""+s+"\"");
			}
		}
		
		return buf.toString();
	}
}
