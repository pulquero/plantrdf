<%@page import="javax.servlet.http.*" %>
<!DOCTYPE html>
<html>
<head>
<title>PlantRDF</title>
</head>
<body>
<%
boolean isOn = false;
Cookie[] cookies = request.getCookies();
if(cookies != null) {
	for(Cookie cookie : cookies) {
		if("observe".equals(cookie.getName())) {
			isOn = true;
		}
	}
}

String param = request.getParameter("ensure");
if(param != null) {
	if("on".equals(param)) {
		Cookie cookie = new Cookie("observe", "on");
		cookie.setMaxAge(-1);
		response.addCookie(cookie);
		isOn = true;
	} else if("off".equals(param)) {
		Cookie cookie = new Cookie("observe", null);
		cookie.setMaxAge(0);
		response.addCookie(cookie);
		isOn = false;
	}
}
%>
<p>Observations are <%= isOn ? "on" : "off" %>:
<% if(isOn) { %>
<a href="observe.jsp?ensure=off">off</a>
<% } else { %>
<a href="observe.jsp?ensure=on">on</a>
<% } %>
.</p>
</body>
</html>
