package com.mediafire.sdk.token;

/**
 * Token is an abstract class used to represent a token String
 */
public abstract class Token {
    private final String mTokenString;

    /**
     * Token Constructor
     * @param tokenString String for the token
     */
    protected Token(String tokenString) {
        mTokenString = tokenString;
    }

    /**
     * Gets the token string
     * @return String token string
     */
    public final String getTokenString() {
        return mTokenString;
    }

    @Override
    public abstract String toString();
}
