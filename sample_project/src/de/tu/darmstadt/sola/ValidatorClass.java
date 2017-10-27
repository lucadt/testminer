package de.tu.darmstadt.sola;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorClass {

	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
			Pattern.CASE_INSENSITIVE);

	public static boolean validate(String emailStr) {
		Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
		return matcher.find();
	}

	public static void isValidEmail(String email) {

		// Code to check if email ends with '.' (period sign)
		boolean checkEndDot = false;
		checkEndDot = email.endsWith(".");

		// Code to find out last index of '@' sign
		int indexOfAt = email.indexOf('@');
		int lastIndexOfAt = email.lastIndexOf('.');

		// Code to check occurence of @ in the email address
		int countOfAt = 0;

		for (int i = 0; i < email.length(); i++) {
			if (email.charAt(i) == '@')
				countOfAt++;
		}

		// Code to check occurence of [period sign i..e, "."] after @
		String buffering = email.substring(email.indexOf('@') + 1, email.length());
		int len = buffering.length();

		int countOfDotAfterAt = 0;
		for (int i = 0; i < len; i++) {
			if (buffering.charAt(i) == '.')
				countOfDotAfterAt++;
		}

		// Code to print userName & domainName
		String userName = email.substring(0, email.indexOf('@'));
		String domainName = email.substring(email.indexOf('@') + 1, email.length());

		System.out.println("\n");

		if ((countOfAt == 1) && (userName.endsWith(".") == false) && (countOfDotAfterAt == 1)
				&& ((indexOfAt + 3) <= (lastIndexOfAt) && !checkEndDot)) {

			System.out.println("\"Valid email address\"");
		}

		else {
			System.out.println("\n\"Invalid email address\"");
		}

		System.out.println("\n");
		System.out.println("User name: " + userName + "\n" + "Domain name: " + domainName);

	}
	
	public static boolean validIP (String ip) {
	    try {
	        if ( ip == null || ip.isEmpty() ) {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }

	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if ( ip.endsWith(".") ) {
	            return false;
	        }

	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	
	
}
