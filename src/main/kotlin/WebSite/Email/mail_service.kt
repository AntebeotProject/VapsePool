package org.antibiotic.pool.main.WebSite.Email
import jakarta.mail.*;
// https://nvd.nist.gov/vuln/detail/cve-2020-15250 is fixed in 1.8
// https://github.com/advisories/GHSA-269g-pwp5-87pp is bull** but not vuln.
// https://docs.cloudmailin.com/outbound/examples/send_email_with_java/
class mail_service {
    companion object {
        fun isValid(email: String): Boolean
        {
            try {
                val internetAddress = jakarta.mail.internet.InternetAddress(email, true)
                internetAddress.validate();
                return true
            } catch(_: Exception) {}
            return false
        }
    }
}