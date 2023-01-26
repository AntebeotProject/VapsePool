function loadTemplate(name, setTitle = name, cID = $('#content'))
{
	cID.empty();
	$(function() {
    		cID.load("templates/"+name+".html");
	});
	document.title = setTitle
}
