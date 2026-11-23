package com.kharcha.common.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class PaiseFormatter {
    private PaiseFormatter() {}

    public static String format(long paise) {
        long rupees = paise / 100L;
        long remainder = Math.abs(paise % 100L);
        NumberFormat format = NumberFormat.getIntegerInstance(new Locale("en", "IN"));
        String amount = "₹" + format.format(rupees);
        return remainder == 0 ? amount : amount + "." + (remainder < 10 ? "0" : "") + remainder;
    }
}
