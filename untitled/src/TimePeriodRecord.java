public interface TimePeriodRecord {
    /**   * Returns the Unix time (milliseconds since Epoch) from which this   * record is considered effective.   *   * @return effective start time, inclusive   */
    long getStart();
    /**   * Returns the Unix time (milliseconds since Epoch) at which this   * record is no longer considered effective.   *   * @return effective end time, exclusive   */
    long getEnd();}