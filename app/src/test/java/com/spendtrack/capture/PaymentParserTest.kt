package com.spendtrack.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentParserTest {

    @Test fun bankDebitSms() {
        val p = PaymentParser.parse(
            "Rs.1,250.50 debited from A/c XX1234 on 13-10-26 to VPA zomato@icici UPI Ref 123456."
        )
        assertNotNull(p)
        assertEquals(1250.50, p!!.amount, 0.001)
    }

    @Test fun upiAppNotification() {
        val p = PaymentParser.parse("Payment successful. Paid ₹200 to Chai Point")
        assertNotNull(p)
        assertEquals(200.0, p!!.amount, 0.001)
        assertEquals("Chai Point", p.merchant)
    }

    @Test fun creditIsIgnored() {
        assertNull(PaymentParser.parse("Rs.500 credited to A/c XX1234 on 13-10-26"))
    }

    @Test fun otpIsIgnored() {
        assertNull(PaymentParser.parse("123456 is your OTP for payment of Rs.500"))
    }
}
