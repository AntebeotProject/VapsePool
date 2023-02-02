import {pageLanguage, defLanguage} from './pageLanguage';
export class ajax {
		m_lang: pageLanguage
		constructor(l: pageLanguage)
		{
			this.m_lang = l
		}
		static userData = function (w) {
			var tmp = null;
			var reqdata = w
			$.ajax({
				'async': false,
				'type': "GET",
				'global': false,
				'dataType': 'json',
				'url': "/user/",
				'data': reqdata,
				'success': function (data) {
					tmp = data;
				}
			});
			return tmp;
			};
		static user = {
			genNewAddress (cryptocoin) {
				return this.userData ({'w':"genAddress", 'cryptocoin': cryptocoin}) 
			},
			updateSession() { 
				return this.userData ({'w':"updateSession"}) 
			},
			changePassword(last_pass, new_pass) { 
				return this.userData ({'w':"changePassword", 'last_pass':last_pass, 'new_pass':new_pass}) 
			},
			getowninput(cryptocoin) {
				return this.userData ({'w':"getowninput", 'cryptocoin': cryptocoin}) 
			},
		}
		// /api rest api
		static apiData = function (w) {
			var tmp = null;
			var reqdata = w
			$.ajax({
				'async': false,
				'type': "GET",
				'global': false,
				'dataType': 'json',
				'url': "/api/",
				'data': reqdata,
				'success': function (data) {
					tmp = data;
				}
			});
			return tmp;
		};
		static api = {
			getAllowCoins() {
				return this.apiData({'w':"getallowcoins"})
			},
			outputMoney(login,password, output_address, count_of_money, coin_name, captchaData) {
				return this.apiData({'w':"output", 'acc': login, 'pass': password, 'oAdr': output_address, 'cMoney': count_of_money, 'coinname': coin_name, 'captchaText':captchaData, 'lang': this.m_lang.lang() })
			}
		}
		// exchange rest api
		static exchangeData = function (w) {
			var tmp = null;
			var reqdata = w
			$.ajax({
				'async': false,
				'type': "GET",
				'global': false,
				'dataType': 'json',
				'url': "/exchange/",
				'data': reqdata,
				'success': function (data) {
					tmp = data;
				}
			});
			return tmp;
		};
		// exchange rest api
		static exchange = {
				addOrderToSellCoin2Coin(toSellName, toSellPrice, toSellLimitMin, toSellLimitMax, toBuyName, toBuyPrice, toBuyLimitMin, toBuyLimitMax, msg = "", tIsBuyer = false)
				{
					console.log("Создаем ордер")
					console.log(toSellName)
					console.log(toSellPrice)
					console.log(toSellLimitMin)
					console.log(toSellLimitMax)
					console.log(toBuyName)
					console.log(toBuyPrice)
					console.log(toBuyLimitMin)
					console.log(msg)
					return this.exchangeData ({'w': 'addOrderToSellCoin2Coin',
						'toSellName': toSellName,
						'toSellPrice': toSellPrice,
						'toSellLimitMin': toSellLimitMin,
						'toSellLimitMax': toSellLimitMax,
						'toBuyName': toBuyName,
						'toBuyPrice': toBuyPrice,
						'toBuyLimitMin': toBuyLimitMin,
						'toBuyLimitMax': toBuyLimitMax,
						'tIsBuyer': tIsBuyer,
						'msg': msg
					});
				},
				getMyDoneTrade()
				{
					return this.exchangeData ({'w': 'getMyDoneTrade'})
				},
				getOwnOrders()
				{
					return this.exchangeData ({'w': 'getOwnOrders'})
				},
				getReviewsByAbout(who)
				{
					return this.exchangeData ({'w': 'getReviewsByAbout', 'who': who})
				},
				getReviewsByReviewer(who)
				{
					return this.exchangeData ({'w': 'getReviewsByReviewer', 'who': who})
				},
				getMyReviews()
				{
					return this.exchangeData ({'w': 'getMyReviews'})
				},
				addReview(id, text)
				{
					return this.exchangeData ({'w': 'addReview', 'id': id, 'text': text})
				},
				getOrders(active=true, offset = 0, lim = 25)
				{
					return this.exchangeData ({'w': 'getOrders', 'a': active, 'offset': offset, 'lim': lim})
				},
				getOrderByName(who, offset = 0, lim = 25)
				{
					// offset, lim
					return this.exchangeData ({'w': 'getOrderByName', 'who': who, 'offset': offset, 'lim': lim})
				},
				getTraderStats(who)
				{
					return this.exchangeData ({'w': 'getTraderStats', 'who': who})
				},
				remOrder(id)
				{
				 // 
				  return this.exchangeData ({'w': 'removeMyOrderByID', 'id': id})
				},
				changeActiveOrder(id, s=true)
				{
					console.log(`changeActiveOrder ${id} to ${s}`)
					return this.exchangeData ({'w': 'changeActiveOrder', 'id': id, 's': s})
				},
				doTrade(id, count = 1)
				{
					return this.exchangeData ({'w': 'doTrade', 'count': count, 'id': id})
				},
			}// end orders
} // end of class