package com.scofu.network.instance;

/**
 * Represents the availability of an instance.
 *
 * @param available whether it is available or not
 * @param status    the status
 * @param full      whether it is full or not
 */
public record Availability(boolean available, String status, boolean full) {

}
