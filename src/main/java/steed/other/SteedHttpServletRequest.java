package steed.other;


import org.apache.struts2.dispatcher.StrutsRequestWrapper;

import javax.servlet.http.HttpServletRequest;

public class SteedHttpServletRequest extends StrutsRequestWrapper{

	public SteedHttpServletRequest(HttpServletRequest request) {
		super(request);
	}
	public Long getLong(String name){
		return Long.parseLong(getParameter(name));
	}
}
