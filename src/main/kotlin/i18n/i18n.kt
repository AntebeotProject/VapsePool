package org.antibiotic.pool.main.i18n

import java.util.*

class i18n(fName: String = "localization", locale: Locale = Locale("en", "US")) {
    val m_resource = ResourceBundle.getBundle("$fName", locale);
    fun getString(s: String) = m_resource.getString(s)
    fun getString(s: String, vararg args: Any?): String {
        println("Format string")
        try {
            val r = String.format(m_resource.getString(s), args)
            println("return $r")
            return r
        } catch(e: Exception)
        {
            return e.toString()
        }
    }
    fun test()
    {
        System.out.println(String.format(getString("unconfirmedTXNotify"), "хэш", "монета"));
        System.out.println(getString("balanceChangedNotify"));
        System.out.println(getString("youBuyCoin2CoinNotify"));
        System.out.println(getString("youSellCoin2CoinNotify"));
        System.out.println(getString("orderSuccessfullyNotify"));
        System.out.println(getString("inputLocalSuccessfully"));
        System.out.println(getString("outputLocalSuccessFully"));
    }
}