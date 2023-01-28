function loadTemplate(name, setTitle = name, cID = $('#content'))
{
	console.log("load templates/"+name+".html")
	cID.empty();
	$(function() {
    		cID.load("templates/"+name+".html");
	});
	document.title = setTitle
}
