const defLanguage = "ru_RU";
class pageLanguage {
	language: string;
	constructor(lang: string) {
		this.language = lang
	}
	change(nLang: string)
	{
		this.language = nLang
	}
	lang() 
	{
		return this.language
	}
}

export { pageLanguage, defLanguage };