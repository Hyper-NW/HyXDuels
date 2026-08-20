package me.alphatct3209.duels.divisions;

public class DivisionConfigurationException extends IllegalArgumentException
{
    public DivisionConfigurationException(String message)
    {
        super(message);
    }

    public DivisionConfigurationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
